package dev.martianzoo.agent

/**
 * Application validation used by clients through the Agent API before accepting their work. For
 * example, card-identity tracking can require the client to identify an anonymous card movement. A
 * guard is installed once on an [Agents] session and shared by its Agents; direct engine calls
 * bypass it.
 *
 * Implementations may annotate events but must not change components or tasks. Validation can run
 * repeatedly within the same enclosing call and must tolerate a later failure restoring earlier
 * history. External state owned by the guard is not restored by
 * [dev.martianzoo.engine.Timeline.atomic].
 *
 * Despite its name, this is an Agent application check, not the final invariant check of every
 * engine transaction.
 */
public interface AgentTransactionGuard {
  /**
   * Checks the current event history and throws if required application information is missing or
   * inconsistent. [AgentInputRequiredException] identifies missing client input; other failures
   * propagate. A normal return accepts this check only, not completion of every pending task.
   *
   * Agent callers normally invoke this within a failure-restoring block. Calling it directly
   * creates no such protection, and existing event annotations are not automatically restored.
   */
  public fun validate()
}
