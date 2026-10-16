package dev.martianzoo.search

import kotlinx.serialization.json.Json

internal actual class XPathDocument actual constructor(xml: String) {
  private val document = parseXml(xml)

  actual fun evaluate(xpath: String, includeXml: Boolean): List<XPathMatch> =
      Json.decodeFromString<List<XPathMatch>>(evaluateXml(document, xpath, includeXml))
}

private fun parseXml(xml: String): dynamic {
  val parse: dynamic =
      js(
          """((xml) => {
  const doc = new DOMParser().parseFromString(xml, "application/xml");
  if (doc.getElementsByTagName("parsererror").length) throw new Error("Invalid query XML");
  return doc;
})"""
      )
  return parse(xml)
}

private fun evaluateXml(document: dynamic, xpath: String, includeXml: Boolean): String {
  val evaluate: dynamic =
      js(
          """((doc, xpath, includeXml) => {
  const result = doc.evaluate(xpath, doc, null, XPathResult.ANY_TYPE, null);
  if (result.resultType === XPathResult.STRING_TYPE) return JSON.stringify([{value: result.stringValue}]);
  if (result.resultType === XPathResult.NUMBER_TYPE) {
    const text = doc.evaluate("string(" + xpath + ")", doc, null, XPathResult.STRING_TYPE, null);
    return JSON.stringify([{value: text.stringValue}]);
  }
  if (result.resultType === XPathResult.BOOLEAN_TYPE) return JSON.stringify([{value: String(result.booleanValue)}]);
  const ordered = doc.evaluate(xpath, doc, null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
  const matches = [];
  for (let i = 0; i < ordered.snapshotLength; i++) {
    const node = ordered.snapshotItem(i);
    let owner = node.nodeType === 2 ? node.ownerElement : node.parentNode;
    while (owner && (!owner.parentNode || owner.parentNode.nodeName !== "Canon")) owner = owner.parentNode;
    const element = node.nodeType === 1;
    matches.push({
      value: node.textContent || "",
      xml: includeXml && (element || node.nodeType === 9) ? new XMLSerializer().serializeToString(node) : null,
      name: element && node.parentNode && node.parentNode.nodeName === "Canon" ? node.getAttribute("name") : null,
      owner: owner ? owner.getAttribute("name") : null,
      pets: element ? node.getAttribute("pets") : null
    });
  }
  return JSON.stringify(matches);
})"""
      )
  return evaluate(document, xpath, includeXml)
}
