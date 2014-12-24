package models
import db.Tables._

abstract class FormFilter {
  def ff: FormsFiltersRow
  def value: Option[_]
  def stringValue: Option[String]

  def id: Int = ff.id
  def name: String = ff.name

  def choices: Seq[(String, String, Boolean)]
  def select(s: String): FormFilter
}

case class FormQuestionFilter(ff: FormsFiltersRow, idQuestion: Int, values: Seq[SelectChoice], value: Option[SelectChoice]) extends FormFilter {
  def choices: Seq[(String, String, Boolean)] = {
    values.map { v => 
      (v.id.toString, v.name, value == Some(v))
    }
  }

  def stringValue = value.map(_.id.toString);

  def select(s: String) = {
    val ov = values.find(_.id.toString == s)
    copy(value = ov)
  }
}
case class FormDirectorFilter(ff: FormsFiltersRow, values: Seq[FormsUsersRow], value: Option[FormsUsersRow]) extends FormFilter {
  def choices: Seq[(String, String, Boolean)] = {
    values.map { v => 
      (v.sciper, v.lastname+" "+v.firstname, value == Some(v))
    }
  }

  def stringValue = value.map(_.sciper);

  def select(s: String) = {
    val ov = values.find(_.sciper == s)
    copy(value = ov)
  }
}
case class FormCoDirectorFilter(ff: FormsFiltersRow, values: Seq[FormsUsersRow], value: Option[FormsUsersRow]) extends FormFilter {
  def choices: Seq[(String, String, Boolean)] = {
    values.map { v => 
      (v.sciper, v.lastname+" "+v.firstname, value == Some(v))
    }
  }

  def stringValue = value.map(_.sciper);

  def select(s: String) = {
    val ov = values.find(_.sciper == s)
    copy(value = ov)
  }
}
