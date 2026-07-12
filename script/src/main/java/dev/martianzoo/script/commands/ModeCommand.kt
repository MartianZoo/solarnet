package dev.martianzoo.script.commands

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionContext
import dev.martianzoo.script.ScriptSession
import dev.martianzoo.script.ScriptSession.ScriptMode
import dev.martianzoo.script.ScriptSession.UsageException

internal class ModeCommand(private val repl: ScriptSession) : ScriptCommand("mode") {
  override val usage = "mode <mode name>"
  override val help =
      """
        Changes modes. Names are red, yellow, green, blue, purple. Just enter a mode and it will
        tell you what it means.
      """

  override fun noArgs() = listOf("Mode ${repl.mode}: ${repl.mode.message}")

  override fun completions(context: ScriptCompletionContext): List<ScriptCompletion> =
      ScriptMode.entries.map { ScriptCompletion(it.name.lowercase(), "modes", it.message) }

  override fun withArgs(args: String): List<String> {
    try {
      repl.mode = ScriptMode.valueOf(args.uppercase())
    } catch (e: Exception) {
      throw UsageException(
          "Valid modes are: ${ScriptMode.entries.joinToString { it.toString().lowercase() }}"
      )
    }
    return noArgs()
  }
}
