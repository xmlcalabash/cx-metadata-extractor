package com.xmlcalabash.ext.metadataextractor

import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import com.xmlcalabash.runtime.{BinaryNode, StaticContext, XProcMetadata, XmlPortSpecification}
import com.xmlcalabash.steps.DefaultXmlStep
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.s9api.{QName, XdmNode, XdmValue}

import java.nio.charset.StandardCharsets
import scala.collection.mutable
import scala.jdk.CollectionConverters.CollectionHasAsScala

class MetadataExtractor extends DefaultXmlStep {
  private val _assert_metadata = new QName("", "assert-metadata")

  override def inputSpec: XmlPortSpecification = XmlPortSpecification.ANYSOURCE
  override def outputSpec: XmlPortSpecification = XmlPortSpecification.XMLRESULTSEQ

  private var bytes = Option.empty[Array[Byte]]
  private var meta: XProcMetadata = _

  override def receive(port: String, item: Any, metadata: XProcMetadata): Unit = {
    meta = metadata
    item match {
      case bin: BinaryNode =>
        bytes = Some(bin.bytes)
      case node: XdmNode =>
        if (meta.contentType.textContentType) {
          bytes = Some(node.getStringValue.getBytes(StandardCharsets.UTF_8))
        }
      case _ => ()
    }
  }

  override def run(context: StaticContext): Unit = {
    super.run(context)

    val assert = booleanBinding(_assert_metadata).getOrElse(false)

    val properties = mutable.HashMap.empty[QName, XdmValue] ++ meta.properties

    val stepProperties = mapBinding(XProcConstants._properties)
    for (name <- stepProperties.keySet().asScala) {
      properties.put(name.getQNameValue, stepProperties.get(name))
    }

    val impl = if (bytes.isDefined) {
      new MetadataExtractorImpl(config.processor, meta, properties.toMap, bytes.get)
    } else {
      new MetadataExtractorImpl(config.processor, meta, properties.toMap)
    }

    try {
      val result = impl.extract()
      consumer.get.receive("result", result, new XProcMetadata(MediaType.XML, meta))
    } catch {
      case ex: MetadataException =>
        if (assert) {
          throw ex
        } else {
          logger.info(s"Failed to load metadata: ${ex.getMessage}")
        }
      case ex: Throwable =>
        throw ex
    }
 }
}
