package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.util.toSetStrict

internal class NewGameCommand(private val repl: ScriptSession) : ScriptCommand("newgame") {
  override val usage =
      "newgame (<options> <player count> [colony tiles...] | \"<game config>\" <player names...>) [purple]"
  override val help =
      """
        Erases your current game and starts a new one. You can't undo that (but you can get your
        command history out of ~/.rego_session and replay it.) For <options>, jam some letters
        together: B=base game (required, with the default Tharsis map),
        R=coRpoRate eRa, H=Hellas & Elysium, U=Utopia & Cimmeria,
        M=Milestones & Awards,
        X=Promos, and the rest
        are what you'd think. The base game is always included. The player count can be from 1 to 5. A count of 1 applies
        the solo starting state.

        When using Colonies, list the selected colony tile names after the player count.
        Instead of the legacy option-code form, quote a comma-separated list of canonical class
        names and then list one to five player names in seat order. Prefix a configuration
        name with `-` to exclude it. Player names may be new names such as `Mom` or `Ellie`. The
        quoted configuration resembles Pets names but is not Pets syntax.
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
      val purple = parts.lastOrNull() == "purple"
      val withoutPurple =
          args.trim().let { if (purple) it.removeSuffix("purple").trimEnd() else it }
      if (withoutPurple.startsWith('"')) {
        val closingQuote = withoutPurple.indexOf('"', startIndex = 1)
        if (closingQuote < 1) throw UsageException()
        val configText = withoutPurple.substring(1, closingQuote)
        val playerNames =
            withoutPurple.substring(closingQuote + 1).trim().split(Regex("\\s+")).filter {
              it.isNotEmpty()
            }
        repl.newGame(configText, playerNames, purple)
        return listOf(
            "New ${repl.playerCount}-player game created with config: $configText; " +
                "players: ${playerNames.joinToString()}"
        ) + (if (purple) listOf("Purple mode: workflow active") else emptyList())
      }

      val optionCodes = parts.getOrNull(0) ?: throw UsageException()
      val playerCount = parts.getOrNull(1)?.toInt() ?: throw UsageException()
      val colonyNames = parts.drop(2).let { if (purple) it.dropLast(1) else it }
      val selectedColonies = colonyNames.map(repl::canonicalColonyName).toSetStrict()

      repl.newGame(optionCodes, playerCount, selectedColonies, purple)
      val effectiveOptionCodes = repl.optionCodes

      return listOf("New $playerCount-player game created with options: $effectiveOptionCodes") +
          (if (purple) listOf("Purple mode: workflow active") else emptyList())
    } catch (e: RuntimeException) {
      throw UsageException(e.message)
    }
  }
}
