package dev.martianzoo.util

import com.google.common.collect.ImmutableMultiset
import dev.martianzoo.tfm.pets.PetsException
import dev.martianzoo.tfm.types.PetType
import dev.martianzoo.tfm.types.PetType.DependencyKey

// should go in libutil but man, so many libs!

fun Iterable<Any?>.joinOrEmpty(sep: String = ", ", prefix: String = "", suffix: String = "") =
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

fun <T> Collection<T>.toSetStrict() = toSet().also { require(it.size == size) }
fun <T> Collection<T>.toSetCarefulP() = toSet().also { if (it.size != size) throw PetsException("{$this}") }

fun <T, K> Collection<T>.associateByStrict(x: (T) -> K) = associateBy(x).also { require(it.size == size) }

fun <K : Any, V : Any> merge(one: Map<out K, V>, two: Map<out K, V>, merger: (V, V) -> V) =
    one.toMutableMap().apply {
      two.forEach { merge(it.key, it.value, merger) }
    }
