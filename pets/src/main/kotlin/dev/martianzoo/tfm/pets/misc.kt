package dev.martianzoo.tfm.pets

import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.AlternativesFailure
import com.github.h0tk3y.betterParse.parser.ErrorResult
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.ParseResult
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.UnexpectedEof

fun <T> parseRepeated(listParser: Parser<List<T>>, tokens: TokenMatchesSequence): List<T> {
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
      result is ErrorResult -> throw ParseException(result)
      else -> error("huh?")
    }
  }
  return parsed
}

private fun isEOF(result: ParseResult<*>?): Boolean = when (result) {
  is UnexpectedEof -> true
  is AlternativesFailure -> result.errors.any(::isEOF)
  else -> false
}
