package dev.martianzoo.pets.util

import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.Parser
import dev.martianzoo.pets.Parsing
import kotlin.reflect.KClass

internal abstract class ParserGroup<B : Any> {
  internal inline fun <reified T : B> parse(source: String, input: TokenMatchesSequence) =
      parse(T::class, source, input)

  internal inline fun <reified T : B> parser() = parser(T::class)

  internal abstract fun <T : B> parse(
      type: KClass<T>,
      source: String,
      matches: TokenMatchesSequence,
  ): T

  internal abstract fun <T : B> parser(type: KClass<T>): Parser<T>

  internal class Builder<B : Any> : ParserGroup<B>() {
    private val parsers = mutableMapOf<KClass<out B>, Parser<B>>()

    internal inline fun <reified T : B> publish(parser: Parser<T>) = publish(T::class, parser)

    internal fun <T : B> publish(type: KClass<T>, parser: Parser<T>) = parser.also {
      parsers[type] = it
    }

    override fun <T : B> parse(
        type: KClass<T>,
        source: String,
        matches: TokenMatchesSequence,
    ): T {
      val parser = parser(type)
      return Parsing.parse(parser, source, matches, type.simpleName)
    }

    override fun <T : B> parser(type: KClass<T>): Parser<T> = parser {
      @Suppress("UNCHECKED_CAST")
      parsers[type] as? Parser<T> ?: error("unrecognized type $type")
    }

    internal fun finish(): ParserGroup<B> = this
  }
}
