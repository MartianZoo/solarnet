package dev.martianzoo.repl

public class ReplCompletionEngine(
    private val repl: ReplSession,
    extraCommands: List<ReplCommand> = emptyList(),
) {
  private val commands: Map<String, ReplCommand> =
      repl.commands + extraCommands.associateBy { it.name }

  public fun completeLine(
      line: String,
      cursor: Int = line.length,
      parsedWord: String = wordAtCursor(line, cursor),
  ): List<ReplCompletion> {
    val chunk = line.take(cursor).substringAfterLast(';')
    val input = chunk.trimStart()
    if (input.isEmpty()) return commandCandidates("", parsedWord)

    val completionArgs = ReplCompletionArgs(input)
    val command = completionArgs.firstWord
    if (!completionArgs.hasRestAfterFirstWord) return commandCandidates(command, parsedWord)

    val replCommand = commands[command.lowercase()] ?: return emptyList()
    val prefix = replCommand.completionPrefix(parsedWord)
    val context = ReplCompletionContext(repl, completionArgs.restAfterFirstWord)
    return replCommand
        .completions(context)
        .filter { it.startsWith(prefix, ignoreCase = true) }
        .distinct()
        .sortedWith(compareBy<ReplCompletion> { it.value.lowercase() }.thenBy { it.value })
        .map { it.replacingFragment(parsedWord) }
  }

  private fun commandCandidates(prefix: String, parsedWord: String): List<ReplCompletion> =
      commands
          .values
          .map { ReplCompletion(it.name, "commands", it.usage) }
          .filter { it.startsWith(prefix, ignoreCase = true) }
          .sortedBy { it.value }
          .map { it.replacingFragment(parsedWord) }

  private companion object {
    fun wordAtCursor(line: String, cursor: Int): String {
      val prefix = line.take(cursor)
      val start = prefix.indexOfLast { it.isWhitespace() || it == ';' } + 1
      return prefix.drop(start)
    }
  }
}
