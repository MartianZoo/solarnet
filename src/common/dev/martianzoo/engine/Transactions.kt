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
internal class Transactions(private val gameWorld: GameWorld) {
  private var open: Transaction? = null
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
   * Records [block]'s events as one transaction attributed to [actor], unless a transaction is
   * already open or initialization has not finished, in which case [block] simply runs.
   *
   * A failed [block] leaves no transaction open, and the serial it would have used stays available,
   * because an enclosing restoration removes the events that would have claimed it.
   */
  internal fun <T> perform(actor: Actor, motivation: Motivation, block: () -> T): T {
    if (initializing || open != null) return block()
    open = Transaction(nextSerial(), actor, motivation)
    return try {
      block()
    } finally {
      open = null
    }
  }

  private fun nextSerial(): Int = (gameWorld.events.lastTransaction?.serial ?: -1) + 1
}
