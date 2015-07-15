package io.github.tcdl

import io.github.tcdl.msb.api.Callback
import io.github.tcdl.msb.api.ResponderServer.RequestHandler
import io.github.tcdl.msb.api.message.Message
import io.github.tcdl.msb.api.Responder
import io.github.tcdl.msb.api.message.payload.Payload

package object msb {

  implicit def function2EndCallback(f: Function1[List[Message], Unit]): Callback[java.util.List[Message]] = new Callback[java.util.List[Message]]() {
	  import scala.collection.JavaConverters._
	  def call(arg: java.util.List[Message]): Unit = f(arg.asScala.toList)
  }

  implicit def functionToCallback[T, R <: Any](f: Function1[T, R]): Callback[T] = new Callback[T]() {
	  def call(arg: T): Unit = f(arg)
  }

  implicit def function2RequestHandler(f: Function2[Payload, Responder, Unit]): RequestHandler = new RequestHandler() {
	  def process(payload: Payload, responder: Responder): Unit = f(payload, responder)
  }

  implicit class ScalaPayload(p: Payload) {
	  def bodyAs[T](implicit manifest: Manifest[T]): T = p.getBodyAs(manifest.runtimeClass).asInstanceOf[T]
  }
}