package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.tfm.pets.CLASS_NAME_PATTERN
import dev.martianzoo.tfm.pets.PetsException
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
      val requirement: Requirement? = null) : FromExpression() {
    init {
      require(className.matches(Regex(CLASS_NAME_PATTERN))) { className }
      if (specializations.count { it is ComplexFrom || it is SimpleFrom } != 1) {
        throw PetsException("Can only have one FROM in an expression")
      }
    }
    override val toType = TypeExpression(className, specializations.map { it.toType })
    override val fromType = TypeExpression(className, specializations.map { it.fromType })

    override fun toString() =
        className +
            specializations.joinOrEmpty(wrap="<>") +
            (requirement?.let { "(HAS $it)" } ?: "")
  }
}
