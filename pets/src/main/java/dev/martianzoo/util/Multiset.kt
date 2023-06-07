package dev.martianzoo.util

import kotlin.collections.Map.Entry

public interface Multiset<E> : Collection<E> {
  public val elements: Set<E>
  val entries: Set<Entry<E, Int>>
  public fun count(element: E): Int

  public fun filter(predicate: (E) -> Boolean): Multiset<E> {
    val result = HashMultiset<E>()
    elements.forEach { if (predicate(it)) result.add(it, count(it)) }
    return result
  }

  public fun <T : Any> map(function: (E) -> T): Multiset<T> {
    val result = HashMultiset<T>()
    entries.forEach { (e, ct) -> result.add(function(e), ct) }
    return result
  }

  public fun <T : Any> flatMap(function: (E) -> Iterable<T>): Multiset<T> {
    val result = HashMultiset<T>()
    entries.forEach { (e, ct) -> function(e).forEach { result.add(it, ct) } }
    return result
  }
}
