package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Requirement

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
