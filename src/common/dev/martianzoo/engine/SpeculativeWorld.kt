package dev.martianzoo.engine

/**
 * A [World] whose local branch can be discarded and reused while its source stays unchanged. Agent
 * transaction guards are absent and cannot be installed on this World.
 */
public interface SpeculativeWorld : World {
  /**
   * Clears this World's local events, component changes, pending-task changes, and completed
   * positions without reversing each event. Existing read views and Agents remain attached to this
   * World and see its source state again. Fails during an atomic interaction or after source
   * mutation.
   */
  public fun reset()
}
