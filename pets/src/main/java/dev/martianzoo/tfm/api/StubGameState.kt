package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.ChangeLogEntry.Cause
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.Multiset

/** A game state that does basically nothing; for tests. */
open class StubGameState(auth: Authority = Authority.Minimal()) : GameState {
  override fun applyChange(
      count: Int,
      removing: TypeExpr?,
      gaining: TypeExpr?,
      cause: Cause?,
      amap: Boolean,
  ): Unit = throe()

  override val setup = GameSetup(auth, "BM", 2)

  override fun countComponents(typeExpr: TypeExpr): Int = throe()

  override fun getComponents(typeExpr: TypeExpr): Multiset<TypeExpr> = throe()

  override fun isMet(requirement: Requirement): Boolean = throe()

  private fun throe(): Nothing = throw RuntimeException("this is just a stub")
}
