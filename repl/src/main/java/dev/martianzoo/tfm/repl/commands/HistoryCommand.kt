package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.tfm.repl.ReplSession.UsageException
import org.jline.reader.History.Entry
import org.jline.reader.impl.history.DefaultHistory

internal class HistoryCommand(val repl: ReplSession) : ReplCommand("history") {
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
  val history: DefaultHistory? = repl.jline?.history

  override fun noArgs() = fmt(history!!)

  override fun withArgs(args: String): List<String> {
    val max = args.toIntOrNull() ?: throw UsageException()
    val drop = (history!!.size() - max).coerceIn(0, null)
    return fmt(history.drop(drop))
  }

  private fun fmt(entries: Iterable<Entry>) =
      entries.map { "${it.index() + 1}: ${it.line()}" }
}
