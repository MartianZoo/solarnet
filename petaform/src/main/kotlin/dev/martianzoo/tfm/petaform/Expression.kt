package dev.martianzoo.tfm.petaform

import dev.martianzoo.tfm.petaform.api.classNamePattern
import dev.martianzoo.util.joinOrEmpty

/**
 * A component type expression in Petaform. This is parsed purely textually, with no knowledge of
 * the symbol table. In adding TfmObjects to a `ComponentTable`, all expressions are checked at
 * that time.
 */
data class Expression(
    val className: String,
    val specializations: List<Expression> = listOf(),
    val predicate: Predicate? = null) : PetaformNode() {
  constructor(className: String, vararg specialization: Expression) :
      this(className, specialization.toList())

  init {
    require(className.matches(classNamePattern())) { className }
  }

  override val children = listOfNotNull(predicate) + specializations


  override fun toString() =
      className +
      specializations.joinOrEmpty(surround = "<>") +
      (predicate?.let { "(HAS $it)" } ?: "")

}
