package dev.martianzoo.pets

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.util.toSetStrict

/** Any object that can be represented in some way as an [Expression]. */
public interface HasExpression {
  /** This object as a minimal expression. */
  public val expression: Expression

  /** This object as a full expression. */
  public val expressionFull: Expression
    get() = expression

  public companion object {
    public fun Iterable<HasExpression>.expressions(): List<Expression> = map { it.expression }

    internal fun Sequence<HasExpression>.expressions(): Sequence<Expression> = map { it.expression }

    public fun Set<HasExpression>.expressions(): Set<Expression> = toSetStrict { it.expression }
  }
}
