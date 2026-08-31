package dev.martianzoo.script.commands

import dev.martianzoo.engine.Agent.TurnLayer
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession

internal class PhaseCommand(private val repl: ScriptSession) : ScriptCommand("phase") {
  override val usage = "phase <phase name>"
  override val help =
      """
        Asks the engine to begin a new phase, e.g. `phase Corporation`
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.phaseNames()

  override fun withArgs(args: String): List<String> {
    // TODO Better way to do it??
    val saved = repl.agent
    return try {
      repl.agent = repl.game.agent(ENGINE) as TurnLayer
      repl.describeExecutionResults(repl.access().phase(args.trim()))
    } finally {
      repl.agent = saved
    }
  }
}
