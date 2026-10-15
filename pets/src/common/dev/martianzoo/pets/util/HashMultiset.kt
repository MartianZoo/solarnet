package dev.martianzoo.pets.util

import kotlin.collections.Map.Entry
import kotlin.math.min

/**
 * Mutable [Multiset] backed by [map]. A caller-supplied map remains shared with this instance; use
 * the no-argument constructor or [of] for independent storage. Iteration repeats each element by
 * its count, but iterator removal is unsupported. [elements] and [entries] are live map views. A
 * supplied backing map must contain only positive counts.
 */
public class HashMultiset<E>(private val map: MutableMap<E, Int> = mutableMapOf()) :
    MutableMultiset<E> {
  public companion object {
    /** Copies [elements], preserving repeated occurrences, into a new mutable multiset. */
    public fun <E> of(elements: Iterable<E>): HashMultiset<E> =
        HashMultiset<E>().also { it.addAll(elements) }
  }

  override val size: Int
    get() = map.values.sum()

  override fun contains(element: E): Boolean = element in map

  /** Tests distinct-element membership; duplicate inputs do not require duplicate occurrences. */
  override fun containsAll(elements: Collection<E>): Boolean = map.keys.containsAll(elements)

  override fun isEmpty(): Boolean = map.isEmpty()

  /** Repeats elements by their current counts; [MutableIterator.remove] is unsupported. */
  override fun iterator(): MutableIterator<E> {
    val iter = map.asSequence().flatMap { (e, ct) -> List(ct) { e } }.iterator()
    return object : MutableIterator<E>, Iterator<E> by iter {
      override fun remove() =
          throw UnsupportedOperationException("multiset iterator does not support removal")
    }
  }

  override fun add(element: E): Boolean {
    add(element, 1)
    return true
  }

  override val elements: Set<E>
    get() = map.keys

  override val entries: Set<Entry<E, Int>>
    get() = map.entries

  override fun count(element: E): Int = map[element] ?: 0

  /**
   * Sets [element]'s count to [newCount], removing it from [elements] when the count is zero.
   * Returns the previous count; a negative new count fails without changing this multiset.
   */
  override fun setCount(element: E, newCount: Int): Int /*old count*/ {
    require(newCount >= 0) { "count cannot be negative for `$element`: `$newCount`" }
    val old = count(element)
    if (newCount == 0) {
      map.remove(element)
    } else {
      map[element] = newCount
    }
    return old
  }

  /** Adds [occurrences] and returns the resulting count; a negative amount fails. */
  override fun add(element: E, occurrences: Int): Int /*new count*/ {
    require(occurrences >= 0)
    val newCount = count(element) + occurrences
    setCount(element, newCount)
    return newCount
  }

  override fun addAll(elements: Collection<E>): Boolean {
    if (elements is Multiset<E>) {
      elements.entries.forEach { (e, ct) -> add(e, ct) }
    } else {
      elements.forEach(::add)
    }
    return elements.isNotEmpty()
  }

  override fun clear(): Unit = map.clear()

  override fun remove(element: E): Boolean = tryRemove(element, 1) == 1

  /**
   * Removes exactly [occurrences] and returns the resulting count. An insufficient current count or
   * a negative amount fails without changing this multiset.
   */
  override fun mustRemove(element: E, occurrences: Int): Int /* new count */ {
    require(occurrences >= 0)
    val newCount = count(element) - occurrences
    setCount(element, newCount)
    return newCount
  }

  /**
   * Removes up to [occurrences] and returns how many were removed. An absent element yields zero; a
   * negative amount fails.
   */
  override fun tryRemove(element: E, occurrences: Int): Int /* how many removed */ {
    val count = count(element)
    val countToRemove = min(count, occurrences)
    mustRemove(element, countToRemove)
    return countToRemove
  }

  /** Removes every occurrence of each distinct element represented by [elements]. */
  override fun removeAll(elements: Collection<E>): Boolean =
      map.keys.removeAll(
          if (elements is MutableMultiset<E>) {
            elements.elements
          } else {
            elements.toSet()
          }
      )

  /** Keeps every occurrence of each distinct element represented by [elements]. */
  override fun retainAll(elements: Collection<E>): Boolean =
      map.keys.retainAll(
          if (elements is MutableMultiset<E>) {
            elements.elements
          } else {
            elements.toSet()
          }
      )
}
