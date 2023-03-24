package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.CustomInstruction.ExecuteInsteadException
import dev.martianzoo.tfm.api.Exceptions.AbstractInstructionException
import dev.martianzoo.tfm.api.Exceptions.RequirementException
import dev.martianzoo.tfm.api.Exceptions.UserException
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.GameStateWriter
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PureTransformers.replaceOwnerWith
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
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
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MType
import kotlin.Int.Companion.MAX_VALUE

/** A view of a [Game] specific to a particular [Actor] (a player or the engine). */
public class PlayerAgent internal constructor(private val game: Game, public val actor: Actor) {

  public val reader = object : GameStateReader by game.reader {
    override fun resolve(expression: Expression): MType = game.resolve(heyItsMe(expression))
    override fun evaluate(requirement: Requirement) = game.reader.evaluate(heyItsMe(requirement))
    override fun count(metric: Metric) = game.reader.count(heyItsMe(metric))
    override fun countComponent(concreteType: Type) = game.reader.countComponent(concreteType)
  }

  private val insertOwner: PetTransformer =
      if (actor == Actor.ENGINE) {
        game.loader.transformers.deprodify()
      } else {
        transformInSeries(replaceOwnerWith(actor.className), game.loader.transformers.deprodify())
      }

  @Suppress("UNCHECKED_CAST")
  private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(insertOwner::transform) as P

  public fun evaluate(requirement: Requirement) = reader.evaluate(requirement)
  public fun count(metric: Metric) = reader.count(metric)
  public fun getComponents(type: Expression) = game.getComponents(reader.resolve(type))

  public fun quietChange(
      count: Int = 1,
      gaining: Expression? = null,
      removing: Expression? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ): ChangeEvent? {
    when (gaining) {
      OK.expr -> {
        require(removing == null)
        return null
      }

      DIE.expr -> {
        if (amap) return null
        throw UserException("Attempt to gain the `Die` component by $cause")
      }
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
    val checkpoint = game.checkpoint()
    val requestedTask: Task = game.taskQueue[taskId]
    // require(requestedTask.actor == actor)

    val prepped = heyItsMe(narrowed)
    prepped?.ensureReifies(requestedTask.instruction, game.einfo)
    val instruction = prepped ?: requestedTask.instruction

    return try {
      doAtomic {
        doInstruction(instruction, requestedTask.cause)
        game.removeTask(taskId)
      }
    } catch (e: UserException) {
      if (requestedTask.whyPending != null) throw e
      val explainedTask = requestedTask.copy(whyPending = e.message)
      game.taskQueue.replaceTask(explainedTask)
      game.eventLog.activitySince(checkpoint)
    }
  }

  public fun removeTask(taskId: TaskId) {
    game.removeTask(taskId)
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
        val scal =
            instruction.count as? ActualScalar
                ?: throw AbstractInstructionException(instruction, "Need a value for X")
        writer.write(
            count = scal.value * multiplier,
            gaining = instruction.gaining?.let(game::resolve),
            removing = instruction.removing?.let(game::resolve),
            amap = isAmap(instruction),
            cause = cause)
      }
      is Gated -> {
        if (game.reader.evaluate(instruction.gate)) {
          doInstruction(instruction.instruction, cause, multiplier)
        } else if (instruction.mandatory) {
          throw RequirementException("Requirement not met: ${instruction.gate}")
        } // else just do nothing
      }
      is Per ->
          doInstruction(instruction.instruction,
              cause,
              game.reader.count(instruction.metric) * multiplier)
      is Custom -> handleCustomInstruction(instruction, cause)
      // TODO this is a bit wrong
      is Then -> split(instruction.instructions).forEach { doInstruction(it, cause, multiplier) }
      is Or -> throw AbstractInstructionException(instruction)
      is Multi -> error("should have been split: $instruction")
      is Transform -> error("should have been transformed already: $instruction")
    }
  }

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val firedSelfEffects: List<FiredEffect> =
        listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
            .map(game::toComponent)
            .flatMap { it.effects(game) }
            .mapNotNull { it.onChangeToSelf(triggerEvent) }

    val firedOtherEffects: List<FiredEffect> =
        game.allActiveEffects().entries.mapNotNull { (afx, count) ->
          afx.onChangeToOther(triggerEvent)?.let { it * count }
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
          val g = gaining?.expressionFull
          val r = removing?.expressionFull

          fun doIt() = quietChange(count, g, r, amap, cause)
          val event = try {
            doIt()
          } catch (e: ExistingDependentsException) {
            for (dept in e.dependents) {
              write(MAX_VALUE, removing = dept.mtype, amap = true, cause = cause)
            }
            doIt()
          }
          event?.let { fireTriggers(it) }
        }
      }

  private fun handleCustomInstruction(instr: Custom, cause: Cause?, multiplier: Int = 1) {
    require(multiplier > 0)
    val arguments = instr.arguments.map(game::resolve)
    if (arguments.any { it.abstract }) {
      throw AbstractInstructionException(instr, "abstract arguments in: $arguments")
    }
    // TODO could inject this earlier
    val custom = game.setup.authority.customInstruction(instr.functionName)
    try {
      val translated: Instruction = custom.translate(game.reader, arguments) * multiplier
      val xers = game.loader.transformers
      val instruction =
          transformInSeries(
                  xers.useFullNames(),
                  xers.atomizer(),
                  xers.insertDefaults(THIS.expr), // TODO context component??
                  xers.deprodify(),
                  replaceOwnerWith(actor.className)
                  // Not needed: ReplaceThisWith, FixUnownedEffect
                  )
              .transform(translated)
      split(instruction).forEach {
        game.taskQueue.addTasks(it, actor, cause)
      }
    } catch (e: ExecuteInsteadException) {
      // this custom fn chose to override execute() instead of translate()
      for (it in 1..multiplier) {
        custom.execute(game.reader, writer, arguments)
      }
    }
  }
  public fun doAtomic(block: () -> Unit): Result {
    val checkpoint = game.checkpoint()
    try {
      block()
    } catch (e: Exception) {
      game.rollBack(checkpoint)
      throw e
    }
    return game.eventLog.activitySince(checkpoint)
  }

  fun isProgrammerError(e: Exception): Boolean =
      e is NullPointerException ||
          e is IllegalArgumentException ||
          e is IllegalStateException ||
          e is NoSuchElementException

  fun tasks(): Map<TaskId, Task> {
    return game.taskQueue.taskMap.toMap()
  }
}
