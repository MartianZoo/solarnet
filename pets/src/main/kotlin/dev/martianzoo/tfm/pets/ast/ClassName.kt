package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

data class ClassName(private val asString: String) : PetNode(), Comparable<ClassName> {
  companion object {
    fun cn(name: String) = ClassName(name)

    private const val CLASS_NAME_PATTERN = "\\b[A-Z]([a-z][A-Za-z0-9_]*|[A-Z0-9]{0,4})\\b"
    private val classNameRegex = Regex(CLASS_NAME_PATTERN)
  }

  init {
    require(asString.matches(classNameRegex)) { "Bad class name: $asString" }
  }

  val type = GenericTypeExpression(this)
  val literal = ClassLiteral(this)

  fun addArgs(specs: List<TypeExpression>) = type.addArgs(specs)
  fun addArgs(vararg specs: TypeExpression) = addArgs(specs.toList())

  @JvmName("addArgsFromClassNames")
  fun addArgs(specs: List<ClassName>) = addArgs(specs.map { it.type })
  fun addArgs(vararg specs: ClassName) = addArgs(specs.toList())

  fun matches(regex: Regex) = asString.matches(regex)

  override fun toString() = asString
  override fun compareTo(other: ClassName) = asString.compareTo(other.asString)

  override val kind = "ClassName"
}
