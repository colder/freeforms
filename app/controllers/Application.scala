package controllers

import javax.inject.Inject

import play.api._
import play.api.mvc._

import _root_.db.Tables._
import _root_.db.TypeMappers._

import models._

import slick.driver.MySQLDriver.api._
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

class Application @Inject() (configuration: Configuration) extends Controller {

  def welcome = Auth.async { implicit r =>
    for (fs <- db.run(FormsFaculties.filter(_.active).result)) yield {
      if (fs.size == 1) {
        Redirect(routes.Forms.list(fs.head.name))
      } else {
        configuration.getString("application.defaultFaculty") match {
          case Some(fac) =>
            Redirect(routes.Forms.list(fac))

          case None =>
            Ok(views.html.main("Welcome") {
              views.html.faculties(fs)
            })
        }
      }
    }
  }

  def notfound = Auth.async { implicit r =>
    for (fs <- db.run(FormsFaculties.result)) yield {
      NotFound(views.html.main("404 - Not found") {
        views.html.faculties(fs)
      })
    }
  }

  def become(sciper: String) = Action.async { implicit r =>
    if (configuration.getBoolean("application.debug").getOrElse(false)) {
      Future(
        Redirect(routes.Application.welcome)
          .withSession("sciper" -> sciper)
          .flashing("success" -> s"Logged in as $sciper")
      )
    } else {
      Future(
        Redirect(routes.Application.welcome)
          .flashing("error" -> s"Unavailable")
      )
    }
  }

  def logout = Action { implicit r =>
    Redirect("http://tequila.epfl.ch/logout").withNewSession
  }
}
