package dev.martianzoo.agent

import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Task.TaskId
import dev.martianzoo.state.TaskResult

/**
 * One caller-held, provisional narrowing of a task. Discarding this object changes no game state.
 */
public class TaskDraft
internal constructor(
    public val taskId: TaskId,
    initialInstruction: InstructionTree,
    private val agent: AgentImpl,
) {
  private var currentInstruction: InstructionTree = initialInstruction

  /** Rechecks this draft against the engine's current task and returns its instruction. */
  public val instruction: InstructionTree
    get() = agent.recheckDraft(taskId, currentInstruction).also { currentInstruction = it }

  /** Remembers one further narrowing locally; it must narrow the current draft. */
  public fun narrow(narrowing: String): InstructionTree {
    val previous = instruction
    val proposed = agent.prepareDraftNarrowing(taskId, narrowing)
    proposed.ensureNarrows(previous, agent.reader)
    currentInstruction = proposed
    return proposed
  }

  /** Submits the accumulated narrowing to the engine, selecting this task if necessary. */
  public fun commit(): TaskResult = agent.commitDraft(taskId, instruction)
}
