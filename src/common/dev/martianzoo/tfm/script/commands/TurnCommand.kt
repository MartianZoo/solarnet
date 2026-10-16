package dev.martianzoo.tfm.script.commands

import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.ScriptMode.PURPLE

internal class TurnCommand(private val repl: ScriptSession) : ScriptCommand("turn") {
  override val usage = "turn"
  override val help =
      """
        Requests the current phase's action offer for the current player.
        Available outside purple mode, after pending work has finished.
      """

  override fun noArgs(): List<String> {
    check(repl.mode != PURPLE) { "not allowed in this mode" }
    return repl.describeExecutionResults(repl.agent.beginOperation("OfferAction"))
  }
}
