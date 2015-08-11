package io.github.tcdl.msb

import io.github.tcdl.msb.api.{RequestOptions, MessageTemplate}
import org.scalatest.{Matchers, WordSpec}

class ConversionsTest extends WordSpec with Matchers {

  "A msb package object" when {
    "converting MsbRequestOptions" should {
      "convert all properties" in {
        val msbOptions = MsbRequestOptions(Some(1), Some(new MessageTemplate().withTtl(2)), Some(3), Some(4))

        val javaOptions : RequestOptions = msbOptions

        javaOptions.getAckTimeout should be(1)
        javaOptions.getMessageTemplate should equal(msbOptions.messageTemplate.get)
        javaOptions.getResponseTimeout should be(3)
        javaOptions.getWaitForResponses should be(4)
      }
    }
  }
}
