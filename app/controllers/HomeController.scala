package controllers

import akka.NotUsed
import akka.stream.scaladsl.Source
import akka.util.ByteString
import io.grpc.examples.{GreeterService, HelloReply, HelloRequest}
import javax.inject._
import play.api.http.HttpEntity
import play.api.mvc._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(@Named("client") greeterService: GreeterService)(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    val source: Source[HelloReply, NotUsed] = greeterService.itKeepsReplying(HelloRequest("Alice"))
    Ok.sendEntity(HttpEntity.Streamed(
      source.map((h: HelloReply) => ByteString(h.message)),
      contentLength = None,
      contentType = Some("text/plain")
    ))
  }
}
