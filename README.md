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

