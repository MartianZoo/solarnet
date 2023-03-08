package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.engine.ComponentGraph.MissingDependenciesException
import dev.martianzoo.tfm.engine.Game.AbstractInstructionException
import dev.martianzoo.tfm.engine.Game.Task
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

class ExecutionContext(
    val game: Game,
    val withEffects: Boolean = true,
    val hidden: Boolean = false,
) {
  val taskQueue = ArrayDeque<Task>()

  fun executeAll(
      instructions: List<Instruction>,
      initialCause: Cause? = null,
  ): List<ChangeRecord> {
    val start = game.nextOrdinal
    taskQueue += instructions.map { Task(game.loader.transformer.deprodify(it), initialCause) }

    while (taskQueue.any()) {
      val next = taskQueue.removeFirst()
      try {
        doExecute(next.instruction, next.cause, 1)
      } catch (e: MissingDependenciesException) {
        val tries = next.attempts
        if (tries > 5) throw e
        taskQueue += next.copy(attempts = tries + 1)
      } catch (e: AbstractInstructionException) {
        game.pendingAbstractTasks += next
      }
    }
    return game.fullChangeLog.subList(start, game.nextOrdinal).toList()
  }

  fun applyChange(
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
      val newCause =
          Cause(
              triggeringChange = triggeringRecord.ordinal,
              contextComponent = gained,
              doer = gainedCpt.owner() ?: triggeringRecord.cause?.doer ?: SpecialClassNames.GAME)

      val selfEffects =
          gainedCpt.activeEffects.mapNotNull { it.onSelfChange(triggeringRecord, game) }

      val activeFx = game.components.allActiveEffects()
      val otherEffects =
          activeFx.elements.mapNotNull {
            it.getInstruction(triggeringRecord, game)?.times(activeFx.count(it))
          }

      taskQueue += (selfEffects + otherEffects).flatMap { Instruction.split(it) }.map {
        Task(it,
            newCause)
      }
    }
  }

  fun doExecute(ins: Instruction, cause: Cause?, multiplier: Int) {
    if (multiplier == 0) return
    require(multiplier > 0)

    when (ins) {
      is Change -> {
        val amap: Boolean =
            when (ins.intensity) {
              OPTIONAL,
              null,
              -> throw AbstractInstructionException("$ins")

              MANDATORY -> false
              AMAP -> true
            }
        applyChange(
            count = ins.count * multiplier,
            gaining = game.toComponent(ins.gaining),
            removing = game.toComponent(ins.removing),
            amap = amap,
            cause = cause)
      }

      is Per -> doExecute(ins.instruction, cause, game.count(ins.metric) * multiplier)
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

  val writer =
      object : GameStateWriter {
        override fun write(
            count: Int,
            gaining: Type?,
            removing: Type?,
            amap: Boolean,
            cause: Cause?,
        ) {
          applyChange(count, toComponent(gaining), toComponent(removing), amap, cause)
        }
      }

  private fun toComponent(type: Type?) = type?.let { Component.ofType(game.loader.resolve(it)) }
}
