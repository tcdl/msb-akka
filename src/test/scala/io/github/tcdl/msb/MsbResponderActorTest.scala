package io.github.tcdl.msb

import scala.concurrent.duration._

import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.Eventually

import akka.actor.ActorSystem
import akka.actor.Props
import akka.testkit.TestKit
import io.github.tcdl.msb.api.RequestOptions
import io.github.tcdl.msb.api.message.payload.Payload

class MsbResponderActorTest extends TestKit(ActorSystem("msb-actor-test"))
  with WordSpecLike with Matchers with Eventually {

  import org.scalatest.OptionValues._

  val msbcontext = Msb(system).context
  val namespace = "msb-akka:responder-test"

  val responder = system.actorOf(Props(new MsbResponderActorForTest()))

  "An MsbResponderActor" when {

    "replying" should {
      "pass the reply to the requester" in {
        var pong: Option[String] = None
        val requestResponse = new RequestOptions.Builder().withWaitForResponses(1).build()

        msbcontext.getObjectFactory.createRequester(namespace, requestResponse)
          .onResponse { p: Payload => pong = Some(p.bodyAs[String]) }
          .publish(new Payload.Builder().withBody("ping").build())

        eventually(timeout(5.seconds)) {
          pong.value shouldBe "pong"
        }
      }
    }
  }


  private class MsbResponderActorForTest extends MsbResponderActor {

	  override val namespace = MsbResponderActorTest.this.namespace

    override def handleRequest = {
      case (p, replyTo) if p.bodyAs[String] == "ping" => replyTo ! response("pong").build()
    }

  }
}