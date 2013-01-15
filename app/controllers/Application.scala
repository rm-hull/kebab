package controllers

import scala.io._
import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.ws.{WS, ResponseHeaders}
import play.api.Play.current
import java.util.concurrent.TimeUnit._
import scala.collection.JavaConverters._

import com.ning.http.client.{
  AsyncHttpClient,
  RequestBuilderBase,
  FluentCaseInsensitiveStringsMap,
  HttpResponseBodyPart,
  HttpResponseHeaders,
  HttpResponseStatus,
  Response => AHCResponse
}

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

    //Ok.stream(rawData(request.uri))
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
    Status(response.status)
      .stream(dataContent)
      .withHeaders(ningHeadersToMap(response.ahcResponse.getHeaders).toArray:_*)
      .withHeaders("X-Proxied-by" -> "kebab")
      .as(response.header(CONTENT_TYPE).getOrElse("plain/text"))
  }

  private def ningHeadersToMap(headers: FluentCaseInsensitiveStringsMap) =
    mapAsScalaMapConverter(headers).asScala.map { case (k,v) =>
      k -> v.asScala.mkString(",")
    }.toMap
}

