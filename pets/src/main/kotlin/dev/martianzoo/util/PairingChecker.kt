package dev.martianzoo.util

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd

object PairingChecker {
  private val parenL = literalToken("(")
  private val parenR = literalToken(")")
  private val bracketL = literalToken("[")
  private val bracketR = literalToken("]")
  private val braceL = literalToken("{")
  private val braceR = literalToken("}")
  private val angleL = literalToken("<")
  private val angleR = literalToken(">")

  private val quote = literalToken("\"")
  private val backslash = literalToken("\\")

  private val rest = regexToken("[^()\\[\\]{}<>\"\\\\]*")

  private val tokenizer = DefaultTokenizer(listOf(
      parenL, parenR,
      bracketL, bracketR,
      braceL, braceR,
      angleL, angleR,
      quote,
      backslash,
      rest
  ))

  private val any = parser { theThing }

  private val pairChar =
      parenL or parenR or
      bracketL or bracketR or
      braceL or braceR or
      angleL or angleR

  private val parenthesized = -parenL and any and -parenR
  private val bracketed = -bracketL and any and -bracketR
  private val braced = -braceL and any and -braceR
  private val angled = -angleL and any and -angleR

  private val stuffInBetween = rest

  private val okInsideQuotes =
      (backslash and quote map { "\\\"" }) or
      (backslash and backslash map { "\\\\" }) or
      pairChar or
      stuffInBetween

  private val quoted = -quote and zeroOrMore(okInsideQuotes) and -quote

  private val paired = parenthesized or bracketed or braced or angled or quoted

  private val whole = zeroOrMore(paired or stuffInBetween)

  private val theThing: Parser<Boolean> = whole map { true }

  fun check(s: String) = theThing.parseToEnd(tokenizer.tokenize(s))

  fun isValid(s: String) =
    try {
      check(s)
    } catch (e: Exception) {
      false
    }
}
