package io.github.tcdl.msb

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import io.github.tcdl.msb.MsbRequester.{Request, Response, Responses, TargetNotConfigured}
import io.github.tcdl.msb.api.message.payload.Payload.Builder
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

class MsbRequesterTest extends TestKit(ActorSystem("msb-requester-test")) with ImplicitSender
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
    val responder = msbContext.responderServer(namespace) { (req, responder) => request.success(req.bodyAs[String]) }
    responder.listen()

    // when: the requester sends out a request
    requester ! Request("ping")

    // then: it's received by the responder
    Await.result(request.future, 5.seconds) shouldBe "ping"
  }

  it should "reply if there's a response" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val requester = system.actorOf(MsbRequester.props(namespace, opts))
    val responder = msbContext.responderServer(namespace) { (req, responder) => responder.send(new Builder().withBody("pong").build()) }
    responder.listen()

    requester ! Request("ping")
    expectMsgClass(classOf[Response]).body[String] shouldBe Some("pong")
    expectMsgClass(classOf[Responses])
  }

  it should "reply from the correct target based on target id" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val targetId = "target1"
    val requester = system.actorOf(MsbRequester.props(namespace = namespace, options = opts))
    val responder = msbContexts(targetId).responderServer(namespace) { (req, responder) => responder.send(new Builder().withBody("pong").build()) }
    responder.listen()

    requester ! Request(body = Some("ping"), targetId = Some(targetId))
    expectMsgClass(classOf[Response]).body[String] shouldBe Some("pong")
    expectMsgClass(classOf[Responses])
  }

  it should "reply TargetNotConfigured when there is no configuration for provided target id" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val requester = system.actorOf(MsbRequester.props(namespace = namespace, options = opts))

    requester ! Request(body = Some("ping"), targetId = Some("missing_target_id"))
    expectMsg(TargetNotConfigured("missing_target_id"))
  }

}