package dev.martianzoo.pets.util

import kotlin.collections.Map.Entry

/**
 * A collection whose distinct elements each have a nonnegative count. [size] counts occurrences,
 * while [elements] counts distinct values. For example, a multiset containing `a`, `a`, and `b` has
 * size 3, elements `{a, b}`, and `count(a) == 2`.
 */
public interface Multiset<E> : Collection<E> {
  /** Distinct elements with positive counts. */
  public val elements: Set<E>
  /** Each distinct element paired with its positive count. */
  public val entries: Set<Entry<E, Int>>

  /** Number of occurrences of [element], or zero if absent. */
  public fun count(element: E): Int

  /**
   * Returns a new multiset retaining every occurrence of each distinct element matching
   * [predicate].
   */
  public fun filter(predicate: (E) -> Boolean): Multiset<E> {
    val result = HashMultiset<E>()
    elements.forEach { if (predicate(it)) result.add(it, count(it)) }
    return result
  }

  /**
   * Returns a new multiset after applying [function] once to each distinct element. Counts of
   * elements with equal results add together; for example, mapping `a, a, b` to the same value
   * produces three occurrences of that value.
   */
  public fun <T : Any> map(function: (E) -> T): Multiset<T> {
    val result = HashMultiset<T>()
    entries.forEach { (e, ct) -> result.add(function(e), ct) }
    return result
  }

  /**
   * Returns a new multiset by replacing each occurrence with every value from [function]. The
   * function is called once per distinct element; duplicate outputs and counts are preserved.
   */
  public fun <T : Any> flatMap(function: (E) -> Iterable<T>): Multiset<T> {
    val result = HashMultiset<T>()
    entries.forEach { (e, ct) -> function(e).forEach { result.add(it, ct) } }
    return result
  }
}
