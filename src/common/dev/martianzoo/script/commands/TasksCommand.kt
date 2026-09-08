package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession

internal class TasksCommand(private val repl: ScriptSession) : ScriptCommand("tasks") {
  override val usage = "tasks"
  override val help =
      """
        List the current Actor's pending tasks. You can execute them by instruction using `task`.
      """
  override val isReadOnly = true

  override fun noArgs() = repl.taskLines()
}
