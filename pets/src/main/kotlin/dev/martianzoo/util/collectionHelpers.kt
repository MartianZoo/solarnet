package dev.martianzoo.util

import com.google.common.collect.ImmutableMultiset
import dev.martianzoo.tfm.petaform.PetaformException

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

fun <T> Collection<T>.toSetCareful() = toSet().also { require(it.size == size) }
fun <T> Collection<T>.toSetCarefulP() = toSet().also { if(it.size != size) throw PetaformException("{$this}") }

fun <T, K> Collection<T>.associateByCareful(x: (T) -> K) =
    associateBy(x).also { require(it.size == size) }
