package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.data.Player
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.repl.SampleGames

internal class TfmSampleCommand(val repl: ReplSession) : ReplCommand("tfm_sample") {
  override val usage: String = "sample <id> <generations>"
  override val help = """
    Executes a sample game so you have useful stuff to look at. For now the only id we have is
    "A" so enjoy it. After that say how many generations of the sample game you want it to play
    through; 0 means to stop right after the prelude phase.
  """.trimIndent()

  override fun withArgs(args: String): List<String> {
    val (id, gens) = args.trim().split(Regex("\\s+"))
    if (id != "A") throw UsageException("unknown id: $id")

    repl.game = SampleGames.sampleGame(gens.toInt())
    repl.setup = repl.game.setup
    repl.tfm = repl.game.tfm(Player.ENGINE) // default autoexec mode
    return listOf("Okay, did that.")
  }
}
