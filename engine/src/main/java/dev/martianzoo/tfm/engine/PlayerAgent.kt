package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.api.GameWriter
import dev.martianzoo.tfm.api.SpecialClassNames.DIE
import dev.martianzoo.tfm.api.SpecialClassNames.OK
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.StateChange
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.ActiveEffect.FiredEffect
import dev.martianzoo.tfm.engine.Exceptions.ExistingDependentsException
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.PureTransformers.replaceOwnerWith
import dev.martianzoo.tfm.pets.PureTransformers.transformInSeries
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MType
import kotlin.Int.Companion.MAX_VALUE

/** A view of a [Game] specific to a particular [Player] (a player or the engine). */
public class PlayerAgent internal constructor(private val game: Game, public val player: Player) {

  public val reader =
      object : GameReader by game.reader {
        override fun resolve(expression: Expression): MType = game.resolve(heyItsMe(expression))

        override fun evaluate(requirement: Requirement) =
            game.reader.evaluate(heyItsMe(requirement))

        override fun count(metric: Metric) = game.reader.count(heyItsMe(metric))
      }

  private val insertOwner: PetTransformer =
      if (player == Player.ENGINE) {
        game.loader.transformers.deprodify()
      } else {
        transformInSeries(replaceOwnerWith(player.className), game.loader.transformers.deprodify())
      }

  @Suppress("UNCHECKED_CAST")
  private fun <P : PetNode?> heyItsMe(node: P): P = node?.let(insertOwner::transform) as P

  public fun evaluate(requirement: Requirement) = reader.evaluate(requirement)
  public fun count(metric: Metric) = reader.count(metric)
  public fun getComponents(type: Expression) = game.getComponents(reader.resolve(type))

  public fun sneakyChange(
      count: Int = 1,
      gaining: Expression? = null,
      removing: Expression? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  ): ChangeEvent? {
    when (gaining) {
      OK.expression -> {
        if (removing != null) throw UserException("Can't remove Ok, ok?")
        return null
      }
      DIE.expression -> {
        return if (amap) {
          null
        } else {
          throw UserException.die(cause)
        }
      }
    }

    val toGain = game.toComponent(heyItsMe(gaining))
    val toRemove = game.toComponent(heyItsMe(removing))
    val change: StateChange? = game.components.update(count, toGain, toRemove, amap)
    return game.eventLog.addChangeEvent(change, player, cause)
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
    return doAtomic {
      val excon = ExecutionContext(reader, writer, game.loader.transformers, player, initialCause)
      for (instr in fixed) {
        excon.doInstruction(instr)
      }
    }
  }

  public fun doTask(taskId: TaskId, narrowed: Instruction? = null): Result {
    val checkpoint = game.checkpoint()
    val requestedTask: Task = game.getTask(taskId)
    // require(requestedTask.player == player)

    val prepped = heyItsMe(narrowed)
    prepped?.ensureReifies(requestedTask.instruction, game.einfo)
    val instruction = prepped ?: requestedTask.instruction

    return try {
      doAtomic {
        val excon =
            ExecutionContext(reader, writer, game.loader.transformers, player, requestedTask.cause)
        excon.doInstruction(instruction)
        game.removeTask(taskId)
      }
    } catch (e: UserException) {
      if (requestedTask.whyPending != null) throw e
      val explainedTask = requestedTask.copy(whyPending = e.message)
      game.taskQueue.replaceTask(explainedTask)
      game.eventLog.activitySince(checkpoint)
    }
  }

  public fun removeTask(taskId: TaskId) = game.removeTask(taskId)

  private fun fireTriggers(triggerEvent: ChangeEvent) {
    val firedSelfEffects: List<FiredEffect> =
        listOfNotNull(triggerEvent.change.gaining, triggerEvent.change.removing)
            .map(game::toComponent)
            .flatMap { it.activeEffects(game) }
            .mapNotNull { it.onChangeToSelf(triggerEvent) }

    val firedOtherEffects: List<FiredEffect> =
        game.activeEffects().mapNotNull { it.onChangeToOther(triggerEvent) }

    val (now, later) = (firedSelfEffects + firedOtherEffects).partition { it.automatic }
    for (fx in now) {
      val excon = ExecutionContext(reader, writer, game.loader.transformers, player, fx.cause)
      for (instr in split(fx.instruction)) {
        excon.doInstruction(instr)
      }
    }
    game.taskQueue.addTasks(later) // TODO what was this TODO about?
  }

  internal val writer =
      object : GameWriter {
        override fun update(
            count: Int,
            gaining: Type?,
            removing: Type?,
            amap: Boolean,
            cause: Cause?,
        ) {
          val g = gaining?.expressionFull
          val r = removing?.expressionFull

          fun tryIt(): ChangeEvent? = sneakyChange(count, g, r, amap, cause)
          val event =
              try {
                tryIt()
              } catch (e: ExistingDependentsException) {
                for (dept in e.dependents) {
                  update(MAX_VALUE, removing = dept.mtype, amap = true, cause = cause)
                }
                tryIt()
              } ?: return

          fireTriggers(event)
        }

        override fun addTasks(instruction: Instruction, taskOwner: Player, cause: Cause?) {
          game.taskQueue.addTasks(instruction, taskOwner, cause)
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

  fun tasks(): Map<TaskId, Task> = game.taskQueue.taskMap.toMap()
}
