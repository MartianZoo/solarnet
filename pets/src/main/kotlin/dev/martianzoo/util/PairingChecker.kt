package dev.martianzoo.util

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.unaryMinus
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.parser.tryParseToEnd

object PairingChecker {
  fun check(s: String) { parser.parse(s) }
  fun isValid(s: String) = parser.isValid(s)

  private val parser: FullParser by lazy {
    val tokens = mutableListOf<Token>()

    fun literal(s: String) = literalToken(s).also { tokens += it }
    fun regex(s: String) = regexToken(s).also { tokens += it }

    val parenL = literal("("); val parenR = literal(")")
    val brackL = literal("["); val brackR = literal("]")
    val braceL = literal("{"); val braceR = literal("}")
    val angleL = literal("<"); val angleR = literal(">")

    val pairChar =
        parenL or parenR or
        brackL or brackR or
        braceL or braceR or
        angleL or angleR

    val any = parser { parser.parser }

    val parend = -parenL and any and -parenR
    val brackd = -brackL and any and -brackR
    val braced = -braceL and any and -braceR
    val angled = -angleL and any and -angleR

    val paired = parend or brackd or braced or angled

    val quote = literal("\"")
    val backslash = literal("\\")
    val filler = regex("""[^()\[\]{}<>"\\]*""")

    val quotable = (backslash and quote) or (backslash and backslash) or pairChar or filler
    val quoted = -quote and zeroOrMore(quotable) and -quote

    val whole = zeroOrMore(paired or quoted or filler)

    FullParser(whole, DefaultTokenizer(tokens))
  }

  private class FullParser(val parser: Parser<*>, val tokenizer: Tokenizer) {
    fun parse(s: String) = parser.parseToEnd(tokenizer.tokenize(s))
    fun isValid(s: String) = parser.tryParseToEnd(tokenizer.tokenize(s), 0) is Parsed
  }
}
