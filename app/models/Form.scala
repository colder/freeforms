package models

case class Form(header: FormHeader,
                sections: Seq[Section]) {

  def student = header.student

  def id = header.formUser.id

  def mapQuestions(f: (Section, Question) => Question) = {
    copy(sections = sections.map{ s =>
      s.mapQuestions(f)
    })
  }

  def questions = {
    for {
      s <- sections
      p <- s.parts
      q <- p.questions
    } yield (s, q)
  }

  def answer(id: Int) = questions.find(_._2.id == id) flatMap { case (_, q) =>
    q.ans map { ans =>
      (q.tpe, ans)
    }
  }
}
