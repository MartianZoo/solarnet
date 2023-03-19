package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression

interface ExpressionInfo {
  fun isAbstract(e: Expression): Boolean
  fun ensureReifies(wide: Expression, narrow: Expression)
}
