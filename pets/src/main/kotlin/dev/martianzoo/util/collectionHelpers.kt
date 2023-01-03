package dev.martianzoo.util

import com.google.common.collect.ImmutableMultiset
import com.google.common.collect.Multiset

fun <T : Any> multiset(vararg pairs: Pair<Int, T>): ImmutableMultiset<T> {
  val builder = ImmutableMultiset.builder<T>()
  pairs.forEach { (count, element) -> builder.addCopies(element, count) }
  return builder.build()
}

// this is how to make sure the whole remove happens?
// who designed this crap?
fun <E : Any?> Multiset<E>.mustRemove(element: E, count: Int) =
    setCount(element, count(element) - count)

fun <T> Collection<T>.toSetStrict() = toSet().also { require(it.size == size) }

fun <T, K> Collection<T>.associateByStrict(x: (T) -> K): Map<K, T> {
  val map: Map<K, T> = associateBy(x)
  require (map.size == size) {
    groupBy(x).filterValues { it.size > 1 }.keys
  }
  return map
}

fun <K : Any, V : Any> mergeMaps(one: Map<out K, V>, two: Map<out K, V>, merger: (V, V) -> V) =
    one.toMutableMap().apply {
      two.forEach { merge(it.key, it.value, merger) }
    }


// TODO fix overload hell

fun <T> Iterable<T>.joinOrEmpty(
    separator: CharSequence = ", ",
    prefix: CharSequence,
    suffix: CharSequence,
    transform: ((T) -> CharSequence)? = null): String {
  return if (any()) {
    joinToString(
        separator = separator,
        prefix = prefix,
        postfix = suffix,
        transform = transform
    )
  } else {
    ""
  }
}

fun <T> Iterable<T>.joinOrEmpty(
    separator: CharSequence = ", ",
    transform: ((T) -> CharSequence)? = null): String {
  return joinOrEmpty(
      separator,
      prefix = "",
      suffix = "",
      transform = transform,
  )
}

fun <T> Iterable<T>.joinOrEmpty(
    separator: CharSequence = ", ",
    wrap: CharSequence,
    transform: ((T) -> CharSequence)? = null): String {
  require(wrap.length == 2)
  return joinOrEmpty(
      separator = separator,
      prefix = wrap.substring(0, 1),
      suffix = wrap.substring(1),
      transform = transform)
}

// Ok the rest of these aren't really *collection* helpers

fun <T : Any?> T.wrap(
    prefix: CharSequence,
    suffix: CharSequence,
    transform: ((T) -> CharSequence) = { "$it" }) =
    this?.let { "$prefix${transform(this)}$suffix" } ?: ""

fun <T : Any?> T.wrap(
    wrap: CharSequence,
    transform: ((T) -> CharSequence) = { "$it" }): String {
  require(wrap.length == 2)
  return wrap(wrap.substring(0, 1), wrap.substring(1), transform)
}

fun <T : Any?> T.pre(prefix: String, transform: (T) -> String = { "$it" }) =
    wrap(prefix, "", transform)

fun <T : Any?> T.suf(suffix: String, transform: (T) -> String = { "$it" }) =
    wrap("", suffix, transform)
