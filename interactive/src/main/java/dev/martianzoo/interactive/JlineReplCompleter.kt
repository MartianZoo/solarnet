package dev.martianzoo.interactive

import dev.martianzoo.repl.ReplCommand
import dev.martianzoo.repl.ReplCompletion
import dev.martianzoo.repl.ReplCompletionEngine
import dev.martianzoo.repl.ReplSession
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

internal class JlineReplCompleter(
    repl: ReplSession,
    extraCommands: List<ReplCommand> = emptyList(),
) : Completer {
  private val engine = ReplCompletionEngine(repl, extraCommands)

  override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
    candidates += completeLine(line.line(), line.cursor(), line.word())
  }

  internal fun completeLine(
      line: String,
      cursor: Int = line.length,
      parsedWord: String = "",
  ): List<Candidate> {
    return engine.completeLine(line, cursor, parsedWord.ifEmpty { wordAtCursor(line, cursor) })
        .map { it.toCandidate() }
  }

  private fun ReplCompletion.toCandidate(): Candidate =
      Candidate(value, value, group, description, null, null, complete)

  private companion object {
    fun wordAtCursor(line: String, cursor: Int): String {
      val prefix = line.take(cursor)
      val start = prefix.indexOfLast { it.isWhitespace() || it == ';' } + 1
      return prefix.drop(start)
    }
  }
}
