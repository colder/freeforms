package models

abstract class AccessMode(val name: String)

case object AccessModeEDIC extends AccessMode("edic")

case object AccessModeEDEE extends AccessMode("edee")

object AccessMode {
  def fromName(s: String): AccessMode = s match {
    case "edic"    => AccessModeEDIC
    case "edee"    => AccessModeEDEE
  }
}

