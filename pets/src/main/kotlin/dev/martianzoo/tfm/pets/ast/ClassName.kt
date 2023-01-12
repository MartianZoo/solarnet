package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class ClassName(val asString: String) : PetNode(), Comparable<ClassName> {
  init {
    require(asString.matches(classNameRegex())) { "Bad class name: $asString" }
  }

  val literal = ClassLiteral(this)
  val type = GenericTypeExpression(this)

  fun specialize(specs: List<TypeExpression>) = type.specialize(specs)
  fun specialize(vararg specs: TypeExpression) = specialize(specs.toList())

  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  override fun toString() = asString
  fun matches(regex: Regex) = asString.matches(regex)

  override val kind = "ClassName"
}

// TODO this weird
const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
val thing by lazy { Regex(CLASS_NAME_PATTERN) }
fun classNameRegex() = thing
