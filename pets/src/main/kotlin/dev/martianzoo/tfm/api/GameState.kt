package dev.martianzoo.tfm.api

import com.google.common.collect.Multiset
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpression
import dev.martianzoo.tfm.pets.ast.TypeExpression.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression

/** This is a simple interface in the `pets` module that code outside the module can implement... */
interface GameState : ReadOnlyGameState {
  fun applyChange(
      count: Int = 1,
      removing: GenericTypeExpression? = null,
      gaining: GenericTypeExpression? = null,
      cause: Cause? = null,
      amap: Boolean = false,
  )
}

interface ReadOnlyGameState {
  val setup: GameSetup
  val authority: Authority
    get() = setup.authority
  val map: MarsMapDefinition
    get() = setup.map

  fun resolve(type: TypeExpression): TypeInfo
  fun count(type: TypeExpression): Int

  fun getAll(type: TypeExpression): Multiset<TypeExpression>
  fun getAll(type: GenericTypeExpression): Multiset<GenericTypeExpression>
  fun getAll(type: ClassLiteral): Set<ClassLiteral>

  fun isMet(requirement: Requirement): Boolean
}
