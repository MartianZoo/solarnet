package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.generateReplayScript
import java.io.File

internal class SaveGameCommand(private val session: ScriptSession) : ScriptCommand("save") {
  override val usage = "save <filename>"
  override val help =
      """
        Writes the current game to the given filename as a replayable REPL script. If the file
        already exists, it is overwritten. Load the saved game with `script <filename>`.
      """

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      ScriptPathCompletions.arguments(context.currentWord)

  override fun completionPrefix(parsedWord: String): String = parsedWord

  override fun withArgs(args: String): List<String> {
    val file = File(args)
    file.writeText(generateReplayScript(session.world))
    return listOf("Saved current game to ${file.path}")
  }
}
