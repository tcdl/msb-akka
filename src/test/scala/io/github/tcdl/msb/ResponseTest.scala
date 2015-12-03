package io.github.tcdl.msb

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.tcdl.msb.MsbRequester.Response
import io.github.tcdl.msb.api.message.payload.Payload.Builder
import io.github.tcdl.msb.support.Utils
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ResponseTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  override def beforeAll = {
    // init jackson for marshalling the json body in the payload
    Utils.getJsonObjectMapper.registerModule(DefaultScalaModule)
  }

  "A Response" should "correctly return the body" in {
    val payload = new Builder().withBody(BodyData("foo")).build()
    val response = Response(payload)
    response.body[BodyData] shouldBe Some(BodyData("foo"))
  }

  it should "correctly return the body buffer" in {
    val payload = new Builder().withBodyBuffer("hiyaaar").build()
    val response = Response(payload)
    response.bodyBuffer shouldBe Some("hiyaaar")
  }

  it should "correctly return the headers" in {
    val payload = new Builder().withHeaders(HeaderData("foo")).build()
    val response = Response(payload)
    response.headers[HeaderData] shouldBe Some(HeaderData("foo"))
  }

  it should "correctly return the params" in {
    val payload = new Builder().withParams(ParamData("bar")).build()
    val response = Response(payload)
    response.params[ParamData] shouldBe Some(ParamData("bar"))
  }

  it should "correctly return the query" in {
    val payload = new Builder().withQuery(QueryData("baz")).build()
    val response = Response(payload)
    response.query[QueryData] shouldBe Some(QueryData("baz"))
  }

  it should "correctly return the status code" in {
    val payload = new Builder().withStatusCode(666).build()
    val response = Response(payload)
    response.statusCode shouldBe Some(666)
  }

  it should "correctly return the status message" in {
    val payload = new Builder().withStatusMessage("Pijnboompit").build()
    val response = Response(payload)
    response.statusMessage shouldBe Some("Pijnboompit")
  }
}

case class BodyData(data: String)
case class HeaderData(data: String)
case class ParamData(data: String)
case class QueryData(data: String)
