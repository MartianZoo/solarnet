package dev.martianzoo.script.commands

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.UsageException

internal class AutoCommand(private val repl: ScriptSession) : ScriptCommand("auto") {
  override val usage = "auto [none|safe|first]"
  override val help =
      """
        Controls how eagerly the engine executes pending tasks. `none` leaves every task for you.
        `safe` executes a task only when no other task could currently succeed, preserving every
        choice allowed by the game. `first` also makes arbitrary, reproducible choices when several
        tasks could succeed; this is convenient, but it can make suboptimal moves. With no argument,
        `auto` reports the current mode. The setting is sticky until you `exit` or `rebuild`, even
        across games.
      """

  override fun noArgs() = listOf("Autoexec mode is: ${repl.gameplay.autoExecMode}")

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.completions("none", "safe", "first", group = "auto modes")

  override fun withArgs(args: String): List<String> {
    repl.gameplay.autoExecMode =
        when (args) {
          "none" -> NONE
          "safe" -> SAFE
          "first" -> FIRST
          else -> throw UsageException()
        }
    return noArgs()
  }
}
