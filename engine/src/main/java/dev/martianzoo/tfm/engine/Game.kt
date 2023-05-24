package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Engine.GameModule
import dev.martianzoo.tfm.engine.Engine.PlayerComponent
import dev.martianzoo.tfm.engine.Engine.PlayerModule
import javax.inject.Singleton

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers accesses directly: a [ComponentGraph], a [TaskQueue], and an [EventLog].
 * These types don't expose mutation operations, but the objects are mutable and always represent
 * the most current state.
 *
 * To read game state at a higher level (e.g. via Pets expressions), use [reader]. To change state
 * use [writer].
 */
@Singleton
@dagger.Component(modules = [GameModule::class])
public abstract class Game {
  /** The current state of the "board". */
  public abstract val components: ComponentGraph

  /** What the game is waiting on someone to do. */
  public abstract val tasks: TaskQueue

  /** Everything that has already happened in the game. */
  public abstract val events: EventLog

  /** Checkpoint, rollback, atomic interactions. */
  public abstract val timeline: Timeline

  /** Higher-level querying of game state (i.e. in Pets language). */
  public abstract val reader: GameReader

  /** All modifications to game state (except rollbacks) go through here. */
  public fun writer(player: Player) = playerModule(PlayerModule(player)).writer

  internal abstract fun playerModule(module: PlayerModule): PlayerComponent
}
