package dev.martianzoo.tfm.script.commands

import dev.martianzoo.state.Checkpoint
import dev.martianzoo.state.iconicLogEvents
import dev.martianzoo.tfm.script.ScriptCommand
import dev.martianzoo.tfm.script.ScriptCompletion
import dev.martianzoo.tfm.script.ScriptCompletionContext
import dev.martianzoo.tfm.script.ScriptSession
import dev.martianzoo.tfm.script.ScriptSession.UsageException

internal class LogCommand(private val repl: ScriptSession) : ScriptCommand("log") {
  override val usage = "log [full]"
  override val help =
      """
        Shows changes since game setup began that have at least one Iconic side (`log`).
        Use `log full` to include engine initialization, non-Iconic changes, and task events.
        Component types are shown in compact form.
      """
  override val isReadOnly = true

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.completions("full", group = "log options")

  override fun noArgs() =
      repl.game.events.changesSinceSetup().iconicLogEvents(repl.game.reader).map { it.toString() }

  override fun withArgs(args: String): List<String> {
    if (args == "full") {
      return repl.game.events.entriesSince(Checkpoint(0)).map { it.toString() }
    } else {
      throw UsageException()
    }
  }
}
