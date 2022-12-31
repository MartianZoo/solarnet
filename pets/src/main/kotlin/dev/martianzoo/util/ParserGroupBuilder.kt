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

abstract class ParserGroup<B : Any> {
  inline fun <reified T: B> parse(input: String) = parse(T::class, input)
  inline fun <reified T: B> isValid(input: String) = isValid(T::class, input)
  inline fun <reified T : B> parser() = parser(T::class)

  abstract fun <T: B> parse(type: KClass<T>, input: String) : T
  abstract fun <T: B> isValid(type: KClass<T>, input: String) : Boolean
  abstract fun <T : B> parser(type: KClass<T>): Parser<T>
}

class ParserGroupBuilder<B : Any> : ParserGroup<B>() {
  val tokens = mutableListOf<Token>()
  var toker: Tokenizer? = null

  fun literal(s: String): Token {
    require(toker == null)
    return literalToken("literal $s", s).also { tokens += it }
  }

  fun regex(s: String): RegexToken {
    require(toker == null)
    return regexToken("regex $s", s).also { tokens += it }
  }

  override fun <T: B> parse(type: KClass<T>, input: String) : T {
    return parser(type).parseToEnd(toker!!.tokenize(input))
  }

  fun freeze(): ParserGroup<B> {
    toker = DefaultTokenizer(tokens)
    return this
  }

  override fun <T: B> isValid(type: KClass<T>, input: String) : Boolean {
    return parser(type).tryParseToEnd(toker!!.tokenize(input), 0) is Parsed
  }

  private val parsers = mutableMapOf<KClass<out B>, Parser<B>>()

  override fun <T : B> parser(type: KClass<T>): Parser<T> = parser { parsers[type] as Parser<T> }

  inline fun <reified T : B> publish(parser: Parser<T>) = publish(T::class, parser)

  fun <T : B> publish(type: KClass<T>, parser: Parser<T>): Parser<T> {
    parsers[type] = parser
    return parser
  }
}
