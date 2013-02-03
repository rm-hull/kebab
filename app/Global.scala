import play.api._
import play.api.mvc._
import play.api.mvc.Results._

object Global extends GlobalSettings {

  override def onRouteRequest(request: RequestHeader) = {
    (request.uri startsWith "/") match {
      case true => super.onRouteRequest(request)
      case false => Some(Proxy.route)
    }
  }

  val echo = Action { request =>
    Ok("Got request [" + request + "]")
  }
}
