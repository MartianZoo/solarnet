package dev.martianzoo.engine

/**
 * Optional game-wide check of work submitted through Agents. [validate] runs while the Agent's
 * transaction can still roll back. Implementations may annotate events, but must not mutate game
 * components or tasks. A guard is installed once on a World and shared by every Agent using it.
 */
public interface AgentTransactionGuard {
  public fun validate()
}
