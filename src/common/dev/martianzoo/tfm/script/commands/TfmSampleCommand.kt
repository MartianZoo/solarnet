package dev.martianzoo.tfm.script.commands

import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.tfm.script.SampleGames
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.UsageException

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

    val agents = SampleGames.sampleGame(gens.toInt())
    repl.agents = agents
    repl.agent = agents[ADMIN] // default autoexec policy
    return listOf("Okay, did that.")
  }
}
