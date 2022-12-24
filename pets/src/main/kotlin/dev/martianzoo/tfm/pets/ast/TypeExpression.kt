package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.classNamePattern
import dev.martianzoo.tfm.pets.te
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
//    val discriminator: Int? = null
    ) : PetsNode() {
  constructor(className: String, vararg specialization: TypeExpression) :
      this(className, specialization.toList())
  init {
    require(className.matches(classNamePattern())) { className }
    if (className == "Class") {
      require(specializations.size == 1) { specializations }
      require(specializations.first().isClassOnly())
    }
  }

  fun isClassOnly() = (this == te(className))

  override fun toString() =
      className +
      specializations.joinOrEmpty(surround = "<>") +
      (requirement?.let { "(HAS $it)" } ?: "")

  override val children = listOfNotNull(requirement) + specializations
}
