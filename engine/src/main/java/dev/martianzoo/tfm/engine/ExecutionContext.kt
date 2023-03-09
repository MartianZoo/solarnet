package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.engine.Game.AbstractInstructionException
import dev.martianzoo.tfm.engine.Game.Task
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Instruction.CompositeInstruction
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Transform

class ExecutionContext(
    val game: Game,
    val withEffects: Boolean = true,
    val hidden: Boolean = false,
) {
  val outerTaskQueue = ArrayDeque<Task>() // for initial task and : effects
  val innerTaskQueue = ArrayDeque<Task>() // for :: effects, and move outer here 1-by-1

  fun autoExecute(instruction: Instruction, initialCause: Cause? = null): List<ChangeRecord> {
    val deprodded = game.loader.transformer.deprodify(instruction)
    val start = game.nextOrdinal

    outerTaskQueue += split(deprodded).map { Task(it, initialCause) }

    while (outerTaskQueue.any()) {
      require(innerTaskQueue.none())
      val outerTask = outerTaskQueue.removeFirst()
      val checkpoint = game.nextOrdinal

      try {
        innerTaskQueue += outerTask

        while (innerTaskQueue.any()) {
          val innerTask = innerTaskQueue.removeFirst()
          doExecute(innerTask.instruction, innerTask.cause, 1)
        }
      } catch (e: Exception) {
        game.failedTasks += FailedTask(outerTask, e.toString())
        innerTaskQueue.clear()
        game.rollBack(checkpoint) // have to worry about rolling back task changes??
        // TODO could also consider an overlay ComponentGraph
      }
    }
    return game.fullChangeLog.subList(start, game.nextOrdinal).toList()
  }

  data class FailedTask(val task: Task, val why: String)

  fun doExecute(ins: Instruction, cause: Cause?, multiplier: Int) {
    if (multiplier == 0) return
    require(multiplier > 0)

    when (ins) {
      is Change -> {
        val amap: Boolean =
            when (ins.intensity) {
              OPTIONAL,
              null -> throw AbstractInstructionException("$ins")
              MANDATORY -> false
              AMAP -> true
            }
        applyChangeAndFireTriggers(
            count = ins.count * multiplier,
            gaining = game.toComponent(ins.gaining),
            removing = game.toComponent(ins.removing),
            amap = amap,
            cause = cause)
      }
      is Per -> doExecute(ins.instruction, cause, game.count(ins.metric) * multiplier)
      is Gated -> {
        if (game.evaluate(ins.gate)) {
          doExecute(ins.instruction, cause, multiplier)
        } else if (ins.mandatory) {
          throw UserException("Requirement not met: ${ins.gate}")
        }
      }
      is Custom -> {
        require(multiplier == 1)
        // TODO could inject this earlier
        val custom = game.setup.authority.customInstruction(ins.functionName)
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
      is Or -> throw AbstractInstructionException("$ins")
      is CompositeInstruction -> {
        ins.instructions.forEach { doExecute(it, cause, multiplier) }
      }
      is Transform -> error("should have been transformed already")
    }
  }

  fun applyChangeAndFireTriggers(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ) {
    val change = game.components.applyChange(count, gaining, removing, amap) ?: return
    val triggeringRecord = ChangeRecord(game.nextOrdinal, change, cause, hidden)
    game.fullChangeLog.add(triggeringRecord)

    if (withEffects) {
      val gained: Expression = triggeringRecord.change.gaining ?: return
      val gainedCpt: Component = game.toComponent(gained)
      val doer = gainedCpt.owner() ?: triggeringRecord.cause?.doer ?: GAME

      // TODO fix bad code

      data class FiredEffect(val ins: Instruction, val cause: Cause, val now: Boolean)

      val selfEffects =
          gainedCpt.activeEffects.mapNotNull { fx ->
            val instr = fx.onSelfChange(triggeringRecord, game) ?: return@mapNotNull null
            val newCause =
                Cause(
                    triggeringChange = triggeringRecord.ordinal,
                    contextComponent = fx.contextComponent.expressionFull,
                    doer = doer)
            FiredEffect(instr, newCause, fx.automatic)
          }

      val activeFx = game.components.allActiveEffects()
      val otherEffects =
          activeFx.elements.mapNotNull { fx ->
            val oneInstruction = fx.getInstruction(triggeringRecord, game) ?: return@mapNotNull null
            val scaled = oneInstruction.times(activeFx.count(fx))
            val newCause =
                Cause(
                    triggeringChange = triggeringRecord.ordinal,
                    contextComponent = fx.contextComponent.expressionFull,
                    doer = doer)
            FiredEffect(scaled, newCause, fx.automatic)
          }

      val pairs = selfEffects + otherEffects
      val now =
          pairs
              .filter { it.now }
              .flatMap { fired -> split(fired.ins).map { fired.copy(ins = it) } }
              .map { Task(it.ins, it.cause) }
      val later =
          pairs
              .filterNot { it.now }
              .flatMap { fired -> split(fired.ins).map { fired.copy(ins = it) } }
              .map { Task(it.ins, it.cause) }
      innerTaskQueue += now
      outerTaskQueue += later
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
          applyChangeAndFireTriggers(count, toComponent(gaining), toComponent(removing), amap, cause)
        }
      }

  private fun toComponent(type: Type?) = type?.let { Component.ofType(game.loader.resolve(it)) }
}
