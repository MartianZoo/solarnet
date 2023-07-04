package dev.martianzoo.repl.commands

import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException

internal class AsCommand(val repl: ReplSession) : ReplCommand("as") {
  override val usage = "as <PlayerN> <full command>"
  override val help =
      """
        For any command you could type normally, put `as Player2` etc. or `as Engine` before it.
        It's handled as if you had first `become` that player, then restored.
      """

  override fun noArgs() = throw UsageException()
  override fun withArgs(args: String): List<String> {
    val (player, rest) = args.trim().split(Regex("\\s+"), 2)

    // TODO Better way to do it??
    val saved = repl.gameplay
    return try {
      repl.gameplay = repl.game.gameplay(repl.player(player)) as TurnLayer
      repl.command(rest)
    } finally {
      repl.gameplay = saved
    }
  }
}
