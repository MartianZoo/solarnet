package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameState
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.data.ChangeRecord.StateChange
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
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

  override fun resolve(expression: Expression): PType = loader.resolve(expression)

  fun resolve(type: Type): PType = loader.resolve(type)

  override fun evaluate(requirement: Requirement) = LiveNodes.from(requirement, this).evaluate(this)

  override fun count(type: Type): Int = components.count(resolve(type))

  override fun count(metric: Metric): Int = LiveNodes.from(metric, this).count(this)

  override fun getComponents(type: Type): Multiset<Type> =
      components.getAll(resolve(type)).map { it.type }

  fun getComponents(type: PType): Multiset<Component> = components.getAll(type)

  fun execute(ins: Instruction, multiplier: Int = 1) {
    require(multiplier >= 0)
    if (multiplier == 0) return

    when (ins) {
      is Instruction.Change -> {
        val amap: Boolean = when (ins.intensity) {
          OPTIONAL, null -> throw UserException("abstract instruction")
          MANDATORY -> false
          AMAP -> true
        }
        applyChangeAndPublish(
            count = ins.count * multiplier,
            removing = ins.removing?.let { resolve(it) },
            gaining = ins.gaining?.let { resolve(it) },
            amap = amap)
      }

      is Instruction.Per -> {
        val metric = LiveNodes.from(ins.metric, this)
        execute(ins.instruction, metric.count(this) * multiplier)
      }

      is Instruction.Gated -> {
        if (LiveNodes.from(ins.gate, this).evaluate(this)) {
          execute(ins.instruction, multiplier)
        } else {
          throw UserException("Requirement not met: ${ins.gate}")
        }
      }

      is Instruction.Custom -> {
        require(multiplier == 1)
        // TODO could inject this earlier
        val custom = authority.customInstruction(ins.functionName)
        val arguments = ins.arguments.map { resolve(it) }
        try {
          var translated: Instruction = custom.translate(this, arguments)
          val xer = loader.transformer
          translated = xer.insertDefaults(translated)
          translated = xer.deprodify(translated)
          execute(translated)

        } catch (e: ExecuteInsteadException) {
          // this custom fn chose to override execute() instead of translate()
          custom.execute(this, arguments)
        }
      }

      is Instruction.Or -> throw UserException("abstract instruction")

      is Instruction.CompositeInstruction -> {
        ins.instructions.forEach { execute(it, multiplier) }
      }

      is Instruction.Transform -> error("should have been transformed already")
    }
  }

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

  public fun component(type: Type?): Component? = type?.let { Component(loader.resolve(it)) }

  public fun component(type: Expression?): Component? =
      type?.let { Component(loader.resolve(it)) }

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
