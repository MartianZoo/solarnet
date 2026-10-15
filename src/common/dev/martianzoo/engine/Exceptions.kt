package dev.martianzoo.engine

/** Exception types owned by the engine module. */
public object Exceptions {
  /** Requests rollback of the current transaction without reporting a failure. */
  public class AbortTransactionException : Exception()
}
