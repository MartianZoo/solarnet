package dev.martianzoo.util

import kotlin.reflect.KClass

class KClassMap<B : Any> {

  private val map = mutableMapOf<KClass<out B>, B>()

  inline operator fun <reified T : B> plusAssign(value: T) = put(T::class, value)

  inline fun <reified T : B> get() = get(T::class)

  fun <T : B> put(type: KClass<T>, value: T) {
    map[type] = value
  }

  fun <T : B> get(type: KClass<T>) = map[type] as T

}
