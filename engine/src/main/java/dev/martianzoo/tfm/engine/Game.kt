package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.engine.ComponentGraph.MissingDependenciesException
import dev.martianzoo.tfm.pets.ast.Effect.Trigger.WhenGain
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.CompositeInstruction
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.PClassLoader
import dev.martianzoo.tfm.types.PType
import dev.martianzoo.util.Multiset

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: PClassLoader) {
  init {
    loader.frozen = true
  }

  val components = ComponentGraph()

  internal val fullChangeLog: MutableList<ChangeRecord> = mutableListOf()

  val authority by setup::authority

  private val nextOrdinal: Int by fullChangeLog::size

  public fun changeLogFull() = fullChangeLog.toList()

  public fun changeLog(): List<ChangeRecord> = fullChangeLog.filterNot { it.hidden }

  fun resolve(expression: Expression): PType = loader.resolve(expression)

  fun resolve(type: Type): PType = loader.resolve(type)

  fun evaluate(requirement: Requirement) = LiveNodes.from(requirement, this).evaluate(this)

  fun count(type: Type): Int = components.count(resolve(type))

  fun count(metric: Metric): Int = LiveNodes.from(metric, this).count(this)

  fun getComponents(type: Type): Multiset<Type> = components.getAll(resolve(type)).map { it.ptype }

  fun getComponents(type: PType): Multiset<Component> = components.getAll(type)

  fun execute(
      instruction: Instruction,
      withEffects: Boolean = false,
      initialCause: Cause? = null,
      hidden: Boolean = false
  ) = executeAll(listOf(instruction), withEffects, initialCause, hidden)

  fun executeAll(
      instruction: List<Instruction>,
      withEffects: Boolean = false,
      cause: Cause? = null,
      hidden: Boolean = false,
  ): List<ChangeRecord> = ExecutionContext(withEffects, hidden).executeAll(instruction, cause)

  data class Task(val ins: Instruction, val cause: Cause?, val attempts: Int = 0)

  inner class ExecutionContext(val withEffects: Boolean = true, val hidden: Boolean = false) {
    val taskQueue = ArrayDeque<Task>()

    fun executeAll(
        instructions: List<Instruction>,
        initialCause: Cause? = null,
    ): List<ChangeRecord> {
      val start = nextOrdinal
      taskQueue += instructions.map { Task(loader.transformer.deprodify(it), initialCause) }

      while (taskQueue.any()) {
        val next = taskQueue.removeFirst()
        try {
          doExecute(next.ins, next.cause, 1)
        } catch (e: MissingDependenciesException) {
          val tries = next.attempts
          if (tries > 3) throw e
          taskQueue += next.copy(attempts = tries + 1)
        }
      }
      return fullChangeLog.subList(start, nextOrdinal).toList()
    }

    fun applyChangeAndTriggerEffects(
        count: Int = 1,
        gaining: Component? = null,
        removing: Component? = null,
        amap: Boolean = false,
        cause: Cause? = null,
    ) {
      val change = components.applyChange(count, gaining, removing, amap) ?: return
      val record = ChangeRecord(nextOrdinal, change, cause, hidden)
      fullChangeLog.add(record)

      if (withEffects) {
        val gained: Expression = change.gaining ?: return
        val newCause = Cause(gained, record.ordinal)

        taskQueue +=
            toComponent(gained)!!
                .effects()
                .filter { it.trigger == WhenGain } // TODO others
                .flatMap { split(it.instruction) }
                .map { Task(it, newCause) }
      }
    }

    fun doExecute(ins: Instruction, cause: Cause?, multiplier: Int) {
      if (multiplier == 0) return
      require(multiplier > 0)

      val game = this@Game

      when (ins) {
        is Change -> {
          val amap: Boolean =
              when (ins.intensity) {
                OPTIONAL,
                null, -> throw UserException("abstract instruction: $ins")
                MANDATORY -> false
                AMAP -> true
              }
          applyChangeAndTriggerEffects(
              count = ins.count * multiplier,
              gaining = toComponent(ins.gaining),
              removing = toComponent(ins.removing),
              amap = amap,
              cause = cause)
        }
        is Per -> {
          val metric = LiveNodes.from(ins.metric, game)
          doExecute(ins.instruction, cause, metric.count(game) * multiplier)
        }
        is Gated -> {
          if (LiveNodes.from(ins.gate, game).evaluate(game)) {
            doExecute(ins.instruction, cause, multiplier)
          } else {
            throw UserException("Requirement not met: ${ins.gate}")
          }
        }
        is Custom -> {
          require(multiplier == 1)
          // TODO could inject this earlier
          val custom = game.authority.customInstruction(ins.functionName)
          val arguments = ins.arguments.map { game.resolve(it) }
          try {
            var translated: Instruction = custom.translate(game.reader, arguments)
            val xer = game.loader.transformer
            translated = xer.spellOutClassNames(translated) // TODO package these up
            translated = xer.insertDefaults(translated)
            translated = xer.deprodify(translated)
            doExecute(translated, cause, 1)
          } catch (e: ExecuteInsteadException) {
            // this custom fn chose to override execute() instead of translate()
            custom.execute(game.reader, writer, arguments)
          }
        }
        is Or -> throw UserException("abstract instruction: $ins")
        is CompositeInstruction -> {
          ins.instructions.forEach { doExecute(it, cause, multiplier) }
        }
        is Transform -> error("should have been transformed already")
      }
    }

    val writer =
        object : GameStateWriter {
          override fun write(
              count: Int,
              gaining: Type?,
              removing: Type?,
              amap: Boolean,
              cause: Cause?,
          ) {
            applyChangeAndTriggerEffects(
                count, toComponent(gaining), toComponent(removing), amap, cause)
          }
        }
  }

  private fun split(ins: Instruction) =
      when (ins) {
        is Or -> listOf(ins)
        is CompositeInstruction -> ins.instructions
        else -> listOf(ins)
      }

  val reader =
      object : GameStateReader {
        override val setup by this@Game::setup
        override val authority by this@Game::authority

        override fun resolve(expression: Expression) = this@Game.resolve(expression)

        override fun evaluate(requirement: Requirement) = this@Game.evaluate(requirement)

        override fun count(metric: Metric) = this@Game.count(metric)

        override fun count(type: Type) = this@Game.count(type)

        override fun getComponents(type: Type) = this@Game.getComponents(type)
      }

  internal fun toComponent(type: Type?) = type?.let { Component.ofType(resolve(it)) }
  internal fun toComponent(expression: Expression?) = expression?.let { toComponent(resolve(it)) }

  public fun rollBack(ordinal: Int) { // TODO kick this out, rolling back starts a new game?
    require(ordinal <= nextOrdinal)
    if (ordinal == nextOrdinal) return
    require(!fullChangeLog[ordinal].hidden)

    val subList = fullChangeLog.subList(ordinal, nextOrdinal)
    for (entry in subList.asReversed()) {
      val change = entry.change.inverse()
      components.updateMultiset(
          change.count,
          gaining = toComponent(change.gaining),
          removing = toComponent(change.removing),
      )
    }
    subList.clear()
  }
}
