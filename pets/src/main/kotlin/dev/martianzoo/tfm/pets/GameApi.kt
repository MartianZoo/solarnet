package dev.martianzoo.tfm.pets

import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

/**
 * This is a simple interface in the `pets` module that code
 * outside the module can implement...
 */
public interface GameApi {
  val authority: Authority
  fun count(type: TypeExpression): Int
  fun isMet(requirement: Requirement): Boolean
  fun applyChange(
      count: Int = 1,
      gaining: GenericTypeExpression? = null,
      removing: GenericTypeExpression? = null,
      cause: Cause? = null,
  )
}
