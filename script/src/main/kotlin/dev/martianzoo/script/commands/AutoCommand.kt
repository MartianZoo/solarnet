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
        TODO fix this
        Turns auto-execute mode on or off, or just `auto` tells you what mode you're in. When you
        initiate an instruction with `exec` or `task`, per the game rules you always get to decide
        what order to do all the resulting tasks in. But that's a pain, so when `auto` is `on` (as
        it is by default) the REPL tries to execute each task (in the order they appear on the
        cards), and leaves it on the queue only if it can't run correctly. This setting is sticky
        until you `exit` or `rebuild`, even across games.
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
