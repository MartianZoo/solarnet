package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class ClassName(val asString: String) : PetNode(), Comparable<ClassName> {
  init {
    require(asString.matches(classNameRegex())) { "Bad class name: $asString" }
  }

  val literal = ClassLiteral(this)
  val baseType = GenericTypeExpression(this)

  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  override fun toString() = asString
  fun matches(regex: Regex) = asString.matches(regex)

  override val kind = "ClassName"
}

// TODO this weird
const val CLASS_NAME_PATTERN = "\\b[A-Z][a-z][A-Za-z0-9_]*\\b"
val thing by lazy { Regex(CLASS_NAME_PATTERN) }
fun classNameRegex() = thing
