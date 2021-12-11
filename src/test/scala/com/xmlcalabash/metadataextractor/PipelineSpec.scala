package com.xmlcalabash.metadataextractor

import com.jafpl.messages.Message
import com.jafpl.steps.DataConsumer
import com.xmlcalabash.XMLCalabash
import com.xmlcalabash.messages.XdmNodeItemMessage
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.util.{PipelineOutputConsumer, S9Api}
import org.scalatest.flatspec.AnyFlatSpec

class PipelineSpec extends AnyFlatSpec {
  "Running pipe.xpl " should " produce XML metadata" in {
    val calabash = XMLCalabash.newInstance()
    val xml = new Consumer()
    val result = new PipelineOutputConsumer("result", xml)
    calabash.args.parse(List("src/test/resources/pipe.xpl"))
    calabash.parameter(result)
    calabash.configure()
    calabash.run()
    if (xml.message.isDefined) {
      xml.message.get match {
        case msg: XdmNodeItemMessage =>
          val root = S9Api.documentElement(msg.item)
          assert(root.isDefined)
          assert(root.get.getNodeName == XProcConstants.c_result)
        case _ => fail()
      }
    } else {
      fail()
    }
  }

  private class Consumer extends DataConsumer {
    var _message = Option.empty[Message]

    def message: Option[Message] = _message

    override def consume(port: String, message: Message): Unit = {
      _message = Some(message)
    }
  }
}
