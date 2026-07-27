package dev.martianzoo.api

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement

/** The state-aware operations needed while resolving and narrowing Pets types. */
public interface TypeInfo {
  public fun isAbstract(e: Expression): Boolean

  public fun ensureNarrows(wide: Expression, narrow: Expression)

  public fun has(requirement: Requirement): Boolean

  /** A context-free sentinel that fails if an operation needs game state. */
  public object NoGameState : TypeInfo {
    private fun missing(): Nothing =
        error("This type operation requires game state; use a GameReader as its TypeInfo")

    override fun isAbstract(e: Expression): Boolean = missing()

    override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = missing()

    override fun has(requirement: Requirement): Boolean = missing()
  }
}
