package io.github.tcdl.msb

import akka.actor.{ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension}
import com.typesafe.config.Config

class MsbConfigImpl(config: Config) extends Extension {
  import scala.collection.JavaConversions._

  private val msbConfigPath: Option[String] = if (config.hasPath("msbConfigPath")) Some(config.getString("msbConfigPath")) else None

  val msbConfig = if (msbConfigPath.isDefined) config.getConfig(msbConfigPath.get).withFallback(config) else config
  val msbTargetsConfig = msbConfig.getConfigList("msbTargets").map(cfg => cfg.getString("targetId") -> cfg.withFallback(config)).toMap
}

object MsbConfig extends ExtensionId[MsbConfigImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MsbConfigImpl = new MsbConfigImpl(system.settings.config)
  override def lookup() = MsbConfig
}


