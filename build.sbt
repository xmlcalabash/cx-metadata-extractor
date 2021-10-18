lazy val extMetadataExtractorVersion = "2.50.1"

name         := "metadata-extractor"
organization := "com.xmlcalabash"
homepage     := Some(url("https://xmlcalabash.com/"))
version      := extMetadataExtractorVersion
scalaVersion := "2.13.5"
//maintainer   := "ndw@nwalsh.com" // for packaging

resolvers += "Restlet" at "https://maven.restlet.com"

libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.32"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.6"
libraryDependencies += "com.xmlcalabash" % "xml-calabash_2.13" % "2.99.5"
libraryDependencies += "com.drewnoakes" % "metadata-extractor" % "2.16.0"
libraryDependencies += "org.apache.pdfbox" % "pdfbox" % "2.0.24"
libraryDependencies += "org.apache.pdfbox" % "xmpbox" % "2.0.24"
libraryDependencies += "javax.xml.bind" % "jaxb-api" % "2.3.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.3" % "test"

TaskKey[Unit]("myTask") := (Compile / runMain).toTask(" com.xmlcalabash.drivers.Main pipe.xpl").value