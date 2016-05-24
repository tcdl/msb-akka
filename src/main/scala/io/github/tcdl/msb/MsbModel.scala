package io.github.tcdl.msb

import io.github.tcdl.msb.api.message.payload.RestPayload

import scala.reflect.ClassTag

object MsbModel {

  sealed trait MsbPayload {
    val body: Option[Any]
    val bodyBuffer: Option[Array[Byte]]
    val headers: Option[Any]
    val params: Option[Any]
    val query: Option[Any]
    val statusCode: Option[Int]
    val statusMessage: Option[String]

    def bodyAs[T <: Any](implicit tag: ClassTag[T]): Option[T] = convert(body)
    def headersAs[T <: Any](implicit tag: ClassTag[T]): Option[T] = convert(headers)
    def paramsAs[T <: Any](implicit tag: ClassTag[T]): Option[T] = convert(params)
    def queryAs[T <: Any](implicit tag: ClassTag[T]): Option[T] = convert(query)

    def payload = {
      val bob = new RestPayload.Builder[Any, Any, Any, Any]()

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
                     bodyBuffer: Option[Array[Byte]] = None,
                     headers: Option[Any] = None,
                     params: Option[Any] = None,
                     query: Option[Any] = None,
                     targetId: Option[String] = None) extends MsbPayload {

    override val statusCode: Option[Int] = None
    override val statusMessage: Option[String] = None
  }

  object Request {
    def apply(body: Any): Request = Request(body = Option(body))

    def apply(p: RestPayload[_, _, _, _]): Request =
      Request(
        body = Option(p.getBody),
        bodyBuffer = Option(p.getBodyBuffer),
        headers = Option(p.getHeaders),
        params = Option(p.getParams),
        query = Option(p.getQuery)
      )
  }

  case class Response(body: Option[Any] = None,
                      bodyBuffer: Option[Array[Byte]] = None,
                      headers: Option[Any] = None,
                      params: Option[Any] = None,
                      query: Option[Any] = None,
                      statusCode: Option[Int] = None,
                      statusMessage: Option[String] = None) extends MsbPayload {
  }

  object Response {
    def apply(body: Any): Response = Response(body = Option(body))

    def apply(payload: RestPayload[_, _, _, _]): Response =
      Response(
        body = Option(payload.getBody),
        bodyBuffer = Option(payload.getBodyBuffer),
        headers = Option(payload.getHeaders),
        params = Option(payload.getParams),
        query = Option(payload.getQuery),
        statusCode = Option(payload.getStatusCode).map(_.intValue),
        statusMessage = Option(payload.getStatusMessage)
      )
  }

}
