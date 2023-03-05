package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.util.toSetStrict

public interface HasExpression {
  val expression: Expression // TODO maybe rename expressionMinimal
  val expressionFull: Expression
}

fun Iterable<HasExpression>.expressions(): List<Expression> = map { it.expression }

fun Sequence<HasExpression>.expressions(): Sequence<Expression> = map { it.expression }

fun Set<HasExpression>.expressions(): Set<Expression> = map { it.expression }.toSetStrict()
