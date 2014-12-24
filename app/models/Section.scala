package models

case class Section(kind: SectionKind, title: String, parts: Seq[Part]) {

  def mapQuestions(f: (Section, Question) => Question): Section = {
    this.copy(parts = parts.map { p =>
      p.copy(questions = p.questions.map {
        f(this, _)
      })
    })
  }
}



abstract class SectionKind(val name: String)
object SectionKind {
  def fromName(name: String) = {
    name match {
      case "joint"    => JointSection
      case "student"  => StudentSection
      case "director" => DirectorSection
      case _          => JointSection
    }
  }
}


case object JointSection extends SectionKind("joint")
case object StudentSection extends SectionKind("student")
case object DirectorSection extends SectionKind("director")
