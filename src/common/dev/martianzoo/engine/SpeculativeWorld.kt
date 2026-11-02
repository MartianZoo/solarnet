package dev.martianzoo.engine

/**
 * Existing World-copy capability created by [Engine.overlay]. Changes remain local while its source
 * stays unchanged. A source event applied or undone invalidates this World and its retained read
 * views, even if the source later returns to the same state. Guards are absent and cannot be
 * installed; application validation does not automatically follow the copy.
 *
 * Internal copies support the selected transaction-success checks without notifying source
 * listeners. The scope and continued need of this public copy API remain under review; see the
 * [intended interaction contract](../../../../../docs/agents/ENGINE_CONTRACT.md).
 */
public interface SpeculativeWorld : World
