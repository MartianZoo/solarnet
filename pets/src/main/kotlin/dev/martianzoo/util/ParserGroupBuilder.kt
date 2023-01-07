package dev.martianzoo.util

import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.DefaultTokenizer
import com.github.h0tk3y.betterParse.lexer.RegexToken
import com.github.h0tk3y.betterParse.lexer.Token
import com.github.h0tk3y.betterParse.lexer.Tokenizer
import com.github.h0tk3y.betterParse.lexer.literalToken
import com.github.h0tk3y.betterParse.lexer.regexToken
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.parser.tryParseToEnd
import kotlin.reflect.KClass

class ParserGroupBuilder<B : Any> : ParserGroup<B>() {
  private val tokens = mutableListOf<Token>()
  private var toker: Tokenizer? = null
  private val parsers = mutableMapOf<KClass<out B>, Parser<B>>()

  fun literal(s: String): Token {
    require(toker == null)
    return literalToken("literal $s", s).also { tokens += it }
  }

  fun regex(s: String): RegexToken {
    require(toker == null)
    return regexToken("regex $s", s).also { tokens += it }
  }

  inline fun <reified T : B> publish(parser: Parser<T>) = publish(T::class, parser)

  fun <T : B> publish(type: KClass<T>, parser: Parser<T>) =
      parser.also { parsers[type] = it }

  fun freeze(): ParserGroup<B> {
    toker = DefaultTokenizer(tokens.toList())
    tokens.clear()
    return this
  }

  override fun <T: B> parse(type: KClass<T>, input: String) : T {
    return parser(type).parseToEnd(toker!!.tokenize(input))
  }

  override fun <T: B> isValid(type: KClass<T>, input: String) : Boolean {
    return parser(type).tryParseToEnd(toker!!.tokenize(input), 0) is Parsed
  }

  override fun <T : B> parser(type: KClass<T>): Parser<T> = parser { parsers[type]  as Parser<T> }
}
