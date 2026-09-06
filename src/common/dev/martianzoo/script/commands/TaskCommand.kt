package dev.martianzoo.script.commands

import dev.martianzoo.pets.data.TaskResult
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class TaskCommand(private val repl: ScriptSession) : ScriptCommand("task") {
  override val usage = "task <Instruction> | task <select | drop>"
  override val help =
      """
        Carry out the pending task matched by an instruction, narrowing it when needed. For
        example, `task -2 Plant<Player1>` can resolve a queued
        `-3 StandardResource<Anyone>?`. The instruction must match only one pending task, though
        identical tasks are interchangeable and a selected task always wins. `task select` and
        `task drop` are available only when exactly one task is pending.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> {
    val actions =
        if (context.argIndex == 0) {
          context.completions("drop", "select", group = "task actions")
        } else {
          emptyList()
        }
    return actions + context.petsWords(PetsCompletionRoot.INSTRUCTION)
  }

  override fun withArgs(args: String): List<String> {
    val result: TaskResult =
        when (args) {
          "drop" -> {
            repl.access().dropTask(repl.onlyTask().id)
            return listOf("Task deleted")
          }
          "select" -> {
            repl.agent.selectTask(repl.onlyTask().id)
            return repl.taskLines()
          }
          else -> repl.agent.tryTask(args)
        }
    return repl.describeExecutionResults(result)
  }
}
