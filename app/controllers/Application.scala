package controllers

import scala.io._
import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.ws.{WS, ResponseHeaders}
import play.api.Play.current
import java.util.concurrent.TimeUnit._

object Application extends Controller {

  def index = TODO

  def route(host: String, path: String) = Action { request =>

    //Logger.info("=> " + request.uri);

    Async {
      Akka.future { WS.url(request.uri).get() }.map { promise =>
        promise.orTimeout("Timed out", 30, SECONDS).value.get.fold(
          response => convert(response),
          timeout => RequestTimeout)
      }
    }
  }

  def streamFrom[A](uri: String)(consumer: ResponseHeaders => Iteratee[Array[Byte], A]) = {
     WS.url(uri).get(consumer)
  }

  def rawData(uri: String) = new Enumerator[Array[Byte]] {
    def apply[A](iteratee: Iteratee[Array[Byte], A]) = {
      streamFrom(uri) { headers => iteratee }
    }
  }

  def convert(response: play.api.libs.ws.Response) = {
    val data = response.ahcResponse.getResponseBodyAsStream
    val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(data)
    // TODO use withHeaders
    Status(response.status).stream(dataContent).withHeaders("X-Proxied-by" -> "kebab").as(response.header(CONTENT_TYPE).getOrElse(""))
  }
}

