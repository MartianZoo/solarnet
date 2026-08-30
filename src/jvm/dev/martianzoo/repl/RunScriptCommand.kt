package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import java.io.File

internal class RunScriptCommand(private val session: ScriptSession) : ScriptCommand("script") {
  override val usage = "script <filename>"
  override val help =
      """
        Reads from the given filename (expressed relative to the solarnet/ directory) and executes
        every command in it, as if you had typed it directly at the prompt, until reaching the
        line "stop" or the end of file. Script files may not contain an `exit` command.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      ScriptPathCompletions.arguments(context.currentWord)

  override fun completionPrefix(parsedWord: String): String = parsedWord

  override fun withArgs(args: String): List<String> {
    val file = File(args)
    val lines = file.readLines().takeWhile { it.trim() != "stop" }
    lines.forEachIndexed { index, line ->
      val command = line.substringBefore("//").trim()
      if (command.equals("exit", ignoreCase = true)) {
        return listOf("${file.path}:${index + 1}: `exit` is not allowed in script files")
      }
    }
    return lines.filter { it.isNotEmpty() }.flatMap { listOf(">>> $it") + session.command(it) + "" }
  }
}
