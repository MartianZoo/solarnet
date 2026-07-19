package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession

internal class TasksCommand(private val repl: ScriptSession) : ScriptCommand("tasks") {
  override val usage = "tasks"
  override val help =
      """
        List all currently pending tasks. You can then execute or drop them using `task`. The
        tasks of all players plus the engine are currently mixed together (but labeled).
      """
  override val isReadOnly = true

  override fun noArgs() =
      repl.game.tasks.extract { it.toStringWithoutCause(queueAssignee = it.assignee) }
}
