package dev.martianzoo.util

import com.github.h0tk3y.betterParse.combinators.and
import com.github.h0tk3y.betterParse.combinators.asJust
import com.github.h0tk3y.betterParse.combinators.map
import com.github.h0tk3y.betterParse.combinators.or
import com.github.h0tk3y.betterParse.combinators.skip
import com.github.h0tk3y.betterParse.combinators.zeroOrMore
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import dev.martianzoo.util.Tokenizer.literal
import dev.martianzoo.util.Tokenizer.regex

const val allowEmpty = false
const val allowRedundant = false

object PairingChecker {
  fun check(s: String) {
    require(parsers.parse<String>(s, Tokenizer.tokenize(s)) != "ERR")
  }

  fun isValid(s: String) =
      try {
        check(s)
        true
      } catch (e: Exception) {
        false
      }

  private val parsers: ParserGroup<String> by lazy {
    val p = ParserGroup.Builder<String>()

    val parenL = literal("(")
    val parenR = literal(")")
    val brackL = literal("[")
    val brackR = literal("]")
    val braceL = literal("{")
    val braceR = literal("}")
    val angleL = literal("<")
    val angleR = literal(">")

    val pairChar = parenL or parenR or brackL or brackR or braceL or braceR or angleL or angleR

    val any = p.parser<String>()

    val parend = parenL and any and skip(parenR)
    val brackd = brackL and any and skip(brackR)
    val braced = braceL and any and skip(braceR)
    val angled = angleL and any and skip(angleR)

    val paired =
        parend or
            brackd or
            braced or
            angled map
            { (opener, contains) ->
              when {
                contains == "ERR" -> "ERR"
                !allowEmpty && contains == "mt" -> "ERR"
                !allowRedundant && contains == opener.text -> "ERR"
                else -> opener.text
              }
            }

    val quote = literal("\"")
    val backslash = literal("\\")
    val filler = regex(Regex("""[^()\[\]{}<>"\\]*""")) asJust ("ok")

    val quotable = (backslash and quote) or (backslash and backslash) or pairChar or filler

    val quoted = skip(quote) and zeroOrMore(quotable) and skip(quote) asJust ("ok")

    val whole =
        zeroOrMore(paired or quoted or filler) map
            {
              when {
                it.isEmpty() -> "mt"
                it.contains("ERR") -> "ERR"
                it.size == 1 -> it.first()
                else -> "ok"
              }
            }
    p.publish(whole)
    p.finish()
  }
}

internal object Tokenizer {
  private val tokens = mutableListOf<Token>()

  private val toker by lazy { DefaultTokenizer(tokens) }
  fun tokenize(input: String) = toker.tokenize(input)

  fun literal(text: String, name: String = text) = remember(literalToken(name, text))

  fun regex(regex: Regex, name: String = regex.toString()) = remember(regexToken(name, regex))

  private fun remember(t: Token): Token {
    require(t.name != null)
    tokens += t
    return t
  }
}
