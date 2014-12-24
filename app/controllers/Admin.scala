package controllers

import play.api._
import play.api.mvc._
import javax.inject.Inject

import play.api.data.{Form => PlayForm}
import play.api.data.Forms._

import _root_.db.Tables._
import _root_.db.Helpers
import _root_.db.TypeMappers._

import play.api.libs.json.Json.toJson

import models._

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import com.unboundid.ldap.sdk.{Filter => LDAPFilter, _}
import org.apache.poi.ss.usermodel.{WorkbookFactory, Row}
import java.io.File


class Admin @Inject() (forms: Forms) extends Controller {

  def isSciper(str: String): Boolean = {
    str.matches("""G?\d+""")
  }

  def extractFirst(str: String): String = {
    Option(str).getOrElse("").split(";").toSeq.head
  }

  def lookup(fac: String, q: String) = AdminAuth.async { implicit r =>
    val limit = 15;

    val uQ = (for {
      u <- FormsUsers if (u.firstname like q+"%") ||
                         (u.lastname like q+"%") ||
                         (u.sciper like q+"%")
    } yield (u)).sortBy(_.firstname).take(limit)

    for (us <- db.run(uQ.result)) yield {
      val results = if (us.size >= limit) {
        us
      } else {
        us// ++ ldapLookupLoose(q)
      }

      Ok(toJson(results.distinct.map { u =>
        toJson(Map(
          "sciper"    -> toJson(u.sciper),
          "firstname" -> toJson(u.firstname),
          "lastname"  -> toJson(u.lastname),
          "email"     -> toJson(u.email)
        ))
      }))
    }
  }

  private def ldapLookupLoose(q: String) = {
    val filter = LDAPFilter.createANDFilter(
      LDAPFilter.createORFilter(
        LDAPFilter.createEqualityFilter("o", "epfl"),
        LDAPFilter.createEqualityFilter("o", "epfl-guests")
      ),
      LDAPFilter.createORFilter(
        LDAPFilter.createEqualityFilter("givenName", q),
        LDAPFilter.createEqualityFilter("sn", q),
        LDAPFilter.createEqualityFilter("uniqueIdentifier", q)
      )
    )

    ldapLookup(filter)
  }

  private def ldapLookupBySciper(scipers: String*) = {
    (for (ss <- scipers.toList.grouped(20)) yield {
      val filter = LDAPFilter.createANDFilter(
        LDAPFilter.createORFilter(
          LDAPFilter.createEqualityFilter("o", "epfl"),
          LDAPFilter.createEqualityFilter("o", "epfl-guests")
        ),
        LDAPFilter.createORFilter(
          ss.map(s => LDAPFilter.createEqualityFilter("uniqueIdentifier", s)) : _*
        )
      )

      ldapLookup(filter)
    }).flatten.toList
  }

  private def ldapLookup(filter: LDAPFilter) = {
    import collection.JavaConversions._

    val c = new LDAPConnection("ldap.epfl.ch", 389);

    val request = new SearchRequest("c=ch", SearchScope.SUB, filter, "mail", "uniqueIdentifier", "givenName", "sn")

    request.setSizeLimit(500);

    try {
      val sr = c.search(request);

      val us = for (e <- sr.getSearchEntries()) yield {
        FormsUsersRow(
          e.getAttributeValue("uniqueIdentifier"),
          Option(e.getAttributeValue("givenName")).getOrElse(""),
          Option(e.getAttributeValue("sn")).getOrElse(""),
          Option(extractFirst(e.getAttributeValue("mail"))).getOrElse("")
        )
      }

      us.distinct
    } catch {
      case e: LDAPSearchException =>
        Logger.error(e.getMessage())
        Nil
    }
  }

  val sciperForm = PlayForm(
    "sciper" -> text
  )

  private def getOrAddUser(sciper: String): Future[Option[FormsUsersRow]] = {
    val uQ = FormsUsers.filter(_.sciper === sciper)

    for (ou <- db.run(uQ.result.headOption)) yield {
      if (ou.isDefined) {
        ou
      } else {
        ldapLookupBySciper(sciper).headOption match {
          case Some(u) =>
            Await.result(db.run(FormsUsers += u), Duration.Inf)
            Some(u)
          case None =>
            None
        }
      }
    }
  }

  private def getOrAddUsers(data: Set[ExcelRecord]): Future[Set[FormsUsersRow]] = {
    val scipers = data.map(_.sciper)

    val uQ = FormsUsers.filter(_.sciper inSetBind scipers)

    for (ous <- db.run(uQ.result)) yield {
      val existing = ous.map(u => u.sciper -> u).toMap

      val missing = data.filter(u => !existing.contains(u.sciper)).map { d =>
        FormsUsersRow(d.sciper, d.firstname, d.lastname, d.email)
      }

      Await.result(db.run(FormsUsers ++= missing), Duration.Inf)

      val updated = data.map(_.toUserRow).filter(u2 => existing.get(u2.sciper) match {
        case Some(u1) => u1 != u2
        case None => false
      })

      val qs = for (uu <- updated) yield {
        val q = for { u <- FormsUsers if u.sciper === uu.sciper } yield (u.firstname, u.lastname, u.email)
        db.run(q.update((uu.firstname, uu.lastname, uu.email)))
      }

      Await.result(Future.sequence(qs), Duration.Inf)

      data.map(_.toUserRow)
    }
  }

  private def getOrAddUsers(data: Set[FormsUsersRow]): Future[Set[FormsUsersRow]] = {
    val scipers = data.map(_.sciper)

    val uQ = FormsUsers.filter(_.sciper inSetBind scipers)

    for (ous <- db.run(uQ.result)) yield {
      val existing = ous.map(u => u.sciper -> u).toMap

      val missing = data.filter(u => !existing.contains(u.sciper))

      Await.result(db.run(FormsUsers ++= missing), Duration.Inf)

      val updated = data.filter(u2 => existing.get(u2.sciper) match {
        case Some(u1) => u1 != u2
        case None => false
      })

      val qs = for (uu <- updated) yield {
        val q = for { u <- FormsUsers if u.sciper === uu.sciper } yield (u.firstname, u.lastname, u.email)
        db.run(q.update((uu.firstname, uu.lastname, uu.email)))
      }

      Await.result(Future.sequence(qs), Duration.Inf)

      data //(ous ++ missing).toSet
    }
  }
  /************************************************************************************************
   * USERS
   ***********************************************************************************************/

  val userForm = PlayForm(tuple(
    "firstname" -> nonEmptyText,
    "lastname"  -> nonEmptyText,
    "email"     -> nonEmptyText,
    "mentor"    -> text
  ))

  def usersEdit(fac: String, sciper: String) = AdminAuth.async { implicit r =>

    val umQ = for {
      (u, m) <- Helpers.user(sciper) joinLeft FormsUsersMetadata on (_.sciper === _.sciper)
    } yield (u, m)

    for {
      oum <- db.run(umQ.result.headOption)
    } yield {
      oum match {
        case Some((u, oum)) =>
          Ok(views.html.main("Admin Users") {
            views.html.admin.main("Users") {
              views.html.admin.userProfile(u, oum)
            }
          })

        case None =>
          Redirect(routes.Admin.formsList(fac)).flashing (
            "error" -> s"User '$sciper' not found."
          )
      }
    }
  }

  private[controllers] def picturePath(uuid: String) = {
    s"data/pictures/$uuid"
  }

  def usersPicture(fac: String, sciper: String, uuid: String) = AdminAuth { implicit r =>
    val f = new java.io.File(picturePath(uuid))
    if (f.exists()) {
      Ok.sendFile(f)
    } else {
      NotFound("File not found")
    }
  }

  def usersSave(fac: String, sciper: String) = AdminAuth.async { implicit r =>

    val umQ = for {
      (u, m) <- Helpers.user(sciper) joinLeft FormsUsersMetadata on (_.sciper === _.sciper)
    } yield (u, m)

    for {
      oum <- db.run(umQ.result.headOption)
    } yield {
      oum match {
        case Some((u, oum)) =>
          userForm.bindFromRequest.fold(
            formError => {
              Redirect(routes.Admin.usersEdit(fac, sciper)).flashing (
                "error" -> s"Failed to save."
              )
            },
            { case (firstname, lastname, email, mentor) => {

              val picture = r.body.asMultipartFormData.flatMap(_.file("picture")).flatMap { picture =>
                import java.io.File

                val filename = picture.filename
                val uuid = java.util.UUID.randomUUID.toString;

                val name = picture.contentType match {
                  case Some("image/png") =>
                    Some(s"$uuid.png")

                  case Some("image/jpeg") =>
                    Some(s"$uuid.jpg")

                  case _ =>
                    None
                }

                name.map { n =>
                  picture.ref.moveTo(new File(picturePath(n)))
                }

                name
              }

              val uQ = for {
                u <- Helpers.user(sciper)
              } yield (u.firstname, u.lastname, u.email)

              val umQ = for {
                um <- FormsUsersMetadata if um.sciper === sciper
              } yield (um.picture, um.mentor)

              val uuQ = uQ.update((firstname, lastname, email))

              val omentor = if (mentor.isEmpty) None else Some(mentor)

              val uumQ = oum match {
                case Some(oum) =>
                  if (picture.nonEmpty) {
                    oum.picture match {
                      case Some(name) =>
                        new File(picturePath(name)).delete()
                      case _ =>
                    }

                    umQ.update((picture, omentor))
                  } else {
                    umQ.update((oum.picture, omentor))
                  }
                case None =>
                  FormsUsersMetadata += FormsUsersMetadataRow(sciper, picture, omentor)
              }


              val f = for {
                r1 <- db.run(uuQ)
                r2 <- db.run(uumQ)
              } yield {
                Redirect(routes.Admin.usersEdit(fac, sciper)).flashing (
                  "success" -> s"User '$sciper' saved!"
                )
              }

              Await.result(f, Duration.Inf)
            }}
          )

        case None =>
          Redirect(routes.Admin.formsList(fac)).flashing (
            "error" -> s"User '$sciper' not found."
          )
      }
    }
  }

  /************************************************************************************************
   * FORMS
   ***********************************************************************************************/

  def formsList(fac: String) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.forms
      d <- FormsDefinitions if d.id === f.idDefinition
    } yield (f,d)).sortBy(_._1.dateFrom)

    for (forms <- db.run(fQ.result)) yield {
      Ok(views.html.main(r.ctx.faculty.name+" Forms") {
        views.html.admin.main("Form Sessions") {
          views.html.admin.forms(forms)
        }
      })
    }
  }

  val sessionForm = PlayForm(tuple(
    "id_definition" -> number,
    "type" -> text,
    "year" -> number,
    "date_from" -> jodaDate("dd.MM.yyyy"),
    "date_to" -> jodaDate("dd.MM.yyyy")
  ))

  def formsEdit(fac: String, fid: Int = 0) = AdminAuth.async { implicit r =>
    val dQ = Helpers.definitions


    val fQ = for {
      f <- Helpers.form(fid)
    } yield f

    for {
      f <- db.run(fQ.result.headOption)
      ds <- db.run(dQ.result)
    } yield {

      Ok(views.html.main(r.ctx.faculty.name+" Forms") {
        views.html.admin.main("Form Sessions") {
          views.html.admin.formsEdit(f, ds)
        }
      })
    }
  }

  def formsEditDo(fac: String, fid: Int = 0) = AdminAuth.async { implicit r =>
    val saveOrCreate = if(fid > 0) "save" else "create"
    sessionForm.bindFromRequest.fold(
      error => {
        Future(Redirect(routes.Admin.formsEdit(fac, fid)).flashing (
          "error" -> s"Failed to $saveOrCreate form session: $error"
        ))
      },
      { case (idDefinition, tpe, year, dateFrom, dateTo) => {
        val q = if (fid > 0) {
          val qF = for { f <- Helpers.form(fid) } yield (f.idDefinition, f.year, f.midterm, f.dateFrom, f.dateTo)
          qF.update((idDefinition, year, tpe == "midterm", dateFrom, dateTo))
        } else {
          FormsForms += FormsFormsRow(0, r.ctx.faculty.id, idDefinition, year, tpe == "midterm", dateFrom, dateTo)
        }

        for (r <- db.run(q)) yield {
          Redirect(routes.Admin.formsList(fac)).flashing (
            "success" -> s"Successfully ${saveOrCreate}d form session"
          )
        }
      }}
    )
  }

  def formsDelete(fac: String, fid: Int) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.form(fid)
    } yield (f))

    for (d <- db.run(fQ.delete)) yield {
      Redirect(routes.Admin.formsList(fac)).flashing (
        "success" -> "Form session deleted!"
      )
    }
  }

  /************************************************************************************************
   * Reminders
   ***********************************************************************************************/

  def reminders(fac: String) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.forms
    } yield (f)).sortBy(_.dateFrom.desc)

    for (of <- db.run(fQ.result.headOption)) yield {
      of match {
        case Some(f) =>
          Redirect(routes.Admin.remindersList(fac, f.id))

        case None =>
          Redirect(routes.Admin.formsList(fac)).flashing(
            "error" -> "No forms defined"
          )
      }
    }
  }

  def remindersList(fac: String, idForm: Int) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.forms
    } yield (f)).sortBy(_.dateFrom)

    val fuQ = for {
      f  <- Helpers.forms if f.id === idForm
      fu <- FormsFormsToUsers if fu.idForm === f.id
    } yield (fu)

    val sectQ = for {
      f <- FormsForms if f.id === idForm
      d <- FormsDefinitions if d.id === f.idDefinition
      s <- FormsSections if s.idDefinition === d.id
    } yield (s)

    val fhsF = Helpers.formHeaders(db, fuQ)

    for {
      fs  <- db.run(fQ.result)
      fhs <- fhsF
      ss  <- db.run(sectQ.result)
    } yield {

      var js = Set[FormsUsersRow]()
      var jd = Set[FormsUsersRow]()
      var ps = Set[FormsUsersRow]()
      var pd = Set[FormsUsersRow]()

      for (fh <- fhs) {
        if (!fh.hasSigned(JointSection)(fh.student)) {
          js += fh.student
        }
        if (!fh.hasSigned(StudentSection)(fh.student)) {
          ps += fh.student
        }

        for (d <- fh.directors) {
          if (!fh.hasSigned(JointSection)(d)) {
            jd += d
          }

          if (!fh.hasSigned(DirectorSection)(d)) {
            pd += d
          }
        }
      }

      Ok(views.html.main(r.ctx.faculty.name+" Forms") {
        views.html.admin.main("Reminders") {
          views.html.admin.reminders(idForm, fs, ss.map(_.section), js, jd, ps, pd)
        }
      })
    }
  }

  /************************************************************************************************
   * STUDENT FORMS
   ***********************************************************************************************/

  def studentForms(fac: String) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.forms
    } yield (f)).sortBy(_.dateFrom.desc)

    for (of <- db.run(fQ.result.headOption)) yield {
      of match {
        case Some(f) =>
          Redirect(routes.Admin.studentFormsList(fac, f.id))

        case None =>
          Redirect(routes.Admin.formsList(fac)).flashing(
            "error" -> "No forms defined"
          )
      }
    }
  }

  def studentFormsDelete(fac: String, idForm: Int, sciper: String) = AdminAuth.async { implicit r =>
    val fuQ = (for {
      fu <- FormsFormsToUsers if fu.idForm === idForm && fu.sciperUser === sciper && (fu.idForm in Helpers.forms.map(_.id))
    } yield (fu))

    for (d <- db.run(fuQ.delete)) yield {
      Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
        "success" -> "Form deleted!"
      )
    }
  }

  def studentFormsInvalidate(fac: String, idForm: Int, sciper: String, section: String) = AdminAuth.async { implicit r =>
    val fuQ = (for {
      fu <- FormsFormsToUsers if fu.idForm === idForm && fu.sciperUser === sciper && (fu.idForm in Helpers.forms.map(_.id))
    } yield (fu))

    val fhsF = Helpers.formHeaders(db, fuQ)

    val skind = SectionKind.fromName(section)


    for (fhs <- fhsF) yield {
      val signs = fhs.toSet.flatMap( (fh: FormHeader) => fh.signatures.filter(s => s.section == skind))

      if (signs.nonEmpty) {
        Await.result(db.run(FormsSignatures.filter(_.id inSetBind signs.map(_.id)).delete), Duration.Inf)
        Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
          "success" -> "Form invalidated!"
        )
      } else {
        Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
          "success" -> "Nothing to invalidate!"
        )
      }
    }
  }

  def studentFormsList(fac: String, idForm: Int) = AdminAuth.async { implicit r =>
    val fQ = (for {
      f <- Helpers.forms
    } yield (f)).sortBy(_.dateFrom)

    val filters = selectFilters(Helpers.formFilters(db, idForm))

    val fuQ = for {
      f  <- Helpers.forms if f.id === idForm
      fu <- FormsFormsToUsers if fu.idForm === f.id
    } yield (fu)

    val fuQFiltered = applyFormsFilters(fuQ, filters)

    val fhsF = Helpers.formHeaders(db, fuQFiltered)

    for {
      fs  <- db.run(fQ.result)
      fhs <- fhsF
    } yield {
      Ok(views.html.main(r.ctx.faculty.name+" Forms") {
        views.html.admin.main("Student Forms") {
          views.html.admin.studentForms(idForm, fs, fhs, filters)
        }
      })
    }
  }

  def studentFormsAdd(fac: String, idForm: Int) = AdminAuth.async { implicit r =>
    sciperForm.bindFromRequest.fold(
      error =>
        Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
          "error" -> "Failed to add student form"
        )),
      sciper => {
        val eQ = (for {
          fu <- FormsFormsToUsers if fu.sciperUser === sciper && fu.idForm === idForm
        } yield (fu))

        val ff = for {
          ou  <- getOrAddUser(sciper)
          ofu <- db.run(eQ.result.headOption)
          of <- db.run(Helpers.form(idForm).result.headOption)
         } yield {

          if (of.isEmpty) {
            Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
              "error" -> "Unknown form"
            ))
          } else if (ou.isEmpty) {
            Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
              "error" -> "Could not find user corresponding to the given SCIPER!"
            ))
          } else if (ofu.isDefined) {
            Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
              "success" -> "This student already has a form!"
            ))
          } else {
            val nf = FormsFormsToUsersRow(0, idForm, sciper, None, None, None, None, "", "", "", false, false, false)

            db.run(FormsFormsToUsers += nf).map { e => 
              Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
                "success" -> "Student Form Added!"
              )
            }
          }
        }

        for (f <- ff; r <- f) yield r
      }
    )
  }

  case class ExcelRecordFormat(
    sciper: Int,
    firstname: Int,
    lastname: Int,
    email: Int,
    dateEnrolment: Option[Int] = None,
    mentor: Option[Int] = None
  ) {
    def extract(row: Row): Option[ExcelRecord] = {
      try {
        Some(ExcelRecord(
          row.getCell(sciper).getStringCellValue(),
          row.getCell(firstname).getStringCellValue(),
          row.getCell(lastname).getStringCellValue(),
          extractFirst(row.getCell(email).getStringCellValue()),
          dateEnrolment.map(i => new DateTime(row.getCell(i).getDateCellValue())),
          mentor.flatMap(i => Some(row.getCell(i).getStringCellValue()).filter(_.nonEmpty))
        )).filter(_.isValid)
      } catch {
        case e: RuntimeException =>
          None
      }
    }
  }

  case class ExcelRecord(
    sciper: String,
    firstname: String,
    lastname: String,
    email: String,
    dateEnrolment: Option[DateTime],
    mentor: Option[String]
  ) {

    def isValid = isSciper(sciper)

    def toUserRow = {
      FormsUsersRow(sciper, firstname, lastname, email)
    }
  }

  def studentFormsImport(fac: String, idForm: Int) = AdminAuth.async { implicit r =>
    import scala.collection.JavaConverters._

    r.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(f) =>
        val data = extractExcelData(f.ref.file, ExcelRecordFormat(0, 2, 1, 3, Some(6), Some(15)))

        if (data.isEmpty) {
          Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
            "error" -> "No data was extracted from the .xlsx file, format is likely invalid!"
          ))
        } else {
          val fuQ = FormsFormsToUsers.filter(_.idForm === idForm)

          for {
            fus <- db.run(fuQ.result)
            ums <- db.run(FormsUsersMetadata.result)
            of  <- db.run(Helpers.form(idForm).result.headOption)
          } yield {
            if (of.isEmpty) {
              Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
                "error" -> "Unknown form"
              )
            } else {
              val existing = fus.map(fu => fu.sciperUser -> fu).toMap

              val toAdd = data.filter(d => !existing.contains(d.sciper)).toSet
              val toDelete = existing.keySet -- data.map(_.sciper)

              // we make sure all scipers are users
              Await.result(getOrAddUsers(toAdd), Duration.Inf)

              val toAddQ = FormsFormsToUsers ++= toAdd.map { u =>
                FormsFormsToUsersRow(0, idForm, u.sciper, None, None, None, u.dateEnrolment, "", "", "", false, false, false)
              }

              val toDeleteQ = FormsFormsToUsers.filter(fu => (fu.idForm === idForm) && (fu.sciperUser inSetBind toDelete)).delete

              val updated = data.filter(u2 => existing.get(u2.sciper) match {
                case Some(u1) => u1.dateEnrolment != u2.dateEnrolment
                case None => false
              })

              val qs = for (fuu <- updated) yield {
                val q = for { fu <- FormsFormsToUsers if fu.sciperUser === fuu.sciper } yield (fu.dateEnrolment)
                db.run(q.update(fuu.dateEnrolment))
              }

              Await.result(db.run(toAddQ andThen toDeleteQ), Duration.Inf)


              // Update all metadata
              val existingM = ums.map(u => u.sciper -> u).toMap

              val missing = data.filter(u => !existingM.contains(u.sciper)).map { d =>
                FormsUsersMetadataRow(d.sciper, None, d.mentor)
              }

              Await.result(db.run(FormsUsersMetadata ++= missing), Duration.Inf)

              val updatedM = data.filter(u2 => existingM.get(u2.sciper) match {
                case Some(u1) => u1.mentor != u2.mentor
                case None => false
              })

              val qsM = for (uum <- updatedM) yield {
                val q = for { um <- FormsUsersMetadata if um.sciper === uum.sciper } yield (um.mentor)
                db.run(q.update(uum.mentor))
              }

              Await.result(Future.sequence(qsM), Duration.Inf)


              Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
                "success" -> (toAdd.size+" form(s) added, "+toDelete.size+" form(s) removed!")
              )
            }
          }
        }
      case None =>
        Future(Redirect(routes.Admin.studentFormsList(fac, idForm)).flashing (
          "error" -> "Failed to upload file!"
        ))
    }
  }

  /************************************************************************************************
   * DIRECTORS
   ***********************************************************************************************/
  def directorsList(fac: String) = AdminAuth.async { implicit r =>
    val duQ = (for {
      d  <- Helpers.directors
      u  <- FormsUsers if u.sciper === d.sciper
    } yield (d, u)).sortBy{ case (d,u) => (u.lastname, u.firstname) }

    for (dus  <- db.run(duQ.result)) yield {
      Ok(views.html.main("Directors") {
        views.html.admin.main("Directors List") {
          views.html.admin.directors(dus)
        }
      })
    }
  }

  def directorsAdd(fac: String) = AdminAuth.async { implicit r =>
    sciperForm.bindFromRequest.fold(
      error =>
        Future(Redirect(routes.Admin.directorsList(fac)).flashing (
          "error" -> "Failed to add director"
        )),
      sciper => {

        val ff = for {
          ou <- getOrAddUser(sciper)
          od <- db.run(Helpers.director(sciper).result.headOption)
         } yield {

          if (ou.isEmpty) {
            Future(Redirect(routes.Admin.directorsList(fac)).flashing (
              "error" -> "Could not find user corresponding to the given SCIPER!"
            ))
          } else if (od.isDefined) {
            Future(Redirect(routes.Admin.directorsList(fac)).flashing (
              "success" -> "This director is already in the list!"
            ))
          } else {
            val nd = FormsDirectorsRow(r.ctx.faculty.id, sciper)

            db.run(FormsDirectors += nd).map { e =>
              Redirect(routes.Admin.directorsList(fac)).flashing (
                "success" -> "Director Added!"
              )
            }
          }
        }

        for (f <- ff; r <- f) yield r
      }
    )
  }

  def directorsDelete(fac: String, sciper: String) = AdminAuth.async { implicit r =>

    //val fudQ = for {
    //  fu <- FormsFormsToUsers if fu.sciperDirector === sciper &&
    //                             (fu.idForm in Helpers.forms.map(_.id))
    //} yield (fu.sciperDirector)

    //val fucdQ = for {
    //  fu <- FormsFormsToUsers if fu.sciperCodirector === sciper &&
    //                             (fu.idForm in Helpers.forms.map(_.id))
    //} yield (fu.sciperCodirector)


    for {
      //r1 <- db.run(fudQ.update(None))
      //r2 <- db.run(fucdQ.update(None))
      r <- db.run(Helpers.director(sciper).delete)
    } yield {

      Redirect(routes.Admin.directorsList(fac)).flashing (
        "success" -> "Director Removed!"
      )
    }
  }

  private def extractExcelData(f: File, rowFormat: ExcelRecordFormat, limit: Option[Int] = None): List[ExcelRecord] = {
    import scala.collection.JavaConverters._

    val wb = WorkbookFactory.create(f);

    val sheet = wb.getSheetAt(0)

    val rows = if (limit.isEmpty) {
      sheet.asScala.tail
    } else {
      sheet.asScala.tail.take(limit.get)
    }

    rows.flatMap(r => rowFormat.extract(r)).toList
  }


  /*
  private def extractExcelData(f: File): Set[FormsUsersRow] = {
    import scala.collection.JavaConverters._

    val wb = WorkbookFactory.create(f);

    val sheet = wb.getSheetAt(0)

    (for (row <- sheet.asScala.tail) yield {
      try {
        val sciper = row.getCell(0).getStringCellValue()
        if (isSciper(sciper)) {
          Some(
            FormsUsersRow(
              sciper,
              row.getCell(2).getStringCellValue(), // firstname
              row.getCell(1).getStringCellValue(), // lastname
              extractFirst(row.getCell(3).getStringCellValue()) // email
            )
          )
        } else {
          None
        }
      } catch {
        case e: RuntimeException =>
          None
      }
    }).flatten.toSet
  }
  */

  def directorsImport(fac: String) = AdminAuth.async { implicit r =>
    val ctx = r.ctx

    r.body.asMultipartFormData.flatMap(_.file("file")) match {
      case Some(f) =>
        val data = extractExcelData(f.ref.file, ExcelRecordFormat(0, 2, 1, 3)).toSet

        if (data.isEmpty) {
          Future(Redirect(routes.Admin.directorsList(fac)).flashing (
            "error" -> "No data was extracted from the .xlsx file, format is likely invalid!"
          ))
        } else {
          val dQ = Helpers.directors.map(_.sciper)

          for (ds <- db.run(dQ.result)) yield {
            val existing = ds.toSet

            val toAdd    = data.filter(u => !existing(u.sciper))
            val toDelete = existing -- data.map(_.sciper)

            // we make sure all scipers are users
            Await.result(getOrAddUsers(data), Duration.Inf)

            val toAddQ = FormsDirectors ++= toAdd.map { u =>
              FormsDirectorsRow(ctx.faculty.id, u.sciper)
            }

            val toDeleteQ = Helpers.directors.filter(_.sciper inSetBind toDelete).delete

            Await.result(db.run(toAddQ andThen toDeleteQ), Duration.Inf)

            Redirect(routes.Admin.directorsList(fac)).flashing (
              "success" -> (toAdd.size+" director(s) added, "+toDelete.size+" director(s) removed")
            )
          }
        }
      case None =>
        Future(Redirect(routes.Admin.directorsList(fac)).flashing (
          "error" -> "Failed to upload file!"
        ))
    }
  }

  def importFormat(fac: String) = AdminAuth { implicit r =>
    Ok(views.html.admin.importFormat())
  }

  /************************************************************************************************
   * ADMINS
   ***********************************************************************************************/
  def adminsList(fac: String) = AdminAuth.async { implicit r =>
    val auQ = (for {
      a  <- Helpers.admins
      u  <- FormsUsers if u.sciper === a.sciper
    } yield (a, u))

    for (aus  <- db.run(auQ.result)) yield {
      Ok(views.html.main("Admins") {
        views.html.admin.main("Admins List") {
          views.html.admin.admins(aus)
        }
      })
    }
  }

  def adminsAdd(fac: String) = AdminAuth.async { implicit r =>
    sciperForm.bindFromRequest.fold(
      error =>
        Future(Redirect(routes.Admin.adminsList(fac)).flashing (
          "error" -> "Failed to add admin"
        )),
      sciper => {
        val ff = for {
          ou <- getOrAddUser(sciper)
          oa <- db.run(Helpers.admin(sciper).result.headOption)
         } yield {

          if (ou.isEmpty) {
            Future(Redirect(routes.Admin.adminsList(fac)).flashing (
              "error" -> "Could not find user corresponding to the given SCIPER!"
            ))
          } else if (oa.isDefined) {
            Future(Redirect(routes.Admin.adminsList(fac)).flashing (
              "success" -> "This admin is already in the list!"
            ))
          } else {
            val na = FormsAdminsRow(r.ctx.faculty.id, sciper)

            db.run(FormsAdmins += na).map { e =>
              Redirect(routes.Admin.adminsList(fac)).flashing (
                "success" -> "Admin added!"
              )
            }
          }
        }

        for (f <- ff; r <- f) yield r
      }
    )
  }

  def adminsDelete(fac: String, sciper: String) = AdminAuth.async { implicit r =>
    for {
      r <- db.run(Helpers.admin(sciper).delete)
    } yield {
      if (r > 0) {
        Redirect(routes.Admin.adminsList(fac)).flashing (
          "success" -> "Admin removed!"
        )
      } else {
        Redirect(routes.Admin.adminsList(fac)).flashing (
          "error" -> "Admin not found!"
        )
      }
    }
  }

  /************************************************************************************************
   * REPORTS
   ***********************************************************************************************/

  def reports(fac: String) = AdminAuth.async { implicit r =>

    val q = (for {
      rp <- Helpers.reports
      f <- Helpers.forms if f.idDefinition === rp.idDefinition
    } yield (rp, f)).sortBy(_._2.dateFrom.desc)

    for {
      ds <- db.run(q.result.headOption)
    } yield {

      if (ds.isEmpty) {
        Redirect(routes.Admin.formsList(fac)).flashing(
          "error" -> "No reports/forms available!"
        )
      } else {
        val (r, f) = ds.head

        Redirect(routes.Admin.reportsDisplay(fac, f.id, r.id))
      }
    }
  }

  def reportsForm(fac: String, idForm: Int) = AdminAuth.async { implicit r =>

    val q = for {
      f <- Helpers.forms if f.id === idForm
      rp <- Helpers.reports if f.idDefinition === rp.idDefinition
    } yield (rp, f)

    for {
      ds <- db.run(q.result.headOption)
    } yield {

      if (ds.isEmpty) {
        Redirect(routes.Admin.formsList(fac)).flashing(
          "error" -> "No reports available for this form!"
        )
      } else {
        val (r, f) = ds.head

        Redirect(routes.Admin.reportsDisplay(fac, f.id, r.id))
      }
    }
  }

  private[controllers] def reportData(fid: Int, rid: Int, fuQ: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq])(implicit ctx: Context) = {
    val fieldsQ = (for {
      fs <- FormsReportsFields if fs.idReport === rid
    } yield (fs)).sortBy(_.order)


    val qQ = for {
      f  <- Helpers.forms if f.id === fid
      fd <- FormsReportsFields if fd.idReport === rid && fd.field === "question"
      q  <- FormsQuestions if q.id === fd.idQuestion
    } yield (q)

    val sectionsQ = for {
      q  <- qQ
      p  <- FormsParts if p.id === q.idPart
      s  <- FormsSections if s.id === p.idSection
    } yield (q.id, s.section)

    val answersQ = for {
      f  <- Helpers.forms if f.id === fid
      fu <- fuQ if fu.idForm === f.id
      fd <- FormsReportsFields if fd.idReport === rid && fd.field === "question"
      a  <- FormsAnswers if a.idFormUser === fu.id
    } yield (a)

    val metaQ = for {
      fu <- fuQ
      m  <- FormsUsersMetadata if m.sciper === fu.sciperUser
    } yield (m.sciper, m)

    for {
      fields <- db.run(fieldsQ.result)
      as     <- db.run(answersQ.result)
      qs     <- db.run(sectionsQ.result)
      ms     <- db.run(metaQ.result)
      tps    <- Helpers.questionsTypes(db, qQ)
    } yield {
      val asMap = as.groupBy(a => (a.idFormUser, a.idQuestion)).flatMap { case ((idfu, idq), as) =>
        tps.get(idq) match {
          case Some(tp) =>
            forms.getAnswer(Some(as.head), tp).map { a =>
              (idfu, idq) -> a
            }
          case None =>
            None
        }
      }
      val qsMap = qs.toMap

      (fields, asMap, qsMap, ms.toMap, tps)
    }
  }

  def reportsDisplay(fac: String, idForm: Int, idReport: Int) = AdminAuth.async { implicit r =>
    val filters = selectFilters(Helpers.formFilters(db, idForm))

    val fuQ = for {
      f  <- Helpers.forms if f.id === idForm
      fu <- FormsFormsToUsers if f.id === fu.idForm
    } yield (fu)

    val fuQFiltered = applyFormsFilters(fuQ, filters)

    val rsQ = for {
      f  <- Helpers.forms if f.id === idForm
      r  <- Helpers.reports if r.idDefinition === f.idDefinition
    } yield (r)

    val fsQ = (for {
      f  <- Helpers.forms
      r  <- Helpers.reports if r.idDefinition === f.idDefinition
    } yield (f)).sortBy(_.dateFrom)


    for {
      rs     <- db.run(rsQ.result)
      fs     <- db.run(fsQ.result).map(_.distinct)
      fhs    <- Helpers.formHeaders(db, fuQFiltered)
      (fields, asMap, qsMap, metaMap, tps) <- reportData(idForm, idReport, fuQFiltered)
    } yield {
      (rs.find(_.id == idReport), fs.find(_.id == idForm)) match {
        case (Some(cr), Some(cf)) =>
          Ok(views.html.main("Reporting", Seq("wide")) {
            views.html.admin.main("Reports") {
              views.html.admin.reports(rs, fs, cr, cf) {
                views.html.admin.report(cr, fields, fhs, asMap, metaMap, tps, filters, qsMap)
              }
            }
          })
        case _ =>
          Redirect(routes.Admin.reports(fac))
      }
    }
  }

}
