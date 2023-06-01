package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.api.GameReader
import dev.martianzoo.tfm.data.Player
import dev.martianzoo.tfm.engine.Engine.PlayerComponent
import dev.martianzoo.tfm.pets.ast.Metric
import dev.martianzoo.tfm.pets.ast.Requirement
import javax.inject.Inject
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
public class Game
@Inject
constructor(

    /** The current state of the "board". */
    public val components: ComponentGraph,

    /** Everything that has already happened in the game. */
    public val events: EventLog,

    /** What the game is waiting on someone to do. */
    public val tasks: TaskQueue,

    /** Checkpoint, rollback, atomic interactions. */
    public val timeline: Timeline,

    /** Higher-level querying of game state (i.e. in Pets language). */
    public val reader: GameReader,
) {

  internal lateinit var playerComponents: Map<Player, PlayerComponent>

  /** All modifications to game state (except rollbacks) go through here. */
  public fun writer(player: Player) = playerComponents[player]!!.writer

  public fun changesLayer(player: Player) = playerComponents[player]!!.changeLayer
  public fun tasksLayer(player: Player) = changesLayer(player).tasksLayer()
  public fun operationsLayer(player: Player) = tasksLayer(player).operationsLayer()
}
