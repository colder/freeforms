package models

import play.api.libs.json._


abstract class Answer
case class FreeAnswer(value: String) extends Answer
case class JsonAnswer(value: JsValue) extends Answer
case class ChoiceAnswer(value: Int, freeValue: Option[String]) extends Answer

case class CoursesGradesAnswer(answers: Seq[CourseGradeAnswer]) extends Answer
case class CourseGradeAnswer(value: Int, grade: Double)

case class FreeGradesAnswer(answers: Seq[FreeGradeAnswer]) extends Answer
case class FreeGradeAnswer(value: String, grade: Double) extends Answer

object JsonAnswer {
  import play.api.libs.functional.syntax._

  implicit val courseGradeAnswerWrites = new Writes[CourseGradeAnswer] {
    def writes(cga: CourseGradeAnswer) = Json.obj(
      "course" -> cga.value,
      "grade"  -> cga.grade
    )
  }

  implicit val courseGradeAnswerReads: Reads[CourseGradeAnswer] = (
    (JsPath \ "course").read[Int] and
    (JsPath \ "grade").read[Double]
  )(CourseGradeAnswer.apply _)


  implicit val coursesGradesAnswerWrites = new Writes[CoursesGradesAnswer] {
    def writes(cga: CoursesGradesAnswer) = Json.obj(
      "answers" -> cga.answers
    )
  }

  implicit val coursesGradesAnswerReads: Reads[CoursesGradesAnswer] = {
    (JsPath \ "answers").read[Seq[CourseGradeAnswer]].map(as => CoursesGradesAnswer(as))
  }

  implicit val freeGradeAnswerWrites = new Writes[FreeGradeAnswer] {
    def writes(fga: FreeGradeAnswer) = Json.obj(
      "course" -> fga.value,
      "grade"  -> fga.grade
    )
  }

  implicit val freeGradeAnswerReads: Reads[FreeGradeAnswer] = (
    (JsPath \ "course").read[String] and
    (JsPath \ "grade").read[Double]
  )(FreeGradeAnswer.apply _)


  implicit val freeGradesAnswerWrites = new Writes[FreeGradesAnswer] {
    def writes(fga: FreeGradesAnswer) = Json.obj(
      "answers" -> fga.answers
    )
  }

  implicit val freeGradesAnswerReads: Reads[FreeGradesAnswer] = {
    (JsPath \ "answers").read[Seq[FreeGradeAnswer]].map(as => FreeGradesAnswer(as))
  }
}
