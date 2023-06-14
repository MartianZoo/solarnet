package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.tfm.repl.ReplSession.UsageException

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

    // This is a sad way to do it TODO
    val saved = repl.tfm
    return try {
      repl.tfm = repl.game.tfm(repl.player(player))
      repl.command(rest)
    } finally {
      repl.tfm = saved
    }
  }
}
