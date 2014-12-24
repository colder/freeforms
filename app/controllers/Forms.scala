package controllers

import play.api._
import play.api.mvc._
import play.api.libs.iteratee._
import play.api.Play.current
import play.api.libs.json._

import _root_.db.Tables._
import _root_.db.Helpers
import _root_.db.TypeMappers._

import models._
import JsonAnswer._

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class Forms extends Controller {

  private[controllers] def getForm(id: Int, sciper: String)(implicit ctx: Context): Future[Option[Form]] = {
    val fuQ = for {
      f <- Helpers.form(id)
      fu <- FormsFormsToUsers if fu.idForm === id && fu.sciperUser === sciper
    } yield (fu)

    getForms(fuQ).map(_.headOption)
  }

  private[controllers] def getAnswer(oa: Option[FormsAnswersRow], tpe: QuestionType): Option[Answer] = (oa, tpe) match {
    case (Some(a), FreeText) =>
      a.freeChoice.map(FreeAnswer(_))

    case (_, _: CompactRangeTitle) =>
      None

    case (Some(a), CompactRange) =>
      a.idChoice.map(ChoiceAnswer(_, None))

    case (Some(a), _: Range) =>
      a.idChoice.map(ChoiceAnswer(_, None))

    case (Some(a), Select(cs)) =>
      a.idChoice.flatMap { idC =>
        cs.find(_.id == idC) match {
          case Some(c) =>
            if(c.isFreeWhenSelected) {
              Some(ChoiceAnswer(idC, a.freeChoice))
            } else {
              Some(ChoiceAnswer(idC, None))
            }
          case _ =>
            None
        }
      }

    case (Some(a), _: FreeGrades) =>
      a.freeChoice.map(str => Json.parse(str).validate[FreeGradesAnswer]) match {
        case Some(JsSuccess(fga, _)) =>
          Some(fga)
        case Some(err) =>
          Logger.error("Invalid Json format: "+err)
          None
        case _ =>
          None
      }

    case (Some(a), _: CoursesGrades) =>
      a.freeChoice.map(str => Json.parse(str).validate[CoursesGradesAnswer]) match {
        case Some(JsSuccess(cga, _)) =>
          Some(cga)
        case Some(err) =>
          Logger.error("Invalid Json format: "+err)
          None
        case _ =>
          None
      }

    case (None, _) =>
      None
    case (a, tpe) =>
      Logger.error("Invalid answer ? "+tpe+" with "+a)
      None
  }


  private[controllers] def getForms(fuQ: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq])(implicit ctx: Context): Future[Seq[Form]] = {

    val defQ = (for {
      id <- fuQ.map(_.idForm).groupBy(x=>x).map(_._1)
      f <- FormsForms if f.id === id
      d <- FormsDefinitions if d.id === f.idDefinition
    } yield (d)).groupBy(x=>x).map(_._1)

    val sectQ = (for {
      d <- defQ
      s <- FormsSections if s.idDefinition === d.id
    } yield (s)).sortBy(_.order)

    val partQ = (for {
      s <- sectQ
      p <- FormsParts if p.idSection === s.id
    } yield (p)).sortBy(_.order)

    val quesQ = (for {
      p <- partQ
      q <- FormsQuestions if q.idPart === p.id
    } yield (q)).sortBy(_.order)

    val ansQ = for {
      fu <- fuQ
      a  <- FormsAnswers if a.idFormUser === fu.id
    } yield (a)

    for {
      sections  <- db.run(sectQ.result)
      parts     <- db.run(partQ.result)
      questions <- db.run(quesQ.result)
      qtypes    <- Helpers.questionsTypes(db, quesQ)
      fhs       <- Helpers.formHeaders(db, fuQ)
      ans       <- db.run(ansQ.result)
    } yield {
      val partsMap     = parts.groupBy(_.idSection)
      val questionsMap = questions.groupBy(_.idPart)
      val answersMap   = ans.groupBy(a => (a.idFormUser, a.idQuestion)).mapValues(_.head)

      for (fh <- fhs) yield {
        Form(fh, sections.map { s =>
          Section(s.section, s.title, partsMap.getOrElse(s.id, Nil).map { p =>
            Part(p.title, questionsMap.getOrElse(p.id, Nil).flatMap { q =>
              qtypes.get(q.id).map { tpe =>

                val oa = getAnswer(answersMap.get((fh.formUser.id, q.id)), tpe)

                Question(q.id, q.title, q.tooltip, tpe, q.optional, oa)
              }
            })
          })
        })
      }
    }
  }


  def displayAuto(fac: String, id: Int, sciper: String) = FullAuth.async { implicit r =>
    for {
      of <- getForm(id, sciper)
    } yield {
      of match {
        case Some(f) =>
          val s = if (f.header.jointComplete) {
            if (f.header.directors contains r.ctx.user) {
              if (f.header.hasDirectorSection) DirectorSection else JointSection
            } else {
              if (f.header.hasStudentSection) StudentSection else JointSection
            }
          } else {
            JointSection
          }
          Redirect(routes.Forms.display(fac, id, sciper, s.name)).flashing(r.flash)
        case None =>
          Redirect(routes.Forms.list(fac)).flashing {
            "error" -> "Form not found!"
          }
      }
    }
  }

  def display(fac: String, id: Int, sciper: String, section: String) = FullAuth.async { implicit r =>
    val allfQ = for {
      f <- Helpers.forms
      fu <- FormsFormsToUsers if fu.idForm === f.id && fu.sciperUser === sciper
    } yield (fu)

    for {
      of <- getForm(id, sciper)
      fs <- Helpers.formHeaders(db, allfQ)
      ds <- db.run(Helpers.directorUsers.result)
    } yield {
      of match {
        case Some(f) =>
          val doValidate = (r.queryString contains "validate")

          val skind = SectionKind.fromName(section)

          if (!f.header.b.accessFor(skind).isReadable) {
            if (skind == JointSection) {
              Redirect(routes.Forms.list(fac)).flashing {
                "error" -> "Form not found!"
              }
            } else {
              Redirect(routes.Forms.display(fac, id, sciper, "joint")).flashing {
                "error" -> "Access denied!"
              }
            }
          } else {
            val s = f.sections.find(_.kind == skind).getOrElse(f.sections.head)

            val fPrefill = if (fs.size > 1) {
              val of = fs.filter(_ != f.header).last

              f.copy(header = f.header.prefillWith(of))

            } else {
              f
            }

            Ok(views.html.main(r.ctx.faculty.name+" Forms") {
              views.html.forms.display(fPrefill, fs, s, ds, doValidate)
            })
          }

        case None =>
          Redirect(routes.Forms.list(fac)).flashing {
            "error" -> "Form not found!"
          }
      }

    }
  }

  def print(fac: String, id: Int, sciper: String) = FullAuth.async { implicit r =>
    for {
      of <- getForm(id, sciper)
      ds <- db.run(Helpers.directorUsers.result)
    } yield {
      of match {
        case Some(f) =>
          Ok(views.html.main(r.ctx.faculty.name+" Forms") {
            views.html.forms.print(f, ds)
          })

        case None =>
          Redirect(routes.Forms.list(fac)).flashing {
            "error" -> "Form not found!"
          }
      }
    }
  }

  private def saveForm(f: Form, doSave: Boolean, doSign: Boolean, skind: SectionKind)(implicit ctx: Context): Form = {
    val ffuq = FormsFormsToUsers
    val aq   = FormsAnswers

    // Step 1: Save header
    val a1 = if (skind == JointSection && doSave && f.header.b.accessFor(skind).isWriteable) {
      Some(db.run(FormsFormsToUsers.filter(_.id === f.id).update(f.header.formUser)))
    } else {
      None
    }

    // Step 2: Save answers
    val as = for {
      s <- f.sections if s.kind == skind && doSave
      p <- s.parts
      q <- p.questions
    } yield {
      if (f.header.b.accessFor(s.kind).isWriteable) {
        val (oId, oFree) = q.ans match {
          case Some(FreeAnswer(v)) =>
            (None, Some(v))
          case Some(ChoiceAnswer(v, fv)) =>
            (Some(v), fv)
          case Some(cga: CoursesGradesAnswer) =>
            (None, Some(Json.prettyPrint(Json.toJson(cga))))
          case Some(fga: FreeGradesAnswer) =>
            (None, Some(Json.prettyPrint(Json.toJson(fga))))
          case _ =>
            (None, None)
        }

        Some(db.run(aq.insertOrUpdate(FormsAnswersRow(
          f.id, q.id, oId, oFree, Some(new DateTime())
        ))))
      } else {
        None
      }
    }


    // Step 3: Remove existing signatures for this section
    val signaturesToRemove: Set[FormsSignaturesRow] = if (doSave && f.header.b.unsignOnSave) {
      f.header.signatures.filter(s => s.section == skind).toSet
    } else if (doSign) {
      f.header.signatures.filter(s => s.section == skind && s.sciper == ctx.user.sciper).toSet
    } else {
      Set()
    }

    val signsDelete = if (signaturesToRemove.nonEmpty) {
      Some(db.run(FormsSignatures.filter(_.id inSetBind signaturesToRemove.map(_.id)).delete))
    } else {
      None
    }

    // Process Step 1-3
    Await.result(Future.sequence(a1 ++ as.flatten ++ signsDelete), Duration.Inf)

    // Step 4: Sign, if requested
    val signaturesToAdd = if (doSign) {
      // Add our own signature now
      val sign = FormsSignaturesRow(0, f.id, skind, ctx.user.sciper, new DateTime())

      Await.result(db.run(FormsSignatures += sign), Duration.Inf)

      Some(sign)
    } else {
      None
    }

    val f2 = f.copy(header = f.header.copy(signatures = f.header.signatures.filterNot(signaturesToRemove(_)) ++ signaturesToAdd))

    val notifier = new EmailNotifier(ctx)

    val h1 = f.header
    val h2 = f2.header

    val events: Set[Event] = h1.b.eventsFor(h2, signaturesToAdd)

    if(events.nonEmpty) {
      // Send notifications
      for {
        data <- db.run(Helpers.notifications(f.header.formUser.idForm, events).result)
        (not, target) <- data
      } {
        val tos = target match {
          case Student    => Some(f.header.student)
          case Director   => f.header.director
          case CoDirector => f.header.codirector
          case Faculty    =>
            (ctx.faculty.replytoEmail, ctx.faculty.replytoName) match {
              case (Some(email), Some(name)) =>
                Some(FormsUsersRow("", name, "", email))

              case _ =>
                Some(FormsUsersRow("", ctx.faculty.fromName, "", ctx.faculty.fromEmail))
            }
        }

        for (to <- tos) {
          notifier.notify(TemplateNotification(f.header, to, not.subject, not.content))
        }
      }

      Logger.info(s"Event for ${f.header.formUser.id}: ${events.mkString(", ")}")
    }

    f2
  }

  def save(fac: String, id: Int, sciper: String, section: String) = FullAuth.async { implicit r =>

    val selfURL = routes.Forms.display(fac, id, sciper, section)
    val autoURL = routes.Forms.displayAuto(fac, id, sciper)

    for {
      of <- getForm(id, sciper)
      ds <- db.run(Helpers.directorUsers.result)
    } yield {
      of match {
        case Some(fOrigin) =>
          val simpleData = r.body.asFormUrlEncoded.getOrElse(Map()).mapValues(_.headOption.getOrElse("").trim)

          val fWithAnswers = updateFromRequest(fOrigin, ds, simpleData)

          val sk = SectionKind.fromName(section)

          val isAnswered = fWithAnswers.header.isAnswered && fWithAnswers.questions.forall {
            case (s, q) if fWithAnswers.header.b.accessFor(s.kind).isWriteable => q.isAnswered
            case _ => true
          }

          val doSign = (simpleData contains "sign")
          val doSave = (fWithAnswers != fOrigin)

          val fSaved = saveForm(fWithAnswers, doSave, doSign && isAnswered, sk)

          if (doSign && isAnswered) {
            Redirect(autoURL).flashing(
              "success" -> "Form validated!"
            )
          } else if (doSign && !isAnswered) {
            Redirect(selfURL+"?validate").flashing(
              "error" -> "Form is not complete! (See below)"
            )
          } else if (doSave) {
            Redirect(selfURL).flashing(
              "success" -> "Form saved!"
            )
          } else {
            Redirect(selfURL).flashing(
              "success" -> "Form did not change!"
            )
          }
        case None =>
          Redirect(routes.Forms.list(fac)).flashing(
            "error" -> "Form not found!"
          )

      }
    }
  }

  private def validateDirector(ds: Seq[FormsUsersRow], str: String): Option[FormsUsersRow] = {
    ds.find(_.sciper.toString == str)
  }

  private def validateDate(str: String): Option[DateTime] = {
    val p = DateTimeFormat.forPattern("dd.MM.YYY")
    try {
      Some(p.parseDateTime(str))
    } catch {
      case e: RuntimeException =>
        None
    }
  }

  private def updateFromRequest(f: Form,
                                ds: Seq[FormsUsersRow],
                                d: Map[String, String])(implicit ctx: Context): Form = {

    val newHeader = if (f.header.b.accessFor(JointSection).isWriteable) {
      val od  = validateDirector(ds, d.getOrElse("director", ""))
      var ocd = validateDirector(ds, d.getOrElse("codirector", ""))

      if (ocd.isDefined && ocd == od) {
        ocd = None
      }


      f.header.copy(
        director      = od,
        codirector    = ocd,
        formUser      = f.header.formUser.copy(
          sciperDirector   = od.map(_.sciper),
          sciperCodirector = ocd.map(_.sciper),
          dateEnrolment    = validateDate(d.getOrElse("dateEnrolment", "")),
          title            = d.getOrElse("title", ""),
          keywords         = d.getOrElse("keywords", ""),
          overview         = d.getOrElse("overview", "")
        )
      )
    } else {
      f.header
    }

    f.mapQuestions((s,q) =>
      if (f.header.b.accessFor(s.kind).isWriteable) {
        q.copy(ans = q.tpe match {
          case FreeText =>
            val v = d.getOrElse("q"+q.id, "")
            if (v.nonEmpty) {
              Some(FreeAnswer(v))
            } else {
              None
            }

          case CompactRange =>
            try {
              val v = d("q"+q.id).toInt

              if (v <= 5 && v >= 1) {
                Some(ChoiceAnswer(v, None))
              } else {
                None
              }
            } catch {
              case _: RuntimeException =>
                None
            }

          case Range(_, _, hasNA) =>
            try {
              val v = d("q"+q.id).toInt

              if (v <= 5 && v >= (if (hasNA) 0 else 1)) {
                Some(ChoiceAnswer(v, None))
              } else {
                None
              }
            } catch {
              case _: RuntimeException =>
                None
            }

          case Select(cs) =>
            val csMap = cs.map(c => c.id -> c).toMap

            try {
              val c = csMap(d("q"+q.id).toInt)

              if (c.isFreeWhenSelected) {
                val v = d.getOrElse("qex"+q.id, "")
                Some(ChoiceAnswer(c.id, Some(v)))
              } else {
                Some(ChoiceAnswer(c.id, None))
              }
            } catch {
              case _: RuntimeException =>
                None
            }

          case CoursesGrades(cs, n, gs) =>
            val csMap = cs.map(c => c.id -> c).toMap

            val ans = (for (i <- 1 to n) yield {
              try {
                val c = csMap(d(s"q${q.id}-course-$i").toInt)
                val g = d(s"q${q.id}-grade-$i").toDouble

                if (gs.validate(g)) {
                  Some(CourseGradeAnswer(c.id, g))
                } else {
                  None
                }
              } catch {
                case re: RuntimeException =>
                  None
              }
            }).flatten

            if (ans.nonEmpty) {
              Some(CoursesGradesAnswer(ans))
            } else {
              None
            }

          case FreeGrades(n, gs) =>
            val ans = (for (i <- 1 to n) yield {
              try {
                val c = d(s"q${q.id}-course-$i")
                val g = d(s"q${q.id}-grade-$i").toDouble

                if (gs.validate(g)) {
                  Some(FreeGradeAnswer(c, g))
                } else {
                  None
                }
              } catch {
                case re: RuntimeException =>
                  None
              }
            }).flatten

            if (ans.nonEmpty) {
              Some(FreeGradesAnswer(ans))
            } else {
              None
            }

          case _ =>
            None
        })
      } else {
        q
      }
    ).copy(header = newHeader)
  }

  def list(fac: String) = FullAuth.async { implicit r =>
    val ctx = r.ctx

    val cfQ = Helpers.forms.sortBy(_.dateFrom.desc).take(1)
    val ofQ = Helpers.forms.sortBy(_.dateFrom.desc).drop(1)

    val csQ = for {
      f <- cfQ
      fu <- FormsFormsToUsers if fu.idForm === f.id && fu.sciperUser === ctx.user.sciper
    } yield (fu)

    val osQ = for {
      f <- ofQ
      fu <- FormsFormsToUsers if fu.idForm === f.id && fu.sciperUser === ctx.user.sciper
    } yield (fu)

    val cdQ = for {
      f <- cfQ
      fu <- FormsFormsToUsers if fu.idForm === f.id && (fu.sciperDirector === ctx.user.sciper || fu.sciperCodirector === ctx.user.sciper)
    } yield (fu)

    val odQ = for {
      f <- ofQ
      fu <- FormsFormsToUsers if fu.idForm === f.id && (fu.sciperDirector === ctx.user.sciper || fu.sciperCodirector === ctx.user.sciper)
    } yield (fu)

    val (cq, oq) = ctx.role match {
      case StudentRole  =>
        (csQ, osQ)

      case DirectorRole | AdminRole =>
        (cdQ, odQ)
    }

    for {
      cf <- db.run(cfQ.result.headOption)
      cfhs <- Helpers.formHeaders(db, cq)
      ofhs <- Helpers.formHeaders(db, oq)
    } yield {
      Ok(views.html.main(ctx.faculty.name+" Forms") {
        views.html.multipart(
          views.html.forms.currentList(cf, cfhs),
          views.html.forms.list(ofhs),
          views.html.forms.admin()
        )
      })
    }
  }

  private[controllers] def answerStringOpt(f: Form, qid: Int): Option[String] = {
    f.answer(qid) flatMap {
      case (FreeText, FreeAnswer(f)) =>
        Some(f)

      case (Select(cs), ChoiceAnswer(cid, freeValue)) =>
        cs.find(_.id == cid).map { c =>
          if (c.isFreeWhenSelected) {
            c.name +" "+freeValue
          } else {
            c.name
          }
        }

      case (CoursesGrades(cs, card, _), CoursesGradesAnswer(cgs)) =>
        val anss = cgs.flatMap{cg =>
          cs.find(_.id == cg.value).map { c =>
            c.name +": "+cg.grade
          }
        }

        if (anss.size > 0) {
          Some(anss.reduceRight(_ + " / " + _))
        } else {
          None
        }

      case (FreeGrades(card, _), FreeGradesAnswer(fgs)) =>
        val anss = fgs.map{ fg =>
          fg.value+": "+fg.grade
        }

        if (anss.size > 0) {
          Some(anss.reduceRight(_ + " / " + _))
        } else {
          None
        }

      case _ => None
    }
  }

  private[controllers] def answerString(f: Form, qid: Int): String = {
    answerStringOpt(f, qid).getOrElse("N/A")
  }

}
