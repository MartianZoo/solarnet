package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression

interface GameApi {
  val authority: Authority
  fun count(type: TypeExpression): Int
  fun isMet(requirement: Requirement): Boolean
  fun applyChange(
      count: Int = 1,
      gaining: TypeExpression? = null,
      removing: TypeExpression? = null,
      cause: Cause? = null)
}
