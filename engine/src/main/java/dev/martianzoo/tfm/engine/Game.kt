package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.ReadOnlyGameState.GameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.Multiset

/** A game in progress. */
public class Game(
    val setup: GameSetup,
    private val components: ComponentGraph,
    val loader: PClassLoader,
) {
  // val tasks = mutableListOf<Task>()

  val authority by setup::authority

  fun changeLog(): List<ChangeRecord> = components.changeLog()

  fun changeLogFull(): List<ChangeRecord> = components.changeLogFull()

  fun resolveType(typeExpr: TypeExpr): PType = loader.resolveType(typeExpr)

  fun resolveType(type: Type): PType = loader.resolveType(type)

  fun isMet(requirement: Requirement) = LiveNodes.from(requirement, this).isMet(this)

  fun countComponents(type: Type): Int = components.count(resolveType(type))

  fun countComponents(typeExpr: TypeExpr): Int = countComponents(resolveType(typeExpr))

  fun getComponents(type: Type): Multiset<Component> = components.getAll(resolveType(type))

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  // Doesn't belong exactly here? TODO
  fun applyChange(
      count: Int = 1,
      removing: Component? = null,
      gaining: Component? = null,
      amap: Boolean = false,
      cause: Cause? = null,
      hidden: Boolean = false,
  ) {
    components.applyChange(
        count = count,
        removing = removing,
        gaining = gaining,
        amap = amap,
        cause = cause,
        hidden = hidden)
  }

  fun rollBackToBefore(ordinal: Int) = components.rollBackToBefore(ordinal, loader)

  // TODO why don't we still implement this directly??
  val asGameState: GameState by lazy {
    object : GameState {
      val g = this@Game
      override fun applyChange(
          count: Int,
          removing: Type?,
          gaining: Type?,
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

      override fun resolveType(typeExpr: TypeExpr): PType = g.resolveType(typeExpr)

      fun resolveType(type: Type): PType = g.resolveType(type)

      override fun isMet(requirement: Requirement) = g.isMet(requirement)

      override fun countComponents(type: Type): Int = g.countComponents(type)

      override fun getComponents(type: Type): Multiset<out Type> = g.getComponents(type)
    }
  }
}
