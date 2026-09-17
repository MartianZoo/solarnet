package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession

internal class PhaseCommand(private val repl: ScriptSession) : ScriptCommand("phase") {
  override val usage = "phase <phase name>"
  override val help =
      """
        Asks Admin to begin a new phase, e.g. `phase Corporation`
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.phaseNames()

  override fun withArgs(args: String): List<String> {
    // TODO Better way to do it??
    val saved = repl.agent
    return try {
      repl.agent = repl.agents[ADMIN]
      repl.describeExecutionResults(repl.access().phase(args.trim()))
    } finally {
      repl.agent = saved
    }
  }
}
