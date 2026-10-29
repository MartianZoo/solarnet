package dev.martianzoo.engine

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.SourceLocation

/** Engine-specific exceptions; game-rule failures use the shared Pets exception types. */
public object Exceptions {
  /**
   * The engine could not finish proving whether an abstract task has a unique legal completion.
   * This does not establish that the request is impossible or that several completions succeed.
   * Failed task transactions restore their game state. Callers can distinguish this result from a
   * known missing choice by checking this subtype of [NotFullySpecifiedException].
   */
  public class IncompleteAnalysisException(
      message: String,
      sourceLocation: SourceLocation? = null,
  ) : NotFullySpecifiedException(message, sourceLocation)

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
