package dev.martianzoo.pets

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.util.toSetStrict

/** Any object that can be represented in some way as an [Expression]. */
public interface HasExpression {
  /** This object's natural expression; particular implementations may offer stronger forms. */
  public val expression: Expression

  /** This object's full expression when it has a distinct full form, otherwise [expression]. */
  public val expressionFull: Expression
    get() = expression

  public companion object {
    public fun Iterable<HasExpression>.expressions(): List<Expression> = map { it.expression }

    public fun Sequence<HasExpression>.expressions(): Sequence<Expression> = map { it.expression }

    public fun Set<HasExpression>.expressions(): Set<Expression> = toSetStrict { it.expression }
  }
}
