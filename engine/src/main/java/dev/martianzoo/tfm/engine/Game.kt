package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.GameStateReader
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.data.ChangeRecord
import dev.martianzoo.tfm.data.ChangeRecord.Cause
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.Instruction
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Metric.Count
import dev.martianzoo.tfm.pets.ast.Metric.Max
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset
import kotlin.math.min

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: MClassLoader) {

  // PROPERTIES

  internal val components = ComponentGraph()

  internal val fullChangeLog: MutableList<ChangeRecord> = mutableListOf()

  internal val nextOrdinal: Int by fullChangeLog::size

  val pendingAbstractTasks = mutableListOf<Task>()

  data class Task(val instruction: Instruction, val cause: Cause?, val attempts: Int = 0)

  // TYPE & COMPONENT CONVERSION

  fun resolve(expression: Expression): MType = loader.resolve(expression)

  internal fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  internal fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

  // QUERIES

  public fun evaluate(requirement: Requirement) = LiveNodes.from(requirement, this).evaluate(this)

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

  fun execute(
      instruction: Instruction,
      withEffects: Boolean = false,
      initialCause: Cause? = null,
      hidden: Boolean = false,
  ) = executeAll(listOf(instruction), withEffects, initialCause, hidden)

  fun executeAll(
      instruction: List<Instruction>,
      withEffects: Boolean = false,
      initialCause: Cause? = null,
      hidden: Boolean = false,
  ): List<ChangeRecord> =
      ExecutionContext(this, withEffects, hidden).executeAll(instruction, initialCause)

  // CHANGE LOG

  public fun changeLogFull() = fullChangeLog.toList()

  public fun changeLog(): List<ChangeRecord> = fullChangeLog.filterNot { it.hidden }

  public fun rollBack(ordinal: Int) { // TODO kick this out, rolling back starts a new game?
    require(ordinal <= nextOrdinal)
    if (ordinal == nextOrdinal) return
    require(!fullChangeLog[ordinal].hidden)

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

  internal class AbstractInstructionException(s: String) : UserException(s)
}
