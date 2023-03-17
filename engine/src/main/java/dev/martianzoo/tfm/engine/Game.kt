package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskAddedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskRemovedEvent
import dev.martianzoo.tfm.data.GameEvent.TaskReplacedEvent
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.pets.ast.Expression
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

  public fun resolve(expression: Expression): MType = loader.resolve(expression)

  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  public fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

  // QUERIES

  public fun forActor(actor: Actor) = PlayerAgent(this, actor)

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

        override fun count(type: Type) = components.count(loader.resolve(type))

        override fun getComponents(type: Type) =
            components.getAll(loader.resolve(type)).map { it.mtype }
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
