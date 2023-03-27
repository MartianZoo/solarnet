package dev.martianzoo.tfm.api

import dev.martianzoo.tfm.data.GameEvent.ChangeEvent.Cause

/**
 * All "normal" changes to game state should go through this interface.
 */
interface GameWriter {
  /**
   * Performs a change to the game state, which updates the component graph, removes dependents,
   * logs the change to the event log, and triggers matching effects.
   *
   * @param [count] how many of the component to gain/remove (must be positive)
   * @param [gaining] the concrete component type to gain, or `null` if this is a remove
   * @param [removing] the concrete component type to remove, or `null` if this is a gain
   * @param [amap] is this an "as many as possible" change (gain/remove *up to* the [count])?
   * @param [cause] why this change is happening, if known
   */
  fun update(
      count: Int = 1,
      gaining: Type? = null,
      removing: Type? = null,
      amap: Boolean = false,
      cause: Cause? = null,
  )
}
