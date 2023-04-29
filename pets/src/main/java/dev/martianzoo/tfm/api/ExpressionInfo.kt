package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement

interface ExpressionInfo {
  fun isAbstract(e: Expression): Boolean
  fun ensureNarrows(wide: Expression, narrow: Expression)
  fun evaluate(requirement: Requirement): Boolean

  object StubExpressionInfo : ExpressionInfo {
    override fun isAbstract(e: Expression) = error("")
    override fun ensureNarrows(wide: Expression, narrow: Expression) = error("")
    override fun evaluate(requirement: Requirement) = error("")
  }
}
