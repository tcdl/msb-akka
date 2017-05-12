package io.github.tcdl.msb

import java.util.UUID

import scala.concurrent.duration._
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatest.concurrent.Eventually
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import io.github.tcdl.msb.api.{MessageContext, MsbContext, RequestOptions}
import io.github.tcdl.msb.api.message.payload.RestPayload

import scala.concurrent.{Await, Future, Promise, blocking}

class MsbResponderActorTest extends TestKit(ActorSystem("msb-actor-test"))
  with WordSpecLike with Matchers with Eventually with ImplicitSender {

  import org.scalatest.OptionValues._
  import system.dispatcher

  val msbContext: MsbContext = Msb(system).context
  val namespace: String = "msb-akka:responder-test"

  val responder: ActorRef = system.actorOf(Props(new MsbResponderActorForTest()))

  "An MsbResponderActor" when {

    "replying" should {
      "pass the reply to the requester" in {
        var pong: Option[String] = None
        sendRequest("ping", (p, _) => pong = Some(p.getBody))

        eventually(timeout(5.seconds)) {
          pong.value shouldBe "pong"
        }
      }
    }

    "the request invokes asynchronous processing" should {
      "not take in all the messages at once" in {
        var promises: List[Promise[String]] = List()

        for(_ <- 1 to 5) {
          val promise = Promise[String]
          sendRequest("async", (p, _) => promise.success(p.getBody))
          promises ::= promise
        }

        Await.ready(Future.sequence(promises.map(_.future)), 30.seconds)

        responder ! "max"
        val actualMaxRunning = expectMsgType[Int]

        // The SafeTestMsbAdapterFactory used by this test is inherently single-threaded, so if it blocks while
        // waiting for the handling to finish, there should never be more than one handler running at the same time.
        assert(actualMaxRunning <= 1, s"No more than 1 jobs should've run in parallel, but was $actualMaxRunning")
      }
    }
  }


  private def sendRequest(body: String,
                          onResponse: (RestPayload[Any, Any, Any, String], MessageContext) => Unit,
                          requestResponse: RequestOptions = new RequestOptions.Builder().withWaitForResponses(1).build()) = {

    msbContext.getObjectFactory
      .createRequester(namespace, requestResponse, classOf[RestPayload[Any, Any, Any, String]])
      .onResponse(onResponse)
      .publish(new RestPayload.Builder().withBody(body).build(), null.asInstanceOf[String])
  }

  private class MsbResponderActorForTest extends MsbResponderActor {

	  override val namespace: String = MsbResponderActorTest.this.namespace
    var running: Map[UUID, Promise[Unit]] = Map()
    var maxRunning: Int = 0

    override def handleRequest: PartialFunction[(MsbModel.Request, Responder), Any] = {
      case (p, replyTo) if p.bodyAs[String].contains("ping") =>
        replyTo ! response("pong")

      case (p, replyTo) if p.bodyAs[String].contains("async") =>
        val id = UUID.randomUUID()
        running += (id -> Promise())
        if(running.size > maxRunning) maxRunning = running.size
        context.system.scheduler.scheduleOnce(100.millis, self, (id, replyTo))(context.dispatcher)

        running(id).future
    }

    override def aroundReceive(receive: Receive, msg: Any): Unit = {
      msg match {
        case (id: UUID, replyTo: Responder) =>
          val promise = running(id)
          running -= id
          promise.success(())
          replyTo ! response("done")

        case "max" => sender ! maxRunning
        case _ => super.aroundReceive(receive, msg)
      }
    }
  }
}