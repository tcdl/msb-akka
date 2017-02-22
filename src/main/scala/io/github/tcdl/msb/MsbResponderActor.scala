package io.github.tcdl.msb

import akka.actor.Actor
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.MsbResponderActor.{MsbRequestHandler}
import io.github.tcdl.msb.api.message.payload.RestPayload
import io.github.tcdl.msb.api.{MessageTemplate, MsbContext, ResponderContext, Responder => JavaResponder}

import scala.concurrent.duration.FiniteDuration

trait MsbResponderActor extends Actor {

  /** The namespace of the topic on which the ResponderServer needs to subscribe. */
  def namespace: String

  /** A partial function from Payload to Unit that's responsible for handling the incoming requests. */
  def handleRequest: MsbRequestHandler

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
    (payload: RestPayload[_, _, _, _], ctx: ResponderContext) => self ! IncomingRequest(payload, ctx)
  }
}

object MsbResponderActor {
  type MsbRequestHandler = PartialFunction[(Request, Responder), Unit]


}

case class IncomingRequest(payload: RestPayload[_, _, _, _], context: ResponderContext)

trait Responder {
  case class Ack(timeout: FiniteDuration, remaining: Int = 0)

  def ! (response: Response)
  def ! (msg: Ack)
}

case class ResponderImpl(private val javaResponder: JavaResponder) extends Responder {

  /** Respond to the requester with the given response. */
  def ! (response: Response) = javaResponder.send(response.payload)

  /** Send an ack back to the requester. */
  def ! (msg: Ack) = javaResponder.sendAck(msg.timeout.toMillis.toInt, msg.remaining)

}