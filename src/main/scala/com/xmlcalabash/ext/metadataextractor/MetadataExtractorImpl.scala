package com.xmlcalabash.ext.metadataextractor

import com.drew.imaging.jpeg.{JpegMetadataReader, JpegProcessingException}
import com.drew.metadata.Metadata
import com.xmlcalabash.model.util.{SaxonTreeBuilder, XProcConstants}
import com.xmlcalabash.runtime.XProcMetadata
import com.xmlcalabash.util.{MediaType, TypeUtils}
import net.sf.saxon.om.{AttributeMap, EmptyAttributeMap, NamespaceMap}
import net.sf.saxon.s9api.{Processor, QName, XdmNode, XdmValue}
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException
import org.apache.xmpbox.XMPMetadata
import org.apache.xmpbox.`type`.{ArrayProperty, Cardinality, DateType, MIMEType, TextType}
import org.apache.xmpbox.xml.DomXmpParser

import java.awt.image.ImageObserver
import java.awt.{Image, Toolkit}
import java.io._
import java.text.SimpleDateFormat
import java.util.{StringTokenizer, TimeZone}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

class MetadataExtractorImpl(processor: Processor, metadata: XProcMetadata, properties: Map[QName,XdmValue]) {
  private val c_tag = new QName("c", XProcConstants.ns_c, "tag")
  private val _dir = new QName("", "dir")
  private val _type = new QName("", "type")
  private val _name = new QName("", "name")
  private val _password = new QName("", "password")
  private val HEADLESS = "java.awt.headless"

  private val controls = Array(
    "0000", "0001", "0002", "0003", "0004", "0005", "0006", "0007",
    "0008",                 "000b", "000c",         "000e", "000f",
    "0010", "0011", "0012", "0013", "0014", "0015", "0016", "0017",
    "0018", "0019", "001a", "001b", "001c", "001d", "001e", "001f",
    "007c")

  private val APPLICATION_PDF = new MediaType("application", "pdf")

  private val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
  df.setTimeZone(TimeZone.getTimeZone("UTC"))

  private var bytes = Option.empty[Array[Byte]]
  private var builder: SaxonTreeBuilder = _

  def this(processor: Processor, meta: XProcMetadata, properties: Map[QName,XdmValue], bytes: Array[Byte]) {
    this(processor, meta, properties)
    this.bytes = Some(bytes)
  }

  def extract(): XdmNode = {
    builder = new SaxonTreeBuilder(processor)
    builder.startDocument(metadata.baseURI)

    if (bytes.isDefined) {
      try {
        if (metadata.contentType == APPLICATION_PDF) {
          extractPdf()
        } else {
          val metadata = JpegMetadataReader.readMetadata(new ByteArrayInputStream(bytes.get))
          extractJpeg(metadata)
        }
      } catch {
        case _: JpegProcessingException =>
          extractIntrinsics()
        case ex: Throwable =>
          throw ex
      }
    } else {
      throw MetadataException.noData()
    }

    builder.endDocument()
    builder.result
  }

  private def extractPdf(): Unit = {
    // There's a lot more metadata that could be in a PDF file...this is just a cursory skim

    val _pages = new QName("", "pages")
    val _width = new QName("", "width")
    val _height = new QName("", "height")
    val _units = new QName("", "units")

    val password = if (properties.contains(_password)) {
      properties(_password).getUnderlyingValue.getStringValue
    } else {
      ""
    }

    val document = try {
      PDDocument.load(bytes.get, password)
    } catch {
      case ex: InvalidPasswordException =>
        throw MetadataException.noPassword(ex, "Incorrect password")
      case ex: Throwable =>
        throw ex
    }

    val firstPage = if (document.getNumberOfPages > 0) {
      Some(document.getPage(0))
    } else {
      None
    }

    val pfxMap = mutable.HashMap.empty[String,String]
    val nsMap = mutable.HashMap.empty[String,String]
    var xmpmeta = Option.empty[XMPMetadata]

    val catalog = document.getDocumentCatalog
    val meta = Option(catalog.getMetadata)
    if (meta.isDefined) {
      val xmpParser = new DomXmpParser()
      xmpmeta = Some(xmpParser.parse(meta.get.createInputStream()))
      for (schema <- xmpmeta.get.getAllSchemas.asScala) {
        for (prop <- schema.getAllProperties.asScala) {
          val prefix = computePrefix(prop.getPrefix, prop.getNamespace, pfxMap, nsMap)
          pfxMap += (prefix -> prop.getNamespace)
          nsMap += (prop.getNamespace -> prefix)
        }
      }
    }

    var amap: AttributeMap = EmptyAttributeMap.getInstance()
    var nsmap = NamespaceMap.emptyMap()
    for (pfx <- pfxMap.keySet) {
      nsmap = nsmap.put(pfx, pfxMap(pfx))
    }

    amap = amap.put(TypeUtils.attributeInfo(XProcConstants._content_type, metadata.contentType.toString))
    if (metadata.baseURI.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(XProcConstants._base_uri, metadata.baseURI.get.toString))
    }
    amap = amap.put(TypeUtils.attributeInfo(_pages, document.getNumberOfPages.toString))

    if (firstPage.isDefined) {
      amap = amap.put(TypeUtils.attributeInfo(_height, firstPage.get.getMediaBox.getHeight.toString))
      amap = amap.put(TypeUtils.attributeInfo(_width, firstPage.get.getMediaBox.getWidth.toString))
      amap = amap.put(TypeUtils.attributeInfo(_units, "pt"))
    }

    builder.addStartElement(XProcConstants.c_result, amap, nsmap)

    if (meta.isDefined) {
      val xmpParser = new DomXmpParser()
      val metadata = xmpParser.parse(meta.get.createInputStream())
      for (schema <- metadata.getAllSchemas.asScala) {
        for (prop <- schema.getAllProperties.asScala) {
          val pfx = nsMap(prop.getNamespace)
          builder.addStartElement(new QName(pfx, prop.getNamespace, prop.getPropertyName))

          prop match {
            case p: ArrayProperty =>
              val outer = if (p.getArrayType == Cardinality.Alt) {
                new QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Alt")
              } else {
                new QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "Seq")
              }
              val inner = new QName("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#", "li")

              builder.addStartElement(outer)
              for (value <- p.getElementsAsString.asScala) {
                builder.addStartElement(inner)
                builder.addText(value)
                builder.addEndElement()
              }
              builder.addEndElement()
            case p: MIMEType =>
              builder.addText(p.getStringValue)
            case p: TextType =>
              builder.addText(p.getStringValue)
            case p: DateType =>
              val cal = p.getValue
              builder.addText(df.format(cal.getTime))
            case _ =>
              println(s"cx:metadata-extractor: unknown property type: $prop")
              builder.addText(prop.toString)
          }

          builder.addEndElement()
        }
      }
    }

    document.close()
    builder.addEndElement()
  }

  private def computePrefix(pfx: String, ns: String, pfxMap: mutable.HashMap[String,String], nsMap: mutable.HashMap[String,String]): String = {
    if (nsMap.contains(ns)) {
      nsMap(ns)
    } else if (!pfxMap.contains(pfx)) {
      pfx
    } else {
      val cpfx = "ns_"
      var count = 1
      while (pfxMap.contains(s"$cpfx$count")) {
        count += 1
      }
      s"$cpfx$count"
    }
  }

  private def extractJpeg(metadata: Metadata): Unit = {
    builder.addStartElement(XProcConstants.c_result)

    for (directory <- metadata.getDirectories.asScala) {
      val dir = directory.getName
      for (tag <- directory.getTags.asScala) {
        var attr: AttributeMap = EmptyAttributeMap.getInstance()
        attr = attr.put(TypeUtils.attributeInfo(_dir, dir))
        attr = attr.put(TypeUtils.attributeInfo(_type, tag.getTagTypeHex))
        attr = attr.put(TypeUtils.attributeInfo(_name, tag.getTagName))
        builder.addStartElement(c_tag, attr)

        var value = tag.getDescription

        // Laboriously escape all the control characters with \\uxxxx, but first replace
        // \\uxxxx with \\u005cuxxxx so we don't inadvertantly change the meaning of a string
        value = value.replaceAll("\\\\u([0-9a-fA-F]{4}+)", "\\\\u005cu$1")
        for (control <- controls) {
          val rematch = s"^.*\\\\u${control}.*$$"
          if (value.matches(rematch)) {
            value = value.replaceAll(s"[\\\\u${control}]", s"\\\\u${control}")
          }
        }

        // Bah humbug. I don't see any way to tell if it's a date/time
        if (value.matches("^\\d\\d\\d\\d:\\d\\d:\\d\\d \\d\\d:\\d\\d:\\d\\d$")) {
          val iso = s"${value.substring(0,4)}-${value.substring(5,7)}-${value.substring(8,10)}T${value.substring(11,19)}"
          value = iso
        }

        builder.addText(value)
        builder.addEndElement()
      }
    }
    builder.addEndElement()
  }

  private def extractIntrinsics(): Unit = {
    val headless = System.getProperty(HEADLESS)
    System.setProperty(HEADLESS, "true")

    val temp = File.createTempFile("xmlcalabash-",".bin")
    temp.deleteOnExit()
    val writer = new FileOutputStream(temp)
    writer.write(bytes.get)
    writer.close()

    builder.addStartElement(XProcConstants.c_result)

    val intrinsics = new ImageIntrinsics()
    intrinsics.run(temp)

    builder.addEndElement()

    temp.delete()

    if (headless == null) {
      System.clearProperty(HEADLESS)
    } else {
      System.setProperty(HEADLESS, headless)
    }
  }

  private class ImageIntrinsics extends ImageObserver {
    private var imageFailed = false
    private var width = -1
    private var height = -1

    def run(data: File): Unit = {
      val image = Toolkit.getDefaultToolkit.getImage(data.getAbsolutePath)
      while (!imageFailed && (width < 0 || height < 0)) {
        try {
          Thread.sleep(50)
        } catch {
          case _: Throwable =>
            ()
        }
        // Do something to get the image loading
        image.getWidth(this)
      }
      image.flush()

      if ((width < 0 || height < 0) && imageFailed) {
        // Maybe it's an EPS or a PDF? Do a crude search for the size
        var ir: BufferedReader = null
        try {
          var limit = 100
          ir = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bytes.get)))
          var line = ir.readLine()

          if (line != null && line.startsWith("%PDF-")) { // We have a PDF!
            while (limit > 0 && line != null) {
              limit -= 1
              if (line.startsWith("/CropBox [")) {
                line = line.substring(10)
                if (line.indexOf("]") >= 0) {
                  line = line.substring(0, line.indexOf("]"))
                }
                parseBox(line)
                limit = 0
              }
              line = ir.readLine()
            }
          } else if (line != null && line.startsWith("%!") && line.contains(" EPSF-")) { // We've got an EPS!
            while (limit > 0 && line != null) {
              limit -= 1
              if (line.startsWith("%%BoundingBox: ")) {
                line = line.substring(15)
                parseBox(line)
                limit = 0
              }
              line = ir.readLine()
            }
          } else {
            throw MetadataException.noMetadata(s"Failed to interpret image: ${metadata.baseURI.getOrElse("(unknown)")}")
          }
        } catch {
          case ex: Throwable =>
            throw MetadataException.noMetadata(ex, s"Failed to load image: ${metadata.baseURI.getOrElse("(unknown)")}")
        }

        if (ir != null) {
          try {
            ir.close()
          } catch {
            case _: Throwable =>
              ()
          }
        }
      }

      if (width > 0) {
        var attr: AttributeMap = EmptyAttributeMap.getInstance()
        attr = attr.put(TypeUtils.attributeInfo(_dir, "Exif"))
        attr = attr.put(TypeUtils.attributeInfo(_type, "0x9000"))
        attr = attr.put(TypeUtils.attributeInfo(_name, "Exif Version"))
        builder.addStartElement(c_tag, attr)
        builder.addText("0")
        builder.addEndElement()

        attr = EmptyAttributeMap.getInstance()
        attr = attr.put(TypeUtils.attributeInfo(_dir, "Jpeg"))
        attr = attr.put(TypeUtils.attributeInfo(_type, "0x0001"))
        attr = attr.put(TypeUtils.attributeInfo(_name, "Image Height"))
        builder.addStartElement(c_tag, attr)
        builder.addText(s"${height} pixels")
        builder.addEndElement()

        attr = EmptyAttributeMap.getInstance()
        attr = attr.put(TypeUtils.attributeInfo(_dir, "Jpeg"))
        attr = attr.put(TypeUtils.attributeInfo(_type, "0x0003"))
        attr = attr.put(TypeUtils.attributeInfo(_name, "Image Width"))
        builder.addStartElement(c_tag, attr)
        builder.addText(s"${width} pixels")
        builder.addEndElement()
      } else {
        throw MetadataException.noMetadata(s"Failed to read image intrinsics: ${metadata.baseURI.getOrElse("(unknown)")}")
      }
    }

    def parseBox(line: String): Unit = {
      val corners = new Array[Int](4)
      var count = 0
      var fail = false
      val st = new StringTokenizer(line)
      while (!fail && count < 4 && st.hasMoreTokens) {
        try {
          corners(count) = st.nextToken().toInt
          count += 1
        } catch {
          case _: Throwable =>
            fail = true
        }
      }

      if (!fail) {
        width = corners(2) - corners(0)
        height = corners(3) - corners(1)
      }
    }

    override def imageUpdate(img: Image, infoflags: Int, x: Int, y: Int, width: Int, height: Int): Boolean = {
      val error = (infoflags & ImageObserver.ERROR) == ImageObserver.ERROR
      val abort = (infoflags & ImageObserver.ABORT) == ImageObserver.ABORT
      if (error || abort) {
        imageFailed = true
        return false
      }

      if ((infoflags & ImageObserver.WIDTH) == ImageObserver.WIDTH) {
        this.width = width
      }

      if ((infoflags & ImageObserver.HEIGHT) == ImageObserver.HEIGHT) {
        this.height = height
      }

      // I really only care about the width and height, but if I return false as
      // soon as those are available, the BufferedInputStream behind the loader
      // gets closed too early.
      val allbits = (infoflags & ImageObserver.ALLBITS) == ImageObserver.ALLBITS
      !allbits
    }
  }
}
