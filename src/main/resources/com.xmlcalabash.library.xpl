<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           xmlns:xs="http://www.w3.org/2001/XMLSchema"
           version="3.0">

<!-- I'm setting use-when=false here because technically you aren't
     allowed to automatically declare extension steps. -->
<p:declare-step type="cx:metadata-extractor" use-when="false()">
  <p:input port="source" content-types="any"/>
  <p:output port="result" content-types="xml" sequence="true"/>
  <p:option name="assert-metadata" as="xs:boolean" select="false()"/>
  <p:option name="properties" as="map(xs:QName,item()*)"/>
</p:declare-step>

</p:library>
