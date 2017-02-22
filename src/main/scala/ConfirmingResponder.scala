import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import io.github.tcdl.msb.MsbConfirmingResponderActor
import io.github.tcdl.msb.MsbConfirmingResponderActor.MsbConfirmingRequestHandler

import scala.util.Random

object ConfirmingResponder extends App {

  val configString = """msbConfig {
                       |
                       |  # Service Details
                       |  serviceDetails = {
                       |    name = "responder"
                       |    version = "1.0.0"
                       |    instanceId = "responder"
                       |  }
                       |
                       |  # Thread pool used for scheduling ack and response timeout tasks
                       |  timerThreadPoolSize = 2
                       |
                       |  # Enable/disable message validation against json schema
                       |  # Do not specify this property to assert that we successfully fall back to msb-java's default config.
                       |  # validateMessage = true
                       |
                       |  brokerAdapterFactory = "io.github.tcdl.msb.adapters.amqp.AmqpAdapterFactory"
                       |
                       |  # Broker Adapter Defaults
                       |  brokerConfig = {
                       |    host = "localhost"
                       |    username = "guest"
                       |    password = "guest"
                       |    virtualHost = "/"
                       |    port = 5672
                       |    useSSL = false
                       |    groupId = ponger
                       |    prefetchCount = 5
                       |  }
                       |  threadingConfig = {
                       |    consumerThreadPoolSize = 20
                       |    # -1 means unlimited
                       |    consumerThreadPoolQueueCapacity = -1
                       |  }
                       |}""".stripMargin

  val config = ConfigFactory.parseString(configString).withFallback(ConfigFactory.load())


  val actorSystem = ActorSystem("testy_mctest", config)

  actorSystem.actorOf(Props(new TestActor), "ResponderTestActor")
}

class TestActor extends MsbConfirmingResponderActor {
  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent._

  override def namespace: String = "ping:pong"

  override def handleRequest: MsbConfirmingRequestHandler = {
    case (_, _) => Future {
      blocking {
        Thread.sleep(1000)
        println(s"future is returning ${Thread.currentThread.getName}")
        if(Random.nextInt(4) % 5 == 0) {
          throw new RuntimeException("Oh snap!")
        }
      }
    }
  }
}
