package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.MarsMapDefinition
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.Multiset

/** The read-only portions of [GameState]. */
interface ReadOnlyGameState {
  val setup: GameSetup

  val authority: Authority
    get() = setup.authority

  val map: MarsMapDefinition
    get() = setup.map

  fun countComponents(type: Type): Int

  fun getComponents(type: Type): Multiset<out Type>

  fun isMet(requirement: Requirement): Boolean

  fun resolveType(typeExpr: TypeExpr): Type

  /** A game engine implements this interface so that [CustomInstruction]s can speak to it. */
  interface GameState : ReadOnlyGameState {
    fun applyChange(
        count: Int = 1,
        removing: Type? = null,
        gaining: Type? = null,
        cause: Cause? = null,
        amap: Boolean = false,
    )
  }
}
