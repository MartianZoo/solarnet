package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType

/**
 * The mutable state of a game in progress. It is essentially an aggregation of three mutable
 * objects: an [EventLog] (representing the "past"), a [ComponentGraph] (representing the
 * "present"), and a [TaskQueue] (representing the "future"). It also has a reference to a frozen
 * [MClassLoader] that it pulls component classes from.
 */
public class Game(
  /**
   * Where the game will get all its information about component types from. Must already be
   * frozen. (TODO MClassTable)
   */
  public val loader: MClassLoader
) {
  // TODO expose only read-only interfaces for these three things

  /** Everything that has happened in this game so far ("past"). */
  public val events = EventLog()

  /** The components that make up the game's current state ("present"). */
  public val components = ComponentGraph()

  /** The tasks the game is currently waiting on ("future"). */
  public val tasks = TaskQueue(events)

  init {
    require(loader.frozen)

    // TODO BAD HACK
    require(loader.game == null)
    loader.game = this
  }

  /** A checkpoint taken when the game initialization process ends. */
  public lateinit var start: Checkpoint
    private set

  // PLAYER AGENT

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

  public fun removeTask(id: TaskId) = tasks.removeTask(id)

  // TYPE & COMPONENT CONVERSION

  public fun resolve(expression: Expression): MType = loader.resolve(expression)

  public fun toComponent(expression: Expression) = Component.ofType(resolve(expression))

  @JvmName("toComponentNullable")
  public fun toComponent(expression: Expression?) =
      expression?.let { Component.ofType(resolve(it)) }

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
            val requirement = RefinementMangler(proposed).transform(refin)
            if (!reader.evaluate(requirement)) throw UserException.requirementNotMet(requirement)

            for ((a, b) in
                abstractTarget.dependencies.typeDependencies.zip(
                    proposed.dependencies.typeDependencies)) {
              checkRefinements(a.boundType, b.boundType)
            }
          }
        }
      }

  // Check MartianIndustries against CardFront(HAS BuildingTag)
  // by testing requirement `BuildingTag<MartianIndustries>`
  // in that example, `proposed` will be `MartianIndustries`
  // and the node being transformed is `BuildingTag`
  inner class RefinementMangler(private val proposed: MType) : PetTransformer() {
    override fun <P : PetNode> transform(node: P): P {
      return if (node is Expression) {
        val tipo = loader.resolve(node)
        try {
          val modded = tipo.specialize(listOf(proposed.expression))
          @Suppress("UNCHECKED_CAST")
          modded.expressionFull as P
        } catch (e: Exception) {
          println(e.message)
          node // don't go deeper
        }
      } else {
        transformChildren(node)
      }
    }
  }

  val reader = GameReaderImpl(loader, components)

  // CHANGE LOG

  public fun checkpoint() = events.checkpoint()

  public fun rollBack(checkpoint: Checkpoint) = rollBack(checkpoint.ordinal)

  public fun rollBack(ordinal: Int) {
    require(ordinal <= events.size)
    if (ordinal == events.size) return
    val subList = events.events.subList(ordinal, events.size)
    for (entry in subList.asReversed()) {
      when (entry) {
        is TaskEvent -> tasks.reverse(entry)
        is ChangeEvent -> {
          val change = entry.change
          components.reverse(
              change.count,
              removeGained = toComponent(change.gaining),
              gainRemoved = toComponent(change.removing),
          )
        }
      }
    }
    subList.clear()
  }

  internal fun activeEffects(): List<ActiveEffect> = components.activeEffects(this)

  internal fun setupFinished() {
    start = checkpoint()
  }

  fun getTask(taskId: TaskId): Task {
    require(taskId in tasks) { taskId }
    return tasks[taskId]
  }
}
