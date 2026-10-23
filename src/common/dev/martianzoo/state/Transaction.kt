package dev.martianzoo.state

/** Why a transaction executed when it did, separately from what authorized its work. */
public enum class Motivation {
  /** A client submitted this work for its Actor. */
  CLIENT_REQUEST,

  /** Admin's eager handling policy performed this work. */
  ADMIN_EAGER_POLICY,

  /** The Actor's own configured handling policy performed this work. */
  PLAYER_POLICY,
}

/**
 * One transaction in a world's retained history, identified by its [serial].
 *
 * Every event the transaction owns carries this same value, so an event names its transaction
 * without a lookup and rollback removes a transaction exactly when it removes the events that carry
 * it. A transaction's events are contiguous, and serials do not decrease along event ordinals, so
 * [EventLog] can find a serial's event range from the events alone.
 *
 * [serial] is a position within retained history, not a globally unique execution id. Discarding
 * history frees the serials it removes, and later work reuses them.
 *
 * This records who began the transaction and why it ran, not what was submitted: the events it owns
 * already say which tasks and components the submission touched.
 *
 * [Motivation] explains why execution happened at that moment. It is deliberately separate from
 * [GameEvent.ChangeEvent.Cause], which records what authorized the change: a placement rule can
 * authorize a bonus while Admin's policy explains why the bonus executed now. Drivers supply the
 * motivation when they call the engine; it is not inferred from a task's cause.
 */
public data class Transaction(
    /** This transaction's position in retained history, counting from zero. */
    public val serial: Int,

    /** The Actor whose submitted work began this transaction. */
    public val actor: Actor,

    /** Why this transaction executed when it did. */
    public val motivation: Motivation,
) {
  init {
    require(serial >= 0)
  }

  override fun toString(): String = "T$serial $actor $motivation"
}
