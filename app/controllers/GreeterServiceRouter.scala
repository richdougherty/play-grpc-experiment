package controllers

import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.Materializer
import io.grpc.examples.{GreeterService, GreeterServiceHandler}
import javax.inject.{Inject, Named, Singleton}
import play.api.mvc.{Handler, RequestHeader}
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.core.server.akkahttp.AkkaHeadersWrapper

import scala.concurrent.Future

// FIXME: Needs Play support
case class RawAkkaHttpHandler(f: HttpRequest => Future[HttpResponse]) extends Handler

@Singleton
class GreeterServiceRouter(prefix: String, serviceImpl: GreeterService, mat: Materializer) extends SimpleRouter {
  println("Starting GreeterServiceRouter")

  @Inject()
  def this(@Named("impl") serviceImpl: GreeterService, mat: Materializer) = this(prefix = "", serviceImpl, mat)

  private val handler: PartialFunction[HttpRequest, Future[HttpResponse]] = GreeterServiceHandler(serviceImpl)(mat)

  private def toAkkaHttpRequest(rh: RequestHeader): HttpRequest = {
    rh.headers.asInstanceOf[AkkaHeadersWrapper].request
  }

  // Note: type Routes = PartialFunction[RequestHeader, Handler]
  override def routes: Routes = new Routes {
    override def isDefinedAt(rh: RequestHeader): Boolean = {
      val request = toAkkaHttpRequest(rh)
      val defined = handler.isDefinedAt(request)
      println(s"GreeterServiceRouter: route to ${rh.path} is $defined")
      defined
    }
    override def apply(rh: RequestHeader): Handler = {
      RawAkkaHttpHandler(handler)
    }
  }
}
