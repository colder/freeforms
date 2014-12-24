package models

abstract class Alert(val cl: String) {
  val icon: Option[String]
  val text: String
  val dismissible: Boolean
}

case class AlertSuccess(
  text: String,
  icon: Option[String] = Some("fa-check"),
  dismissible: Boolean = false
) extends Alert("alert-success")

case class AlertWarning(
  text: String,
  icon: Option[String] = None,
  dismissible: Boolean = false
) extends Alert("alert-warning")

case class AlertError(
  text: String,
  icon: Option[String] = Some("fa-exclamation-circle"),
  dismissible: Boolean = false
) extends Alert("alert-danger")

case class AlertInfo(
  text: String,
  icon: Option[String] = None,
  dismissible: Boolean = false
) extends Alert("alert-info")
