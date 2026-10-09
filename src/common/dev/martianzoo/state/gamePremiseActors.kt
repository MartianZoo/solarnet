package dev.martianzoo.state

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.state.Actor.Companion.ADMIN

/** Constructs the runtime Actors occupying the static premise's configured seats. */
public val GamePremise.actors: List<Actor>
  get() = playerNames.map(::Player) + ADMIN
