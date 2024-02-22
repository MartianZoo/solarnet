package dev.martianzoo.repl.commands

import dev.martianzoo.data.Player
import dev.martianzoo.data.Player.Companion
import dev.martianzoo.data.Player.Companion.ENGINE
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession

internal class PhaseCommand(val repl: ReplSession) : ReplCommand("phase") {
  override val usage = "phase <phase name>"
  override val help =
      """
        Asks the engine to begin a new phase, e.g. `phase Corporation`
      """
  override fun withArgs(args: String): List<String> {
    // TODO Better way to do it??
    val saved = repl.gameplay
    return try {
      repl.gameplay = repl.game.gameplay(ENGINE) as TurnLayer
      repl.describeExecutionResults(repl.access().phase(args.trim()))
    } finally {
      repl.gameplay = saved
    }
  }
}
