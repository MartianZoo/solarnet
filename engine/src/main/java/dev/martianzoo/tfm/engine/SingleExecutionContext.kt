package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames.GAME
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeEvent
import dev.martianzoo.tfm.data.ChangeEvent.Cause
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.engine.Exceptions.RequirementException
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
import dev.martianzoo.tfm.types.Transformers.CompositeTransformer
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.ReplaceShortNames

class SingleExecutionContext(
    val game: Game,
    val withEffects: Boolean = true,
    val hidden: Boolean = false,
) {
  val outerTaskQueue = ArrayDeque<Task>() // for initial task and : effects
  val innerTaskQueue = ArrayDeque<Task>() // for :: effects, and move outer here 1-by-1

  fun autoExecute(instruction: Instruction, initialCause: Cause? = null): List<ChangeEvent> {
    val deprodded = Deprodify(game.loader).transform(instruction)
    val start = game.nextOrdinal

    outerTaskQueue += split(deprodded).map { Task(game.nextTaskId++, it, initialCause) }

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
        innerTaskQueue.clear()
        game.rollBack(checkpoint) // TODO rolling back task changes??

        if (e is IllegalArgumentException || e is IllegalStateException) {
          throw e
        } else {
          game.pendingTasks += outerTask.copy(why = e.toString())
        }
      }
    }
    return game.fullChangeLog.subList(start, game.nextOrdinal).toList()
  }

  fun doExecute(instr: Instruction, cause: Cause?, multiplier: Int) {
    if (multiplier == 0) return
    require(multiplier > 0)

    when (instr) {
      is Change -> {
        val amap: Boolean =
            when (instr.intensity) {
              OPTIONAL,
              null -> throw AbstractInstructionException(instr, instr.intensity)
              MANDATORY -> false
              AMAP -> true
            }
        applyChangeAndFireTriggers(
            count = instr.count * multiplier,
            gaining = game.toComponent(instr.gaining),
            removing = game.toComponent(instr.removing),
            amap = amap,
            cause = cause)
      }
      is Per -> doExecute(instr.instruction, cause, game.count(instr.metric) * multiplier)
      is Gated -> {
        if (game.evaluate(instr.gate)) {
          doExecute(instr.instruction, cause, multiplier)
        } else if (instr.mandatory) {
          throw RequirementException("Requirement not met: ${instr.gate}")
        }
      }
      is Custom -> {
        // TODO could inject this earlier
        val custom = game.setup.authority.customInstruction(instr.functionName)
        val arguments = instr.arguments.map { game.resolve(it) }
        try {
          val translated: Instruction = custom.translate(game.reader, arguments)
          val xer = CompositeTransformer(
              ReplaceShortNames(game.loader),
              InsertDefaults(game.loader), // TODO context component??
              Deprodify(game.loader),
              // Not needed: ReplaceThisWith, ReplaceOwnerWith, FixUnownedEffect
          )
          doExecute(xer.transform(translated), cause, 1)
        } catch (e: ExecuteInsteadException) {
          // this custom fn chose to override execute() instead of translate()
          custom.execute(game.reader, writer, arguments)
        }
      }
      is Or -> throw AbstractInstructionException(instr)
      is CompositeInstruction -> {
        instr.instructions.forEach { doExecute(it, cause, multiplier) }
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

    val triggerEvent = ChangeEvent(game.nextOrdinal, change, cause, hidden)
    game.fullChangeLog.add(triggerEvent)
    if (withEffects) fireTriggers(triggerEvent)
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    // TODO ComponentGraph should do more of this itself?
    val gained: Expression = triggerEvent.change.gaining ?: return
    val gainedComponent: Component = game.toComponent(gained)

    val allFiredEffects: List<FiredEffect> =
        gainedComponent.activeEffects.mapNotNull { it.onChangeToSelf(triggerEvent, game) } +
            game.components.allActiveEffects().elements.mapNotNull {
              it.onChangeToOther(triggerEvent, game)
            }

    val doer = gainedComponent.owner() ?: triggerEvent.cause?.doer ?: GAME
    val withDoer = allFiredEffects.map { it.withDoer(doer) }

    innerTaskQueue += tasks(withDoer.filter { it.automatic })
    outerTaskQueue += tasks(withDoer.filter { !it.automatic })
  }

  private fun tasks(firedFx: List<FiredEffect>) =
      firedFx
          .flatMap { fired -> split(fired.instruction).map { fired.copy(instruction = it) } }
          .map { Task(game.nextTaskId++, it.instruction, it.cause) }

  val writer =
      object : GameStateWriter {
        override fun write(
            count: Int,
            gaining: Type?,
            removing: Type?,
            amap: Boolean,
            cause: Cause?,
        ) {
          applyChangeAndFireTriggers(
              count, toComponent(gaining), toComponent(removing), amap, cause)
        }
      }

  private fun toComponent(type: Type?) = type?.let { Component.ofType(game.loader.resolve(it)) }
}
