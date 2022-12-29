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
  val components = mutableListOf<T>()
  var isEOF = false
  while (!isEOF) {
    val parseResult = listParser.tryParse(tokens, index)
    when (parseResult) {
      is Parsed -> {
        components += parseResult.value
        index = parseResult.nextPosition
      }
      is UnexpectedEof -> isEOF = true
      is AlternativesFailure -> parseResult.errors.any(::isEOF)
      is ErrorResult -> throw ParseException(parseResult)
    }
  }
  return components
}

private fun isEOF(result: ParseResult<*>?): Boolean =
    when (result) {
      is UnexpectedEof -> true
      is AlternativesFailure -> result.errors.any(::isEOF)
      else -> false
    }

val CLASS_NAME_PATTERN = "\\b[A-Z][a-z][A-Za-z0-9_]*\\b"
