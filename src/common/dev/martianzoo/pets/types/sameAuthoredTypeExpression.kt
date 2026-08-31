package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.Expression

/** Whether two parsed Type expressions have the same whitespace-normalized authored spelling. */
internal fun Expression.sameAuthoredTypeExpressionAs(that: Expression): Boolean =
    toString() == that.toString()
