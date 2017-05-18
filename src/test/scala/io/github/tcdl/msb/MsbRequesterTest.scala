package io.github.tcdl.msb

import java.util.UUID

import akka.testkit.{ImplicitSender, TestKit}
import io.github.tcdl.msb.MsbModel.{Request, Response}
import io.github.tcdl.msb.MsbRequester.{PublishEnded, TargetNotConfigured}
import io.github.tcdl.msb.api.message.payload.RestPayload
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._
import scala.concurrent.{Await, Promise}

class MsbRequesterTest extends TestKit(MsbTests.actorSystem) with ImplicitSender
  with FlatSpecLike with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {

  val msbContext = Msb(system).context
  val msbContexts = Msb(system).multiTargetContexts
  var namespace: String = _

  before {
    namespace = s"msb-test:requester:${UUID.randomUUID.toString}"
  }

  override def afterAll(): Unit = {
    msbContext.shutdown()
    msbContexts.foreach(_._2.shutdown())
  }

  "An MsbRequester" should "build a payload and send it into the bus" in {

    val requester = system.actorOf(MsbRequester.props(namespace))

    // given: a responder to catch the request
    val request = Promise[String]()
    val responder = msbContext.responderServer(namespace, payloadClass = classOf[RestPayload[_,_,_,String]]) {
      (req, responder) => request.success(req.getBody)
    }
    responder.listen()

    // when: the requester sends out a request
    requester ! Request("ping")

    // then: it's received by the responder
    Await.result(request.future, 5.seconds) shouldBe "ping"
  }

  it should "reply if there's a response" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val requester = system.actorOf(MsbRequester.props(namespace, opts))
    val responder = msbContext.responderServer(namespace, payloadClass = classOf[RestPayload[_, _, _, String]]) {
      (req, ctx) => ctx.getResponder.send(new RestPayload.Builder().withBody("pong").build())
    }
    responder.listen()

    requester ! Request("ping")
    expectMsgClass(classOf[Response]).bodyAs[String] shouldBe Some("pong")
    expectMsg(PublishEnded)
  }

  it should "reply from the correct target based on target id" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val targetId = "target1"
    val requester = system.actorOf(MsbRequester.props(namespace, opts))
    val responder = msbContexts(targetId).responderServer(namespace, payloadClass = classOf[RestPayload[_, _, _, String]]) {
      (req, ctx) => ctx.getResponder.send(new RestPayload.Builder().withBody("pong").build())
    }
    responder.listen()

    requester ! Request(body = Some("ping"), targetId = Some(targetId))
    expectMsgClass(classOf[Response]).bodyAs[String] shouldBe Some("pong")
    expectMsg(PublishEnded)
  }

  it should "reply TargetNotConfigured when there is no configuration for provided target id" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val requester = system.actorOf(MsbRequester.props(namespace = namespace, options = opts))

    requester ! Request(body = Some("ping"), targetId = Some("missing_target_id"))
    expectMsg(TargetNotConfigured("missing_target_id"))
  }

}