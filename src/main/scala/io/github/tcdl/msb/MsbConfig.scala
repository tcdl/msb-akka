package io.github.tcdl.msb

import akka.actor.{ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}
import com.typesafe.config.Config

import scala.concurrent.duration._

class MsbConfigImpl(config: Config) extends Extension {
  import scala.collection.JavaConversions._

  private val msbConfigPath: Option[String] = if (config.hasPath("msbConfigPath")) Some(config.getString("msbConfigPath")) else None

  val msbConfig = if (msbConfigPath.isDefined) config.getConfig(msbConfigPath.get).withFallback(config) else config
  val msbTargetsConfig = {
    if (msbConfig.hasPath("msbTargets"))
      msbConfig.getConfigList("msbTargets").map(cfg => cfg.getString("targetId") -> cfg.withFallback(config)).toMap
    else Map[String, Config]()
  }

  def responderConfig(target: Option[String] = None): ResponderConfig = {
    require(target.forall(msbTargetsConfig.contains), s"Target '${target.get}' is not a configured in the 'msbTargets' configuration.")
    val config = target.map(msbTargetsConfig(_)).getOrElse(msbConfig)
    new ResponderConfig(config)
  }

  def responderConfig(target: String): ResponderConfig = this.responderConfig(Some(target))
}

object MsbConfig extends ExtensionId[MsbConfigImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MsbConfigImpl = new MsbConfigImpl(system.settings.config)
  override def lookup() = MsbConfig
}

class ResponderConfig(msbConfig: Config) {
  val `request-handling-timeout`: FiniteDuration = msbConfig.getDuration("msbConfig.responder.request-handling-timeout").toMillis.millis
}