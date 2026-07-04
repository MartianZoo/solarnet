package dev.martianzoo.repl

import dev.martianzoo.repl.ReplSession.UsageException

internal abstract class ReplCommand(public val name: String) {
  public open val isReadOnly: Boolean = false // not currently used
  public abstract val usage: String
  public abstract val help: String
  public open fun noArgs(): List<String> = throw UsageException()
  public open fun withArgs(args: String): List<String> = throw UsageException()
  public open fun completions(context: ReplCompletionContext): List<ReplCompletion> = emptyList()
  public open fun completionPrefix(parsedWord: String): String = ReplCompletionContext.fragment(parsedWord)
}
