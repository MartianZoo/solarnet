package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.util.toSetStrict

/** Any object that can be represented in some way as an [Expression]. */
public interface HasExpression {
  /** This object as a minimal expression. */
  val expression: Expression

  /** This object as a full expression. */
  val expressionFull: Expression

  public companion object {
    fun Iterable<HasExpression>.expressions(): List<Expression> = map { it.expression }
    fun Sequence<HasExpression>.expressions(): Sequence<Expression> = map { it.expression }
    fun Set<HasExpression>.expressions(): Set<Expression> = toSetStrict { it.expression }
  }
}
