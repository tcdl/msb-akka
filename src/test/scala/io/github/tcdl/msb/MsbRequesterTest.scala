package io.github.tcdl.msb

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import io.github.tcdl.msb.MsbRequester.{Request, Response}
import io.github.tcdl.msb.api.message.payload.Payload
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._

class MsbRequesterTest extends TestKit(ActorSystem("msb-requester-test")) with ImplicitSender
  with FlatSpecLike with Matchers
  with BeforeAndAfter
  with BeforeAndAfterAll {
  
  val msbcontext = Msb(system).context
  
  var namespace: String = _
  
  before {
    namespace = s"msb-test:requester:${UUID.randomUUID.toString}"
  }
  
  override def afterAll(): Unit = msbcontext.shutdown()
  
  "An MsbRequester" should "build a payload and send it into the bus" in {

    val requester = system.actorOf(MsbRequester.props(namespace))
    
    // given: a responder to catch the request
    val request = Promise[String]() 
    val responder = msbcontext.responderServer(namespace) { (req, responder) => request.success(req.bodyAs[String]) }
    responder.listen()

    // when: the requester sends out a request
    requester ! Request("ping")
    
    // then: it's received by the responder
    Await.result(request.future, 5.seconds) shouldBe "ping"
  }

  it should "reply if there's a response" in {
    val opts = MsbRequestOptions().withWaitForResponses(1)
    val requester = system.actorOf(MsbRequester.props(namespace, opts))
    val responder = msbcontext.responderServer(namespace) { (req, responder) => responder.send(Response("pong").payload) }
    responder.listen()
    
    requester ! Request("ping")
    expectMsgClass(classOf[Payload]).bodyAs[String] shouldBe "pong"
  }
  
}