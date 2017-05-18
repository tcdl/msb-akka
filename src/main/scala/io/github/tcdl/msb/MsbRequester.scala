package io.github.tcdl.msb

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.MessageDispatcher
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.MsbRequester.{PublishEnded, TargetNotConfigured}
import io.github.tcdl.msb.api.message.payload.RestPayload
import io.github.tcdl.msb.api.{MessageContext, MsbContext, Requester}

import scala.concurrent.{Future, blocking}

class MsbRequester(namespace: String, options: MsbRequestOptions) extends Actor with ActorLogging {

  private val msbDispatcher: MessageDispatcher = context.system.dispatchers.lookup("msb-publishing-dispatcher")
  def requester(context: MsbContext): Requester[RestPayload[_, _, _, _]] =
    context.getObjectFactory.createRequester(namespace, options, classOf[RestPayload[_,_,_,_]])

  override def receive: PartialFunction[Any, Unit] = {
    case r: Request =>
      r.targetId match {
        case Some(id) =>
          Msb(context.system).multiTargetContexts.get(id) match {
            case Some(msbContext) => publish(r, msbContext)
            case None =>
              log.error(s"Can not publish to '$namespace'. Configuration for target '$r.targetId' can not be found.")
              sendMessage(sender, TargetNotConfigured(id))
          }
        case None => publish(r, Msb(context.system).context)
      }
  }

  def publish(r: Request, msbContext: MsbContext, replyTo: ActorRef = sender()): Future[Unit] = {
    Future { blocking {
      requester(msbContext)
        .onResponse(sendResponseBackTo(replyTo))
        .onEnd(sendEndMessageBackToSender(replyTo))
        .publish(r.payload, null.asInstanceOf[String])
    }} (msbDispatcher)
  }

  private def sendEndMessageBackToSender(s: ActorRef) = { _: Void => sendMessage(s, PublishEnded) }
  private def sendResponseBackTo(s: ActorRef) = { (payload: RestPayload[_,_,_,_], _: MessageContext) => sendMessage(s, Response(payload)) }
  private def sendMessage(s: ActorRef, r: Any) = s ! r
}

object MsbRequester {

  def props(namespace: String, options: MsbRequestOptions = MsbRequestOptions()) =
    Props(new MsbRequester(namespace, options))

  case object PublishEnded
  case class TargetNotConfigured(targetId: String)
}