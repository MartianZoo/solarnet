package dev.martianzoo.util

import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.Parsed
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import com.github.h0tk3y.betterParse.parser.tryParseToEnd
import kotlin.reflect.KClass

abstract class ParserGroup<B : Any> {
  inline fun <reified T : B> parse(input: TokenMatchesSequence) = parse(T::class, input)
  inline fun <reified T : B> isValid(input: TokenMatchesSequence) = isValid(T::class, input)
  inline fun <reified T : B> parser() = parser(T::class)

  abstract fun <T : B> parse(type: KClass<T>, input: TokenMatchesSequence): T
  abstract fun <T : B> isValid(type: KClass<T>, input: TokenMatchesSequence): Boolean

  abstract fun <T : B> parser(type: KClass<T>): Parser<T>

  class Builder<B : Any> : ParserGroup<B>() {
    private val parsers = mutableMapOf<KClass<out B>, Parser<B>>()

    inline fun <reified T : B> publish(parser: Parser<T>) = publish(T::class, parser)

    fun <T : B> publish(type: KClass<T>, parser: Parser<T>) = parser.also { parsers[type] = it }

    override fun <T : B> parse(type: KClass<T>, input: TokenMatchesSequence): T {
      return parser(type).parseToEnd(input)
    }

    override fun <T : B> isValid(type: KClass<T>, input: TokenMatchesSequence): Boolean {
      return parser(type).tryParseToEnd(input, 0) is Parsed
    }

    override fun <T : B> parser(type: KClass<T>): Parser<T> = parser { parsers[type] as Parser<T> }

    fun finish(): ParserGroup<B> = this
  }
}
