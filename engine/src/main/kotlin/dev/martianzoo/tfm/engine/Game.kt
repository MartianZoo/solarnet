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

public class Game(
    val setup: GameSetup,
    val components: ComponentGraph,
    val classTable: PClassLoader,
) {
  // val tasks = mutableListOf<Task>()

  val authority by setup::authority

  // TODO maybe have `beginChangeLogging` instead of passing in a prebuilt multiset
  fun changeLog(): List<StateChange> = components.changeLog()

  fun resolve(typeExpr: TypeExpr): PType = classTable.resolveType(typeExpr)

  fun isMet(requirement: Requirement) = LiveNodes.from(requirement, this).isMet(this)

  fun count(ptype: PType) = components.count(ptype)

  fun count(typeExpr: TypeExpr) = count(resolve(typeExpr))

  fun getAll(ptype: PType): Multiset<Component> = components.getAll(ptype)

  fun getAll(typeExpr: TypeExpr): Multiset<TypeExpr> {
    val all: Multiset<Component> = getAll(resolve(typeExpr))
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

  val asGameState: GameState by lazy {
    object : GameState {
      override fun applyChange(
          count: Int,
          removing: TypeExpr?,
          gaining: TypeExpr?,
          cause: Cause?,
          amap: Boolean,
      ) {
        // TODO order
        return this@Game.applyChange(
            count = count,
            removing = removing?.let { Component(resolve(it)) },
            gaining = gaining?.let { Component(resolve(it)) },
            amap = amap,
            cause = cause)
      }

      override val setup = this@Game.setup
      override val authority = this@Game.authority
      override val map = this@Game.setup.map

      override fun resolve(typeExpr: TypeExpr) = this@Game.resolve(typeExpr)

      override fun isMet(requirement: Requirement) = this@Game.isMet(requirement)

      override fun count(typeExpr: TypeExpr) = this@Game.count(typeExpr)

      override fun getAll(typeExpr: TypeExpr) = this@Game.getAll(typeExpr)
    }
  }
}
