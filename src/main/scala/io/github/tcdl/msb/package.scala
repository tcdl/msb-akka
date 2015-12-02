package io.github.tcdl

import io.github.tcdl.msb.api.{Responder => JavaResponder}
import io.github.tcdl.msb.api.Callback
import io.github.tcdl.msb.api.MsbContext
import io.github.tcdl.msb.api.MessageTemplate
import io.github.tcdl.msb.api.ResponderServer
import io.github.tcdl.msb.api.RequestOptions
import io.github.tcdl.msb.api.ResponderServer.RequestHandler
import io.github.tcdl.msb.api.message.Message
import io.github.tcdl.msb.api.message.payload.Payload

package object msb {

  implicit def function2EndCallback(f: (List[Message]) => Unit): Callback[java.util.List[Message]] = new Callback[java.util.List[Message]]() {
	  import scala.collection.JavaConverters._
	  def call(arg: java.util.List[Message]): Unit = f(arg.asScala.toList)
  }

  implicit def functionToCallback[T, R <: Any](f: (T) => R): Callback[T] = new Callback[T]() {
	  def call(arg: T): Unit = f(arg)
  }

  implicit def function2RequestHandler(f: (Payload, JavaResponder) => Unit): RequestHandler = new RequestHandler() {
	  def process(payload: Payload, responder: JavaResponder): Unit = f(payload, responder)
  }

  implicit class ScalaPayload(p: Payload) {
	  def bodyAs[T](implicit manifest: Manifest[T]): T = p.getBodyAs(manifest.runtimeClass).asInstanceOf[T]
  }

  implicit class ScalaMsbContext(ctx: MsbContext) {
    def responderServer(ns: String, template: MessageTemplate = new MessageTemplate())(f: (Payload, JavaResponder) => Unit): ResponderServer = {
      ctx.getObjectFactory.createResponderServer(ns, template, f)
    }
  }

  case class MsbRequestOptions(acktimeout: Option[Int] = None,
                               messageTemplate: Option[MessageTemplate] = None,
                               responseTimeout: Option[Int] = None,
                               waitForResponses: Option[Int] = None) {

    def withAckTimeout(timeout: Int) = copy(acktimeout = Some(timeout))
    def withMessageTemplate(template: MessageTemplate) = copy(messageTemplate = Some(template))
    def withResponseTimeout(timeout: Int) = copy(responseTimeout = Some(timeout))
    def withWaitForResponses(nrOrResponses: Int) = copy(waitForResponses = Some(nrOrResponses))
  }

  implicit def scalaRequestOptions2requestOptions(options: MsbRequestOptions): RequestOptions = {
    val result = new RequestOptions.Builder()
    options.acktimeout.foreach(result.withAckTimeout(_))
    options.messageTemplate.foreach(result.withMessageTemplate(_))
    options.responseTimeout.foreach(result.withResponseTimeout(_))
    options.waitForResponses.foreach(result.withWaitForResponses(_))
    result.build()
  }
}