package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.RequirementException
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Change
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
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
import dev.martianzoo.tfm.types.Transformers.AtomizeGlobalParameterGains
import dev.martianzoo.tfm.types.Transformers.CompositeTransformer
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.UseFullNames
import dev.martianzoo.util.Multiset
import dev.martianzoo.util.toSetStrict

/**
 * Carries out a single instruction, plus any "automatic" triggers that result, with failure
 * atomicity. It writes both components and tasks directly into the game state, then verifies
 * invariants (TODO), and returns the log entries that were generated.
 */
class SingleExecution(val game: Game, val actor: Actor, val doEffects: Boolean = true) {
  // Our internal tasks don't have much relation with Task
  data class InternalTask(val instruction: Instruction, val cause: Cause?) {
    init {
      require(instruction !is Multi) { "$instruction" }
    }
  }

  private val automaticTasks = ArrayDeque<InternalTask>()

  var spent = false

  fun initiateAtomic(
      instruction: Instruction,
      initialCause: Cause?,
  ): ExecutionResult {
    require(!spent)
    spent = true

    return game.doAtomic {
      doOneInstruction(instruction, cause = initialCause, 1)
      while (automaticTasks.any()) {
        val autoTask = automaticTasks.removeFirst()
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
    // require(requestedTask.actor == actor) TODO meh

    narrowedInstruction?.checkReifies(requestedTask.instruction, game.einfo)
    val instruction = narrowedInstruction ?: requestedTask.instruction
    automaticTasks += split(instruction).map { InternalTask(it, requestedTask.cause) }

    return try {
      game.doAtomic {
        while (automaticTasks.any()) {
          val autoTask = automaticTasks.removeFirst()
          doOneInstruction(autoTask.instruction, autoTask.cause, 1)
        }
      }
    } catch (e: Exception) {
      if (requireSuccess) throw e
      handleFailure(e, requestedTask)
      ExecutionResult(listOf(), setOf(requestedTask.id), fullSuccess = false)
    }
  }

  private fun handleFailure(e: Exception, requestedTask: Task) {
    if (isProgrammerError(e)) throw e
    val taskWithExplanation = requestedTask.copy(whyPending = e.message)
    game.taskQueue.replaceTask(taskWithExplanation)
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
    if (gaining?.expressionFull == OK.expr) { // TODO more principled
      require(removing == null)
      return
    } else if (!amap && gaining?.expressionFull == DIE.expr) {
      throw RuntimeException("fix this")
    }
    val change = game.components.applySingleChange(count, gaining, removing, amap) ?: return
    val event = game.eventLog.addChangeEvent(change, actor, cause)
    if (doEffects) fireTriggers(event)
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val gained: Expression = triggerEvent.change.gaining ?: return
    val gainedComponent: Component = game.toComponent(gained)

    val activeEffects: Multiset<ActiveEffect> = game.components.allActiveEffects()

    val firedSelfEffects: List<FiredEffect> =
        gainedComponent.activeEffects.mapNotNull { it.onChange(triggerEvent, game, isSelf = true) }

    val firedOtherEffects: List<FiredEffect> =
        activeEffects.entries.mapNotNull { (afx, count) ->
          afx.onChange(triggerEvent, game, isSelf = false)?.let { it * count }
        }

    val (now, later) = (firedSelfEffects + firedOtherEffects).partition { it.automatic }

    // TODO always add to beginning of queue? why not just recurse?
    val elements = now.flatMap { fx -> split(fx.instruction).map { InternalTask(it, fx.cause) } }
    automaticTasks.addAll(0, elements)

    later.forEach {
      game.taskQueue.addTasks(it.instruction, it.actor, it.cause) // TODO
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

  private fun doOneInstruction(instr: Instruction, cause: Cause?, multiplier: Int) {
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
      is Multi -> error("should have been split: $instr")
      is Transform -> error("should have been transformed already: $instr")
    }
  }

  private fun isAmap(instr: Change) =
      when (instr.intensity) {
        OPTIONAL,
        null -> throw AbstractInstructionException(instr, instr.intensity)
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
              UseFullNames(game.loader),
              AtomizeGlobalParameterGains(game.loader),
              InsertDefaults(game.loader), // TODO context component??
              Deprodify(game.loader),
              // Not needed: ReplaceThisWith, ReplaceOwnerWith, FixUnownedEffect
          )
      val split = split(xer.transform(translated))
      split.forEach {
        try {
          game.doAtomic { doOneInstruction(it, cause, 1) }
        } catch (e: Exception) {
          if (isProgrammerError(e)) throw e
          game.taskQueue.addTasks(it, actor, cause, e.message)
        }
      }
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