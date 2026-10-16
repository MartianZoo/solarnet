package dev.martianzoo.search

import kotlinx.serialization.Serializable

/**
 * A selected XML node or a scalar XPath result. [xml] is present for element/document nodes only
 * when requested through [CanonQuery.evaluate]; [value] is the XPath string value. [name]
 * identifies a selected Card/Class; [owner] identifies the containing Card/Class for a nested node.
 * [pets] is its normalized Pets spelling, if any.
 */
@Serializable
public data class XPathMatch(
    public val value: String,
    public val xml: String? = null,
    public val name: String? = null,
    public val owner: String? = null,
    public val pets: String? = null,
) {
  /** Human-readable card name, Pets evidence with its owner, or scalar value. */
  public fun describe(): String =
      name
          ?: if (pets == null && xml == null) value
          else listOfNotNull(owner, pets ?: xml).joinToString("\t")
}
