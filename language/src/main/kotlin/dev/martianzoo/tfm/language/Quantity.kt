package dev.martianzoo.tfm.language

import dev.martianzoo.pets.ast.ScaledExpression.Scalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.pets.ast.ScaledExpression.Scalar.XScalar

/** A renderer-facing quantity resolved from a Pets scalar. */
internal sealed interface Quantity {
  data class Fixed(val count: Int) : Quantity

  data class Variable(val multiple: Int) : Quantity {
    override fun toString(): String = if (multiple == 1) "X" else "${multiple}X"
  }
}

internal fun Scalar.quantity(): Quantity =
    when (this) {
      is ActualScalar -> Quantity.Fixed(value)
      is XScalar -> Quantity.Variable(multiple)
    }

internal fun Scalar.fixedQuantity(): Int? = (quantity() as? Quantity.Fixed)?.count

internal fun Scalar.variableQuantity(): Quantity.Variable? = quantity() as? Quantity.Variable
