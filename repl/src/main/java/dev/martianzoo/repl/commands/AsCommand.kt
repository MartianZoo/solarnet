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

    val delegated = context.args.substringAfterWhitespace()
    if (delegated.isBlank()) return context.commandNames()

    val delegatedCommand = delegated.substringBeforeWhitespace()
    val delegatedArgs = delegated.substringAfterWhitespace()
    if (delegatedArgs.isEmpty() && !delegated.endsWithWhitespace()) return context.commandNames()

    return context.commandArguments(delegatedCommand, delegatedArgs)
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

  private fun String.substringBeforeWhitespace(): String =
      substringBefore(' ').substringBefore('\t')

  private fun String.substringAfterWhitespace(): String {
    val firstWhitespace = indexOfFirst { it.isWhitespace() }
    return if (firstWhitespace == -1) "" else drop(firstWhitespace).trimStart()
  }

  private fun String.endsWithWhitespace() = lastOrNull()?.isWhitespace() == true
}
