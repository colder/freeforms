package db

import models._
import Tables._
import TypeMappers._

import slick.driver.MySQLDriver.api._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

object Helpers {
  def forms(implicit ctx: Context) = {
    FormsForms.filter(_.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0))
  }

  def form(idForm: Int)(implicit ctx: Context) = {
    forms.filter(_.id === idForm)
  }

  def formFilters(db: Database, idForm: Int)(implicit ctx: Context) = {
    val ffq = (for {
      f  <- FormsForms if f.id === idForm
      d  <- FormsDefinitions if d.id === f.idDefinition && d.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0)
      ff <- FormsFilters  if ff.idDefinition === d.id
    } yield (ff)).sortBy(_.order)

    val qs = for {
      ff <- ffq if ff.`type` === "question" 
      q <- FormsQuestions if q.id === ff.idQuestion.getOrElse(0)
    } yield (q)


    val ff = for {
      ffs <- db.run(ffq.result)
      ds  <- db.run(directorUsers.result)
      qts <- questionsTypes(db, qs)
    } yield {
      ffs.flatMap{
        case ff @ FormsFiltersRow(_, _, Some(idq), "question", name, _) =>
          qts.get(idq) flatMap {
            case Select(cs) =>
              Some(FormQuestionFilter(ff, idq, cs, None))
            case _ =>
              None
          }
        case ff @ FormsFiltersRow(_, _, None, "director", name, _) =>
          Some(FormDirectorFilter(ff, ds, None))

        case ff @ FormsFiltersRow(_, _, None, "codirector", name, _) =>
          Some(FormCoDirectorFilter(ff, ds, None))

        case _ =>
          None
      }

    }

    Await.result(ff, Duration.Inf)
  }

  def directors(implicit ctx: Context) = {
    FormsDirectors.filter(_.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0))
  }

  def director(sciper: String)(implicit ctx: Context) = {
    directors.filter(_.sciper === sciper)
  }

  def directorUsers(implicit ctx: Context) = {
    (for {
      d <- directors
      u <- FormsUsers if u.sciper === d.sciper
    } yield (u)).sortBy(u => (u.lastname, u.firstname))
  }

  def admins(implicit ctx: Context) = {
    FormsAdmins.filter(_.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0))
  }

  def admin(sciper: String)(implicit ctx: Context) = {
    admins.filter(_.sciper === sciper)
  }

  def user(sciper: String) = {
    FormsUsers.filter(_.sciper === sciper)
  }

  def definitions(implicit ctx: Context) = {
    for {
      d <- FormsDefinitions if d.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0)
    } yield (d)
  }

  def reports(implicit ctx: Context) = {
    for {
      d <- FormsDefinitions if d.idFaculty === ctx.ofaculty.map(_.id).getOrElse(0)
      r <- FormsReports if r.idDefinition === d.id
    } yield (r)
  }

  def notifications(idForm: Int, events: Set[Event])(implicit ctx: Context) = {
    for {
      f <- form(idForm)
      n <- FormsNotifications if n.idDefinition === f.idDefinition
      nt <- FormsNotificationsToEvents if (nt.idNotification === n.id) && (nt.event inSetBind events)
    } yield ((n, nt.target))
  }

  def questionsTypes(db: Database, qQ: Query[FormsQuestions, FormsQuestionsRow, Seq]): Future[Map[Int, QuestionType]] = {
    val qrQ = (for {
      q <- qQ
      r <- FormsQuestionsRanges if q.id === r.idQuestion
    } yield (q.id, r))

    val qgQ = (for {
      q <- qQ
      g <- FormsQuestionsGrades if q.id === g.idQuestion
    } yield (q.id, g))

    val chQ = (for {
      q <- qQ
      c <- FormsQuestionsChoices if q.id === c.idQuestion
    } yield (q.id, c)).sortBy(_._2.order)

    for {
      qs <- db.run(qQ.result)
      rs <- db.run(qrQ.result)
      gs <- db.run(qgQ.result)
      cs <- db.run(chQ.result)
    } yield {
      val ranges  = rs.groupBy(_._1).mapValues(_.head._2)
      val grades  = gs.groupBy(_._1).mapValues(_.head._2)
      val choices = cs.groupBy(_._1).mapValues(_.map(_._2))

      (for (q <- qs) yield {
        val tpe = q.answerType match {
          case "compactrangetitle" =>
            ranges.get(q.id) match {
              case Some(r) =>
                CompactRangeTitle(r.left, r.right)
              case None =>
                CompactRangeTitle("N/A", "N/A")
            }
          case "compactrange" =>
            CompactRange
          case "free" =>
            FreeText
          case "range" =>
            ranges.get(q.id) match {
              case Some(r) =>
                Range(r.left, r.right, r.hasna)
              case None =>
                Range("N/A", "N/A", false)
            }
          case "freegrades" =>
            grades.get(q.id) match {
              case Some(r) =>
                FreeGrades(r.cardinality, GradeFormat(r.gradeFrom.toDouble, r.gradeTo.toDouble, r.gradeStep.toDouble))

              case None =>
                FreeGrades(1, GradeFormat.default)
            }

          case "coursesgrades" =>
            val (card, g) = grades.get(q.id) match {
              case Some(r) =>
                (r.cardinality, GradeFormat(r.gradeFrom.toDouble, r.gradeTo.toDouble, r.gradeStep.toDouble))

              case None =>
                (1, GradeFormat.default)
            }

            val cs = choices.getOrElse(q.id, Seq()).map { c =>
              SelectChoice(c.id, c.value, c.freeifselected)
            }

            CoursesGrades(cs, card, g)

          case "select" =>
            Select(choices.getOrElse(q.id, Seq()).map { c =>
              SelectChoice(c.id, c.value, c.freeifselected)
            })
        }

        q.id -> tpe
      }).toMap
    }
  }

  def formHeaders(db: Database, qF: Query[FormsFormsToUsers, FormsFormsToUsersRow, Seq]): Future[Seq[FormHeader]] = {

    val fhQ = (for {
      ((fu, di), cdi) <- ((qF joinLeft FormsUsers on (_.sciperDirector === _.sciper))
                            joinLeft FormsUsers on (_._1.sciperCodirector === _.sciper))
      f <- FormsForms if f.id === fu.idForm
      d <- FormsDefinitions if d.id === f.idDefinition
      fac <- FormsFaculties if fac.id === f.idFaculty
      u <- FormsUsers if fu.sciperUser === u.sciper
    } yield (fu, d, fac, f, u, di, cdi)).sortBy(r => (r._4.dateFrom, r._5.lastname, r._5.firstname))

    val sectQ = for {
      fu <- qF//.distinctOn(_.idForm)
      f <- FormsForms if f.id === fu.idForm
      d <- FormsDefinitions if d.id === f.idDefinition
      s <- FormsSections if s.idDefinition === d.id
    } yield (d.id, s)

    val sQ = for {
      fu <- qF
      s <- FormsSignatures if s.idFormUser === fu.id &&
                              (s.sciper === fu.sciperUser ||
                               s.sciper === fu.sciperDirector ||
                               s.sciper === fu.sciperCodirector)
    } yield (s)

    for {
      res1 <- db.run(fhQ.result)
      res2 <- db.run(sQ.result)
      res3 <- db.run(sectQ.result)
    } yield {

      val perForm = res2.groupBy(s => s.idFormUser)
      val sects = res3.groupBy(_._1).mapValues(_.map(_._2.section).toSet)

      for ( (fu, d, fac, f, u, od, ocd) <- res1) yield {
        FormHeader(fac, d, sects.getOrElse(d.id, Set()), f, fu, u, od, ocd, perForm.getOrElse(fu.id, Nil))
      }
    }
  }
}
