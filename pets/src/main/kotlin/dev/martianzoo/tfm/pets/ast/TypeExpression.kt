package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.CLASS_NAME_PATTERN
import dev.martianzoo.tfm.pets.SpecialComponent.CLASS
import dev.martianzoo.tfm.pets.SpecialComponent.THIS
import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in PETS. This is parsed purely textually, with no knowledge of
 * the symbol table. In adding `PetsNode`s to a `ComponentClassTable`, all expressions are checked at
 * that time.
 */
data class TypeExpression(
    val className: String,
    val specializations: List<TypeExpression> = listOf(),
    val requirement: Requirement? = null,
) : PetsNode() {
  constructor(className: String, vararg specialization: TypeExpression) :
      this(className, specialization.toList())
  init {
    require(className.matches(Regex(CLASS_NAME_PATTERN))) { className }
    if (className == "$CLASS") {
      require(specializations.size <= 1) { specializations }
      require(specializations.isEmpty() || specializations.first().isClassOnly())
    }
  }

  fun isClassOnly() = (this != THIS.type && this == te(className))

  override fun toString() =
      className +
      specializations.joinOrEmpty(surround = "<>") +
      (requirement?.let { "(HAS $it)" } ?: "")

  override val children = listOfNotNull(requirement) + specializations

  companion object {
    fun te(s: String) : TypeExpression = TypeExpression(s)
    fun te(s: String, first: TypeExpression, vararg rest: TypeExpression): TypeExpression = TypeExpression(s, listOf(first) + rest)
    fun te(s: String, specs: List<String>): TypeExpression = TypeExpression(s, specs.map(::te))
    fun te(s: String, first: String, vararg rest: String): TypeExpression {
      return te(s, listOf(first) + rest)
    }

  }
}
