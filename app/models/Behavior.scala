package models
import db.Tables._

abstract class Behavior(val fh: FormHeader) {
  import fh._

  val unsignOnSave: Boolean

  def isComplete(s: SectionKind): Boolean = {
    s match {
      case JointSection =>
        hasSigned(s)(student) && director.exists { hasSigned(s) }
      case StudentSection =>
        !hasStudentSection || hasSigned(s)(student)
      case DirectorSection =>
        !hasDirectorSection || directors.nonEmpty && directors.forall { hasSigned(s) }
    }
  }

  def accessFor(role: Role, s: SectionKind)(implicit ctx: Context): Access = {
    (role, s) match {
      case (StudentRole, JointSection) if ctx.user == student =>
        if (isComplete(JointSection)) {
          ReadOnly
        } else {
          Writeable
        }

      case (StudentRole, StudentSection)  if ctx.user == student =>
        if (!isComplete(JointSection)) {
          ReadOnly
        } else {
          if (isComplete(StudentSection)) {
            ReadOnly
          } else {
            Writeable
          }
        }

      case (DirectorRole, JointSection) if directors contains ctx.user =>
        ReadOnly

      case (DirectorRole, DirectorSection) if directors contains ctx.user =>
        if (!isComplete(JointSection) || isComplete(DirectorSection)) {
          ReadOnly
        } else {
          if (Some(ctx.user) == director) {
            Writeable
          } else if (Some(ctx.user) == codirector) {
            ReadOnly
          } else {
            ReadOnly
          }
        }

      case (AdminRole, s) =>
        if (ctx.user == student) {
          accessFor(StudentRole, s)
        } else if (directors contains ctx.user) {
          accessFor(DirectorRole, s)
        } else {
          ReadOnly
        }

      case _ =>
        Denied
    }
  }

  def accessFor(s: Section)(implicit ctx: Context): Access = accessFor(s.kind)
  def accessFor(s: SectionKind)(implicit ctx: Context): Access = accessFor(ctx.role, s)


  def actionsFor(s: SectionKind)(implicit ctx: Context): Seq[(String, String, String)] = {
    if (isComplete(s)) {
      Nil
    } else if (s == JointSection) {
      if (ctx.user == student) {
        if (hasSigned(s)(ctx.user)) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Update""")
          )
        } else {
          List(
            ("btn-primary", "save", """<i class="fa fa-floppy-o"></i> Save"""),
            ("btn-success", "sign", """<i class="fa fa-check"></i> Notify Advisor""")
          )
        }
      } else if (director contains ctx.user) {
        if (hasSigned(s)(student)) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
          )
        } else {
          Nil
        }
      } else {
        Nil
      }
    } else if (s == DirectorSection) {
      if (Some(ctx.user) == director) {
        if (hasSigned(s)(ctx.user)) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Update""")
          )
        } else {
          List(
            ("btn-primary", "save",   """<i class="fa fa-floppy-o"></i> Save"""),
            if (codirector.isDefined) {
              ("btn-success", "sign", """<i class="fa fa-check"></i> Notify Co-Director""")
            } else {
              ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
            }
          )
        }
      } else if (Some(ctx.user) == codirector) {
        if (director.exists(hasSigned(s))) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
          )
        } else {
          Nil
        }
      } else {
        Nil
      }
    } else {
      if (accessFor(s).isWriteable) {
        List(
          ("btn-primary", "save",   """<i class="fa fa-floppy-o"></i> Save"""),
          ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
        )
      } else {
        Nil
      }
    }
  }

  def alertsFor(s: SectionKind)(implicit ctx: Context): Seq[Alert] = {
    if (isComplete(s)) {
      List(
        AlertSuccess("This section is complete and validated!")
      )
    } else if (s == JointSection) {
      if (directors contains ctx.user) {
        if (hasSigned(s)(student)) {
          if (Some(ctx.user) == director) {
            List(
              AlertSuccess("The student validated this part. Please validate or communicate the necessary changes to the student, as he is the only one able to update.", Some("fa-arrow-down"))
            )
          } else {
            List(
              AlertWarning("The student validated this part, waiting for the thesis director to validate.", Some("fa-clock-o"))
            )
          }
        } else {
          List(
            AlertWarning("Only the student can edit this section. Waiting on him to validate.", Some("fa-clock-o"))
          )
        }
      } else if (student == ctx.user && hasSigned(s)(student)) {
        List(
          AlertSuccess("Waiting for the thesis director to validate. You can update the joint part if necessary. Directors will be notified of your changes.", Some("fa-clock-o"))
        )
      } else {
        Nil
      }
    } else {
      if (!isComplete(JointSection)) {
        List(
          AlertWarning("The joint section is not complete yet. You will only be able to edit the private part as soon as the joint part is complete and validated.", Some("fa-ban"))
        )
      } else {
        if (s == DirectorSection) {
          if (Some(ctx.user) == director && hasSigned(s)(ctx.user) && codirector.isDefined) {
            List(
              AlertSuccess("Waiting for the thesis co-director to validate. You can still update the private part if necessary.", Some("fa-clock-o"))
            )
          } else if (Some(ctx.user) == codirector) {
            if (director.exists(hasSigned(s))) {
              List(
                AlertWarning("The thesis director validated this part. Please validate or communicate the necessary changes, as he is the only one able to update.", Some("fa-arrow-down"))
              )
            } else {
              List(
                AlertWarning("Waiting for the thesis director to complete this part.", Some("fa-clock-o"))
              )
            }
          } else {
            Nil
          }
        } else {
          Nil
        }
      }
    }
  }

  def eventsFor(h2: FormHeader, signaturesToAdd: Option[FormsSignaturesRow]): Set[Event] = {
    val h1 = fh

    val b1 = h1.b
    val b2 = h2.b

    if (!h1.jointComplete && h2.jointComplete) {
      Set(JointCompleted)
    } else if (!h2.jointComplete && signaturesToAdd.exists(_.sciper == h2.student.sciper)) {
      if (h1.hasSigned(JointSection)(h1.student)) {
        Set(JointUpdated)
      } else {
        Set(JointValidated)
      }
    } else if (!h1.directorComplete && h2.directorComplete) {
      Set(DirectorCompleted) ++ (if (h2.studentComplete) List(FormCompleted) else Nil)
    } else if (h1.jointComplete &&
               !h2.directorComplete &&
               signaturesToAdd.exists(s => Some(s.sciper) == h2.director.map(_.sciper))) {

      if (h1.director.exists(h1.hasSigned(DirectorSection))) {
        Set(DirectorUpdated)
      } else {
        Set(DirectorValidated)
      }
    } else if (!h1.studentComplete && h2.studentComplete) {
      Set(StudentCompleted) ++ (if (h2.directorComplete) List(FormCompleted) else Nil)
    } else {
      Set()
    }
  }
}

class BehaviorEDEE(fh: FormHeader) extends Behavior(fh) {
  val unsignOnSave: Boolean = true;
}

class BehaviorEDIC(fh: FormHeader) extends Behavior(fh) {
  import fh._

  val unsignOnSave: Boolean = false;

  override def actionsFor(s: SectionKind)(implicit ctx: Context): Seq[(String, String, String)] = {
    if (isComplete(s)) {
      Nil
    } else if (s == JointSection) {
      if (ctx.user == student) {
        if (hasSigned(s)(ctx.user)) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Update""")
          )
        } else {
          List(
            ("btn-primary", "save", """<i class="fa fa-floppy-o"></i> Save"""),
            ("btn-success", "sign", """<i class="fa fa-check"></i> Notify Advisor""")
          )
        }
      } else if (director contains ctx.user) {
        if (hasSigned(s)(student)) {
          List(
            ("btn-primary", "save", """<i class="fa fa-floppy-o"></i> Save"""),
            ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
          )
        } else {
          Nil
        }
      } else {
        Nil
      }
    } else if (s == DirectorSection) {
      if (Some(ctx.user) == director) {
        if (hasSigned(s)(ctx.user)) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Update""")
          )
        } else {
          List(
            ("btn-primary", "save",   """<i class="fa fa-floppy-o"></i> Save"""),
            ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
          )
        }
      } else if (Some(ctx.user) == codirector) {
        if (director.exists(hasSigned(s))) {
          List(
            ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
          )
        } else {
          Nil
        }
      } else {
        Nil
      }
    } else {
      if (accessFor(s).isWriteable) {
        List(
          ("btn-primary", "save",   """<i class="fa fa-floppy-o"></i> Save"""),
          ("btn-success", "sign", """<i class="fa fa-check"></i> Validate""")
        )
      } else {
        Nil
      }
    }
  }

  override def accessFor(role: Role, s: SectionKind)(implicit ctx: Context): Access = {
    (role, s) match {
      case (StudentRole, JointSection) if ctx.user == student =>
        if (isComplete(JointSection)) {
          ReadOnly
        } else {
          Writeable
        }

      case (StudentRole, DirectorSection) if ctx.user == student =>
        if (isComplete(DirectorSection)) {
          ReadOnly
        } else {
          Denied
        }

      case (DirectorRole, JointSection) if directors contains ctx.user =>
        if (Some(ctx.user) == director) {
          if (isComplete(JointSection) || !hasSigned(s)(student)) {
            ReadOnly
          } else {
            Writeable
          }
        } else {
          // co-director
          ReadOnly
        }

      case (DirectorRole, DirectorSection) if directors contains ctx.user =>
        if (!isComplete(JointSection) || isComplete(DirectorSection)) {
          ReadOnly
        } else {
          if (Some(ctx.user) == director) {
            Writeable
          } else {
            ReadOnly
          }
        }

      case (AdminRole, s) =>
        if (ctx.user == student) {
          accessFor(StudentRole, s)
        } else if (directors contains ctx.user) {
          accessFor(DirectorRole, s)
        } else {
          ReadOnly
        }

      case _ =>
        Denied
    }
  }

  override def alertsFor(s: SectionKind)(implicit ctx: Context): Seq[Alert] = {
    if (isComplete(s)) {
      List(
        AlertSuccess("This section is complete and validated!")
      )
    } else if (s == JointSection) {
      if (directors contains ctx.user) {
        if (hasSigned(s)(student)) {
          if (Some(ctx.user) == director) {
            List(
              AlertSuccess("The student validated this part. Please perform the necessary changes and validate to move to the next part.")
            )
          } else {
            List(
              AlertWarning("The student validated this part, waiting for the thesis director to validate.", Some("fa-clock-o"))
            )
          }
        } else {
          List(
            AlertWarning("Waiting on the student to pre-fill (and validate) this section.", Some("fa-clock-o"))
          )
        }
      } else if (student == ctx.user && hasSigned(s)(student)) {
        List(
          AlertSuccess("Waiting for the thesis director to validate. You can update the joint part if necessary. Directors will be notified of your changes.", Some("fa-clock-o"))
        )
      } else {
        Nil
      }
    } else {
      super.alertsFor(s)
    }
  }

}
