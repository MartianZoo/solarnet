package dev.martianzoo.engine

import dev.martianzoo.api.GameReader
import dev.martianzoo.data.Player
import dev.martianzoo.engine.Engine.GameScoped
import dev.martianzoo.engine.Engine.PlayerComponent
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import dev.martianzoo.types.MClassTable
import javax.inject.Inject

/**
 * The mutable state of a game in progress. This state is the aggregation of three mutable child
 * objects, which callers access directly: a [ComponentGraph], an [EventLog], and a [TaskQueue].
 * These types embody the present, past, and future of the game state (respectively).
 *
 * These three state objects are read-only, but are always up-to-date (i.e., they are not
 * immutable). Modifying game state is done through [gameplay].
 *
 * The component graph can be queried programmatically, but a [GameReader] is also provided which
 * can answer queries expressed as a Pets [Metric] or [Requirement].
 */
@GameScoped
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

    /** Classes loaded in response to this game setup. */
    public val classes: MClassTable,
) {

  internal lateinit var playerComponents: Map<Player, PlayerComponent>

  public fun gameplay(player: Player): Gameplay = playerComponents[player]!!.gameplay
}
