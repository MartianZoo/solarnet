package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange
import dev.martianzoo.tfm.data.Player

internal interface ChangeLogger {
  fun addChangeEvent(change: StateChange, player: Player, cause: Cause?): ChangeEvent
}
