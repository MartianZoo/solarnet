package dev.martianzoo.util

import kotlin.reflect.KClass

class KClassMultimap<B : Any>(list: Collection<B> = listOf()) {
  val map = mutableMapOf<KClass<out B>, MutableList<B>>()

  init { this += list }

  fun <T : B> put(type: KClass<T>, value: T) = doPut(type, value)

  private fun doPut(type: KClass<out B>, value: B) {
    map.computeIfAbsent(type) { mutableListOf() } += value
  }

  inline operator fun <reified T : B> plusAssign(value: T) {
    put(T::class, value)
  }

  operator fun plusAssign(values: Collection<B>) =
      values.forEach { doPut(it::class, it) }

  inline fun <reified T : B> get() = get(T::class)

  fun <T : B> get(type: KClass<T>) = (map[type] as List<T>?) ?: listOf()
}
