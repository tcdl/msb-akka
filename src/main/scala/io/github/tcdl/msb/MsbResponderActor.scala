package io.github.tcdl.msb

import akka.actor.Actor
import scala.concurrent.duration.FiniteDuration
import io.github.tcdl.msb.api.message.payload.Payload
import io.github.tcdl.msb.api.ResponderServer.RequestHandler
import io.github.tcdl.msb.MsbResponderActor.{ MsbRequestHandler, Ack, IncomingRequest }
import io.github.tcdl.msb.api.message.Message
import io.github.tcdl.msb.api.{MessageTemplate, Responder, MsbContext}

trait MsbResponderActor extends Actor {

  /** The namespace of the topic on which the ResponderServer needs to subscribe. */
  def namespace: String

  /** A partial function from Payload to Unit that's responsible for handling the incoming requests. */
  def handleRequest: MsbRequestHandler

  def msbcontext: MsbContext = Msb(context.system).context

  /** A handle to access the responder, beware this accesses actor state and should only be called
   *  from the handleRequest thread.
   *
   *  @throws java.util.NoSuchElementException if the responder is not set, most likely because it's
   *  not called from the handleRequest thread. */
  def responder: Responder = _responder.get

  /** Respond to the requester with the given payload. */
  def reply(payload: Payload): Unit = _responder.foreach { r => r.send(payload) }

  /** Send an ack back to the requester. */
  def send(msg: Ack): Unit = _responder.foreach { r => r.sendAck(msg.timeout.toMillis.toInt, msg.remaining) }

  /** Get the original message {@see io.github.tcdl.msb.api.Responder#getOriginalMessage()}.  */
  def orginalMessage: Message = responder.getOriginalMessage

  /** Create a payload builder with the given body. */
  def response(body: Any): Payload.Builder = new Payload.Builder().withBody(body)

  private var _responder: Option[Responder] = None

  override def preStart(): Unit = {
    super.preStart()
    responderServer.listen()
  }

  override def receive = {
    case IncomingRequest(payload, responder) =>
      _responder = Some(responder)
      handleRequest(payload)
      _responder = None
  }

  private lazy val responderServer = objectFactory.createResponderServer(namespace, new MessageTemplate(), requestHandler)
  private def objectFactory = msbcontext.getObjectFactory

  private val requestHandler: RequestHandler = {
    (payload: Payload, responder: Responder) => self ! IncomingRequest(payload, responder)
  }
}

object MsbResponderActor {
  type MsbRequestHandler = PartialFunction[Payload, Unit]
  case class Ack(timeout: FiniteDuration, remaining: Int = 0)

  private case class IncomingRequest(payload: Payload, reponder: Responder)
}