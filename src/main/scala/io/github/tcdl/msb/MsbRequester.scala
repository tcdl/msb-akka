package io.github.tcdl.msb

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.github.tcdl.msb.MsbRequester.{Responses, Response, Request}
import io.github.tcdl.msb.api.message.{Message, Acknowledge}
import io.github.tcdl.msb.api.message.payload.Payload

import scala.reflect.ClassTag
import scala.collection.JavaConverters._

class MsbRequester(namespace: String, options: MsbRequestOptions) extends Actor with ActorLogging {
  
  val msbcontext = Msb(context.system).context 
  def requester = msbcontext.getObjectFactory.createRequester(namespace, options)
  
  override def receive = {
    case r: Request => 
      log.info(s"Publishing request to namespace '$namespace'.")
      requester.onResponse(sendResponseBackTo(sender)).onEnd(sendResponseListBackTo(sender)).publish(r.payload)
  }

  def sendResponseListBackTo(s: ActorRef) = {l : java.util.List[Message] => s ! Responses(l.asScala.map(msg => Response(msg.getPayload)))}
  def sendResponseBackTo(s: ActorRef) = { p: Payload => s ! Response(p) }
}

object MsbRequester {

  def props(namespace: String, options: MsbRequestOptions = MsbRequestOptions()) = 
    Props(new MsbRequester(namespace, options))
  
  sealed trait MsbPayload {
    val body: Option[Any]
    val bodyBuffer: Option[String] 
    val headers: Option[Any] 
    val params: Option[Any] 
    val query: Option[Any] 
    val statusCode: Option[Int] 
    val statusMessage: Option[String] 

    def payload = {
      val bob = new Payload.Builder()

      body.foreach(bob.withBody)
      bodyBuffer.foreach(bob.withBodyBuffer)
      headers.foreach(bob.withHeaders)
      params.foreach(bob.withParams)
      query.foreach(bob.withQuery)
      statusCode.foreach(bob.withStatusCode(_))
      statusMessage.foreach(bob.withStatusMessage)

      bob.build()
    }
  }
  
  case class Request(body: Option[Any] = None, 
                     bodyBuffer: Option[String] = None,
                     headers: Option[Any] = None,
                     params: Option[Any] = None,
                     query: Option[Any] = None) extends MsbPayload {
    
    val statusCode: Option[Int] = None
    val statusMessage: Option[String] = None
  }

  case class Response(payload: Payload) {
    def body[T <: Any](implicit tag: ClassTag[T]) : Option[T] = Option(payload.getBodyAs(tag.runtimeClass).asInstanceOf[T])
    def bodyBuffer: Option[String] = Option(payload.getBodyBuffer)
    def headers[T <: Any](implicit tag: ClassTag[T]) : Option[T] = Option(payload.getHeadersAs(tag.runtimeClass).asInstanceOf[T])
    def params[T <: Any](implicit tag: ClassTag[T]) : Option[T] = Option(payload.getParamsAs(tag.runtimeClass).asInstanceOf[T])
    def query[T <: Any](implicit tag: ClassTag[T]) : Option[T] = Option(payload.getQueryAs(tag.runtimeClass).asInstanceOf[T])
    def statusCode: Option[Int] = Option(payload.getStatusCode)
    def statusMessage: Option[String] = Option(payload.getStatusMessage)
  }

  case class Responses(responses: Seq[Response])
                     
  object Request {
    def apply(body: Any): Request = Request(body = Some(body))
  }
  
}