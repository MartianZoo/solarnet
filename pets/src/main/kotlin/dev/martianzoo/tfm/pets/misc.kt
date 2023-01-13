package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.lexer.TokenMatch
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.MismatchedToken
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.UnexpectedEof
import dev.martianzoo.util.Debug

fun <T> parseRepeated(listParser: Parser<List<T>>, tokens: TokenMatchesSequence): List<T> {
  Debug.d(tokens.filterNot { it.type.ignored }.joinToString(" ") {
    it.type.name?.replace("\n", "\\n") ?: "NULL"
  })
  var index = 0
  val parsed = mutableListOf<T>()
  while (true) {
    val result = listParser.tryParse(tokens, index)
    when {
      result is Parsed -> {
        parsed += result.value
        require(result.nextPosition != index) { index }
        index = result.nextPosition
      }
      result is UnexpectedEof -> break
      result is AlternativesFailure && result.errors.any(::isEOF) -> break
      result is ErrorResult -> myThrow(result)
      else -> error("huh?")
    }
  }
  return parsed
}

fun myThrow(result: ErrorResult) {
  var message = StringBuilder()
  var ctr = 0
  val locations = mutableMapOf<Pair<Int, Int>, Int>()
  var input: String? = null
  fun visit(result: ErrorResult) {
    when (result) {
      is AlternativesFailure -> result.errors.forEach(::visit)
      is MismatchedToken -> {
        val match: TokenMatch = result.found
        val loc = match.row to match.column
        val thisLoc = if (loc in locations) {
          locations[loc]
        } else {
          locations[loc] = ctr
          ctr++
        }
        input = match.input.toString()
        val found = match.text.replace("\n", "\\n")
        val expec = result.expected.name?.replace("\n", "\\n")
        message.append("$thisLoc: at ${match.row}:${match.column}, looking for $expec, but found $found\n")
      }
      else -> message.append(result.toString())
    }
  }

  visit(result)

  message.append("\nNow, here is the input:\n")
  input!!.split("\n").forEachIndexed { lineNum, line ->
    message.append("$line\n")
    (1..100).forEach { columnNum ->
      val loc = (lineNum + 1) to columnNum
      message.append(if (loc in locations) "${locations[loc]}" else " ")
    }
    message.append("\n")
  }

  throw RuntimeException(message.toString())
}


private fun isEOF(result: ParseResult<*>?): Boolean = when (result) {
  is UnexpectedEof -> true
  is AlternativesFailure -> result.errors.any(::isEOF)
  else -> false
}
