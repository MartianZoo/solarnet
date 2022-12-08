package dev.martianzoo.tfm.petaform

import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in Petaform. This is parsed purely textually, with no knowledge of
 * the symbol table. In adding `PetaformNode`s to a `ComponentClassTable`, all expressions are checked at
 * that time.
 */
data class TypeExpression(
    val className: String,
    val specializations: List<TypeExpression> = listOf(),
    val predicate: Predicate? = null) : PetaformNode() {
  constructor(className: String, vararg specialization: TypeExpression) :
      this(className, specialization.toList())
  init { require(className.matches(classNamePattern())) { className } }
  override fun toString() =
      className +
      specializations.joinOrEmpty(surround = "<>") +
      (predicate?.let { "(HAS $it)" } ?: "")

  override val children = listOfNotNull(predicate) + specializations
}
