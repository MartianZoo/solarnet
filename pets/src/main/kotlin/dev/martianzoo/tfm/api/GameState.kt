package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.Multiset

/** This is a simple interface in the `pets` module that code outside the module can implement... */
interface GameState : ReadOnlyGameState {
  fun applyChange(
      count: Int = 1,
      removing: TypeExpr? = null,
      gaining: TypeExpr? = null,
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

  fun resolve(typeExpr: TypeExpr): TypeInfo
  fun count(typeExpr: TypeExpr): Int

  fun getAll(typeExpr: TypeExpr): Multiset<TypeExpr>

  fun isMet(requirement: Requirement): Boolean
}
