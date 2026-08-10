package dev.martianzoo.util

import kotlin.random.Random

public fun <T> Array<T>.toSetStrict(): Set<T> = toList().toSetStrict()

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

internal fun <K : Any, V : Any> mergeMaps(
    one: Map<out K, V>,
    two: Map<out K, V>,
    merger: (V, V) -> V,
) =
    one.toMutableMap().apply {
      two.forEach { (key, value) -> this[key] = this[key]?.let { merger(it, value) } ?: value }
    }

internal fun <K : Any, V : Any> overlayMaps(front: Map<out K, V>, back: Map<out K, V>): Map<K, V> {
  // Not the easiest way but this way makes front's ordering more significant
  val overlay = front.toMutableMap()
  back.forEach { (k, v) -> if (k !in front) overlay[k] = v }
  return overlay
}

public fun <T> random(iter: Collection<T>, count: Int): Set<T> {
  var misses = 0
  val results = mutableSetOf<T>()
  while (results.size < count) {
    if (!results.add(iter.random())) {
      require(++misses < 10_000)
    }
  }
  return results
}

public fun <T> Sequence<T>.random(): T {
  var i = 0
  return findLast { Random.nextInt(++i) == 0 } ?: error("empty")
}

internal fun <T> Iterable<T>.joinOrEmpty(
    separator: CharSequence = ", ",
    prefix: CharSequence,
    suffix: CharSequence,
    transform: ((T) -> CharSequence)? = null,
): String {
  return if (any()) {
    joinToString(separator = separator, prefix = prefix, postfix = suffix, transform = transform)
  } else {
    ""
  }
}

internal fun <T> Iterable<T>.joinOrEmpty(
    separator: CharSequence = ", ",
    wrap: CharSequence,
    transform: ((T) -> CharSequence)? = null,
): String {
  require(wrap.length == 2)
  return joinOrEmpty(
      separator = separator,
      prefix = wrap.substring(0, 1),
      suffix = wrap.substring(1),
      transform = transform,
  )
}

internal fun <T : Any> Iterable<List<T?>>.filterWithoutNulls(): List<List<T>> {
  val noNulls = filter { null !in it }
  @Suppress("UNCHECKED_CAST")
  return noNulls as List<List<T>>
}

internal fun <T> List<T?>.checkNoNulls(): List<T> {
  require(null !in this)
  @Suppress("UNCHECKED_CAST")
  return this as List<T>
}

internal infix fun <T> T.plus(more: Collection<T>): List<T> = listOf(this) + more

internal infix fun <T> T.plus(another: T): List<T> = listOf(this, another)

internal fun <T : Comparable<T>> Iterable<T>.extras() =
    sorted().windowed(2).mapNotNull { it.distinct().singleOrNull() }

internal fun <T : Comparable<T>> Sequence<T>.extras() =
    sorted().windowed(2).mapNotNull { it.distinct().singleOrNull() }

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
