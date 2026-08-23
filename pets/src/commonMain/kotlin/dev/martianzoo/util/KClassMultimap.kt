package dev.martianzoo.util

import kotlin.reflect.KClass

internal class KClassMultimap<B : Any>(list: Collection<B> = emptyList()) {
  internal val map = mutableMapOf<KClass<out B>, MutableList<B>>()

  init {
    this += list
  }

  private fun <T : B> put(type: KClass<T>, value: T) = doPut(type, value)

  private fun doPut(type: KClass<out B>, value: B) {
    val list = map.getOrPut(type) { mutableListOf() }
    list += value
  }

  private inline operator fun <reified T : B> plusAssign(value: T) {
    put(T::class, value)
  }

  private operator fun plusAssign(values: Collection<B>) = values.forEach { doPut(it::class, it) }

  internal inline fun <reified T : B> get(): List<T> = get(T::class)

  @Suppress("UNCHECKED_CAST")
  internal fun <T : B> get(type: KClass<T>) = map[type]?.let { it as List<T> }.orEmpty()
}
