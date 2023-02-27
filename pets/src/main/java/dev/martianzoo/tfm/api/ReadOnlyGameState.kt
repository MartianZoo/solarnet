package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.util.Multiset

/** The read-only portions of [GameState]. */
interface ReadOnlyGameState {
  val setup: GameSetup

  fun countComponents(type: Type): Int

  fun getComponents(type: Type): Multiset<out Type>

  fun evaluate(requirement: Requirement): Boolean

  fun resolveType(typeExpr: TypeExpr): Type
}
