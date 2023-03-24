package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.Exceptions.InvalidReificationException
import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.Actor
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Max
import dev.martianzoo.tfm.pets.ast.Metric.Plus
import dev.martianzoo.tfm.pets.ast.Metric.Scaled
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Counting
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.Requirement.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Scalar.ActualScalar
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset
import kotlin.math.min

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: MClassLoader) {

  // PROPERTIES

  /** A record of everything that's happened in the game. */
  public val eventLog = EventLog()

  /** The components that make up the game's state. */
  internal val components = ComponentGraph()

  /** The tasks the game is currently waiting on. */
  public val taskQueue = TaskQueue(eventLog)

  public lateinit var start: Checkpoint
    private set

  // PLAYER AGENT

  public fun agent(actor: Actor) = PlayerAgent(this, actor) // asActor? TODO

  public fun removeTask(id: TaskId) = taskQueue.removeTask(id)

  // TYPE & COMPONENT CONVERSION

  public fun resolve(expression: Expression): MType = loader.resolve(expression)
  private fun resolve(type: Type): MType = type as? MType ?: loader.resolve(type.expressionFull)

  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  public fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

  public fun getComponents(type: Type): Multiset<Component> = components.getAll(resolve(type))

  val einfo =
      object : ExpressionInfo {
        override fun isAbstract(e: Expression) = resolve(e).abstract
        override fun ensureReifies(wide: Expression, narrow: Expression) {
          val abstractTarget = resolve(wide)
          val proposed = resolve(narrow)
          proposed.ensureReifies(abstractTarget)
          checkRefinements(abstractTarget, proposed)

          // wide might be CityTile<P1, LA(HAS MAX 0 NBR<CT<ANY>>)>
          // narrow might be CityTile<P1, M11>
          // as pure types they check out
          // but check whether `HAS MAX 0 NBR<CT<ANY>, M11>` is true
        }
        private fun checkRefinements(abstractTarget: MType, proposed: MType) {
          val refin = abstractTarget.refinement
          if (refin != null) {
            // HAS 0 NBR<CT<ANY>> -> HAS 0 NBR<CT<ANY>, M11>
            val requirement: Requirement = object : PetTransformer() {
              override fun <P : PetNode> transform(node: P): P {
                return if (node is Expression) {
                  val modded = node.addArgs(proposed.expressionFull)
                  try {
                    resolve(modded)
                    modded as P
                  } catch (e: Exception) {
                    node // don't go deeper
                  }
                } else {
                  transformChildren(node)
                }
              }
            }.transform(refin)
            if (!reader.evaluate(requirement)) {
              throw InvalidReificationException("requirement is not met: $requirement")
            }
            for ((a, b) in abstractTarget.dependencies.asSet.zip(proposed.dependencies.asSet)) {
              checkRefinements(a.boundType, b.boundType)
            }
          }
        }
      }
  val reader =
      object : GameStateReader {
        override val setup by this@Game::setup
        override val authority by setup::authority

        override fun resolve(expression: Expression) = this@Game.resolve(expression)

        override fun evaluate(requirement: Requirement): Boolean =
            when (requirement) {
              is Counting -> {
                val actual = count(Count(requirement.scaledEx.expression))
                val target = (requirement.scaledEx.scalar as ActualScalar).value
                when (requirement) {
                  is Min -> actual >= target
                  is Requirement.Max -> actual <= target
                  is Exact -> actual == target
                }
              }
              is Or -> requirement.requirements.any { evaluate(it) }
              is And -> requirement.requirements.all { evaluate(it) }
              is Transform -> error("should have been transformed by now")
            }

        override fun count(metric: Metric): Int =
            when (metric) {
              is Count -> components.count(resolve(metric.expression))
              is Scaled -> count(metric.metric) / metric.unit
              is Max -> min(count(metric.metric), metric.maximum)
              is Plus -> metric.metrics.map { count(it) }.sum()
            }

        override fun count(type: Type) = components.count(loader.resolve(type))
        override fun countComponent(concreteType: Type) =
            components.countComponent(Component.ofType(resolve(concreteType)))

        override fun getComponents(type: Type) =
            components.getAll(loader.resolve(type)).map { it.mtype }
      }

  // CHANGE LOG

  public fun checkpoint() = eventLog.checkpoint()

  public fun rollBack(checkpoint: Checkpoint) = rollBack(checkpoint.ordinal)

  public fun rollBack(ordinal: Int) {
    require(ordinal <= eventLog.size)
    if (ordinal == eventLog.size) return
    val subList = eventLog.events.subList(ordinal, eventLog.size)
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskEvent -> taskQueue.reverse(entry)
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

  internal fun allActiveEffects(): Multiset<ActiveEffect> = components.allActiveEffects(this)

  internal fun setupFinished() {
    start = checkpoint()
  }
}
