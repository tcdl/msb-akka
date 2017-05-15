package io.github.tcdl.msb

import akka.actor.{Actor, ActorLogging}
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.MsbResponderActor.{Ack, IncomingRequest, MsbRequestHandler}
import io.github.tcdl.msb.api.message.payload.RestPayload
import io.github.tcdl.msb.api.{MessageTemplate, MsbContext, ResponderContext, Responder => JavaResponder}

import scala.concurrent.{Await, Future, Promise}
import scala.concurrent.duration.FiniteDuration

trait MsbResponderActor extends Actor with ActorLogging {

  private val msb = Msb(context.system)
  private lazy val responderConfig: ResponderConfig = MsbConfig(context.system).responderConfig(msbTarget)

  /** The namespace of the topic on which the ResponderServer needs to subscribe. */
  def namespace: String

  /** A partial function from Payload to Unit that's responsible for handling the incoming requests.
    *
    * If this handler returns a Future[_], the responder will block and wait until it either completes or the
    * request-handling-timeout expires (cfr. configuration).
    */
  def handleRequest: MsbRequestHandler

  /** The msbTarget that needs to be used to choose the correct msbContext. */
  def msbTarget: Option[String] = None

  /** The msb context that'll be used for this responder. */
  def msbContext: MsbContext = msbTarget.map(t => msb.multiTargetContexts(t)) getOrElse msb.context

  /** Create a response with the given body. */
  def response(body: Any) = Response(body)

  /** The number of times the responder should attempt to handle an incoming message if the processing fails. */
  def retryAttempts: Int = 0

  override def preStart(): Unit = {
    super.preStart()
    responderServer.listen()
  }

  override def receive: Receive = {
    case IncomingRequest(payload, responder, promise) =>
      try {
        handleRequest(Request(payload), responder) match {
          case f: Future[_] => promise.completeWith(f)
          case somethingElse => promise.success(somethingElse)
        }
      } catch {
        case e: Throwable => promise.failure(e)
      }
  }

  private lazy val responderServer = objectFactory.createResponderServer(namespace, new MessageTemplate(), requestHandler, classOf[RestPayload[_, _, _, _]])
  private def objectFactory = msbContext.getObjectFactory

  private val requestHandler: (RestPayload[_, _, _, _], ResponderContext) => Unit = { (payload, ctx) =>
    def tryHandlingRequest(attemptsLeft: Int): Unit = {
      val request = IncomingRequest(payload, ResponderImpl(ctx.getResponder))

      self ! request
      Await.ready(request.promise.future, responderConfig.`request-handling-timeout`)

      if(attemptsLeft > 0 && request.promise.future.failed.isCompleted) {
        import context.dispatcher
        request.promise.future onFailure { case e: Throwable => log.error(e, s"Exception was thrown while processing request, retries left: $attemptsLeft")}
        tryHandlingRequest(attemptsLeft - 1)
      }
    }

    tryHandlingRequest(retryAttempts)
  }
}

object MsbResponderActor {
  type MsbRequestHandler = PartialFunction[(Request, Responder), Any]

  case class Ack(timeout: FiniteDuration, remaining: Int = 0)

  private case class IncomingRequest(payload: RestPayload[_, _, _, _],
                                     responder: Responder,
                                     promise: Promise[Any] = Promise[Any])
}

trait Responder {
  def ! (response: Response)
  def ! (msg: Ack)
}

case class ResponderImpl(private val javaResponder: JavaResponder) extends Responder {

  /** Respond to the requester with the given response. */
  def ! (response: Response) = javaResponder.send(response.payload)

  /** Send an ack back to the requester. */
  def ! (msg: Ack) = javaResponder.sendAck(msg.timeout.toMillis.toInt, msg.remaining)

}