package dev.martianzoo.engine

import dev.martianzoo.state.Actor
import dev.martianzoo.state.GameWorld
import dev.martianzoo.state.Motivation
import dev.martianzoo.state.Transaction

/**
 * The transaction whose events are currently being recorded.
 *
 * A public engine mutation opens one, and the engine's own internal work joins it, so one submitted
 * mutation and the immediate consequences it needs share a transaction. A driver performing
 * automatic handling opens one itself, through [ActorEngine.transaction], to supply its own
 * [Motivation].
 *
 * Serials come from the retained events rather than a counter kept here, so rollback, truncation,
 * and overlay reset need no bookkeeping: the next transaction follows whatever serial the last
 * retained event still carries, which is how a discarded serial comes to be reused.
 */
internal class Transactions(
    private val gameWorld: GameWorld,
    private val promoteDeferredWork: () -> Unit,
) {
  private var open: Transaction? = null
  private var depth = 0
  private var initializing = true

  /** Stops treating subsequent events as part of the initialization prefix. */
  internal fun initializationFinished() {
    initializing = false
  }

  /**
   * The transaction to record on an event being applied now, or null while initializing.
   *
   * @throws IllegalStateException if gameplay produced an event outside any transaction, which
   *   means an engine entry point did not open one
   */
  internal fun forEvent(): Transaction? {
    if (initializing) return null
    return checkNotNull(open) { "an event was produced outside any transaction" }
  }

  /**
   * Records [block]'s events as one transaction attributed to [actor]. A nested call joins the
   * transaction already open instead of starting its own, and initialization opens none at all.
   *
   * Deferred work is promoted once this transaction is complete, so its task edits belong to the
   * transaction that made the work eligible rather than to a moment when the queue happened to be
   * empty. A failed [block] promotes nothing, leaves no transaction open, and keeps the serial it
   * would have used available, because an enclosing restoration removes the events that would have
   * claimed it.
   */
  internal fun <T> perform(actor: Actor, motivation: Motivation, block: () -> T): T {
    if (depth == 0 && !initializing) open = Transaction(nextSerial(), actor, motivation)
    depth++
    return try {
      block().also { if (depth == 1) promoteDeferredWork() }
    } finally {
      depth--
      if (depth == 0) open = null
    }
  }

  private fun nextSerial(): Int = (gameWorld.events.lastTransaction?.serial ?: -1) + 1
}
