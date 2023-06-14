package dev.martianzoo.tfm.repl.commands

import dev.martianzoo.tfm.repl.ReplCommand
import dev.martianzoo.tfm.repl.ReplSession
import java.io.File

internal class ScriptCommand(val repl: ReplSession) : ReplCommand("script") {
  override val usage = "script <filename>"
  override val help =
      """
        Reads from the given filename (expressed relative to the solarnet/ directory) and executes
        every command in it, as if you had typed it directly at the prompt, until reaching the
        line "stop" or the end of file. You probably don't want to put "exit" in that file.
      """

  override fun withArgs(args: String) =
      File(args)
          .readLines()
          .takeWhile { it.trim() != "stop" }
          .filter { it.isNotEmpty() }
          .flatMap { listOf(">>> $it") + repl.command(it) + "" }
}
