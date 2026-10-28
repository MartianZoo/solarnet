package dev.martianzoo.search

import dev.martianzoo.pets.PetElaborator
import dev.martianzoo.pets.ast.PetNode
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import dev.martianzoo.pets.ast.PropertyValue.RequirementValue
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.canon.cardBack
import dev.martianzoo.tfm.canon.cardResourceType
import dev.martianzoo.tfm.canon.cardTags

/**
 * XPath 1.0 queries over a [TfmCatalog], on JVM or in a JS/Wasm browser. Choose [form] explicitly;
 * reuse this object to reuse its lazily constructed XML document. The catalog remains the source of
 * truth, and results describe its syntax rather than executing game instructions.
 *
 * For example, `evaluate("/Canon/Card[@cost <= 10]/@name")` returns matching card names as values.
 * See `docs/agents/canon-xpath.md` for the complete mapping and the differences between forms.
 */
public class CanonQuery(public val form: Form, catalog: TfmCatalog = Canon) {
  /** Which instruction representation the query sees. */
  public enum class Form {
    /** Declared actions and effects, retaining PROD transforms. */
    AUTHORED,
    /** Compiled class effects, including inherited effects, lowered production and actions. */
    ELABORATED,
  }

  private val document: XPathDocument by lazy { XPathDocument(xml) }

  /** XML projection of the selected catalog; built only when first queried or exported. */
  public val xml: String by lazy {
    val canon = XmlElement("Canon")
    canon.setAttribute("form", form.name.lowercase())
    val xml = PetsXml()
    val table = catalog.classTable
    val cards = catalog.cards.toSet()
    val elaborator = PetElaborator(table)
    val transforms = table.transformDispatcher()
    fun selected(node: PetNode): PetNode =
        if (form == Form.AUTHORED) node else transforms.transformWithoutKindCheck(node)
    table
        .allClasses()
        .sortedBy { it.className.toString() }
        .forEach { klass ->
          val declaration = klass.declaration
          val card = klass in cards
          val element =
              xml.element(
                  canon,
                  if (card) "Card" else "Class",
                  mapOf(
                      "id" to klass.className,
                      "name" to klass.className,
                      "abstract" to klass.abstract,
                  ),
              )
          val hierarchy = xml.element(element, "isA")
          klass
              .allSuperclasses()
              .sortedBy { it.className.toString() }
              .forEach {
                xml.element(hierarchy, "Type", mapOf("name" to it.className))
              }
          if (card) {
            element.setAttribute("hasPersistentBehavior", hasPersistentBehavior(klass).toString())
            cardBack(klass)?.let { element.setAttribute("deck", it.className.toString()) }
            cardResourceType(klass)?.let { element.setAttribute("resource", it.toString()) }
            listOf("EventCard", "ActiveCard", "AutomatedCard")
                .firstOrNull { name ->
                  klass.allSuperclasses().any { it.className.toString() == name }
                }
                ?.let { element.setAttribute("category", it) }
            val tags = xml.element(element, "tags")
            cardTags(klass)
                .elements
                .sortedBy { it.toString() }
                .forEach { tag ->
                  xml.element(
                      tags,
                      "Tag",
                      mapOf("name" to tag, "count" to cardTags(klass).count(tag)),
                  )
                }
          }
          klass.properties.forEach { (name, value) ->
            if (name.toString() == "cost" && value is NumberValue)
                element.setAttribute("cost", value.value.toString())
            if (
                name.toString() in setOf("requirement", "autoSelectWhen") &&
                    value is RequirementValue
            ) {
              xml.node(xml.element(element, name.toString()), selected(value.value))
            }
          }
          xml.nodes(element, "supertypes", declaration.supertypes.map(::selected))
          xml.nodes(element, "dependencies", declaration.dependencies.map(::selected))
          xml.nodes(element, "invariants", declaration.invariants.map(::selected))
          xml.nodes(element, "effectiveInvariants", klass.invariants.map(::selected))
          val properties = xml.element(element, "properties")
          klass.properties.forEach { (name, value) ->
            xml.node(xml.element(properties, "Property", mapOf("name" to name)), selected(value))
          }
          val defaults = xml.element(element, "defaults")
          declaration.defaultsDeclaration.let { d ->
            listOf(
                    "universal" to d.universal,
                    "gainOnly" to d.gainOnly,
                    "triggerOnly" to d.triggerOnly,
                )
                .forEach { (name, value) ->
                  val target = xml.element(defaults, name, mapOf("quantifier" to value.quantifier))
                  value.specs.forEach { xml.node(target, selected(it)) }
                }
            xml.element(defaults, "removeOnly", mapOf("quantifier" to d.removalQuantifier))
          }
          xml.nodes(element, "extraNodes", declaration.extraNodes.map(::selected))
          if (form == Form.AUTHORED) {
            xml.nodes(element, "actions", declaration.authoredActions)
            xml.nodes(element, "effects", declaration.authoredEffects)
          } else {
            xml.nodes(element, "effects", elaborator.classEffects(klass))
          }
        }
    canon.toString()
  }

  /**
   * Evaluates XPath 1.0 against the document root. Returns nodes in document order, an empty list
   * when none match, or a single scalar result. Set [includeXml] to serialize matching element and
   * document nodes into [XPathMatch.xml]; otherwise no result XML is serialized. Invalid
   * expressions throw an evaluator exception.
   */
  public fun evaluate(xpath: String, includeXml: Boolean = false): List<XPathMatch> =
      document.evaluate(xpath, includeXml)
}
