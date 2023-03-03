package dev.martianzoo.util

public interface Multiset<E> : Collection<E> {
  public val elements: Set<E>
  public fun count(element: E): Int

  public fun filter(predicate: (E) -> Boolean): Multiset<E> {
    val result = HashMultiset<E>()
    elements.filter(predicate).forEach { result.add(it, count(it)) }
    return result
  }

  public fun <T : Any> map(function: (E) -> T): Multiset<T> {
    val result = HashMultiset<T>()
    elements.forEach { result.add(function(it), count(it)) }
    return result
  }
}
