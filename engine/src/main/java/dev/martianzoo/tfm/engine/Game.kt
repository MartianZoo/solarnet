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
    override val setup: GameSetup,
    internal val components: ComponentGraph,
    public val loader: PClassLoader, // TODO unpublic it
) : GameState {
  // val tasks = mutableListOf<Task>()

  override val authority by setup::authority

  fun changeLog(): List<ChangeRecord> = components.changeLog()

  fun changeLogFull(): List<ChangeRecord> = components.changeLogFull()

  override fun resolveType(typeExpr: TypeExpr): PType = loader.resolveType(typeExpr)

  fun resolveType(type: Type): PType = loader.resolveType(type)

  override fun isMet(requirement: Requirement) = LiveNodes.from(requirement, this).isMet(this)

  override fun countComponents(type: Type): Int = components.count(resolveType(type))

  fun countComponents(typeExpr: TypeExpr): Int = countComponents(resolveType(typeExpr))

  override fun getComponents(type: Type): Multiset<Component> = components.getAll(resolveType(type))

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  override fun applyChange(
      count: Int,
      removing: Type?,
      gaining: Type?,
      amap: Boolean,
      cause: Cause?,
  ) {
    components.applyChange(
        count,
        removing = component(removing),
        gaining = component(gaining),
        amap = amap,
        cause = cause,
        hidden = false)
  }

  private fun component(type: Type?): Component? = type?.let { Component(loader.resolveType(it)) }

  fun rollBackToBefore(ordinal: Int) = components.rollBackToBefore(ordinal, loader)
}
