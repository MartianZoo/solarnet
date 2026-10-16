package dev.martianzoo.search

import java.io.StringReader
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.jaxen.dom.DOMXPath
import org.jaxen.dom.DocumentNavigator
import org.jaxen.function.StringFunction
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

internal actual class XPathDocument actual constructor(xml: String) {
  private val document =
      DocumentBuilderFactory.newDefaultInstance()
          .apply {
            // Elaborated Canon contains large normalized Pets attributes; this XML is generated
            // locally.
            setAttribute("jdk.xml.maxGeneralEntitySizeLimit", "0")
            setAttribute("jdk.xml.totalEntitySizeLimit", "0")
          }
          .newDocumentBuilder()
          .parse(InputSource(StringReader(xml)))
          .apply {
            val elements = getElementsByTagName("*")
            for (i in 0 until elements.length) {
              val element = elements.item(i) as Element
              if (element.hasAttribute("id")) element.setIdAttribute("id", true)
            }
          }

  actual fun evaluate(xpath: String, includeXml: Boolean): List<XPathMatch> {
    val result = DOMXPath(xpath).evaluate(document)
    return when (result) {
      is List<*> -> result.map { match(it as Node, includeXml) }
      is Node -> listOf(match(result, includeXml))
      else -> listOf(XPathMatch(StringFunction.evaluate(result, DocumentNavigator.getInstance())))
    }
  }

  private fun match(node: Node, includeXml: Boolean): XPathMatch {
    val element = node as? Element
    val owner =
        generateSequence(
                if (node.nodeType == Node.ATTRIBUTE_NODE) (node as org.w3c.dom.Attr).ownerElement
                else node.parentNode
            ) {
              it.parentNode
            }
            .filterIsInstance<Element>()
            .firstOrNull { it.parentNode?.nodeName == "Canon" }
    val xml =
        if (includeXml && (element != null || node.nodeType == Node.DOCUMENT_NODE)) {
          val writer = StringWriter()
          TransformerFactory.newDefaultInstance()
              .newTransformer()
              .apply {
                setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
              }
              .transform(DOMSource(node), StreamResult(writer))
          writer.toString()
        } else null
    return XPathMatch(
        node.textContent.orEmpty(),
        xml,
        element
            ?.takeIf { it.parentNode?.nodeName == "Canon" }
            ?.getAttribute("name")
            ?.ifEmpty { null },
        owner?.getAttribute("name")?.ifEmpty { null },
        element?.getAttribute("pets")?.ifEmpty { null },
    )
  }
}
