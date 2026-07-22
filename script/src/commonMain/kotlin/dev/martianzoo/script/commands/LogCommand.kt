package dev.martianzoo.script.commands

import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException
import dev.martianzoo.util.toStrings

internal class LogCommand(private val repl: ScriptSession) : ScriptCommand("log") {
  override val usage = "log [full]"
  override val help =
      """
        Shows everything that has happened in the current game (`log full`) or just the more
        interesting bits (i.e., filtering out Task changes, and filtering out changes to Hidden
        components other than phases -- just like the default output after `exec` or `task`
        does).
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.completions("full", group = "log options")

  override fun noArgs() =
      repl.game.events
          .changesSinceSetup()
          .filterNot { repl.isHidden(it, repl.game.reader) }
          .toStrings()

  override fun withArgs(args: String): List<String> {
    if (args == "full") {
      return repl.game.events.entriesSince(Checkpoint(0)).toStrings()
    } else {
      throw UsageException()
    }
  }
}
