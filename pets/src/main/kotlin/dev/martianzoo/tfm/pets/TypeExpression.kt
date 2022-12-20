package dev.martianzoo.tfm.pets

import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in PETS. This is parsed purely textually, with no knowledge of
 * the symbol table. In adding `PetsNode`s to a `ComponentClassTable`, all expressions are checked at
 * that time.
 */
data class TypeExpression(
    val className: String,
    val specializations: List<TypeExpression> = listOf(),
    val predicate: Predicate? = null,
//    val discriminator: Int? = null
    ) : PetsNode() {
  constructor(className: String, vararg specialization: TypeExpression) :
      this(className, specialization.toList())
  init { require(className.matches(classNamePattern())) { className } }
  override fun toString() =
      className +
      specializations.joinOrEmpty(surround = "<>") +
      (predicate?.let { "(HAS $it)" } ?: "")

  override val children = listOfNotNull(predicate) + specializations
}
