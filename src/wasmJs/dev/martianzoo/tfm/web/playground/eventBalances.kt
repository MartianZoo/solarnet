package dev.martianzoo.tfm.web.playground

import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.pets.api.SystemClasses.SIGNAL
import dev.martianzoo.state.GameEvent
import dev.martianzoo.state.GameEvent.ChangeEvent
import dev.martianzoo.state.GameWorld

/** Replays new events only, retaining the exact world used to measure earlier balances. */
internal class EventBalances(premise: GamePremise) {
  private val playback = GameWorld(premise)
  private val signal = playback.reader.resolve(SIGNAL.expression)
  private val values = mutableMapOf<Int, Int>()
  private var applied = 0
  private var lastEvent: GameEvent? = null

  fun canExtend(events: List<GameEvent>): Boolean =
      applied <= events.size && (applied == 0 || events[applied - 1] == lastEvent)

  fun extend(events: List<GameEvent>): Map<Int, Int> {
    require(canExtend(events)) { "The event history changed before its last measured event" }
    for (index in applied until events.size) {
      val event = events[index]
      playback.apply(event)
      if (event is ChangeEvent) {
        val change = event.change
        val component = requireNotNull(change.gaining ?: change.removing)
        if (change.gaining != change.removing || !component.type.isSubtypeOf(signal)) {
          values[event.ordinal] = playback.reader.countComponent(component.type)
        }
      }
      lastEvent = event
    }
    applied = events.size
    return values.toMap()
  }
}

/** Plays the exact history into a game world and reads each balance after its event is applied. */
internal fun eventBalances(premise: GamePremise, events: List<GameEvent>): Map<Int, Int> =
    EventBalances(premise).extend(events)
