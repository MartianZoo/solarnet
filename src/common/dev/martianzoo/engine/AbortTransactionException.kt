package dev.martianzoo.engine

/** Requests rollback of the current transaction without reporting a failure. */
internal class AbortTransactionException : Exception()
