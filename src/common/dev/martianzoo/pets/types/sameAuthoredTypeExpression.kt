package dev.martianzoo.pets.types

import dev.martianzoo.pets.ast.Expression

/** Whether two parsed Type expressions have the same whitespace-normalized authored spelling. */
internal fun Expression.sameAuthoredTypeExpressionAs(that: Expression): Boolean =
    toString() == that.toString()

/** Whether two header Types have the same authored structure apart from variable-name syntax. */
internal fun Expression.sameUnnamedTypeExpressionAs(that: Expression): Boolean =
    withoutTypeVariableNames().toString() == that.withoutTypeVariableNames().toString()

private fun Expression.withoutTypeVariableNames(): Expression =
    copy(
        arguments = arguments.map(Expression::withoutTypeVariableNames),
        typeVariableName = null,
    )
