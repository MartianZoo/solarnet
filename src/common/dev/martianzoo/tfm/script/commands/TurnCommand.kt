package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptSession

internal class TurnCommand(private val repl: ScriptSession) : ScriptCommand("turn") {
  override val usage = "turn"
  override val help =
      """
        Asks the engine to start a new turn for the current player.
      """

  override fun noArgs() = repl.describeExecutionResults(repl.access().newTurn())
}
