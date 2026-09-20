package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.script.PetsCompletionRoot
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

internal class ExecCommand(private val repl: ScriptSession) : ScriptCommand("exec") {
  override val usage = "exec <Instruction>"
  override val help =
      """
        Initiates the specified instruction; see pets-language-spec.md on github for syntax. If
        `auto` mode is on, it will also try to execute any tasks that result from this. Otherwise
         use `tasks` to see which tasks are waiting for you.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.petsWords(PetsCompletionRoot.INSTRUCTION)

  override fun withArgs(args: String) = repl.describeExecutionResults(repl.access().exec(args))
}
