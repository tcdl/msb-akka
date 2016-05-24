package io.github.tcdl.msb

import akka.actor._
import com.typesafe.config.Config
import io.github.tcdl.msb.api.{MsbContext, MsbContextBuilder}

class MsbImpl(system: ActorSystem) extends Extension {

  private val config = MsbConfig(system)

  val context = buildContext(config.msbConfig)
  val multiTargetContexts = config.msbTargetsConfig.transform((_, v) => buildContext(v))

  private def buildContext(cfg: Config): MsbContext = {
    val ctx = new MsbContextBuilder()
      .withConfig(cfg)
      .enableShutdownHook(true)
      .withPayloadMapper(objectMapper)
      .build()
    system.registerOnTermination { ctx.shutdown() }
    ctx
  }
}

object Msb extends ExtensionId[MsbImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MsbImpl = new MsbImpl(system)
  override def lookup = Msb
}