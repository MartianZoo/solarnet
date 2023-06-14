package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession

internal class TurnCommand(val repl: ReplSession) : ReplCommand("turn") {
  override val usage = "turn"
  override val help =
      """
        Asks the engine to start a new turn for the current player.
      """
  override fun noArgs() = repl.describeExecutionResults(repl.access().newTurn())
}
