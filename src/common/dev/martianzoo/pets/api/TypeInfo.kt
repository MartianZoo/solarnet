package dev.martianzoo.pets.api

import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.pets.types.ClassTable

/** The universe and state-dependent facts used for Type and instruction judgments. */
public interface TypeInfo {
  /** The universe in which these judgments are made, including any premise-local Classes. */
  public val classTable: ClassTable

  public fun isAbstract(e: Expression): Boolean

  public fun ensureNarrows(wide: Expression, narrow: Expression)

  /** Instruction choices retain predicates on candidates whose structure is still abstract. */
  public fun ensureSelectionNarrows(wide: Expression, narrow: Expression)

  public fun has(requirement: Requirement): Boolean

  /** A context-free sentinel that fails if an operation needs a world. */
  public object NoGameState : TypeInfo {
    private fun missing(): Nothing = error("type operation requires a World-backed TypeInfo")

    override val classTable: ClassTable
      get() = missing()

    override fun isAbstract(e: Expression): Boolean = missing()

    override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = missing()

    override fun ensureSelectionNarrows(wide: Expression, narrow: Expression): Unit = missing()

    override fun has(requirement: Requirement): Boolean = missing()
  }
}
