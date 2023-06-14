package dev.martianzoo.repl.commands

import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class ExecCommand(val repl: ReplSession) : ReplCommand("exec") {
  override val usage = "exec <Instruction>"
  override val help =
      """
        Initiates the specified instruction; see syntax.md on github for details on syntax. If
        `auto` mode is on, it will also try to execute any tasks that result from this. Otherwise
         use `tasks` to see which tasks are waiting for you.
      """

  override fun withArgs(args: String) = repl.describeExecutionResults(repl.access().exec(args))
}
