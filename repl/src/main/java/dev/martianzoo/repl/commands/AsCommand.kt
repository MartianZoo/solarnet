package dev.martianzoo.repl.commands

import dev.martianzoo.engine.Gameplay.TurnLayer
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplCompletion
import dev.martianzoo.repl.ReplCompletionContext
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException

internal class AsCommand(private val repl: ReplSession) : ReplCommand("as") {
  override val usage = "as <PlayerN> <full command>"
  override val help =
      """
        For any command you could type normally, put `as Player2` etc. or `as Engine` before it.
        It's handled as if you had first `become` that player, then restored.
      """

  override fun noArgs() = throw UsageException()

  override fun completions(context: ReplCompletionContext): List<ReplCompletion> {
    if (context.argIndex == 0) return context.playerNames()

    val delegated = context.droppingLeadingWords(1)
    if (delegated.args.isBlank()) return context.commandNames()

    if (!delegated.hasRestAfterFirstWord) return context.commandNames()

    return context.commandArguments(delegated.firstWord, delegated.restAfterFirstWord)
  }

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
