package dev.martianzoo.engine

import dev.martianzoo.pets.data.Actor
import dev.martianzoo.pets.data.GameEvent

/** Optional Catalog policy for translating generic history into its readable Routine vocabulary. */
public fun interface RoutineReplayEncoder {
  public fun encode(world: World, events: List<GameEvent>): List<Entry>

  public sealed interface Entry {
    public val actor: Actor

    public data class Call(
        override val actor: Actor,
        val name: String,
        val arguments: List<String> = emptyList(),
    ) : Entry

    public data class Correction(
        override val actor: Actor,
        val instruction: String,
    ) : Entry
  }
}
