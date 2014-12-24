import play.api._
import play.api.mvc._
import play.api.mvc.Results._

import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration


object Global extends GlobalSettings {

  //override def onStart(app: Application) = {
  //  System.setProperty("java.library.path", "/home/ekneuss/git/forms/lib/")
  //}

  // called when a route is found, but it was not possible to bind the request parameters
  override def onBadRequest(request: RequestHeader, error: String) = {
    Future(BadRequest("Bad Request: " + error))
  }

  // 500 - internal server error
  override def onError(request: RequestHeader, throwable: Throwable) = {
    Future(InternalServerError("Error: "+throwable.getMessage))
  }

  // 404 - page not found error
  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future(Redirect(controllers.routes.Application.notfound).flashing(
      "error" -> "The page you requested could not be found!"
    ))
  }

}
