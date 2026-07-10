package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.PetsCompletionRoot
import dev.martianzoo.script.ScriptSession

internal class ExecCommand(private val repl: ScriptSession) : ScriptCommand("exec") {
  override val usage = "exec <Instruction>"
  override val help =
      """
        Initiates the specified instruction; see syntax.md on github for details on syntax. If
        `auto` mode is on, it will also try to execute any tasks that result from this. Otherwise
         use `tasks` to see which tasks are waiting for you.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.INSTRUCTION)

  override fun withArgs(args: String) = repl.describeExecutionResults(repl.access().exec(args))
}
