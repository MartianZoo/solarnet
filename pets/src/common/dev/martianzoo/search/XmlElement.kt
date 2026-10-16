package dev.martianzoo.search

/** Temporary XML projection, shared by all platforms. */
internal class XmlElement(val name: String) {
  private val attributes = linkedMapOf<String, String>()
  private val children = mutableListOf<XmlElement>()

  fun setAttribute(name: String, value: String) {
    attributes[name] = value
  }

  fun appendChild(child: XmlElement) {
    children.add(child)
  }

  override fun toString(): String = buildString { appendXml(this) }

  private fun appendXml(out: StringBuilder) {
    out.append('<').append(name)
    attributes.forEach { (key, value) ->
      out.append(' ').append(key).append("=\"")
      value.forEach { c ->
        out.append(
            when (c) {
              '&' -> "&amp;"
              '<' -> "&lt;"
              '>' -> "&gt;"
              '"' -> "&quot;"
              '\n' -> "&#10;"
              '\r' -> "&#13;"
              '\t' -> "&#9;"
              else -> c.toString()
            }
        )
      }
      out.append('"')
    }
    if (children.isEmpty()) out.append("/>")
    else {
      out.append('>')
      children.forEach { it.appendXml(out) }
      out.append("</").append(name).append('>')
    }
  }
}
