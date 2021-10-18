package com.xmlcalabash.metadataextractor

import com.xmlcalabash.ext.metadataextractor.{MetadataException, MetadataExtractorImpl}
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.MediaType
import net.sf.saxon.s9api.{Processor, QName, XdmAtomicValue, XdmValue}
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec

import java.nio.file.{Files, Paths}
import scala.collection.mutable

class ExtractorSpec extends AnyFlatSpec with BeforeAndAfter {
  var processor: Processor = _

  before {
    processor = new Processor(false)
  }

  "Loading a JPEG " should " succeed" in {
    val meta = new XProcMetadata(MediaType.parse("image/jpeg"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/amaryllis.jpg"))
    val impl = new MetadataExtractorImpl(processor, meta, Map(), bytes)
    val xml = impl.extract().toString.replaceAll("\"", "'") // crude serialization

    assert(xml.startsWith("<c:result"))
    assert(xml.contains("dir='JPEG"))
    assert(xml.contains("336 pixels"))
    assert(xml.contains("500 pixels"))
  }

  "Loading a PDF " should " succeed" in {
    val meta = new XProcMetadata(MediaType.parse("application/pdf"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/amaryllis.pdf"))
    val impl = new MetadataExtractorImpl(processor, meta, Map(), bytes)
    val xml = impl.extract().toString.replaceAll("\"", "'") // crude serialization

    assert(xml.startsWith("<c:result"))
    assert(xml.contains("content-type='application/pdf'"))
    assert(xml.contains("height='336.0'"))
    assert(xml.contains("width='500.0'"))
  }

  "Loading a password protected PDF " should " fail without the password " in {
    val meta = new XProcMetadata(MediaType.parse("application/pdf"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/document.pdf"))
    val impl = new MetadataExtractorImpl(processor, meta, Map(), bytes)

    try {
      impl.extract().toString.replaceAll("\"", "'") // crude serialization
      fail()
    } catch {
      case ex: MetadataException =>
        assert(ex.code == MetadataException.cx_no_password)
      case _: Exception =>
        fail()
    }
  }

  "Loading a password protected PDF " should " succeed with the password " in {
    val meta = new XProcMetadata(MediaType.parse("application/pdf"))

    val props = mutable.HashMap.empty[QName, XdmValue]
    props.put(new QName("", "password"), new XdmAtomicValue("this is sekrit"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/document.pdf"))
    val impl = new MetadataExtractorImpl(processor, meta, props.toMap, bytes)
    val xml = impl.extract().toString.replaceAll("\"", "'") // crude serialization

    assert(xml.startsWith("<c:result"))
    assert(xml.contains("content-type='application/pdf'"))
    assert(xml.contains("<rdf:li>Norman Walsh"))
  }

  "Loading an EPS " should " succeed" in {
    val meta = new XProcMetadata(MediaType.parse("image/eps"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/amaryllis.eps"))
    val impl = new MetadataExtractorImpl(processor, meta, Map(), bytes)
    val xml = impl.extract().toString.replaceAll("\"", "'") // crude serialization

    assert(xml.startsWith("<c:result"))
    assert(xml.contains("Exif Version'>0</c:tag>"))
    assert(xml.contains("336 pixels"))
    assert(xml.contains("500 pixels"))
  }

  "Loading a BMP " should " fail to load metadata" in {
    val meta = new XProcMetadata(MediaType.parse("image/bmp"))

    val bytes = Files.readAllBytes(Paths.get("src/test/resources/amaryllis.bmp"))
    val impl = new MetadataExtractorImpl(processor, meta, Map(), bytes)
    try {
      impl.extract()
    } catch {
      case ex: MetadataException =>
        assert(ex.code == MetadataException.cx_no_metadata)
      case _: Throwable =>
        fail()
    }
  }
}
