package io.github.tcdl

import java.util.function.BiConsumer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.tcdl.msb.api.ResponderServer.RequestHandler
import io.github.tcdl.msb.api._
import io.github.tcdl.msb.api.message.Message
import io.github.tcdl.msb.support.Utils
import org.slf4j.LoggerFactory

import scala.reflect.ClassTag
import scala.util.Try

package object msb {

  import scala.language.implicitConversions

  private val log = LoggerFactory.getLogger(getClass)

  implicit def function2EndCallback(f: (List[Message]) => Unit): Callback[java.util.List[Message]] = new Callback[java.util.List[Message]]() {
	  import scala.collection.JavaConverters._
	  def call(arg: java.util.List[Message]): Unit = f(arg.asScala.toList)
  }

  implicit def functionToCallback[T, R <: Any](f: (T) => R): Callback[T] = new Callback[T]() {
	  def call(arg: T): Unit = f(arg)
  }

  implicit def function2RequestHandler[T](f: (T, ResponderContext) => Unit): RequestHandler[T] = new RequestHandler[T]() {
	  def process(request: T, responderContext: ResponderContext): Unit = f(request, responderContext)
  }

  implicit def functionToBiConsumer[T, U, R <: Any](f: (T, U) => R): BiConsumer[T, U] = new BiConsumer[T, U]() {
    def accept(arg1: T, arg2: U): Unit = f(arg1, arg2)
  }

  implicit class ScalaMsbContext(ctx: MsbContext) {
    def responderServer[T](ns: String, template: MessageTemplate = new MessageTemplate(), payloadClass: Class[T])(f: (T, ResponderContext) => Unit): ResponderServer = {
      ctx.getObjectFactory.createResponderServer(ns, template, f, payloadClass)
    }
  }

  def convert[T <: Any](source: Option[Any])(implicit tag: ClassTag[T]): Option[T] = {
    source.flatMap(s => {
      try {
        Option(Utils.convert(s, tag.runtimeClass, objectMapper).asInstanceOf[T])
      } catch { case t: Throwable =>
        log.error(s"Unable to convert source to an instance of ${tag.runtimeClass}", t)
        None
      }
    })
  }

  val objectMapper = new ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .registerModule(new JSR310Module)
    .registerModule(DefaultScalaModule)

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