package dev.martianzoo.api

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement

public interface TypeInfo {
  public fun isAbstract(e: Expression): Boolean

  public fun ensureNarrows(wide: Expression, narrow: Expression)

  public fun has(requirement: Requirement): Boolean

  public object StubTypeInfo : TypeInfo {
    override fun isAbstract(e: Expression): Boolean = error("")

    override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = error("")

    override fun has(requirement: Requirement): Boolean = error("")
  }
}
