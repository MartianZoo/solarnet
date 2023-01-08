package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.CLASS_NAME_PATTERN
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import dev.martianzoo.util.joinOrEmpty

sealed class FromExpression : PetsNode() {
  override val kind = "FromExpression"

  abstract val toType: TypeExpression
  abstract val fromType: TypeExpression

  data class TypeInFrom(val type: TypeExpression) : FromExpression() {
    override val toType = type
    override val fromType = type

    override fun toString() = "$type"
  }

  data class SimpleFrom(
      override val toType: TypeExpression,
      override val fromType: TypeExpression) : FromExpression() {

    override fun toString() = "$toType FROM $fromType"
  }

  data class ComplexFrom(
      val className: String,
      val specializations: List<FromExpression> = listOf(),
      val refinement: Requirement? = null) : FromExpression() {
    init {
      require(className.matches(Regex(CLASS_NAME_PATTERN))) { className }
      if (specializations.count { it is ComplexFrom || it is SimpleFrom } != 1) {
        throw PetsException("Can only have one FROM in an expression")
      }
    }
    override val toType = gte(className, specializations.map { it.toType }, null)
    override val fromType = gte(className, specializations.map { it.fromType }, refinement)

    override fun toString() =
        className +
        specializations.joinOrEmpty(wrap="<>") +
        (refinement?.let { "(HAS $it)" } ?: "")
  }
}
