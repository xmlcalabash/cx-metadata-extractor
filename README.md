# Metadata extractor step

This is an XML Calabash 3.x extension step. 

```
<p:declare-step type="cx:metadata-extractor">
  <p:input port="source" content-types="any"/>
  <p:output port="result" content-types="xml" sequence="true"/>
  <p:option name="assert-metadata" as="xs:boolean" select="false()"/>
  <p:option name="properties" as="map(xs:QName,item()*)"/>
</p:declare-step>
```

This step reads the source file and returns an XML document of metadata. It uses
Drew Noakes [Metadata Extractor](https://drewnoakes.com/code/exif/) on images,
falling back to simple image metadata from the Java AWT toolkit. It uses
It uses the [Apache PDFBox](https://pdfbox.apache.org/) library on PDF files.

To read password protected PDF files, the `password` property must be
provided in either the document properties or the `properties` option.
If it’s provided in both places, the step option value is used.

For example, `cx:metadata-extract` on [this image](https://github.com/xmlcalabash/cx-metadata-extractor/blob/main/src/test/resources/amaryllis.jpg):

![Photograph of an amaryllis](https://github.com/xmlcalabash/cx-metadata-extractor/blob/main/src/test/resources/amaryllis.jpg?raw=true)

Produces something like this (some of the metadata has been elided for the sake of appearance on this page):

```
<c:result xmlns:c="http://www.w3.org/ns/xproc-step">
   <c:tag dir="JPEG" type="0xfffffffd" name="Compression Type">Baseline</c:tag>
   <c:tag dir="JPEG" type="0x0000" name="Data Precision">8 bits</c:tag>
   <c:tag dir="JPEG" type="0x0001" name="Image Height">336 pixels</c:tag>
   <c:tag dir="JPEG" type="0x0003" name="Image Width">500 pixels</c:tag>
   <c:tag dir="JPEG" type="0x0005" name="Number of Components">3</c:tag>
   <c:tag dir="JPEG" type="0x0006" name="Component 1">Y component: Quantization table 0, Sampling factors 1 horiz/1 vert</c:tag>
   <c:tag dir="JPEG" type="0x0007" name="Component 2">Cb component: Quantization table 1, Sampling factors 1 horiz/1 vert</c:tag>
   <c:tag dir="JPEG" type="0x0008" name="Component 3">Cr component: Quantization table 1, Sampling factors 1 horiz/1 vert</c:tag>
   <c:tag dir="JFIF" type="0x0005" name="Version">1.1</c:tag>
   <c:tag dir="JFIF" type="0x0007" name="Resolution Units">inch</c:tag>
   <c:tag dir="JFIF" type="0x0008" name="X Resolution">72 dots</c:tag>
   <c:tag dir="JFIF" type="0x000a" name="Y Resolution">72 dots</c:tag>
   <c:tag dir="JFIF" type="0x000c" name="Thumbnail Width Pixels">0</c:tag>
   <c:tag dir="JFIF" type="0x000d" name="Thumbnail Height Pixels">0</c:tag>
   <c:tag dir="ICC Profile" type="0x0000" name="Profile Size">3144</c:tag>
   <c:tag dir="ICC Profile" type="0x0004" name="CMM Type">Lino</c:tag>
   <c:tag dir="ICC Profile" type="0x0008" name="Version">2.1.0</c:tag>
   <c:tag dir="ICC Profile" type="0x000c" name="Class">Display Device</c:tag>
   <c:tag dir="ICC Profile" type="0x0010" name="Color space">RGB </c:tag>
   <c:tag dir="ICC Profile" type="0x0014" name="Profile Connection Space">XYZ </c:tag>
   <c:tag dir="ICC Profile" type="0x0018" name="Profile Date/Time">1998-02-09T06:49:00</c:tag>
   <c:tag dir="ICC Profile" type="0x0024" name="Signature">acsp</c:tag>
   <c:tag dir="ICC Profile" type="0x0028" name="Primary Platform">Microsoft Corporation</c:tag>
   <c:tag dir="ICC Profile" type="0x0030" name="Device manufacturer">IEC </c:tag>
   <c:tag dir="ICC Profile" type="0x0034" name="Device model">sRGB</c:tag>
   <c:tag dir="ICC Profile" type="0x0044" name="XYZ values">0.964 1 0.825</c:tag>
   <c:tag dir="ICC Profile" type="0x0080" name="Tag Count">17</c:tag>
   <c:tag dir="ICC Profile" type="0x63707274" name="Profile Copyright">Copyright (c) 1998 Hewlett-Packard Company</c:tag>
   <c:tag dir="ICC Profile" type="0x64657363" name="Profile Description">sRGB IEC61966-2.1</c:tag>
   <c:tag dir="ICC Profile" type="0x77747074" name="Media White Point">(0.9505, 1, 1.0891)</c:tag>
   <c:tag dir="ICC Profile" type="0x626b7074" name="Media Black Point">(0, 0, 0)</c:tag>
   <c:tag dir="ICC Profile" type="0x7258595a" name="Red Colorant">(0.4361, 0.2225, 0.0139)</c:tag>
   <c:tag dir="ICC Profile" type="0x6758595a" name="Green Colorant">(0.3851, 0.7169, 0.0971)</c:tag>
   <c:tag dir="ICC Profile" type="0x6258595a" name="Blue Colorant">(0.1431, 0.0606, 0.7141)</c:tag>
   <c:tag dir="ICC Profile" type="0x646d6e64" name="Device Mfg Description">IEC http://www.iec.ch</c:tag>
   <c:tag dir="ICC Profile" type="0x646d6464" name="Device Model Description">IEC 61966-2.1 Default RGB colour space - sRGB</c:tag>
   <c:tag dir="ICC Profile"
          type="0x76756564"
          name="Viewing Conditions Description">Reference Viewing Condition in IEC61966-2.1</c:tag>
   <c:tag dir="ICC Profile" type="0x76696577" name="Viewing Conditions">view (0x76696577): 36 bytes</c:tag>
   <c:tag dir="ICC Profile" type="0x6c756d69" name="Luminance">(76.0365, 80, 87.1246)</c:tag>
   <c:tag dir="ICC Profile" type="0x6d656173" name="Measurement">1931 2° Observer, Backing (0, 0, 0), Geometry Unknown, Flare 1%, Illuminant D65</c:tag>
   <c:tag dir="ICC Profile" type="0x74656368" name="Technology">CRT </c:tag>

   <c:tag dir="Huffman" type="0x0001" name="Number of Tables">4 Huffman tables</c:tag>
</c:result>
```

The underlying metadata extractor library doesn’t operate on PNG files,
so the step returns substantially less information:

```
<c:result xmlns:c="http://www.w3.org/ns/xproc-step">
   <c:tag dir="Exif" type="0x9000" name="Exif Version">0</c:tag>
   <c:tag dir="Jpeg" type="0x0001" name="Image Height">336 pixels</c:tag>
   <c:tag dir="Jpeg" type="0x0003" name="Image Width">500 pixels</c:tag>
</c:result>
```

The Exif version of 0 is used to indicate that only the intrinsic
metadata was available.

Finally, for completeness, here’s a PDF example. This is the output from the
[document](https://github.com/xmlcalabash/cx-metadata-extractor/blob/main/src/test/resources/document.pdf) PDF which is protected with the password “this is sekrit”.

```
<cx:metadata-extractor properties="map { 'password': 'this is sekrit' }">
  <p:with-input>
    <p:document href="src/test/resources/document.pdf"/>
  </p:with-input>
</cx:metadata-extractor>
```

returns

```
<c:result xmlns:dc="http://purl.org/dc/elements/1.1/"
          xmlns:pdf="http://ns.adobe.com/pdf/1.3/"
          xmlns:xmp="http://ns.adobe.com/xap/1.0/"
          xmlns:xmpMM="http://ns.adobe.com/xap/1.0/mm/"
          content-type="application/pdf"
          base-uri="file:/Users/ndw/Projects/xproc/ext-metadata-extractor/src/test/resources/document.pdf"
          pages="1"
          height="792.0"
          width="612.0"
          units="pt">
   <xmp:ModifyDate>2021-10-18T07:43:48Z</xmp:ModifyDate>
   <xmp:CreateDate>2018-10-19T00:22:35Z</xmp:CreateDate>
   <xmp:MetadataDate>2021-10-18T07:43:48Z</xmp:MetadataDate>
   <xmp:CreatorTool>Word</xmp:CreatorTool>
   <xmpMM:DocumentID>uuid:3f2a0902-b27b-11b2-0a00-69983c647348</xmpMM:DocumentID>
   <xmpMM:InstanceID>uuid:3f2a0a50-b27b-11b2-0a00-e3a9e27946e1</xmpMM:InstanceID>
   <dc:format>application/pdf</dc:format>
   <dc:title>
      <rdf:Alt xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
         <rdf:li>Document1</rdf:li>
      </rdf:Alt>
   </dc:title>
   <dc:creator>
      <rdf:Seq xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#">
         <rdf:li>Norman Walsh</rdf:li>
      </rdf:Seq>
   </dc:creator>
   <pdf:Producer>Mac OS X 10.13.6 Quartz PDFContext</pdf:Producer>
</c:result>
```
