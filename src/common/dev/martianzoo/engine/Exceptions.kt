package dev.martianzoo.engine

/** Engine-specific control exceptions; game-rule failures use the shared Pets exception types. */
public object Exceptions {
  /**
   * Cancels the innermost [Timeline.atomic] block. That block restores its starting game state and
   * returns an empty result normally; surrounding call handling can still run or fail. Outside such
   * a block, this propagates as an ordinary exception; creating it alone performs no restoration.
   *
   * @constructor Creates a cancellation signal with no additional diagnostic data.
   *
   * TODO(ENGINE_CONTRACT): Investigate whether this public cancellation signal is needed.
   *   Client-controlled rollback must remain available; silently cancelling work does not report a
   *   failed client call.
   */
  public class AbortTransactionException : Exception()
}
