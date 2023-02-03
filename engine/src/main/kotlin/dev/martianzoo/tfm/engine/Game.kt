package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.StateChange.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.map

/** A game in progress. */
public class Game(
    val setup: GameSetup,
    val components: ComponentGraph,
    val loader: PClassLoader,
) {
  // val tasks = mutableListOf<Task>()

  val authority by setup::authority

  // TODO maybe have `beginChangeLogging` instead of passing in a prebuilt multiset
  fun changeLog(): List<StateChange> = components.changeLog()

  fun resolveType(typeExpr: TypeExpr): PType = loader.resolveType(typeExpr)

  fun isMet(requirement: Requirement) = LiveNodes.from(requirement, this).isMet(this)

  fun countComponents(ptype: PType) = components.count(ptype)

  fun countComponents(typeExpr: TypeExpr) = countComponents(resolveType(typeExpr))

  fun getComponents(ptype: PType): Multiset<Component> = components.getAll(ptype)

  fun getComponents(typeExpr: TypeExpr): Multiset<TypeExpr> {
    val all: Multiset<Component> = getComponents(resolveType(typeExpr))
    return all.map { it.asTypeExpr }
  }

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  fun applyChange(
      count: Int,
      removing: Component? = null,
      gaining: Component? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ) {
    components.applyChange(
        count = count, removing = removing, gaining = gaining, amap = amap, cause = cause)
  }

  // TODO why don't we still implement this directly??
  val asGameState: GameState by lazy {
    object : GameState {
      val g = this@Game
      override fun applyChange(
          count: Int,
          removing: TypeExpr?,
          gaining: TypeExpr?,
          cause: Cause?,
          amap: Boolean,
      ) {
        // TODO order
        return g.applyChange(
            count = count,
            removing = removing?.let { Component(resolveType(it)) },
            gaining = gaining?.let { Component(resolveType(it)) },
            amap = amap,
            cause = cause)
      }

      override val setup by g::setup
      override val authority by g::authority
      override val map by setup::map

      fun resolveType(typeExpr: TypeExpr) = g.resolveType(typeExpr)

      override fun isMet(requirement: Requirement) = g.isMet(requirement)

      override fun countComponents(typeExpr: TypeExpr) = g.countComponents(typeExpr)

      override fun getComponents(typeExpr: TypeExpr) = g.getComponents(typeExpr)
    }
  }
}
