package dev.martianzoo.script

import dev.martianzoo.script.ScriptSession.UsageException

public abstract class ScriptCommand(public val name: String) {
  public open val isReadOnly: Boolean = false // not currently used
  public abstract val usage: String
  public abstract val help: String
  public open fun noArgs(): List<String> = throw UsageException()
  public open fun withArgs(args: String): List<String> = throw UsageException()
  public open fun completions(context: ScriptCompletionContext): List<ScriptCompletion> = emptyList()
  public open fun completionPrefix(parsedWord: String): String = ScriptCompletionContext.fragment(parsedWord)
}
