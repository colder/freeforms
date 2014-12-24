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
import org.apache.poi.xslf.usermodel._
import org.apache.poi.sl.usermodel.PictureData.PictureType

import java.io.ByteArrayOutputStream
import java.awt.{List => _, _}

class PPT @Inject() (forms: Forms, admin: Admin) extends Controller {
  def all(fac: String, id: Int) = AdminAuth.async { implicit r =>
    val filters = selectFilters(Helpers.formFilters(db, id))

    val fuQ = for {
      f  <- Helpers.forms if f.id === id
      fu <- FormsFormsToUsers if fu.idForm === f.id
    } yield (fu)

    val fuQFiltered = applyFormsFilters(fuQ, filters)

    generatePPT(fac, id, fuQFiltered)
  }

  def overallGrade(f: Form): Option[String] = {
    f.header.definition.id match {
      case 2 => forms.answerStringOpt(f, 44)
      case 3 => forms.answerStringOpt(f, 56)
      case 4 => forms.answerStringOpt(f, 68)
      case _ => None
    }
  }


  private[controllers] def generateSlide(f: Form, om: Option[FormsUsersMetadataRow], pfs: Seq[Form], ppt: XMLSlideShow)(implicit ctx: Context) = {

    val overall = overallGrade(f)

    if (overall.nonEmpty) {

      val s = ppt.createSlide();

      val s1 = s.createTextBox()
      s1.setAnchor(new Rectangle(50, 30, 600, 70))

      val r1 = s1.addNewTextParagraph().addNewTextRun()
      r1.setText(f.header.student.firstname+" "+f.header.student.lastname);
      r1.setBold(true)
      r1.setFontSize(20)

      val r2 = s1.addNewTextParagraph().addNewTextRun()
      r2.setText(overall.get)
      r2.setFontSize(20)

      val s2 = s.createTextBox()
      s2.setAnchor(new Rectangle(50, 100, 600, 400))

      for (m <- om) {
        for (picture <- m.picture) {
          import java.nio.file.{Files, Paths}
          val data = Files.readAllBytes(Paths.get(admin.picturePath(picture)))
          //val source = scala.io.Source.fromFile(Admin.picturePath(picture))
          //val byteArray = source.map(_.toByte).toArray
          //source.close()


          val pic0 = ppt.addPicture(data, PictureType.PNG);
          val pic = s.createPicture(pic0)
          val dim = pic0.getImageDimension

          val width = Math.min(80, dim.width)
          val height = (dim.height*width)/dim.width

          pic.setAnchor(new Rectangle(570, 30, width, height));
        }
      }

      var values = new ArrayBuffer[(String, String)]()

      values += "Advisor" -> f.header.directors.map(d => d.firstname+" "+d.lastname).mkString(" / ")

      values += "Mentor" -> om.flatMap(_.mentor).getOrElse("N/A")

      values += "Enrolment Date" -> f.header.formUser.dateEnrolment.map(DateTimeFormat.forPattern("dd.MM.YYYY").print).getOrElse("N/A")

      if (f.header.definition.id == 4) {
        val courses = (forms.answerStringOpt(f, 62).toList ++ forms.answerStringOpt(f, 75).toList)
        val coursesString = if (courses.isEmpty) {
          "N/A"
        } else {
          courses.mkString(" / ")
        }

        values += ("Course"  -> coursesString)
      }

      for (pf <- pfs) {
        values += (pf.header.title -> overallGrade(pf).getOrElse("N/A"))
      }

      for ( (title, content) <- values) {
        val p = s2.addNewTextParagraph();
        p.setLineSpacing(120);
        p.setIndent(-10);
        p.setLeftMargin(20);
        p.setBullet(true);

        val rt = p.addNewTextRun()
        rt.setText(title+": ")
        rt.setBold(true)
        val rv = p.addNewTextRun()
        rv.setText(content)
      }
    }
  }

  private def generatePPT(fac: String, id: Int, fuQ: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq])(implicit ctx: Context) = {
    val muQ = for {
      u <- fuQ
      mu <- FormsUsersMetadata if mu.sciper === u.sciperUser
    } yield {mu}

    val pfQ = (for {
      fid <- fuQ.map(_.idForm).groupBy(x=>x).map(_._1)
      f1  <- FormsForms if f1.id === fid
      f2  <- FormsForms if f2.dateFrom < f1.dateFrom
      fu  <- FormsFormsToUsers if fu.idForm === f2.id
    } yield (f2.dateFrom, fu)).sortBy(_._1).map(_._2)

    for {
      mus <- db.run(muQ.result)
      fs  <- forms.getForms(fuQ)
      pfs <- forms.getForms(pfQ)
      ds  <- db.run(Helpers.directorUsers.result)
    } yield {
      val metaMap = mus.map { mu =>
        mu.sciper -> mu
      }.toMap

      val pastForms = pfs.groupBy(_.student.sciper)

      val ppt = new XMLSlideShow();

      for(f <- fs) {
        generateSlide(f, metaMap.get(f.student.sciper), pastForms.getOrElse(f.student.sciper, Seq()), ppt);
      }

      val baos = new ByteArrayOutputStream()
      ppt.write(baos);

      val bs = ByteString(baos.toByteArray)

      val contentType = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
      Result(
        header = ResponseHeader(200, Map(
          CONTENT_DISPOSITION -> "Content-Disposition: attachment; filename=\"forms.pptx\""
        )),
        body   = new HttpEntity.Strict(bs, Some(contentType).asJava).asScala
      )
    }
  }

}
