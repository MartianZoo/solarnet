package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession

internal class TasksCommand(private val repl: ScriptSession) : ScriptCommand("tasks") {
  override val usage = "tasks"
  override val help =
      """
        List the current Actor's pending tasks in their current order. You can execute them by
        instruction using `task`. The list has no ids; its 1-based positions are available only
        for temporary disambiguation, such as `task 2 Ok`.
      """
  override val isReadOnly = true

  override fun noArgs() = repl.taskLines()
}
