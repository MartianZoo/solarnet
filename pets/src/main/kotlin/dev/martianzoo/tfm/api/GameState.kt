package dev.martianzoo.tfm.api

import com.google.common.collect.Multiset
import dev.martianzoo.tfm.data.Authority
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

/**
 * This is a simple interface in the `pets` module that code
 * outside the module can implement...
 */
public interface GameState : ReadOnlyGameState {
  fun applyChange(
      count: Int = 1,
      gaining: GenericTypeExpression? = null,
      removing: GenericTypeExpression? = null,
      cause: Cause? = null,
  )
}

public interface ReadOnlyGameState {
  val authority: Authority

  fun resolve(type: TypeExpression): TypeInfo

  fun count(type: String): Int
  fun count(type: TypeExpression): Int

  fun getAll(type: TypeExpression): Multiset<TypeExpression>
  fun getAll(type: ClassLiteral): Set<ClassLiteral>
  fun getAll(type: String): Multiset<TypeExpression>

  fun isMet(requirement: Requirement): Boolean
}