package io.github.tcdl.msb

import io.github.tcdl.msb.MsbModel.Response
import io.github.tcdl.msb.api.message.payload.RestPayload.Builder
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class ResponseTest extends FlatSpec with Matchers with BeforeAndAfterAll {

  "A Response" should "correctly return the body" in {
    val response = Response(new Builder().withBody(BodyData("foo")).build())
    response.bodyAs[BodyData] shouldBe Some(BodyData("foo"))
  }

  it should "return None if trying to get body of unexpected type" in {
    val response = Response(new Builder().withBody("foo").build())
    response.bodyAs[Int] shouldBe None
  }

  it should "return None if body is null" in {
    val response = Response(new Builder().withBody(null).build())
    response.bodyAs[String] shouldBe None
  }

  it should "correctly return the body buffer" in {
    val response = Response(new Builder().withBodyBuffer("hiyaaar".getBytes).build())
    response.bodyBuffer shouldBe 'defined
    response.bodyBuffer.get should contain theSameElementsAs "hiyaaar".getBytes
  }

  it should "correctly return the headers" in {
    val response = Response(new Builder().withHeaders(HeaderData("foo")).build())
    response.headersAs[HeaderData] shouldBe Some(HeaderData("foo"))
  }

  it should "correctly return the params" in {
    val response = Response(new Builder().withParams(ParamData("bar")).build())
    response.paramsAs[ParamData] shouldBe Some(ParamData("bar"))
  }

  it should "correctly return the query" in {
    val response = Response(new Builder().withQuery(QueryData("baz")).build())
    response.queryAs[QueryData] shouldBe Some(QueryData("baz"))
  }

  it should "correctly return the status code" in {
    val response = Response(new Builder().withStatusCode(666).build())
    response.statusCode shouldBe Some(666)
  }

  it should "correctly return the status message" in {
    val response = Response(new Builder().withStatusMessage("Pijnboompit").build())
    response.statusMessage shouldBe Some("Pijnboompit")
  }
}

case class BodyData(data: String)
case class HeaderData(data: String)
case class ParamData(data: String)
case class QueryData(data: String)
