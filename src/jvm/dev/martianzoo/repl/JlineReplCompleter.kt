package dev.martianzoo.repl

import dev.martianzoo.script.ScriptCommand
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionEngine
import dev.martianzoo.script.ScriptSession
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReader
import org.jline.reader.ParsedLine

internal class JlineReplCompleter(
    repl: ScriptSession,
    extraCommands: List<ScriptCommand> = emptyList(),
) : Completer {
  private val engine = ScriptCompletionEngine(repl, extraCommands)

  override fun complete(reader: LineReader, line: ParsedLine, candidates: MutableList<Candidate>) {
    candidates += completeLine(line.line(), line.cursor(), line.word())
  }

  internal fun completeLine(
      line: String,
      cursor: Int = line.length,
      parsedWord: String = "",
  ): List<Candidate> {
    return engine
        .completeLine(line, cursor, parsedWord.ifEmpty { wordAtCursor(line, cursor) })
        .map { it.toCandidate() }
  }

  private fun ScriptCompletion.toCandidate(): Candidate =
      Candidate(value, value, group, description, null, null, complete)

  private companion object {
    fun wordAtCursor(line: String, cursor: Int): String {
      val prefix = line.take(cursor)
      val start = prefix.indexOfLast { it.isWhitespace() || it == ';' } + 1
      return prefix.drop(start)
    }
  }
}
