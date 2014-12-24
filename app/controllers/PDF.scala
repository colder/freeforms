package controllers

import scala.compat.java8.OptionConverters._
import javax.inject.Inject

import play.api._
import play.http._
import play.api.mvc._
import play.api.libs.iteratee._
import akka.stream.scaladsl._
import akka.util._

import _root_.db.Tables._
import _root_.db.Helpers
import _root_.db.TypeMappers._

import models._

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import com.pdflib._

import models.PDFHelpers._

class PDF @Inject() (forms: Forms, configuration: Configuration) extends Controller {
  def fullname(u: FormsUsersRow): String = {
    u.firstname + " " + u.lastname
  }

  def date(d: DateTime): String = {
    DateTimeFormat.forPattern("dd.MM.YYYY").print(d)
  }

  private[controllers] def generatePDF(f: Form, ds: Seq[FormsUsersRow])(implicit ctx: Context): pdflib = {
    val p = new pdflib

    val oDefault = ("fontname" := "Helvetica") + ("encoding" := "unicode") + ("fontsize" := 11) + ("begoptlistchar" := "none")

    val oTitle  = oDefault + ("fontsize" := 20)
    val oTitle1 = oDefault + ("fontname" := "Helvetica-Bold") + ("fontsize" := 18) + ("fillcolor" := OColor("#336699"))
    val oTitle2 = oDefault + ("fontname" := "Helvetica-Bold") + ("fontsize" := 15) + ("fillcolor" := OColor("#6D98C2"))
    val oTitle3 = oDefault + ("fontname" := "Helvetica-Bold") + ("alignment" := "justify")

    val oHeader = oDefault + ("fontname" := "Helvetica-Oblique") + ("alignment" := "right")

    val oBold   = oDefault + ("fontname" := "Helvetica-Bold")
    val oNormal = oDefault + ("alignment" := "justify")


    val api = new PDFAPI(p) {
      override def onNewPage(n: Int) {
        if (n > 1) {
          text(f.header.title+" - "+f.student.sciper+" - "+fullname(f.student), oHeader).fity(ytop, ytop+20)
        }
      }

      override def onEnd() {
        // Display page numbers
        for (pi <- 1 to pages) {
          p.resume_page(s"pagenumber $pi");
          text(s"$pi / $pages", oHeader).fity(ybottom, ybottom+20)
          p.end_page_ext("")
        }
      }
    }

    import api._

    p.set_option("license="+configuration.getString("pdflib.license").getOrElse("0"));
    p.begin_document("", "")
    p.set_option("topdown=true");
    p.set_option("errorpolicy=return");
    p.set_info("Creator", "Annual Forms Application")
    p.set_info("Title",   f.header.title)

    newPage()

    // Header
    val logo  = p.open_pdi_document("images/logo.pdf", "");
    val logop = p.open_pdi_page(logo, 1, "");
    p.fit_pdi_page(logop, xright-150, ytop+60, "boxsize={150 50}")

    // Title
    text(f.header.title, oTitle + ("fillcolor" := OColor("#336699"))).fity(ytop-10)
    text(fullname(f.student)+" - "+f.student.sciper, oTitle).fity(ytop+20, xright=xright-160)

    // Preamble
    var row   = 1
    var tfcol = -1
    var tfval = -1


    val oCellLeft  = ("fittextflow" := (("verticalalign" := "top"))) + ("margin" := 4) + ("colwidth" := 110)
    val oCellRight = ("fittextflow" := (("verticalalign" := "top"))) + ("margin" := 4)

    y = ytop+90

    if (f.header.b.accessFor(JointSection).isReadable) {
      val tbl = table()

      tbl.addCell(text("Thesis director", oBold), oCellLeft)
      tbl.addCell(text(f.header.director.map(fullname).getOrElse("N/A"), oNormal), oCellRight)
      tbl.addRow()

      if (f.header.codirector.isDefined) {
        tbl.addCell(text("Thesis co-director", oBold), oCellLeft)
        tbl.addCell(text(f.header.codirector.map(fullname).getOrElse("N/A"), oNormal), oCellRight)
        tbl.addRow()
      }

      tbl.addCell(text("Date of enrollment", oBold), oCellLeft)
      tbl.addCell(text(f.header.formUser.dateEnrolment.map(date(_)).getOrElse(""), oNormal), oCellRight)
      tbl.addRow()

      tbl.addCell(text("Provisional title", oBold), oCellLeft)
      tbl.addCell(text(f.header.formUser.title, oNormal), oCellRight)
      tbl.addRow()

      if(f.header.faculty.name == "EDEE") {
        tbl.addCell(text("Keywords", oBold), oCellLeft)
        tbl.addCell(text(f.header.formUser.keywords, oNormal), oCellRight)
        tbl.addRow()

        tbl.addCell(text("Overview", oBold), oCellLeft)
        tbl.addCell(text(f.header.formUser.overview, oNormal), oCellRight)
        tbl.addRow()
      }

      tbl.fitInFlow()
    }


    for (s <- f.sections if f.header.b.accessFor(s.kind).isReadable) {
      pageBreak(650)
      text(s.title, oTitle1).fitInFlow()
      y += 10

      for (part <- s.parts) {
        pageBreak(700)
        text(part.title, oTitle2).fitInFlow()
        y += 10

        var lastQ: Option[Question] = None
        for (q <- part.questions) {

          // spaces between questions
          if (q.tpe == CompactRange || lastQ.isEmpty) {
            // no space
          } else {
            y += 10
          }

          pageBreak(700)
          q.tpe match {
            case CompactRange =>
              // no title on top for compact ranges

            case _ =>
              text(q.title, oTitle3).fitInFlow()
          }

          val oCell       = ("margin" := 4) + ("colwidth" := "7%")
          val oCellSel    = oCell + ("fittextflow" := "showborder")

          q.tpe match {
            case FreeText =>
              val answer = q.ans.collect{ case FreeAnswer(a) => a }.getOrElse("")
              text(answer, oNormal).fitInFlow()

            case CompactRangeTitle(left, right) =>
              val tbl = table()

              tbl.addCell(text(left, oNormal + ("alignment" := "right")), oCell + ("colwidth" := "45%"))
              for (i <- 1 to 5) {
                tbl.addCell(text(""+i, oNormal + ("alignment" := "center")), oCell)
              }
              tbl.addCell(text(right, oNormal + ("alignment" := "left")), oCell + ("colwidth" := "20%"))

              tbl.fitInFlow()

            case CompactRange =>
              val answer = q.ans.collect{ case ChoiceAnswer(id, _) => id }.getOrElse(-1)

              val tbl = table()

              tbl.addCell(text(q.title, oNormal + ("alignment" := "right")), oCell + ("colwidth" := "45%"))
              for (i <- 1 to 5) {
                tbl.addCell(text(""+i, oNormal + ("alignment" := "center")), if (i == answer) oCellSel else oCell)
              }
              tbl.addCell(text("", oNormal), oCell + ("colwidth" := "20%"))

              tbl.fitInFlow()



            case Range(left, right, hasNA) =>
              val answer = q.ans.collect{ case ChoiceAnswer(id, _) => id }.getOrElse(-1)

              val tbl = table()

              tbl.addCell(text(left, oNormal + ("alignment" := "right")), oCell + ("colwidth" := "28%"))
              for (i <- ((1 to 5) :+ 0)) {
                val txt = if (i == 0) (if (hasNA) "N/A" else "") else ""+i
                tbl.addCell(text(txt, oNormal + ("alignment" := "center")), if (i == answer) oCellSel else oCell)
              }
              tbl.addCell(text(right, oNormal + ("alignment" := "left")), oCell + ("colwidth" := "20%"))

              tbl.fitInFlow()

            case Select(choices) =>
              val (idSelected, of) = q.ans.collect{ case ChoiceAnswer(id, of) => (id, of) }.getOrElse((-1, None))

              for(SelectChoice(id, c, explain) <- choices if idSelected == id) {
                val answer = (List(c) ++ of).mkString("\n")

                text(answer, oNormal).fitInFlow()
              }

            case CoursesGrades(choices, n, gs) =>
              val anss = q.ans.collect { case CoursesGradesAnswer(ans) => ans.map(a => (a.value, a.grade)) }.getOrElse(Seq())
              for (i <- 1 to n) {
                val (idSelected, g) = anss.lift.apply(i-1).getOrElse((-1, 0))

                for (SelectChoice(id, c, explain) <- choices if idSelected == id) {
                  val answer = s"$c ($g)"

                  text(answer, oNormal).fitInFlow()
                }
              }

            case FreeGrades(n, gs) =>
              val anss = q.ans.collect { case FreeGradesAnswer(ans) => ans.map(a => (a.value, a.grade)) }.getOrElse(Seq())

              for (i <- 1 to n) {
                anss.lift.apply(i-1) match {
                  case Some((c, g)) =>
                    val answer = s"$c ($g)"
                    text(answer, oNormal).fitInFlow()

                  case _ =>
                }
              }

            case _ =>
          }

          lastQ = Some(q)
        }
        y += 30
      }
      y += 10
    }

    end()
    p
  }

  def nameOf(f: Form) = {
    val form = f.header.form;

    val prefix = if (form.midterm) {
      form.year+"_midterm"
    } else {
      form.year
    }

    val n = f.student.sciper+"_"+prefix+"_"+f.student.lastname+"_"+f.student.firstname+".pdf"

    n.replaceAll("[^\\w.]", "_")
  }

  def all(fac: String, id: Int) = AdminAuth.async { implicit r =>
    val filters = selectFilters(Helpers.formFilters(db, id))

    val fuQ = for {
      f  <- Helpers.forms if f.id === id
      fu <- FormsFormsToUsers if fu.idForm === f.id
    } yield (fu)

    val fuQFiltered = applyFormsFilters(fuQ, filters)

    generateZip(fac, fuQFiltered)
  }

  def multiple(fac: String, fids: List[Int], scipers: List[String]) = FullAuth.async { implicit r =>
    val fuQ = for {
      f <- Helpers.forms if f.id inSetBind fids
      fu <- FormsFormsToUsers if fu.idForm === f.id && (fu.sciperUser inSetBind scipers.toSet)
    } yield (fu)

    generateZip(fac, fuQ)
  }

  private def generateZip(fac: String, fuQ: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq])(implicit ctx: Context) = {
    for {
      fs <- forms.getForms(fuQ)
      ds <- db.run(Helpers.directorUsers.result)
    } yield {
      import java.io.ByteArrayOutputStream
      import java.util.zip._

      var files = 0;
      val baos = new ByteArrayOutputStream()
      val zop = new ZipOutputStream(baos)
      for (f <- fs) {
        var p: pdflib = null

        try {
          p = generatePDF(f, ds)

          val ze = new ZipEntry(nameOf(f));
          zop.putNextEntry(ze)
          zop.write(p.get_buffer())
          zop.closeEntry();

          files += 1

        } catch {
          case e: java.lang.NoClassDefFoundError =>
            Logger.error("PDFLib Library Unavailable", e)
            Redirect(routes.Forms.list(fac)).flashing {
              "error" -> "PDFLib Library Unavailable!"
            }

          case e: java.lang.UnsatisfiedLinkError =>
            Logger.error("PDFLib Library Unavailable", e)
            Redirect(routes.Forms.list(fac)).flashing {
              "error" -> "PDFLib Library Unavailable!"
            }

          case e: PDFlibException =>
            Logger.error("Exception while generating pdf (form: "+f.id+", sciper: "+f.student.sciper+")", e)

        } finally {
          if (p ne null) {
            p.delete()
          }
        }
      }
      zop.close();
      if (files > 0) {
        //Result(
        //  header = ResponseHeader(200, Map(
        //    CONTENT_TYPE        -> "application/zip",
        //    CONTENT_DISPOSITION -> "Content-Disposition: attachment; filename=\"forms.zip\""
        //  )),
        //  body   = Enumerator(baos.toByteArray)
        //)

        val bs = ByteString(baos.toByteArray)

        Result(
          header = ResponseHeader(200, Map(
            CONTENT_DISPOSITION -> "Content-Disposition: attachment; filename=\"forms.zip\""
          )),
          body   = new HttpEntity.Strict(bs, Some("application/zip").asJava).asScala
        )
      } else {
        Redirect(routes.Forms.list(fac)).flashing {
          "error" -> "Resulting forms package is empty!"
        }
      }
    }
  }

  def single(fac: String, id: Int, sciper: String, name: String) = FullAuth.async { implicit r =>
    for {
      of <- forms.getForm(id, sciper)
      ds <- db.run(Helpers.directorUsers.result)
    } yield {
      of match {
        case Some(f) =>
          var p: pdflib = null

          try {
            p = generatePDF(f, ds)
            //Result(
            //  header = ResponseHeader(200, Map(
            //    CONTENT_TYPE        -> "application/pdf",
            //    CONTENT_DISPOSITION -> ("Content-Disposition: attachment; filename=\""+nameOf(f)+"\"")
            //  )),
            //  body   = Enumerator(p.get_buffer())
            //)

            val bs = ByteString(p.get_buffer())

            Result(
              header = ResponseHeader(200, Map(
                CONTENT_DISPOSITION -> ("Content-Disposition: attachment; filename=\""+nameOf(f)+"\"")
              )),
              body   = new HttpEntity.Strict(bs, Some("application/pdf").asJava).asScala
            )
          } catch {
            case e: java.lang.NoClassDefFoundError =>
              Logger.error("PDFLib Library Unavailable", e)
              Redirect(routes.Forms.list(fac)).flashing {
                "error" -> "PDFLib Library Unavailable!"
              }
            case e: java.lang.UnsatisfiedLinkError =>
              Logger.error("PDFLib Library Unavailable", e)
              Redirect(routes.Forms.list(fac)).flashing {
                "error" -> "PDFLib Library Unavailable!"
              }

            case e: PDFlibException =>
              Logger.error("Exception while generating pdf", e)
              Redirect(routes.Forms.list(fac)).flashing {
                "error" -> "Failed to generate PDF!"
              }

          } finally {
            if (p ne null) {
              p.delete()
            }
          }
        case None =>
          Redirect(routes.Forms.list(fac)).flashing {
            "error" -> "Form not found!"
          }
      }
    }

  }

}
