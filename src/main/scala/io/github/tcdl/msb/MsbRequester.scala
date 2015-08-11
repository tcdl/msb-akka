package io.github.tcdl.msb

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import io.github.tcdl.msb.MsbRequester.Request
import io.github.tcdl.msb.api.message.payload.Payload

class MsbRequester(namespace: String, options: MsbRequestOptions) extends Actor with ActorLogging {
  
  val msbcontext = Msb(context.system).context 
  def requester = msbcontext.getObjectFactory.createRequester(namespace, options)
  
  override def receive = {
    case r: Request => 
      log.info(s"Publishing request to namespace '$namespace'.")
      requester.onResponse(sendResponseBackTo(sender)).publish(r.payload)
  }
  
  def sendResponseBackTo(s: ActorRef) = { p: Payload => s ! p }
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

      body.foreach(bob.withBody(_))
      bodyBuffer.foreach(bob.withBodyBuffer(_))
      headers.foreach(bob.withHeaders(_))
      params.foreach(bob.withParams(_))
      query.foreach(bob.withQuery(_))
      statusCode.foreach(bob.withStatusCode(_))
      statusMessage.foreach(bob.withStatusMessage(_))

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

  case class Response(body: Option[Any] = None, 
                      bodyBuffer: Option[String] = None,
                      headers: Option[Any] = None,
                      params: Option[Any] = None,
                      query: Option[Any] = None,
                      statusCode: Option[Int] = None,
                      statusMessage: Option[String] = None) extends MsbPayload
                     
  object Request {
    def apply(body: Any): Request = Request(body = Some(body))
  }
  
  object Response {
    def apply(body: Any): Response = Response(body = Some(body))
  }

}