package dev.martianzoo.engine

/**
 * Existing World-copy capability created by [Engine.overlay]. Changes remain local while its source
 * stays unchanged. A source event applied or undone invalidates this World and its retained read
 * views, even if the source later returns to the same state. Guards are absent and cannot be
 * installed; application validation does not automatically follow the copy.
 *
 * The engine creates these copies to check possible transactions without notifying source
 * listeners. Agent uses this marker to reject installing an application guard on such a copy.
 */
public interface SpeculativeWorld : World
