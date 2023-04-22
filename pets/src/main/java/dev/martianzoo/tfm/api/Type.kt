package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.HasClassName
import dev.martianzoo.tfm.pets.ast.HasExpression
import dev.martianzoo.tfm.pets.ast.Requirement

/**
 * A type, for which an [Expression] is only a textual representation. There are many ways in which
 * distinct expressions might resolve to the same [Type].
 */
interface Type : HasExpression, HasClassName {
  /**
   * True if this type is abstract, in which case occurrences of the type can be counted in a game
   * state but neither gained nor removed.
   */
  val abstract: Boolean

  /**
   * Returns whether this type is a subtype of [that]; note that refinements cannot be evaluated
   * here, so this sometimes returns `false` even when the types should logically be subtypes in the
   * current game state.
   */
  fun isSubtypeOf(that: Type): Boolean

  /** The optional requirement attached to this type. */
  val refinement: Requirement?
}
