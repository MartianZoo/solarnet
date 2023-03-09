package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeEvent
import dev.martianzoo.tfm.data.ChangeEvent.Cause
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Max
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.pets.ast.Requirement.And
import dev.martianzoo.tfm.pets.ast.Requirement.Exact
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Requirement.Or
import dev.martianzoo.tfm.pets.ast.Requirement.Transform
import dev.martianzoo.tfm.pets.ast.ScaledExpression.Companion.scaledEx
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset
import kotlin.math.min

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: MClassLoader) {

  // PROPERTIES

  internal val components = ComponentGraph()

  internal val fullChangeLog: MutableList<ChangeEvent> = mutableListOf()

  internal val nextOrdinal: Int by fullChangeLog::size

  val pendingTasks: ArrayDeque<Task> = ArrayDeque()

  data class Task(
      val instruction: Instruction,
      val cause: Cause?,
      val reasonPending: String? = null
  )

  // TYPE & COMPONENT CONVERSION

  fun resolve(expression: Expression): MType = loader.resolve(expression)

  internal fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  internal fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

  // QUERIES

  public fun evaluate(requirement: Requirement): Boolean {
    fun count(expression: Expression) = count(Count(scaledEx(expression)))
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
      is Count -> components.count(resolve(metric.scaledEx.expression)) / metric.scaledEx.scalar
      is Max -> min(count(metric.metric), metric.maximum)
    }
  }

  public fun getComponents(type: MType): Multiset<Component> = components.getAll(type)

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

  fun autoExecute( // TODO create execute also, remove withEffects param
      instruction: Instruction,
      withEffects: Boolean = false,
      initialCause: Cause? = null,
      hidden: Boolean = false,
  ): List<ChangeEvent> =
      SingleExecutionContext(this, withEffects, hidden).autoExecute(instruction, initialCause)

  // CHANGE LOG

  public fun changeLogFull() = fullChangeLog.toList()

  public fun changeLog(): List<ChangeEvent> = fullChangeLog.filterNot { it.hidden }

  public fun rollBack(ordinal: Int) { // TODO kick this out, rolling back starts a new game?
    require(ordinal <= nextOrdinal) // TODO protect in callers
    if (ordinal == nextOrdinal) return
    val subList = fullChangeLog.subList(ordinal, nextOrdinal)
    for (entry in subList.asReversed()) {
      val change = entry.change.inverse()
      components.updateMultiset(
          change.count,
          gaining = toComponent(change.gaining),
          removing = toComponent(change.removing),
      )
    }
    subList.clear()
  }
}
