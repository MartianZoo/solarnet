package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import dev.martianzoo.tfm.repl.ReplSession.UsageException
import dev.martianzoo.util.toStrings

internal class LogCommand(val repl: ReplSession) : ReplCommand("log") {
  override val usage = "log [full]"
  override val help =
      """
        Shows everything that has happened in the current game (`log full`) or just the more
        interesting bits (i.e., filtering out Task changes, and filtering out changes to System
        components -- just like the default output after `exec` or `task` does).
      """
  override val isReadOnly = true

  override fun noArgs() =
      repl.tfm.game.events.changesSinceSetup().filterNot { repl.isSystem(it, repl.tfm.reader) }.toStrings()

  override fun withArgs(args: String): List<String> {
    if (args == "full") {
      return repl.tfm.game.events.entriesSince(Checkpoint(0)).toStrings()
    } else {
      throw UsageException()
    }
  }
}
