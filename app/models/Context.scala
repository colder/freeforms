package models

import _root_.db.Tables._
import _root_.db.TypeMappers._

import play.api.Play.current

import slick.driver.MySQLDriver.api._

case class Context(db: Database,
                   user: FormsUsersRow,
                   ofaculty: Option[FormsFacultiesRow] = None,
                   odirector: Option[FormsDirectorsRow] = None,
                   oadmin: Option[FormsAdminsRow] = None) {

  def faculty = ofaculty.get

  def isAdmin = oadmin.isDefined

  def isDirector = odirector.isDefined

  def role: Role = {
    if (isAdmin) {
      AdminRole
    } else if (isDirector) {
      DirectorRole
    } else {
      StudentRole
    }
  }

  lazy val isDebug: Boolean = {
    current.configuration.getBoolean("application.debug").getOrElse(false)
  }
}
