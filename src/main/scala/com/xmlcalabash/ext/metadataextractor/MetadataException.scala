package com.xmlcalabash.ext.metadataextractor

import com.jafpl.graph.Location
import com.xmlcalabash.exceptions.XProcException
import com.xmlcalabash.model.util.XProcConstants
import net.sf.saxon.s9api.QName

object MetadataException {
  val cx_no_metadata = new QName("cx", XProcConstants.ns_cx, "EXMETA01")
  val cx_no_password = new QName("cx", XProcConstants.ns_cx, "EXMETA02")
  val cx_no_data = new QName("cx", XProcConstants.ns_cx, "EXMETA03")
  def noMetadata(msg: String): MetadataException = new MetadataException(cx_no_metadata, Some(msg), None)
  def noMetadata(cause: Throwable, msg: String): MetadataException = new MetadataException(cx_no_metadata, Some(msg), Some(cause))
  def noPassword(cause: Throwable, msg: String): MetadataException = new MetadataException(cx_no_password, Some(msg), Some(cause))
  def noData(): MetadataException = new MetadataException(cx_no_password, None, None)
}

class MetadataException(override val code: QName,
                        override val message: Option[String],
                        val cause: Option[Throwable])
  extends XProcException(code, 1, message, None, List()) {
}
