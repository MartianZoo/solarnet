package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.ExpressionInfo
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.Type
import dev.martianzoo.tfm.api.UserException
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.TaskEvent
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.data.Task.TaskId
import dev.martianzoo.tfm.engine.EventLog.Checkpoint
import dev.martianzoo.tfm.pets.PetTransformer
import dev.martianzoo.tfm.pets.ast.Expression
import dev.martianzoo.tfm.pets.ast.PetNode
import dev.martianzoo.tfm.pets.ast.Requirement
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.tfm.types.MType
import dev.martianzoo.util.Multiset

/** A game in progress. */
public class Game(val setup: GameSetup, public val loader: MClassLoader) {

  // Mutable game state consists of a component graph, event log, and task queue

  /** The components that make up the game's state. */
  internal val components = ComponentGraph()

  /** A record of everything that's happened in the game. */
  public val eventLog = EventLog()

  /** The tasks the game is currently waiting on. */
  public val taskQueue = TaskQueue(eventLog)

  public lateinit var start: Checkpoint
    private set

  // PLAYER AGENT

  public fun asPlayer(player: Player) = PlayerAgent(this, player)

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
            val requirement: Requirement =
                object : PetTransformer() {
                      override fun <P : PetNode> transform(node: P): P {
                        return if (node is Expression) {
                          val modded = node.addArgs(proposed.expressionFull)
                          try {
                            resolve(modded)
                            @Suppress("UNCHECKED_CAST")
                            modded as P
                          } catch (e: Exception) {
                            node // don't go deeper
                          }
                        } else {
                          transformChildren(node)
                        }
                      }
                }
                    .transform(refin)

            if (!reader.evaluate(requirement)) throw UserException.requirementNotMet(requirement)

            for ((a, b) in abstractTarget.dependencies.asSet.zip(proposed.dependencies.asSet)) {
              checkRefinements(a.boundType, b.boundType)
            }
          }
        }
      }
  val reader = GameReaderImpl(setup, loader, components)

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

  internal fun allActiveEffects(): Multiset<ActiveEffect> = components.allActiveEffects(this)

  internal fun setupFinished() {
    start = checkpoint()
  }
}
