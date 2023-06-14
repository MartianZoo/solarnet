package dev.martianzoo.repl.commands

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplSession
import dev.martianzoo.repl.ReplSession.UsageException

internal class AutoCommand(val repl: ReplSession) : ReplCommand("auto") {
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

  override fun noArgs() = listOf("Autoexec mode is: ${repl.tfm.autoExecMode}")

  override fun withArgs(args: String): List<String> {
    repl.tfm.autoExecMode =
        when (args) {
          "none" -> NONE
          "safe" -> SAFE
          "first" -> FIRST
          else -> throw UsageException()
        }
    return noArgs()
  }
}
