import java.util
import java.util.concurrent._
import java.util.function.BiConsumer

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JSR310Module
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.github.tcdl.msb.api.{Callback, MessageContext, MsbContextBuilder, RequestOptions}
import com.typesafe.config.ConfigFactory
import io.github.tcdl.msb.api.message.payload.RestPayload

object Pinger extends App {

  implicit def functionToCallback[T, R <: Any](f: (T) => R): Callback[T] = new Callback[T]() {
    def call(arg: T): Unit = f(arg)
  }

  implicit def functionToBiConsumer[T, U, R <: Any](f: (T, U) => R): BiConsumer[T, U] = new BiConsumer[T, U]() {
    def accept(arg1: T, arg2: U): Unit = f(arg1, arg2)
  }

  val configString =
    """msbConfig {
      |
      |  # Service Details
      |  serviceDetails = {
      |    name = "pinger"
      |    version = "1.0.0"
      |    instanceId = "pinger"
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
      |    groupId = pinger
      |  }
      |  threadingConfig = {
      |    consumerThreadPoolSize = 20
      |    # -1 means unlimited
      |    consumerThreadPoolQueueCapacity = -1
      |  }
      |}""".stripMargin

  val config = ConfigFactory.parseString(configString).withFallback(ConfigFactory.load())

  val namespace = "ping:pong"

  val objectMapper = new ObjectMapper()
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .registerModule(new JSR310Module)
    .registerModule(DefaultScalaModule)

  val msbcontext = new MsbContextBuilder()
    .withConfig(config)
    .enableShutdownHook(true)
    .withPayloadMapper(objectMapper)
    .build()

  val requestResponse = new RequestOptions.Builder().withWaitForResponses(1).build()

  def requester = msbcontext.getObjectFactory.createRequester(namespace, requestResponse)

  var sent = 0
  var received = 0

  val tpe = new ThreadPoolExecutor(1, 1, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(50))

  while (true) {
    try {
      if(tpe.getQueue.size() < 10) {
        tpe.submit(new RequestResponseRunner())
      }
    } catch {
      case e: RejectedExecutionException => println(e)
      case e: Exception => println("exception: " + e.getMessage)
    }
  }


}

class RequestResponseRunner extends Runnable {
  import Pinger.functionToBiConsumer

  override def run(): Unit = {
    Pinger.requester.onResponse{(node: JsonNode, ctx: MessageContext) =>
      println("received")
    }.publish(
      new RestPayload.Builder().withBody("ping").build(), null.asInstanceOf[String]
    )
  }
}
