package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.tfm.data.GameSetup

internal class NewGameCommand(private val repl: ScriptSession) : ScriptCommand("newgame") {
  override val usage = "newgame <bundles> <player count> [purple]"
  override val help =
      """
        Erases your current game and starts a new one. You can't undo that (but you can get your
        command history out of ~/.rego_session and replay it.) For <bundles>, jam some letters
        together: B=Base, R=coRpoRate eRa, M=Tharsis, H=Hellas, X=Promos, and the rest are what
        you'd think. The player count can be from 1 to 5, but if you choose 1, you are NOT getting
        any of the actual solo rules!

        Add `purple` at the end to run in purple mode, where the engine controls the game flow
        automatically and you only need to respond to tasks.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      when (context.argIndex) {
        0 -> context.bundleSuggestions()
        1 -> (1..5).map { ScriptCompletion(it.toString(), "player counts") }
        2 -> context.completions("purple", group = "workflow modes")
        else -> emptyList()
      }

  @Suppress("TooGenericExceptionCaught") // TODO investigate
  override fun withArgs(args: String): List<String> {
    try {
      val parts = args.trim().split(Regex("\\s+"))
      val purple = parts.getOrNull(2) == "purple"
      val bundleString = parts.getOrNull(0) ?: throw UsageException()
      val playerCount = parts.getOrNull(1)?.toInt() ?: throw UsageException()

      repl.setup = GameSetup(repl.setup.authority, bundleString, playerCount)
      repl.newGame(repl.setup, purple)

      return listOf("New $playerCount-player game created with bundles: $bundleString") +
          (if (purple) listOf("Purple mode: workflow active") else listOf()) +
          (if (playerCount == 1) listOf("NOTE: No solo mode rules are implemented.") else listOf())
    } catch (e: RuntimeException) {
      throw UsageException(e.message)
    }
  }
}
