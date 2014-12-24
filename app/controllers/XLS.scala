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
import scala.collection.mutable.ArrayBuffer

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import org.apache.poi.xssf.usermodel._

import java.io.ByteArrayOutputStream
import java.awt.{List => _, _}

class XLS @Inject() (forms: Forms, admin: Admin) extends Controller {
  def all(fac: String, fid: Int, rid: Int) = AdminAuth.async { implicit r =>
    val filters = selectFilters(Helpers.formFilters(db, fid))

    val fuQ = for {
      f  <- Helpers.forms if f.id === fid
      fu <- FormsFormsToUsers if fu.idForm === f.id
    } yield (fu)

    val fuQFiltered = applyFormsFilters(fuQ, filters)

    generateXLS(fac, fid, rid, fuQFiltered)
  }

  private def generateXLS(fac: String, id: Int, rid: Int, fuQ: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq])(implicit ctx: Context) = {

    for {
      r      <- db.run(Helpers.reports.filter(r => r.id === rid).result.head)
      fhs    <- Helpers.formHeaders(db, fuQ)
      (fields, answers, sections, meta, types) <- admin.reportData(id, rid, fuQ)
    } yield {

      val xls = new XSSFWorkbook();
      val sheet = xls.createSheet(r.name);

      val width = 3 + fields.size

      val row  = sheet.createRow(0)
      row.createCell(0).setCellValue("SCIPER")
      row.createCell(1).setCellValue("Lastname")
      row.createCell(2).setCellValue("Firstname")

      for((fd, i) <- fields.zipWithIndex) {
        row.createCell(3+i).setCellValue(fd.name)
      }

      for((fh, i) <- fhs.zipWithIndex) {
        val row  = sheet.createRow(i+1)

        row.createCell(0).setCellValue(fh.student.sciper)
        row.createCell(1).setCellValue(fh.student.lastname)
        row.createCell(2).setCellValue(fh.student.firstname)

        for((fd, i) <- fields.zipWithIndex) {
          val cell = row.createCell(3+i)

          fd.field match {
            case "progress" =>
              val pj = if(fh.jointComplete) {
                Some("validated")
              } else {
                if(fh.hasSigned(JointSection)(fh.student)) {
                  Some("filled")
                } else {
                  Some("empty")
                }
              }

              val ps = if(fh.hasStudentSection) {
                if(fh.studentComplete) {
                  Some("validated")
                } else {
                  Some("pending")
                }
              } else {
                None
              }

              val pd = if(fh.hasDirectorSection) {

                if(fh.directorComplete) {
                  Some("validated")
                } else {
                  if (fh.director.forall(d => fh.hasSigned(DirectorSection)(d))) {
                    Some("awaiting co-supervisor")
                  } else {
                    Some("awaiting supervisor")
                  }
                }
              } else {
                None
              }

              val res = (pj.toSeq ++ ps ++ pd).mkString(" / ")
              cell.setCellValue(res)

            case "director" => 
              cell.setCellValue(fh.director.map(d => d.lastname+" "+d.firstname).getOrElse(""))

            case "codirector" =>
              cell.setCellValue(fh.codirector.map(d => d.lastname+" "+d.firstname).getOrElse(""))

            case "date_enrolment" =>
              val ds = fh.formUser.dateEnrolment.map(DateTimeFormat.forPattern("dd.MM.YYYY").print).getOrElse("N/A")
              cell.setCellValue(ds)

            case "mentor" =>
              cell.setCellValue(meta.get(fh.formUser.sciperUser).flatMap(_.mentor).getOrElse(""))

            case "question" =>
              val qid    = fd.idQuestion.get
              val answer = answers.get((fh.formUser.id, qid))
              val tp     = types(qid)
              val s      = sections(qid)

              (answer, tp, s) match {
                case (None, _, _) =>
                  cell.setCellValue("N/A")

                case (_, _, s) if !fh.b.accessFor(s).isReadable =>
                  cell.setCellValue("hidden")
              
                case (Some(FreeAnswer(v)), t, _) =>
                  cell.setCellValue(v)

                case (Some(ChoiceAnswer(v, free)), Select(cs), _) =>
                  cell.setCellValue(cs.find(_.id == v).map(_.name).orElse(free).getOrElse("N/A"))

                case (Some(a), t, _) =>
                  cell.setCellValue(a.toString)

                case _ =>
                  cell.setCellValue("?")
              }
          }

        }
      }

      for(i <- 0 until width) {
        sheet.autoSizeColumn(i);
      }

      val baos = new ByteArrayOutputStream()
      xls.write(baos);

      val bs = ByteString(baos.toByteArray)

      val contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
      Result(
        header = ResponseHeader(200, Map(
          CONTENT_DISPOSITION -> "Content-Disposition: attachment; filename=\"forms.xlsx\""
        )),
        body   = new HttpEntity.Strict(bs, Some(contentType).asJava).asScala
      )
    }
  }

}
