package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
private val classNameRegex = Regex(CLASS_NAME_PATTERN)

data class ClassName(val string: String) : PetNode(), Comparable<ClassName> {
  companion object {
    fun cn(name: String) = ClassName(name)
  }

  init {
    require(string.matches(classNameRegex)) { "Bad class name: $string" }
  }

  val type = GenericTypeExpression(this)
  val literal = ClassLiteral(this)

  fun addArgs(specs: List<TypeExpression>) = type.addArgs(specs)
  fun addArgs(vararg specs: TypeExpression) = addArgs(specs.toList())

  @JvmName("addArgsFromClassNames")
  fun addArgs(specs: List<ClassName>) = addArgs(specs.map { it.type })
  fun addArgs(vararg specs: ClassName) = addArgs(specs.toList())

  fun matches(regex: Regex) = string.matches(regex)

  override fun toString() = string
  override fun compareTo(other: ClassName) = string.compareTo(other.string)

  override val kind = "ClassName"
}
