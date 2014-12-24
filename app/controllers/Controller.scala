package controllers

import play.api._
import play.api.mvc.{Session => PlaySession, Controller => PlayController, _}
import play.api.db._
import play.api.Play.current
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits._

import _root_.slick.driver.MySQLDriver
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfig

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import models._

import _root_.db.Tables._
import _root_.db.Helpers
import _root_.db.TypeMappers._

class Controller extends PlayController {
  lazy val dbConfig = DatabaseConfigProvider.get[MySQLDriver](Play.current)

  import dbConfig.driver.api._

  lazy val db = dbConfig.db


  class ACLRequest[A](val ctx: Context,
                      val request: Request[A]) extends WrappedRequest[A](request) {
  }

  implicit def aclReqToContext[A](implicit acl: ACLRequest[A]): Context = acl.ctx

  protected def getUser(sciper: String): Future[Option[FormsUsersRow]] = {
    db.run(Helpers.user(sciper).result.headOption)
  }

  protected def getFaculty(name: String): Future[Option[FormsFacultiesRow]] = {
    db.run(FormsFaculties.filter(_.name === name).result.headOption)
  }

  case object Auth extends ActionBuilder[ACLRequest] {
    def invokeBlock[A](r: Request[A], block: (ACLRequest[A]) => Future[Result]) = {
      import ch.epfl.tequila.client.model._
      import ch.epfl.tequila.client.service._

      val cfg = new ClientConfig()

      cfg.setHost("tequila.epfl.ch")
      cfg.setRequest("name firstname name uniqueid email")
      cfg.setService("Annual Forms")
      cfg.setAllows("org=EPFL")
      cfg.setAllows("categorie=epfl-guests")

      val s   = TequilaService.instance()

      def requestLogin() = {
        val url = current.configuration.getString("application.url").getOrElse("")
        val key = s.createRequest(cfg, url+r.path)
        Future.successful(Redirect("https://tequila.epfl.ch/cgi-bin/tequila/auth?requestkey="+key).withNewSession)
      }

      val defaultUser = current.configuration.getString("application.defaultUser")

      val ou: Future[Option[FormsUsersRow]] = (r.session.get("sciper") orElse defaultUser) match {
        case Some(sciper) =>
          try {
            getUser(sciper)
          } catch {
            case re: RuntimeException =>
              Future(None)
          }

        case None =>
          r.queryString.get("key").map(_.head) match {
            case Some(key) =>
              try {
                val principal = s.validateKey(cfg, key)

                def normalizeEntry(s: String) = {
                  Option(s).map(_.split(",").head).getOrElse("")
                }

                val sciper    = principal.getAttribute("uniqueid")
                val firstname = normalizeEntry(principal.getAttribute("firstname"))
                val lastname  = normalizeEntry(principal.getAttribute("name"))
                val email     = normalizeEntry(principal.getAttribute("email"))

                Logger.info("Authenticated "+firstname+" "+lastname)

                getUser(sciper).flatMap {
                  case Some(u) => Future(Some(u))
                  case None =>
                    val u = FormsUsersRow(sciper, firstname, lastname, email)
                    Logger.info("Creating new user for "+firstname+" "+lastname)
                    db.run(FormsUsers += u).map(id => Some(u))
                }
              } catch {
                case e: SecurityException =>
                  Future(None)
              }
            case None =>
              Future(None)
          }
      }

      ou.flatMap {
        case Some(user) =>
          block(new ACLRequest(Context(db, user), r)).map(_.withSession("sciper" -> user.sciper))
        case None =>
          requestLogin()
      }
    }
  }

  // Authenticate, load faculty, admin/supervisor
  case object FullAuth extends ActionBuilder[ACLRequest] {
    def invokeBlock[A](r: Request[A], block: (ACLRequest[A]) => Future[Result]) = {
      Auth.invokeBlock(r, { (areq: ACLRequest[A]) =>
        val ctx = areq.ctx

        val ffac = r.path.split("/", 3).toList match {
          case _ :: fac :: _ =>
            getFaculty(fac)
          case _ =>
            Future(None)
        }

        ffac.flatMap {
          case Some(f) =>
            val aQ = (for {
                a <- FormsAdmins    if a.idFaculty === f.id && a.sciper === ctx.user.sciper
              } yield (a))

            val dQ = (for {
                d <- FormsDirectors if d.idFaculty === f.id && d.sciper === ctx.user.sciper
              } yield (d))

            for {
              a   <- db.run(aQ.result.headOption)
              d   <- db.run(dQ.result.headOption)
              res <- block(new ACLRequest(ctx.copy(ofaculty = Some(f), odirector = d, oadmin = a), r))
            } yield {
              res
            }

          case None =>
            Future(NotFound("Faculty not found"))
        }
      })
    }
  }

  case object AdminAuth extends ActionBuilder[ACLRequest] {
    def invokeBlock[A](r: Request[A], block: (ACLRequest[A]) => Future[Result]) = {
      FullAuth.invokeBlock(r, { (areq: ACLRequest[A]) =>
        val ctx = areq.ctx

        ctx.role match {
          case AdminRole =>
            block(areq)

          case _ =>
            val url =ctx.ofaculty match {
              case Some(f) =>
                routes.Forms.list(f.name)

              case None =>
                routes.Application.welcome()
            }

            Future(Redirect(url).flashing {
              "error" -> "Access Denied"
            })
        }
      })
    }
  }

  def applyFormsFilters(q: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq], filters: Seq[FormFilter]) = {
    var qFinal = q

    for (f <- filters) f match {
      case FormDirectorFilter(_, _, Some(v)) =>
        qFinal = qFinal.filter(_.sciperDirector === v.sciper)
      case FormCoDirectorFilter(_, _, Some(v)) =>
        qFinal = qFinal.filter(_.sciperCodirector === v.sciper)
      case FormQuestionFilter(_, idQ, _, Some(v)) =>
        qFinal = for {
          fu <- qFinal
          a <- FormsAnswers if a.idFormUser === fu.id && a.idChoice === v.id && a.idQuestion === idQ
        } yield (fu)
      case _ =>
    }

    qFinal
  }

  def selectFilters(filters: Seq[FormFilter])(implicit r: Request[AnyContent]): Seq[FormFilter] = {
    for (f <- filters) yield {
      r.getQueryString(s"filter${f.id}") match {
        case None =>
          f
        case Some(v) =>
          f.select(v)
      }
    }
  }
}
