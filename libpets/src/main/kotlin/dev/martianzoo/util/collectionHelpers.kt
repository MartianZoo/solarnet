package dev.martianzoo.util

import com.google.common.collect.ImmutableMultiset
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multiset

// should go in libutil but man, so many libs!

fun Iterable<Any>.joinOrEmpty(sep: String = ", ", prefix: String = "", suffix: String = "") =
    if (any()) joinToString(sep, prefix, suffix) else ""

fun Iterable<Any>.joinOrEmpty(sep: String = ", ", surround: String): String {
  require(surround.length == 2)
  return joinOrEmpty(sep, surround.substring(0, 1), surround.substring(1, 2))
}

fun <T> multiset(vararg pairs: Pair<Int, T>): ImmutableMultiset<T> {
  val builder = ImmutableMultiset.builder<T>()
  pairs.forEach { builder.addCopies(it.second, it.first) }
  return builder.build()
}

fun <T> toListWeirdly(multiset: Multiset<T>): List<T> {
  val multimap = MultimapBuilder.treeKeys().arrayListValues().build<Double, T>()
  multiset.entrySet().forEach {
    for (i in 1..it.count) {
      multimap.put(i.toDouble() / it.count, it.element)
    }
  }
  return multimap.values().toList()
}
