package models

abstract class Event(val name: String)

case object JointValidated    extends Event("joint_validated")
case object JointUpdated      extends Event("joint_updated")
case object JointCompleted    extends Event("joint_completed")
case object StudentCompleted  extends Event("student_completed")
case object FormCompleted     extends Event("form_completed")
case object DirectorValidated extends Event("director_validated")
case object DirectorUpdated   extends Event("director_updated")
case object DirectorCompleted extends Event("director_completed")

object Event {
  def fromName(s: String): Event = s match {
    case "joint_validated"    => JointValidated
    case "joint_updated"      => JointUpdated
    case "joint_completed"    => JointCompleted
    case "student_completed"  => StudentCompleted
    case "form_completed"     => FormCompleted
    case "director_validated" => DirectorValidated
    case "director_updated"   => DirectorUpdated
    case "director_completed" => DirectorCompleted
    
  }
}

