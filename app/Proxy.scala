import scala.io._
import play.api._
import play.api.mvc._
import play.api.libs.concurrent._
import play.api.libs.iteratee._
import play.api.libs.ws.{WS, ResponseHeaders}
import play.api.Play.current
import scala.collection.JavaConverters._
import scala.concurrent.duration._

import Execution.Implicits.defaultContext

import com.ning.http.client.{
  AsyncHttpClient,
  RequestBuilderBase,
  FluentCaseInsensitiveStringsMap,
  HttpResponseBodyPart,
  HttpResponseHeaders,
  HttpResponseStatus,
  Response => AHCResponse
}

object Proxy extends Controller {

  val TIMEOUT = Duration(30, SECONDS)

  def route = Action { request =>

    val future = forward(request).orTimeout("waiting for: " + request.uri, 30, SECONDS)
    Async {
      future.map {
        case Left(response) => convert(response)
        case Right(message) => RequestTimeout(message)
      }
    }
    //Ok.stream(rawData(request.uri))
  }

  private def forward(request: RequestHeader) = {
    WS.url(request.uri).execute(request.method)
  }

  private def streamFrom[A](uri: String)(consumer: ResponseHeaders => Iteratee[Array[Byte], A]) = {
     WS.url(uri).get(consumer)
  }

  private def rawData(uri: String) = new Enumerator[Array[Byte]] {
    def apply[A](iteratee: Iteratee[Array[Byte], A]) = {
      streamFrom(uri) { headers => iteratee }
    }
  }

  private def convert(response: play.api.libs.ws.Response) = {
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

