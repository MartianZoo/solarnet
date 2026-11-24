package dev.martianzoo.agent

/**
 * A named client interaction implemented through Agent calls, outside the game model.
 *
 * Definitions are shared across sessions and forks. Use the supplied [Agents] and [Agent] rather
 * than capturing a live game or actor. [Agent.doRoutine] supplies the invocation's atomic
 * restoration and result; the implementation owns argument validation and submits ordinary Agent
 * commands, including their automatic work. The releasing Agent is interpreted by the particular
 * routine.
 */
public fun interface Routine {
  /** Performs this interaction for [agent] in [agents], using its complete [arguments]. */
  public fun perform(
      agents: Agents,
      agent: Agent,
      arguments: List<String>,
      releasedBy: Agent?,
  )
}
