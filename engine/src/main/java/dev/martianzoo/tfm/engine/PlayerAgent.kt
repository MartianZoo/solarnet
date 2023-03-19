package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
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
import dev.martianzoo.tfm.pets.PetTransformer
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
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.Transformers.AtomizeGlobalParameters
import dev.martianzoo.tfm.types.Transformers.Deprodify
import dev.martianzoo.tfm.types.Transformers.InsertDefaults
import dev.martianzoo.tfm.types.Transformers.ReplaceOwnerWith
import dev.martianzoo.tfm.types.Transformers.UseFullNames
import dev.martianzoo.tfm.types.Transformers.transformInSeries
import dev.martianzoo.util.Multiset

public class PlayerAgent(val game: Game, val actor: Actor) {
  private val setOwner: PetTransformer =
      if (actor == Actor.ENGINE) {
        Deprodify(game.loader)
      } else {
        transformInSeries(ReplaceOwnerWith(actor.className), Deprodify(game.loader))
      }

  @Suppress("UNCHECKED_CAST")
  private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(setOwner::transform) as P

  public fun evaluate(requirement: Requirement) = game.evaluate(heyItsMe(requirement))

  public fun count(metric: Metric) = game.count(heyItsMe(metric))

  public fun getComponents(type: Expression) = game.getComponents(game.resolve(heyItsMe(type)))

  public fun quietChange(
      count: Int = 1,
      gaining: Expression? = null,
      removing: Expression? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ): ChangeEvent? {
    if (gaining == OK.expr) { // TODO more principled
      require(removing == null)
      return null
    } else if (!amap && gaining == DIE.expr) {
      throw RuntimeException("fix this")
    }
    val change =
        game.components.update(
            count,
            game.toComponent(heyItsMe(gaining)),
            game.toComponent(heyItsMe(removing)),
            amap,
        )
    return change?.let { game.eventLog.addChangeEvent(it, actor, cause) }
  }

  fun enqueueTasks(instruction: Instruction): Result = doAtomic {
    game.taskQueue.addTasks(heyItsMe(instruction), actor, cause = null)
  }

  /**
   * Attempts to carry out the entirety of [instruction] "manually" or "out of the blue", plus any
   * *automatic* triggered effects that result. If any of that fails the game state will remain
   * unchanged and an exception will be thrown. If it succeeds, any non-automatic triggered effects
   * will be left in the task queue. No other changes to the task queue will happen (for example,
   * existing tasks are left alone, and [instruction] itself is never left enqueued.
   *
   * @param [instruction] an instruction to be performed as-is (no transformations will be applied)
   */
  fun initiate(instruction: Instruction, initialCause: Cause? = null): Result {
    val fixed = split(heyItsMe(instruction))
    return doAtomic { fixed.forEach { doInstruction(it, cause = initialCause) } }
  }

  public fun doTask(taskId: TaskId, narrowed: Instruction? = null): Result {
    val cp = game.eventLog.checkpoint()
    val requestedTask: Task = game.taskQueue[taskId]
    // require(requestedTask.actor == actor) TODO meh

    val prepped = heyItsMe(narrowed)
    prepped?.checkReifies(requestedTask.instruction, game.einfo)
    val instruction = prepped ?: requestedTask.instruction

    return try {
      doAtomic {
        doInstruction(instruction, requestedTask.cause)
        game.taskQueue.removeTask(taskId)
      }
    } catch (e: Exception) {
      if (isProgrammerError(e)) throw e
      val taskWithExplanation = requestedTask.copy(whyPending = e.message)
      game.taskQueue.replaceTask(taskWithExplanation)
      game.eventLog.resultsSince(cp)
    }
  }

  private fun doInstruction(instruction: Instruction, cause: Cause?, multiplier: Int = 1) {
    if (multiplier == 0) return
    require(multiplier > 0)

    fun isAmap(instr: Change) =
        when (instr.intensity ?: error("$instr")) {
          MANDATORY -> false
          AMAP -> true
          OPTIONAL -> throw AbstractInstructionException(instr, instr.intensity)
        }

    when (instruction) {
      is Change -> {
        writer.write(
            count = instruction.count * multiplier,
            gaining = instruction.gaining?.let(game::resolve),
            removing = instruction.removing?.let(game::resolve),
            amap = isAmap(instruction),
            cause = cause)
      }
      is Gated -> {
        if (game.evaluate(instruction.gate)) {
          doInstruction(instruction.instruction, cause, multiplier)
        } else if (instruction.mandatory) {
          throw RequirementException("Requirement not met: ${instruction.gate}")
        } // else just do nothing
      }
      is Per ->
          doInstruction(instruction.instruction, cause, game.count(instruction.metric) * multiplier)
      is Custom -> handleCustomInstruction(instruction, cause)
      // TODO this is a bit wrong
      is Then -> split(instruction.instructions).forEach { doInstruction(it, cause, multiplier) }
      is Or -> throw AbstractInstructionException(instruction)
      is Multi -> error("should have been split: $instruction")
      is Transform -> error("should have been transformed already: $instruction")
    }
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
    for (fx in now) {
      split(fx.instruction).forEach { doInstruction(it, fx.cause) }
    }
    game.taskQueue.addTasks(later) // TODO
  }

  internal val writer =
      object : GameStateWriter {
        override fun write(
            count: Int,
            gaining: Type?,
            removing: Type?,
            amap: Boolean,
            cause: Cause?,
        ) {
          if (removing != null) {
            val c = game.toComponent(removing.expressionFull)
            val dependents = game.components.dependentsOf(c)
            dependents.entries.forEach { (e, ct) ->
              write(ct, removing = e.mtype, amap = false, cause = cause)
            }
          }
          val event =
              quietChange(count, gaining?.expressionFull, removing?.expressionFull, amap, cause)
          event?.let { fireTriggers(it) }
        }
      }

  private fun handleCustomInstruction(instr: Custom, cause: Cause?, multiplier: Int = 1) {
    require(multiplier > 0)
    // TODO could inject this earlier
    val custom = game.setup.authority.customInstruction(instr.functionName)
    val arguments = instr.arguments.map(game::resolve)
    try {
      val translated: Instruction = custom.translate(game.reader, arguments) * multiplier
      val instruction =
          transformInSeries(
                  UseFullNames(game.loader),
                  AtomizeGlobalParameters(game.loader),
                  InsertDefaults(game.loader), // TODO context component??
                  Deprodify(game.loader),
                  ReplaceOwnerWith(actor.className)
                  // Not needed: ReplaceThisWith, FixUnownedEffect
                  )
              .transform(translated)
      split(instruction).forEach {
        try {
          doAtomic { doInstruction(it, cause) }
        } catch (e: Exception) {
          if (isProgrammerError(e)) throw e
          game.taskQueue.addTasks(it, actor, cause, e.message)
        }
      }
    } catch (e: ExecuteInsteadException) {
      // this custom fn chose to override execute() instead of translate()
      for (it in 1..multiplier) {
        custom.execute(game.reader, writer, arguments)
      }
    }
  }
  internal fun doAtomic(block: () -> Unit): Result {
    val checkpoint = game.eventLog.checkpoint()
    try {
      block()
    } catch (e: Exception) {
      game.rollBack(checkpoint)
      throw e
    }
    return game.eventLog.resultsSince(checkpoint)
  }

  fun isProgrammerError(e: Exception): Boolean =
      e is NullPointerException ||
          e is IllegalArgumentException ||
          e is IllegalStateException ||
          e is NoSuchElementException
}
