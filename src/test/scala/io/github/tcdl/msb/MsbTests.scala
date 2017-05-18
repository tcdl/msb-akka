package io.github.tcdl.msb

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

object MsbTests {
  def actorSystem: ActorSystem = ActorSystem("msb-requester-test",
    config = ConfigFactory.parseString("""msbConfigPath = "msb-test-config"""").resolve()
  )
}
