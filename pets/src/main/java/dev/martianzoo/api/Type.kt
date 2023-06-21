package dev.martianzoo.api

import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.pets.HasClassName
import dev.martianzoo.pets.HasExpression
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Expression.Refinement

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

  fun narrows(that: Type, info: TypeInfo = StubTypeInfo): Boolean

  /** The optional requirement attached to this type. */
  val refinement: Refinement?
}
