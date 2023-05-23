package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.StateChange

internal interface Updater {
  fun update(count: Int, gaining: Component?, removing: Component?): StateChange
}
