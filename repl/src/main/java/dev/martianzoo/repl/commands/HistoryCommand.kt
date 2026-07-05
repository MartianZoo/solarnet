package dev.martianzoo.repl.commands

import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException

internal class HistoryCommand(private val repl: ReplSession) : ReplCommand("history") {
  override val usage = "history <count>"
  override val help =
      """
        This shows the history of the commands you've typed into the REPL. It should contain
        history from your previous sessions too (hopefully). `history 20` would show you only
        the last 20. These are numbered, and if one command is numbered 123 you can type `!123`
        to repeat it. You can also write `!` plus the first few letters of the command and you'll
        get the most recent match. There's other stuff you can do; look for info on the `jline`
        library if curious.
      """
  override val isReadOnly = true

  override fun noArgs() =
      repl.historyLines() ?: listOf("See your own shell history!")

  override fun withArgs(args: String): List<String> {
    val max = args.toIntOrNull() ?: throw UsageException()
    return repl.historyLines(max) ?: listOf("See your own shell history!")
  }
}
