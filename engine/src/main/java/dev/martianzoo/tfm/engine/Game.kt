package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.engine.SingleExecution.ExecutionResult
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Instruction.Companion.split
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Max
import dev.martianzoo.tfm.pets.ast.Metric.Plus
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.Requirement.Transform
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset
import kotlin.math.min

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: MClassLoader) {

  // PROPERTIES

  public val eventLog = EventLog()

  public val components = ComponentGraph()

  public val taskQueue = TaskQueue(this)

  // TYPE & COMPONENT CONVERSION

  fun resolve(expression: Expression): MType = loader.resolve(expression)

  internal fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  internal fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

  // QUERIES

  public fun evaluate(requirement: Requirement): Boolean {
    fun count(expression: Expression) = count(Count(expression))
    return when (requirement) {
      is Min -> count(requirement.scaledEx.expression) >= requirement.scaledEx.scalar
      is Requirement.Max -> {
        count(requirement.scaledEx.expression) <= requirement.scaledEx.scalar
      }
      is Exact -> count(requirement.scaledEx.expression) == requirement.scaledEx.scalar
      is Or -> requirement.requirements.any { evaluate(it) }
      is And -> requirement.requirements.all { evaluate(it) }
      is Transform -> error("should have been transformed by now")
    }
  }

  public fun count(metric: Metric): Int {
    return when (metric) {
      is Count -> components.count(resolve(metric.expression))
      is Scaled -> count(metric.metric) / metric.unit
      is Max -> min(count(metric.metric), metric.maximum)
      is Plus -> metric.metrics.map { count(it) }.sum()
    }
  }

  public fun getComponents(type: MType): Multiset<Component> = components.getAll(type)

  val einfo =
      object : ExpressionInfo {
        override fun isAbstract(e: Expression) = resolve(e).abstract
        override fun checkReifies(wide: Expression, narrow: Expression) {
          resolve(wide).checkReifies(resolve(narrow))
          // wide might be CityTile<P1, LA(HAS MAX 0 NBR<CT<ANY>>)>
          // narrow might be CityTile<P1, M11>
          // as pure types they check out
          // but check whether `HAS MAX 0 NBR<CT<ANY>, M11>` is true
        }
      }
  val reader =
      object : GameStateReader {
        override val setup by this@Game::setup
        override val authority by setup::authority

        override fun resolve(expression: Expression) = this@Game.resolve(expression)

        override fun evaluate(requirement: Requirement) = this@Game.evaluate(requirement)

        override fun count(metric: Metric) = this@Game.count(metric)

        override fun count(type: Type) = this@Game.components.count(this@Game.loader.resolve(type))

        override fun getComponents(type: Type) =
            components.getAll(loader.resolve(type)).map { it.mtype }
      }

  // EXECUTION

  fun executeWithoutEffects(instruction: Instruction, actor: Actor): ExecutionResult {
    val checkpoint = eventLog.checkpoint()
    split(instruction).forEach {
      SingleExecution(this, actor, doEffects = false).initiateAtomic(it, initialCause = null)
    }
    return eventLog.resultsSince(checkpoint, fullSuccess = true).also {
      require(it.newTaskIdsAdded.none())
    }
  }

  /**
   * Attempts to carry out the entirety of [instruction] "manually" or "out of the blue", plus any
   * automatic triggered effects that result. If any of that fails the game state will remain
   * unchanged and an exception will be thrown. If it succeeds, any non-automatic triggered effects
   * will be left in the task queue. No other changes to the task queue will happen (for example,
   * existing tasks are left alone, and [instruction] itself is never left enqueued.
   *
   * @param [instruction] an instruction to be performed as-is (no transformations will be applied)
   * @param [actor] the instruction will be executed as if by this player (or GAME); in particular,
   *   if unowned components are created that have `This:` triggers, this is who will own those
   *   resulting tasks.
   * @param [fakeCause] optionally, the instruction can be performed as if caused in the way
   *   described by this object
   */
  fun initiate(
      instruction: Instruction,
      actor: Actor,
      fakeCause: Cause?,
  ): ExecutionResult {
    val checkpoint = eventLog.checkpoint()

    val instructions = split(instruction)
    val results = instructions.map { SingleExecution(this, actor).initiateAtomic(it, fakeCause) }
    return eventLog.resultsSince(checkpoint, results.all { it.fullSuccess })
  }

  fun doOneExistingTask(id: TaskId, actor: Actor, narrowed: Instruction? = null): ExecutionResult {
    val result = SingleExecution(this, actor).doOneTaskAtomic(id, true, narrowed)
    require(result.fullSuccess) // should be redundant
    taskQueue.removeTask(id)
    return result
  }

  fun tryOneExistingTask(id: TaskId, actor: Actor, narrowed: Instruction? = null): ExecutionResult {
    val result = SingleExecution(this, actor).doOneTaskAtomic(id, false, narrowed)
    if (result.fullSuccess) taskQueue.removeTask(id)
    return result
  }

  fun enqueueTasks(instruction: Instruction, actor: Actor) =
      doAtomic { taskQueue.addTasks(instruction, actor, cause = null) }

  internal fun doAtomic(block: () -> Unit): ExecutionResult {
    val checkpoint = eventLog.checkpoint()
    try {
      block()
    } catch (e: Exception) {
      rollBack(checkpoint)
      throw e
    }
    return eventLog.resultsSince(checkpoint, fullSuccess = true)
  }

  // CHANGE LOG

  public fun rollBack(checkpoint: Checkpoint) {
    // game?
    val ordinal = checkpoint.ordinal
    require(ordinal <= eventLog.size)
    if (ordinal == eventLog.size) return
    val subList = eventLog.events.subList(ordinal, eventLog.size)
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskAddedEvent -> taskQueue.taskMap.remove(entry.task.id)
        is TaskRemovedEvent -> taskQueue.taskMap[entry.task.id] = entry.task
        is TaskReplacedEvent ->
            require(taskQueue.taskMap.put(entry.task.id, entry.oldTask) == entry.task)
        is ChangeEvent -> {
          val change = entry.change
          components.updateMultiset(
              change.count,
              gaining = toComponent(change.removing),
              removing = toComponent(change.gaining),
          )
        }
      }
    }
    subList.clear()
  }
}
