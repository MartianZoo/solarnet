package dev.martianzoo.util

import com.github.h0tk3y.betterParse.grammar.parser
import com.github.h0tk3y.betterParse.lexer.TokenMatchesSequence
import com.github.h0tk3y.betterParse.parser.ParseException
import com.github.h0tk3y.betterParse.parser.Parser
import com.github.h0tk3y.betterParse.parser.parseToEnd
import kotlin.reflect.KClass

abstract class ParserGroup<B : Any> {
  inline fun <reified T : B> parse(source: String, input: TokenMatchesSequence) =
      parse(T::class, source, input)

  inline fun <reified T : B> parser() = parser(T::class)

  abstract fun <T : B> parse(type: KClass<T>, source: String, matches: TokenMatchesSequence): T

  abstract fun <T : B> parser(type: KClass<T>): Parser<T>

  class Builder<B : Any> : ParserGroup<B>() {
    private val parsers = mutableMapOf<KClass<out B>, Parser<B>>()

    inline fun <reified T : B> publish(parser: Parser<T>) = publish(T::class, parser)

    fun <T : B> publish(type: KClass<T>, parser: Parser<T>) = parser.also { parsers[type] = it }

    override fun <T : B> parse(
        type: KClass<T>,
        source: String,
        matches: TokenMatchesSequence,
    ): T {
      val parser = parser(type)
      try {
        return parser.parseToEnd(matches)
      } catch (e: ParseException) {
        val tokenDesc = matches
            .filterNot { it.type.ignored }
            .joinToString(" ") { it.type.name?.replace("\n", "\\n") ?: "NULL" }

        // TODO probably make this a PetSyntaxException
        throw IllegalArgumentException(
            """
              Expecting: ${type.simpleName}
              Token stream: $tokenDesc
              Input was:
              ${source.replaceIndent("  ")}
            """
                .trimIndent(),
            e)
      }
    }

    override fun <T : B> parser(type: KClass<T>): Parser<T> = parser {
      @Suppress("UNCHECKED_CAST")
      parsers[type] as Parser<T>
    }

    fun finish(): ParserGroup<B> = this
  }
}
