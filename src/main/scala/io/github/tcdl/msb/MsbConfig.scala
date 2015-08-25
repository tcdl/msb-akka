package io.github.tcdl.msb

import akka.actor.{ExtendedActorSystem, ExtensionIdProvider, ExtensionId, Extension}
import com.typesafe.config.Config

class MsbConfigImpl(config: Config) extends Extension {

  private val msbConfigPath: Option[String] = if (config.hasPath("msbConfigPath")) Some(config.getString("msbConfigPath")) else None

  val msbConfig = if (msbConfigPath.isDefined) config.getConfig(msbConfigPath.get).withFallback(config) else config

}

object MsbConfig extends ExtensionId[MsbConfigImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): MsbConfigImpl = new MsbConfigImpl(system.settings.config)

  override def lookup() = MsbConfig
}


