package dev.martianzoo.repl

import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

internal class ReplCompleter(private val repl: ReplSession) : Completer {
  override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
    candidates += completeLine(line.line(), line.cursor(), line.word())
  }

  internal fun completeLine(
      line: String,
      cursor: Int = line.length,
      parsedWord: String = wordAtCursor(line, cursor),
  ): List<Candidate> {
    val chunk = line.take(cursor).substringAfterLast(';')
    val input = chunk.trimStart()
    if (input.isEmpty()) return commandCandidates("", parsedWord)

    val command = input.substringBeforeWhitespace()
    val afterCommand = input.drop(command.length)
    if (afterCommand.isEmpty()) return commandCandidates(command, parsedWord)

    val args = afterCommand.trimStart()
    val words = if (args.isEmpty()) listOf() else args.split(Regex("\\s+"))
    val argIndex = if (args.endsWithWhitespace()) words.size else (words.size - 1).coerceAtLeast(0)
    val replCommand = repl.commands[command.lowercase()] ?: return emptyList()
    val prefix = replCommand.completionPrefix(parsedWord)
    val context = ReplCompletionContext(repl, args, words, argIndex)
    return replCommand
        .completions(context)
        .filter { it.startsWith(prefix, ignoreCase = true) }
        .distinct()
        .sortedWith(compareBy<ReplCompletion> { it.value.lowercase() }.thenBy { it.value })
        .map { it.toCandidate(parsedWord) }
  }

  private fun commandCandidates(prefix: String, parsedWord: String): List<Candidate> =
      ReplCompletionContext(repl, "", emptyList(), 0)
          .commandNames()
          .filter { it.startsWith(prefix, ignoreCase = true) }
          .sortedBy { it.value }
          .map { it.toCandidate(parsedWord) }

  private fun String.endsWithWhitespace() = lastOrNull()?.isWhitespace() == true

  private fun String.substringBeforeWhitespace(): String =
      substringBefore(' ').substringBefore('\t')

  private companion object {
    fun wordAtCursor(line: String, cursor: Int): String {
      val prefix = line.take(cursor)
      val start = prefix.indexOfLast { it.isWhitespace() || it == ';' } + 1
      return prefix.drop(start)
    }
  }
}
