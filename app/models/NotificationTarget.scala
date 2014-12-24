package models

abstract class NotificationTarget(val name: String)
case object Student extends NotificationTarget("student")
case object Director extends NotificationTarget("director")
case object CoDirector extends NotificationTarget("codirector")
case object Faculty extends NotificationTarget("faculty")

object NotificationTarget {
  def fromName(s: String): NotificationTarget = s match {
    case "student" => Student
    case "director" => Director
    case "codirector" => CoDirector
    case "faculty" => Faculty
    
  }
}
