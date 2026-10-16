package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameWorld

/** Plays the exact history into a game world and reads each balance after its event is applied. */
internal fun eventBalances(premise: GamePremise, events: List<GameEvent>): Map<Int, Int> {
  val playback = GameWorld(premise)
  val signal = playback.reader.resolve(SIGNAL.expression)
  return buildMap {
    for (event in events) {
      playback.apply(event)
      if (event !is ChangeEvent) continue
      val change = event.change
      val component = requireNotNull(change.gaining ?: change.removing)
      if (change.gaining == change.removing && component.type.isSubtypeOf(signal)) continue
      put(event.ordinal, playback.reader.countComponent(component.type))
    }
  }
}
