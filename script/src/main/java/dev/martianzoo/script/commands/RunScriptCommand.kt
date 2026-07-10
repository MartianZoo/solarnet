package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import java.io.File

internal class RunScriptCommand(private val repl: ScriptSession) : ScriptCommand("script") {
  override val usage = "script <filename>"
  override val help =
      """
        Reads from the given filename (expressed relative to the solarnet/ directory) and executes
        every command in it, as if you had typed it directly at the prompt, until reaching the
        line "stop" or the end of file. You probably don't want to put "exit" in that file.
        TODO: detect and reject "exit" within script files (it shuts down the server in server mode)
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      context.fileArguments()

  override fun completionPrefix(parsedWord: String): String = parsedWord

  override fun withArgs(args: String) =
      File(args)
          .readLines()
          .takeWhile { it.trim() != "stop" }
          .filter { it.isNotEmpty() }
          .flatMap { listOf(">>> $it") + repl.command(it) + "" }
}
