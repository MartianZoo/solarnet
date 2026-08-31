package dev.martianzoo.engine

import dev.martianzoo.engine.Agent.TurnLayer

/** The live game and Actor-scoped engine facade available to a [Routine]. */
public class RoutineContext(
    public val game: World,
    public val agent: TurnLayer,
)
