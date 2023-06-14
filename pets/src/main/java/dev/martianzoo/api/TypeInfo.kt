package dev.martianzoo.api

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement

interface TypeInfo {
  fun isAbstract(e: Expression): Boolean
  fun ensureNarrows(wide: Expression, narrow: Expression)
  fun has(requirement: Requirement): Boolean

  object StubTypeInfo : TypeInfo {
    override fun isAbstract(e: Expression) = error("")
    override fun ensureNarrows(wide: Expression, narrow: Expression) = error("")
    override fun has(requirement: Requirement) = error("")
  }
}
