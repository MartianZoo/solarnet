package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException

internal class NewGameCommand(private val repl: ScriptSession) : ScriptCommand("newgame") {
  override val usage = "newgame <options> <player count> [purple]"
  override val help =
      """
        Erases your current game and starts a new one. You can't undo that (but you can get your
        command history out of ~/.rego_session and replay it.) For <options>, jam some letters
        together: R=coRpoRate eRa, M=Tharsis, H=Hellas, I=Terra Cimmeria, U=Utopia Planitia,
        X=Promos, and the rest
        are what you'd think. The base game is always included. The player count can be from 1 to 5. A count of 1 applies
        the solo starting state.

        Add `purple` at the end to run in purple mode, where the engine controls the game flow
        automatically and you only need to respond to tasks.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      when (context.argIndex) {
        0 -> context.optionSuggestions()
        1 -> (1..5).map { ScriptCompletion(it.toString(), "player counts") }
        2 -> context.completions("purple", group = "workflow modes")
        else -> emptyList()
      }

  @Suppress("TooGenericExceptionCaught") // TODO investigate
  override fun withArgs(args: String): List<String> {
    try {
      val parts = args.trim().split(Regex("\\s+"))
      val purple = parts.getOrNull(2) == "purple"
      val optionCodes = parts.getOrNull(0) ?: throw UsageException()
      val playerCount = parts.getOrNull(1)?.toInt() ?: throw UsageException()

      repl.newGame(optionCodes, playerCount, purple)
      val effectiveOptionCodes = repl.setup.optionCodes

      return listOf("New $playerCount-player game created with options: $effectiveOptionCodes") +
          (if (purple) listOf("Purple mode: workflow active") else emptyList())
    } catch (e: RuntimeException) {
      throw UsageException(e.message)
    }
  }
}
