package dev.martianzoo.tfm.pets.ast

import dev.martianzoo.util.toSetStrict

public interface HasExpression {
  val expression: Expression
  val expressionFull: Expression
}

fun Iterable<HasExpression>.expressions(): List<Expression> = map { it.expression }

fun Sequence<HasExpression>.expressions(): Sequence<Expression> = map { it.expression }

fun Set<HasExpression>.expressions(): Set<Expression> = toSetStrict { it.expression }
