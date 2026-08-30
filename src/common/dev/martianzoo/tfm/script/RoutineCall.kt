package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession.UsageException

/** A parsed call from the temporary `DO` REPL surface. */
internal data class RoutineCall(
    val name: String,
    val arguments: List<String>,
) {
  internal companion object {
    private val namePattern = Regex("[a-z][A-Za-z0-9]*")
    private val closingDelimiter = mapOf('(' to ')', '[' to ']', '<' to '>', '{' to '}')

    internal fun parse(text: String): RoutineCall {
      val trimmed = text.trim()
      val name = trimmed.takeWhile { it.isLetterOrDigit() }
      if (!namePattern.matches(name)) throw UsageException("Invalid Routine name")

      val remainder = trimmed.drop(name.length).trim()
      if (!remainder.startsWith('(') || !remainder.endsWith(')')) {
        throw UsageException("A Routine call must have the form routineName(...)")
      }

      val body = remainder.substring(1, remainder.lastIndex)
      return RoutineCall(name, splitArguments(body))
    }

    private fun splitArguments(body: String): List<String> {
      if (body.isBlank()) return emptyList()

      val arguments = mutableListOf<String>()
      val delimiters = ArrayDeque<Char>()
      var argumentStart = 0

      body.forEachIndexed { index, char ->
        when {
          char in closingDelimiter -> delimiters.addLast(char)
          char in closingDelimiter.values -> {
            val opening = delimiters.removeLastOrNull()
            if (opening == null || closingDelimiter.getValue(opening) != char) {
              throw UsageException("Unbalanced delimiters in Routine call")
            }
          }
          char == ',' && delimiters.isEmpty() -> {
            arguments += argument(body, argumentStart, index)
            argumentStart = index + 1
          }
        }
      }
      if (delimiters.isNotEmpty()) throw UsageException("Unbalanced delimiters in Routine call")
      arguments += argument(body, argumentStart, body.length)
      return arguments
    }

    private fun argument(body: String, start: Int, end: Int): String =
        body.substring(start, end).trim().ifEmpty {
          throw UsageException("Routine arguments may not be empty")
        }
  }
}
