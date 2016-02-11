package io.github.tcdl.msb

import com.fasterxml.jackson.module.scala.DefaultScalaModule
import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import com.typesafe.config.Config
import io.github.tcdl.msb.api.{MsbContext, MsbContextBuilder}
import io.github.tcdl.msb.support.Utils

class MsbImpl(system: ActorSystem) extends Extension {

  // init jackson for marshalling the json body in the payload
  Utils.getJsonObjectMapper.registerModule(DefaultScalaModule)

  private val config = MsbConfig(system)

  val context = buildContext(config.msbConfig)
  val multiTargetContexts = config.msbTargetsConfig.mapValues(buildContext)

  private def buildContext(cfg: Config): MsbContext = {
    val ctx = new MsbContextBuilder()
      .withConfig(cfg)
      .withShutdownHook(true)
      .build()
    system.registerOnTermination { ctx.shutdown() }
    ctx
  }
}

object Msb extends ExtensionId[MsbImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MsbImpl = new MsbImpl(system)
  override def lookup = Msb
}