package io.github.tcdl.msb

import akka.actor.{Actor, PoisonPill, Props}
import io.github.tcdl.msb.MsbConfirmingResponderActor.MsbConfirmingRequestHandler
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.api.message.payload.RestPayload
import io.github.tcdl.msb.api.{MessageTemplate, MsbContext, ResponderContext, Responder => JavaResponder}

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

trait MsbConfirmingResponderActor extends Actor  {

  /** The namespace of the topic on which the ResponderServer needs to subscribe. */
  def namespace: String

  /** A partial function from Payload to Unit that's responsible for handling the incoming requests. */
  def handleRequest: MsbConfirmingRequestHandler

  def msbcontext: MsbContext = Msb(context.system).context

  /** Create a response with the given body. */
  def response(body: Any) = Response(body)

  override def preStart(): Unit = {
    super.preStart()
    responderServer.listen()
  }

  override def receive = {
    case IncomingRequest(payload, ctx) =>
      handleRequest(Request(payload), ResponderImpl(ctx.getResponder))
  }

  private lazy val responderServer = objectFactory.createResponderServer(namespace, new MessageTemplate(), requestHandler, classOf[RestPayload[_, _, _, _]])
  private def objectFactory = msbcontext.getObjectFactory

  private val requestHandler = {
    (payload: RestPayload[_, _, _, _], ctx: ResponderContext) =>
      val promise = Promise[Unit]()
      context.actorOf(ConfirmableMessageHandler.props(handleRequest, promise)) ! IncomingRequest(payload, ctx)
      Await.result(promise.future, 1 hour)
  }
}

object MsbConfirmingResponderActor {
  type MsbConfirmingRequestHandler = PartialFunction[(Request, Responder), Future[Unit]]

  private case class IncomingRequest(payload: RestPayload[_, _, _, _], context: ResponderContext)
}

case class ConfirmingResponderImpl(private val javaResponder: JavaResponder) extends Responder {

  /** Respond to the requester with the given response. */
  def ! (response: Response) = javaResponder.send(response.payload)

  /** Send an ack back to the requester. */
  def ! (msg: Ack) = javaResponder.sendAck(msg.timeout.toMillis.toInt, msg.remaining)

}

class ConfirmableMessageHandler(delegate: MsbConfirmingRequestHandler, promise: Promise[Unit]) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  var tries = 2

  override def receive: Receive = {
    case IncomingRequest(payload, ctx) =>
      delegate(Request(payload), ResponderImpl(ctx.getResponder)).onComplete{
        case Success(_) =>
          promise.success(Unit)
          self ! PoisonPill
        case Failure(ex) if tries > 0 =>
          println(s"Retrying, retries left: $tries")
          tries -= 1
          self ! IncomingRequest(payload, ctx)
        case Failure(ex) =>
          println("No retries left, failing.")
          promise.failure(ex)
          self ! PoisonPill
      }

  }
}


object ConfirmableMessageHandler {
  def props(delegate: MsbConfirmingRequestHandler, promise: Promise[Unit]) = Props(new ConfirmableMessageHandler(delegate, promise))
}

