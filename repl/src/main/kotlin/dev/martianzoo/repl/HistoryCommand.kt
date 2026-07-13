package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptSession.UsageException

internal class HistoryCommand(private val terminal: ReplTerminal) : ScriptCommand("history") {
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

  override fun noArgs() = terminal.historyLines()

  override fun withArgs(args: String): List<String> {
    val max = args.toIntOrNull() ?: throw UsageException()
    return terminal.historyLines(max)
  }
}
