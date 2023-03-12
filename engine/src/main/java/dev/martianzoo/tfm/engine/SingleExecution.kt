package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent
import dev.martianzoo.tfm.data.LogEntry.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.engine.Exceptions.RequirementException
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Custom
import dev.martianzoo.tfm.pets.ast.Instruction.Gated
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import dev.martianzoo.tfm.pets.ast.Instruction.Multi
import dev.martianzoo.tfm.pets.ast.Instruction.Or
import dev.martianzoo.tfm.pets.ast.Instruction.Per
import dev.martianzoo.tfm.pets.ast.Instruction.Then
import dev.martianzoo.tfm.pets.ast.Instruction.Transform
import dev.martianzoo.tfm.types.Transformers.CompositeTransformer
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.ReplaceShortNames
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toSetStrict

/**
 * Carries out a single instruction, plus any "automatic" triggers that result, with failure
 * atomicity. It writes both components and tasks directly into the game state, then verifies
 * invariants (TODO), and returns the log entries that were generated.
 */
class SingleExecution(val game: Game, val doEffects: Boolean = true) {
  // Our internal tasks don't have much relation with Task
  data class InternalTask(val instruction: Instruction, val cause: Cause?)

  private val internalTaskQueue = ArrayDeque<InternalTask>()

  var spent = false

  fun initiateAtomic(
      instruction: Instruction,
      cause: Cause?,
  ): ExecutionResult {
    require(!spent)
    spent = true

    internalTaskQueue += InternalTask(instruction, cause)
    return doAtomic {
      while (internalTaskQueue.any()) {
        val autoTask = internalTaskQueue.removeFirst()
        doOneInstruction(autoTask.instruction, autoTask.cause, 1)
      }
    }
  }

  fun doOneTaskAtomic(
      taskId: TaskId,
      requireSuccess: Boolean,
      narrowedInstruction: Instruction? = null,
  ): ExecutionResult {
    require(!spent)
    spent = true

    val requestedTask: Task = game.taskQueue[taskId]

    // check narrowing TODO
    val instruction = narrowedInstruction ?: requestedTask.instruction
    internalTaskQueue += InternalTask(instruction, requestedTask.cause)

    return try {
      doAtomic {
        while (internalTaskQueue.any()) {
          val autoTask = internalTaskQueue.removeFirst()
          doOneInstruction(autoTask.instruction, autoTask.cause, 1)
        }
      }
    } catch (e: Exception) {
      if (requireSuccess || isProgrammerError(e)) throw e

      val taskWithExplanation = requestedTask.copy(whyPending = e.message)
      game.taskQueue.replaceTask(taskWithExplanation)
      ExecutionResult(listOf(), setOf(taskWithExplanation.id), fullSuccess = false)
    }
  }

  private fun doAtomic(block: () -> Unit): ExecutionResult {
    val checkpoint = game.gameLog.checkpoint()
    try {
      block()
    } catch (e: Exception) {
      game.rollBack(checkpoint)
      throw e
    }
    return game.gameLog.resultsSince(checkpoint, fullSuccess = true)
  }

  data class ExecutionResult
  constructor(
      val changes: List<ChangeEvent>, // TODO Set
      val newTaskIdsAdded: Set<TaskId>,
      val fullSuccess: Boolean,
  ) {
    companion object {
      fun concat(results: List<ExecutionResult>): ExecutionResult {
        return ExecutionResult(
            results.flatMap { it.changes },
            results.flatMap { it.newTaskIdsAdded }.toSetStrict(),
            results.all { it.fullSuccess })
      }
    }
  }

  fun applyChangeAndFireTriggers(
      count: Int = 1,
      gaining: Component? = null,
      removing: Component? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ) {
    if (handleSpecialTypes(gaining, removing, amap)) return
    val change = game.components.applySingleChange(count, gaining, removing, amap) ?: return
    val event = game.gameLog.addChangeEvent(change, cause)
    if (doEffects) fireTriggers(event)
  }

  private fun handleSpecialTypes(
      gaining: Component?,
      removing: Component?,
      amap: Boolean,
  ): Boolean {
    if (gaining?.expressionFull == OK.expr) { // TODO more principled
      require(removing == null)
      return true
    }
    if (!amap && gaining?.expressionFull == DIE.expr) {
      throw RuntimeException("fix this")
    }
    return false
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val gained: Expression = triggerEvent.change.gaining ?: return
    val gainedComponent: Component = game.toComponent(gained)

    val activeEffects: Multiset<ActiveEffect> = game.components.allActiveEffects()

    val firedSelfEffects: List<FiredEffect> =
        gainedComponent.activeEffects.mapNotNull { it.onChangeToSelf(triggerEvent, game) }

    val firedOtherEffects: List<FiredEffect> =
        activeEffects.elements.mapNotNull { afx ->
          afx.onChangeToOther(triggerEvent, game)?.let { it * activeEffects.count(afx) }
        }

    val (now, later) = (firedSelfEffects + firedOtherEffects).partition { it.automatic }

    // TODO always add to beginning of queue? why not just recurse?
    internalTaskQueue.addAll(0, now.map { InternalTask(it.scaledInstruction(), it.cause) })

    later.forEach {
      game.taskQueue.addTasks(it.scaledInstruction(), it.cause.actor!!, it.cause) // TODO
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
          applyChangeAndFireTriggers(
              count, toComponent(gaining), toComponent(removing), amap, cause)
        }
      }

  private fun toComponent(type: Type?) = type?.let { Component.ofType(game.loader.resolve(it)) }

  fun doOneInstruction(instr: Instruction, cause: Cause?, multiplier: Int) {
    if (multiplier == 0) return
    require(multiplier > 0)

    when (instr) {
      is Change -> {
        applyChangeAndFireTriggers(
            count = instr.count * multiplier,
            gaining = game.toComponent(instr.gaining),
            removing = game.toComponent(instr.removing),
            amap = isAmap(instr),
            cause = cause)
      }

      is Per -> doOneInstruction(instr.instruction, cause, game.count(instr.metric) * multiplier)
      is Gated -> {
        if (game.evaluate(instr.gate)) {
          doOneInstruction(instr.instruction, cause, multiplier)
        } else if (instr.mandatory) {
          throw RequirementException("Requirement not met: ${instr.gate}")
        } // else just do nothing
      }

      is Custom -> handleCustomInstruction(instr, cause)
      is Then -> instr.instructions.forEach { doOneInstruction(it, cause, multiplier) }
      is Or -> throw AbstractInstructionException(instr)
      is Multi -> error("should have been split")
      is Transform -> error("should have been transformed already")
    }
  }

  private fun isAmap(instr: Change) =
      when (instr.intensity) {
        OPTIONAL,
        null,
        -> throw AbstractInstructionException(instr, instr.intensity)

        MANDATORY -> false
        AMAP -> true
      }

  private fun handleCustomInstruction(instr: Custom, cause: Cause?) {
    // TODO could inject this earlier
    val custom = game.setup.authority.customInstruction(instr.functionName)
    val arguments = instr.arguments.map { game.resolve(it) }
    try {
      val translated: Instruction = custom.translate(game.reader, arguments)
      val xer =
          CompositeTransformer(
              ReplaceShortNames(game.loader),
              InsertDefaults(game.loader), // TODO context component??
              Deprodify(game.loader),
              // Not needed: ReplaceThisWith, ReplaceOwnerWith, FixUnownedEffect
          )
      doOneInstruction(xer.transform(translated), cause, 1)
    } catch (e: ExecuteInsteadException) {
      // this custom fn chose to override execute() instead of translate()
      custom.execute(game.reader, writer, arguments)
    }
  }
}

fun isProgrammerError(e: Exception): Boolean =
    e is NullPointerException ||
        e is IllegalArgumentException ||
        e is IllegalStateException ||
        e is NoSuchElementException
