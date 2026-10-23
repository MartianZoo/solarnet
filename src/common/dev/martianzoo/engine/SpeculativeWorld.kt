package dev.martianzoo.engine

/**
 * Existing World-copy capability created by [Engine.overlay]. Changes remain local while its source
 * stays unchanged. A source event applied or undone invalidates this World and its retained read
 * views, even if the source later returns to the same state. Guards are absent and cannot be
 * installed; application validation does not automatically follow the copy.
 *
 * TODO(ENGINE_CONTRACT): Investigate removing this API and its reset machinery. Speculative
 *   execution has not been selected in the
 *   [proposed interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md), which remains
 *   under human review. The descriptions here explain existing behavior without approving it as
 *   part of the replacement design.
 */
public interface SpeculativeWorld : World {
  /**
   * Discards this World's local events, component and task edits, commentary changes, and recorded
   * positions after its creation point. Restores the source state and resets the seek floor to that
   * point. Existing read views and clients remain attached to this same World; count listeners
   * observe the restored counts. The source is unchanged.
   *
   * Fails if the source has invalidated this World or a failure-restoring block is running.
   * Mutations attempted from reset listeners are rejected until notification finishes. This method
   * does not clear external client state or recordings held outside the World.
   */
  public fun reset()
}
