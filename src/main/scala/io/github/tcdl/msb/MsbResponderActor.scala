package io.github.tcdl.msb

import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration
import io.github.tcdl.msb.api.message.payload.Payload
import io.github.tcdl.msb.api.ResponderServer.RequestHandler
import io.github.tcdl.msb.MsbResponderActor.{ MsbRequestHandler, Ack, IncomingRequest }
import io.github.tcdl.msb.api.message.Message
import io.github.tcdl.msb.api.{MessageTemplate, Responder => JavaResponder, MsbContext}

trait MsbResponderActor extends Actor {

  /** The namespace of the topic on which the ResponderServer needs to subscribe. */
  def namespace: String

  /** A partial function from Payload to Unit that's responsible for handling the incoming requests. */
  def handleRequest: MsbRequestHandler

  def msbcontext: MsbContext = Msb(context.system).context

  /** Create a payload builder with the given body. */
  def response(body: Any): Payload.Builder = new Payload.Builder().withBody(body)

  override def preStart(): Unit = {
    super.preStart()
    responderServer.listen()
  }

  override def receive = {
    case IncomingRequest(payload, responder) =>
      handleRequest(payload, ResponderImpl(responder))
  }

  private lazy val responderServer = objectFactory.createResponderServer(namespace, new MessageTemplate(), requestHandler)
  private def objectFactory = msbcontext.getObjectFactory

  private val requestHandler: RequestHandler = {
    (payload: Payload, responder: JavaResponder) => self ! IncomingRequest(payload, responder)
  }
}

object MsbResponderActor {
  type MsbRequestHandler = PartialFunction[(Payload, Responder), Unit]
  case class Ack(timeout: FiniteDuration, remaining: Int = 0)

  private case class IncomingRequest(payload: Payload, responder: JavaResponder)
}

trait Responder {
  def ! (payload: Payload)
  def ! (msg: Ack)
  def originalMessage : Message
}

case class ResponderImpl(private val javaResponder: JavaResponder) extends Responder {

  /** Respond to the requester with the given payload. */
  def ! (payload: Payload) = javaResponder.send(payload)

  /** Send an ack back to the requester. */
  def ! (msg: Ack) = javaResponder.sendAck(msg.timeout.toMillis.toInt, msg.remaining)

  /** Get the original message {@see io.github.tcdl.msb.api.Responder#getOriginalMessage()}.  */
  def originalMessage : Message = javaResponder.getOriginalMessage
}