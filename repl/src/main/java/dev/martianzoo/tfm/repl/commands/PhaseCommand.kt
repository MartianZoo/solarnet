package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession

internal class PhaseCommand(val repl: ReplSession) : ReplCommand("phase") {
  override val usage = "phase <phase name>"
  override val help =
      """
        Asks the engine to begin a new phase, e.g. `phase Corporation`
      """
  override fun withArgs(args: String) =
      repl.describeExecutionResults(repl.access().phase(args.trim()))
}
