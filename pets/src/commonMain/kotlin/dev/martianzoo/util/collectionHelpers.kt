package dev.martianzoo.util

import kotlin.random.Random

public fun <T> Iterable<T>.toSetStrict(): Set<T> =
    toSet().also { require(it.size == count()) { "dupes: $this" } }

public inline fun <T, K> Iterable<T>.toSetStrict(fn: (T) -> K): Set<K> = map(fn).toSetStrict()

internal fun <T, K> Collection<T>.associateByStrict(x: (T) -> K): Map<K, T> {
  val map: Map<K, T> = associateBy(x)
  require(map.size == size) { groupBy(x).filterValues { it.size > 1 }.keys }
  return map
}

internal fun <T, K, V> Collection<T>.associateStrict(x: (T) -> Pair<K, V>): Map<K, V> {
  val map: Map<K, V> = associate(x)
  require(map.size == size) { groupBy(x).filterValues { it.size > 1 }.keys }
  return map
}

public fun <C : Iterable<Any?>> C.toStrings(): List<String> = map { it?.toString() ?: "null" }

internal fun <C : Sequence<Any?>> C.toStrings(): Sequence<String> = map { it?.toString() ?: "null" }

public fun <T> Sequence<T>.random(): T {
  var i = 0
  return findLast { Random.nextInt(++i) == 0 } ?: error("empty")
}

internal infix fun <T> T.plus(more: Collection<T>): List<T> = listOf(this) + more

internal infix fun <T> T.plus(another: T): List<T> = listOf(this, another)

internal fun <T> List<Sequence<T>>.cartesianProduct(): Sequence<List<T>> {
  if (isEmpty()) return sequenceOf(listOf())

  return sequence {
    val firstList = first()
    for (t in firstList) {
      val prefix = listOf(t)
      for (suffix in drop(1).cartesianProduct()) {
        yield(prefix + suffix)
      }
    }
  }
}
