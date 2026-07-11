package dev.martianzoo.tfm.script.commands

import dev.martianzoo.data.Player
import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.tfm.script.SampleGames

internal class TfmSampleCommand(private val repl: ScriptSession) : ScriptCommand("tfm_sample") {
  override val usage: String = "tfm_sample <id> <generations>"
  override val help =
      """
    Executes a sample game so you have useful stuff to look at. For now the only id we have is
    "A" so enjoy it. After that say how many generations of the sample game you want it to play
    through; 0 means to stop right after the prelude phase.
  """
          .trimIndent()

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      when (context.argIndex) {
        0 -> context.completions("A", group = "sample games")
        1 -> (0..8).map { ScriptCompletion(it.toString(), "generations") }
        else -> emptyList()
      }

  override fun withArgs(args: String): List<String> {
    val parts = args.trim().split(Regex("\\s+"))
    if (parts.size != 2) throw UsageException()
    val (id, gens) = parts
    if (id != "A") throw UsageException("unknown id: $id")

    repl.game = SampleGames.sampleGame(gens.toInt())
    repl.setup = repl.game.setup
    repl.gameplay = repl.game.gameplay(Player.ENGINE) as TurnLayer // default autoexec mode
    return listOf("Okay, did that.")
  }
}
