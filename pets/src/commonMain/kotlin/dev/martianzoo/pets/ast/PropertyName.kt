package dev.martianzoo.pets.ast

import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.PetTokenizer

/** A lower-camel-case name identifying one class property. */
public data class PropertyName(public val value: String) : PetNode(), Comparable<PropertyName> {
  public companion object {
    private val propertyNameRegex = Regex("[a-z][A-Za-z0-9]*")

    internal fun parser(): Parser<PropertyName> = Parsing.parser
  }

  init {
    require(value.matches(propertyNameRegex)) { "Bad property name: $value" }
  }

  override fun toString(): String = value

  override fun compareTo(other: PropertyName): Int = value.compareTo(other.value)

  override val kind: kotlin.reflect.KClass<out PetNode> = PropertyName::class

  override fun visitChildren(visitor: Visitor): Unit = Unit

  private object Parsing : PetTokenizer() {
    val parser: Parser<PropertyName> = _lowerCamelRE map { PropertyName(it.text) }
  }
}
