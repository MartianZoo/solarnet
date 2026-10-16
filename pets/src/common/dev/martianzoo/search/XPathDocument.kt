package dev.martianzoo.search

/** Platform XPath 1.0 evaluator over a reusable parsed XML document. */
internal expect class XPathDocument(xml: String) {
  fun evaluate(xpath: String, includeXml: Boolean = false): List<XPathMatch>
}
