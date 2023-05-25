package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Engine.GameModule
import dev.martianzoo.tfm.engine.Engine.PlayerComponent
import dev.martianzoo.tfm.engine.Engine.PlayerModule
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import javax.inject.Singleton

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers access directly: a [ComponentGraph], an [EventLog], and a [TaskQueue].
 * These types embody the present, past, and future of the game state (respectively).
 *
 * These three state objects are read-only, but are always up-to-date (i.e., they are not
 * immutable). All changes to game state must go through `game.writer(player)`, which returns a
 * [GameWriter]. That type offers only very basic task manipulations, accepting only well-formed
 * Pets [Instruction]s, but it also has a [GameWriter.unsafe] view that enables "cheats".
 *
 * The component graph can be queried programmatically, but a [GameReader] is also provided which
 * can answer queries expressed as a Pets [Metric] or [Requirement].
 */
@Singleton
@dagger.Component(modules = [GameModule::class])
public abstract class Game {
  /** The current state of the "board". */
  public abstract val components: ComponentGraph

  /** Everything that has already happened in the game. */
  public abstract val events: EventLog

  /** What the game is waiting on someone to do. */
  public abstract val tasks: TaskQueue

  /** Checkpoint, rollback, atomic interactions. */
  public abstract val timeline: Timeline

  /** Higher-level querying of game state (i.e. in Pets language). */
  public abstract val reader: GameReader

  internal abstract val setup: GameSetup

  private val writers: Map<Player, GameWriter> by lazy { Engine.writers(this, setup) }

  /** All modifications to game state (except rollbacks) go through here. */
  public fun writer(player: Player): GameWriter = writers[player]!!

  internal abstract fun playerModule(module: PlayerModule): PlayerComponent
}
