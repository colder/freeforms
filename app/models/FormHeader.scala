package models
import db.Tables._

case class FormHeader(faculty: FormsFacultiesRow,
                      definition: FormsDefinitionsRow,
                      sections: Set[SectionKind],
                      form: FormsFormsRow,
                      formUser: FormsFormsToUsersRow,
                      student: FormsUsersRow,
                      director: Option[FormsUsersRow],
                      codirector: Option[FormsUsersRow],
                      signatures: Seq[FormsSignaturesRow]) {

  val users: List[FormsUsersRow] = student :: (director ++ codirector).toList

  val directors: List[FormsUsersRow] = (director ++ codirector).toList

  def isAnswered: Boolean = {
    val base = formUser.sciperDirector.nonEmpty &&
               formUser.dateEnrolment.nonEmpty &&
               formUser.title.nonEmpty

    if (faculty.name == "EDIC") {
      base
    } else {
      base &&
      formUser.keywords.nonEmpty &&
      formUser.overview.nonEmpty
    }
  }

  def title = if (form.midterm) {
    s"Midterm Report ${form.year} - ${form.year+1}"
  } else {
    s"Annual Report ${form.year}"
  }

  val b = definition.accessMode match {
    case AccessModeEDIC => new BehaviorEDIC(this)
    case AccessModeEDEE => new BehaviorEDEE(this)
  }

  def signaturesFor(s: SectionKind): Seq[(FormsUsersRow, FormsSignaturesRow)] = {
    signatures.filter(_.section == s).flatMap { s =>
      (student :: directors).find(_.sciper == s.sciper).map { u =>
        (u, s)
      }
    }
  }

  def hasSection(s: SectionKind): Boolean = {
    sections contains s
  }

  lazy val hasJointSection    = hasSection(JointSection)
  lazy val hasStudentSection  = hasSection(StudentSection)
  lazy val hasDirectorSection = hasSection(DirectorSection)


  def hasSigned(s: SectionKind)(by: FormsUsersRow): Boolean = {
    signatures.exists {
      sign => sign.sciper == by.sciper &&
              sign.section == s
    }
  }

  lazy val jointComplete    = b.isComplete(JointSection)
  lazy val studentComplete  = b.isComplete(StudentSection)
  lazy val directorComplete = b.isComplete(DirectorSection)

  def prefillWith(h2: FormHeader): FormHeader = {
    // if incomplete, prefill with values from h2
    val fu2 = h2.formUser

    var h = this

    if (director.isEmpty) {
      h = h.copy(
        director   = h2.director,
        codirector = h2.codirector
      )
    }

    def firstOf(a: String, b: String) = if (a.nonEmpty) a else b


    val fu = if (director.isEmpty) {
        formUser.copy(
          dateEnrolment = formUser.dateEnrolment orElse fu2.dateEnrolment,
          title         = firstOf(formUser.title, fu2.title),
          keywords      = firstOf(formUser.keywords, fu2.keywords),
          overview      = firstOf(formUser.overview, fu2.overview)
        )
    } else {
      formUser
    }

    if (fu != formUser) {
      h = h.copy(formUser = fu)
    }

    h
  }

}
