package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.api.ReadOnlyGameState.GameState
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.Multiset

/** A game state that does basically nothing; for tests. */
open class StubGameState(auth: Authority = Authority.Minimal()) : GameState {
  override fun applyChange(
      count: Int,
      removing: Type?,
      gaining: Type?,
      amap: Boolean,
      cause: Cause?,
  ): Unit = throe()

  override val setup = GameSetup(auth, "BM", 2)

  override fun countComponents(type: Type): Int = throe()

  override fun getComponents(type: Type): Multiset<out Type> = throe()

  override fun isMet(requirement: Requirement): Boolean = throe()

  override fun resolveType(typeExpr: TypeExpr): Type = throe()

  private fun throe(): Nothing = throw RuntimeException("this is just a stub")
}
