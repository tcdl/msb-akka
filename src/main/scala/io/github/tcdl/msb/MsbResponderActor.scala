package io.github.tcdl.msb

import akka.actor.{Actor, ActorLogging}
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.MsbResponderActor._
import io.github.tcdl.msb.api.message.payload.RestPayload
import io.github.tcdl.msb.api.metrics.{Gauge, MetricSet}
import io.github.tcdl.msb.api.{MessageTemplate, MsbContext, ResponderContext, Responder => JavaResponder}

import scala.concurrent.Future.firstCompletedOf
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Future, Promise}
import scala.util.Failure

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

  /** A partial function that will determine the retry mode to choose based on the exception that was thrown during the
    * processing of the message.
    *
    * Possible outcomes are:
    *   - NoRetry: The message gets rejected.
    *   - RetryOnce: The message gets retried once, after that it gets rejected.
    *   - Retry: The message gets retried, potentially creating an infinite loop.
    */
  def retryMode: PartialFunction[Throwable, RetryMode] = PartialFunction.empty

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
    case GetMessageCount =>
      val count = Option(responderServer.getMetrics.getMetric(MetricSet.MESSAGE_COUNT_METRIC)).flatMap {
        case m: Gauge[_] => Option(m.getValue).map { case l: Long => l }
        case _ => None
      }
      sender ! count
  }

  private lazy val responderServer = objectFactory.createResponderServer(namespace, new MessageTemplate(), requestHandler, classOf[RestPayload[_, _, _, _]])
  private def objectFactory = msbContext.getObjectFactory

  import context.dispatcher

  private val requestHandler: (RestPayload[_, _, _, _], ResponderContext) => Unit = { (payload, ctx) =>
    val request = IncomingRequest(payload, ResponderImpl(ctx.getResponder))

    // Don't auto acknowledge
    ctx.getAcknowledgementHandler.setAutoAcknowledgement(false)

    // Trigger request
    self ! request

    // Set up a timer to trigger the timeout
    val timeout = akka.pattern.after(responderConfig.`request-handling-timeout`, context.system.scheduler) {
      throw new IllegalStateException(s"MSBResponder message handling timed out after ${responderConfig.`request-handling-timeout`}.")
    }

    // Handle the results
    firstCompletedOf(timeout :: request.promise.future :: Nil) onComplete {
      case scala.util.Success(_) => ctx.getAcknowledgementHandler.confirmMessage()
      case Failure(e) =>
        val mode = retryMode.orElse[Throwable, RetryMode]({ case _: Throwable => NoRetry })(e)
        log.error(e, s"Unexpected failure while handling MSB message (retry mode for this message: {}).", mode)

        mode match {
          case NoRetry => ctx.getAcknowledgementHandler.rejectMessage()
          case RetryOnce => ctx.getAcknowledgementHandler.retryMessageFirstTime()
          case Retry => ctx.getAcknowledgementHandler.retryMessage()
        }
    }
  }
}

object MsbResponderActor {
  type MsbRequestHandler = PartialFunction[(Request, Responder), Any]

  case class Ack(timeout: FiniteDuration, remaining: Int = 0)

  private case class IncomingRequest(payload: RestPayload[_, _, _, _],
                                     responder: Responder,
                                     promise: Promise[Any] = Promise[Any])

  case object GetMessageCount

  sealed trait RetryMode
  case object NoRetry extends RetryMode
  case object RetryOnce extends RetryMode
  case object Retry extends RetryMode
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