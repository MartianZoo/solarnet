package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.api.TypeInfo.StubTypeInfo
import dev.martianzoo.tfm.pets.HasClassName
import dev.martianzoo.tfm.pets.HasExpression
import dev.martianzoo.tfm.pets.ast.Expression
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

  fun narrows(that: Type, info: TypeInfo = StubTypeInfo): Boolean

  /** The optional requirement attached to this type. */
  val refinement: Requirement?
}
