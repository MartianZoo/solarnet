package dev.martianzoo.util

import com.github.h0tk3y.betterParse.parser.Parser
import kotlin.reflect.KClass

abstract class ParserGroup<B : Any> {
  inline fun <reified T: B> parse(input: String) = parse(T::class, input)
  inline fun <reified T: B> isValid(input: String) = isValid(T::class, input)
  inline fun <reified T : B> parser() = parser(T::class)

  abstract fun <T: B> parse(type: KClass<T>, input: String) : T
  abstract fun <T: B> isValid(type: KClass<T>, input: String) : Boolean
  abstract fun <T : B> parser(type: KClass<T>): Parser<T>
}
