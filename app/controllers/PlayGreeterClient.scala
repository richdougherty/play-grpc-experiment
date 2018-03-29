package controllers

import java.util.concurrent.TimeUnit

import akka.{Done, NotUsed}
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import io.grpc.CallOptions
import io.grpc.examples.{GreeterService, GreeterServiceClient, HelloReply, HelloRequest}
import io.grpc.internal.testing.TestUtils
import io.grpc.netty.shaded.io.grpc.netty.{GrpcSslContexts, NegotiationType, NettyChannelBuilder}
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import javax.inject.{Inject, Singleton}
import play.api.inject.ApplicationLifecycle

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class PlayGreeterClient @Inject()(appLifecycle: ApplicationLifecycle)(implicit mat: Materializer, ec: ExecutionContext)
    extends GreeterService {

  private val channel = {
    val serverHost = "127.0.0.1"
    val serverPort = 8080
    val useTls = true
    val useTestCa = true
    val serverHostOverride: String = "foo.test.google.fr"

    val sslContext: SslContext = {
      if (useTestCa) {
        try // FIXME issue #89
        GrpcSslContexts.forClient.trustManager(TestUtils.loadCert("ca.pem")).build()
        catch {
          case ex: Exception => throw new RuntimeException(ex)
        }
      } else null
    }

    val channelBuilder =
      NettyChannelBuilder
        .forAddress(serverHost, serverPort)
        .flowControlWindow(65 * 1024)
        .negotiationType(if (useTls) NegotiationType.TLS else NegotiationType.PLAINTEXT)
        .sslContext(sslContext)

    if (useTls && serverHostOverride != null)
      channelBuilder.overrideAuthority(serverHostOverride)

    channelBuilder.build()
  }
  appLifecycle.addStopHook { () => Future(channel.shutdown().awaitTermination(10, TimeUnit.SECONDS)) }

  private val callOptions = CallOptions.DEFAULT
  private val internalClient = new GreeterServiceClient(channel, callOptions)

  override def sayHello(in: HelloRequest): Future[HelloReply] =
    internalClient.sayHello(in)
  override def itKeepsTalking(in: Source[HelloRequest, NotUsed]): Future[HelloReply] =
    internalClient.itKeepsTalking(in)
  override def itKeepsReplying(in: HelloRequest): Source[HelloReply, NotUsed] =
    internalClient.itKeepsReplying(in)
  override def streamHellos(in: Source[HelloRequest, NotUsed]): Source[HelloReply, NotUsed] =
    internalClient.streamHellos(in)


  def run(): Unit = {

    def singleRequestReply(): Unit = {
      val reply = internalClient.sayHello(HelloRequest("Alice"))
      println(s"got single reply: ${Await.result(reply, 5.seconds).message}")
    }

    def streamingRequest(): Unit = {
      val requests = List("Alice", "Bob", "Peter").map(HelloRequest.apply)
      val reply = internalClient.itKeepsTalking(Source(requests))
      println(s"got single reply for streaming requests: ${Await.result(reply, 5.seconds).message}")
    }

    def streamingReply(): Unit = {
      val responseStream = internalClient.itKeepsReplying(HelloRequest("Alice"))
      val done: Future[Done] =
        responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))
      Await.ready(done, 1.minute)
    }

    def streamingRequestReply(): Unit = {
      val requestStream: Source[HelloRequest, NotUsed] =
        Source
          .tick(100.millis, 1.second, "tick")
          .zipWithIndex
          .map { case (_, i) => i }
          .map(i => HelloRequest(s"Alice-$i"))
          .take(10)
          .mapMaterializedValue(_ => NotUsed)

      val responseStream: Source[HelloReply, NotUsed] = internalClient.streamHellos(requestStream)
      val done: Future[Done] =
        responseStream.runForeach(reply => println(s"got streaming reply: ${reply.message}"))
      Await.ready(done, 1.minute)
    }

    singleRequestReply()
    streamingRequest()
    streamingReply()
    streamingRequestReply()
  }
}