package models

import db.Tables.FormsAnswersRow

case class Question(id: Int, title: String, tooltip: Option[String], tpe: QuestionType, optional: Boolean, ans: Option[Answer] = None) {
  def isAnswered = tpe.isAnswered(ans) || optional
}

case class SelectChoice(id: Int, name: String, isFreeWhenSelected: Boolean = false)


abstract class QuestionType {
  def isAnswered(oa: Option[Answer]): Boolean = oa.isDefined
}

case object FreeText extends QuestionType {
  override def isAnswered(oa: Option[Answer]): Boolean = {
    oa match {
      case Some(FreeAnswer(str)) => str.nonEmpty
      case _ => false
    }
  }
}

case class Range(left: String, right: String, hasNA: Boolean = false) extends QuestionType {
}

case class GradeFormat(from: Double, to: Double, step: Double) {
  require(from < to)

  def steps: Seq[Double] = {
    from.to(to).by(step)
  }

  def validate(d: Double): Boolean = {
    steps contains d
  }
}

object GradeFormat {
  def default = GradeFormat(1, 6, 0.5)
}

case class CoursesGrades(choices: Seq[SelectChoice], n: Int, g: GradeFormat) extends QuestionType {
}

case class FreeGrades(n: Int, g: GradeFormat) extends QuestionType {
}

case class Select(choices: Seq[SelectChoice]) extends QuestionType {
  override def isAnswered(oa: Option[Answer]): Boolean = {
    oa match {
      case Some(ChoiceAnswer(id, ofree)) =>
        choices.find(_.id == id) match {
          case Some(c) =>
            !c.isFreeWhenSelected || ofree.getOrElse("").nonEmpty
          case _ =>
            false
        }
      case _ => false
    }
  }
}

case class CompactRangeTitle(left: String, right: String) extends QuestionType {
}

case object CompactRange extends QuestionType {
}
