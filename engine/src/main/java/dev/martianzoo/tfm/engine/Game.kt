package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.Multiset

/** A game in progress. */
public class Game(
    override val setup: GameSetup,
    public val loader: PClassLoader, // TODO not public
) : GameState {
  init {
    loader.frozen = true
  }

  val components = ComponentGraph()
  internal val fullChangeLog: MutableList<ChangeRecord> = mutableListOf()
  // val tasks = mutableListOf<Task>()

  override val authority by setup::authority

  private val nextOrdinal: Int by fullChangeLog::size

  public fun changeLogFull() = fullChangeLog.toList()

  public fun changeLog(): List<ChangeRecord> = fullChangeLog.filterNot { it.hidden }

  override fun resolveType(expression: Expression): PType = loader.resolveType(expression)

  fun resolveType(type: Type): PType = loader.resolveType(type)

  override fun evaluate(requirement: Requirement) = LiveNodes.from(requirement, this).evaluate(this)

  override fun count(type: Type): Int = components.count(resolveType(type))

  override fun count(metric: Metric): Int = LiveNodes.from(metric, this).count(this)

  override fun getComponents(type: Type): Multiset<Type> =
      components.getAll(resolveType(type)).map { it.type }

  fun getComponents(type: PType): Multiset<Component> = components.getAll(type)

  fun execute(instr: Instruction) = LiveNodes.from(instr, this).execute(this)

  // BIGTODO trigger triggers
  override fun applyChangeAndPublish(
      count: Int,
      removing: Type?,
      gaining: Type?,
      amap: Boolean,
      cause: Cause?,
      hidden: Boolean,
  ) {
    val change: StateChange =
        components.applyChange(
            count,
            removing = component(removing),
            gaining = component(gaining),
            amap = amap,
        )
    logChange(change, cause, hidden)
    // publish change
  }

  private fun logChange(change: StateChange, cause: Cause?, hidden: Boolean) {
    fullChangeLog.add(ChangeRecord(nextOrdinal, change, cause, hidden))
  }

  public fun component(type: Type?): Component? = type?.let { Component(loader.resolveType(it)) }

  public fun component(type: Expression?): Component? =
      type?.let { Component(loader.resolveType(it)) }

  public fun rollBack(ordinal: Int) {
    require(ordinal <= nextOrdinal)
    if (ordinal == nextOrdinal) return
    require(!fullChangeLog[ordinal].hidden)

    val subList = fullChangeLog.subList(ordinal, nextOrdinal)
    for (entry in subList.asReversed()) {
      val change = entry.change.inverse()
      components.updateMultiset(
          change.count,
          gaining = component(change.gaining),
          removing = component(change.removing),
      )
    }
    subList.clear()
  }
}
