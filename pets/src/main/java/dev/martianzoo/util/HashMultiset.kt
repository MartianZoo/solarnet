package dev.martianzoo.util

import kotlin.math.min

class HashMultiset<E> : MutableMultiset<E> {
  private val map = mutableMapOf<E, Int>()

  override val size
    get() = map.values.sum()

  override fun contains(element: E) = element in map

  override fun containsAll(elements: Collection<E>) = map.keys.containsAll(elements)

  override fun isEmpty() = map.isEmpty()

  override fun iterator(): MutableIterator<E> {
    val iter = map.asSequence().flatMap { (e, ct) -> List(ct) { e } }.iterator()
    return object : MutableIterator<E>, Iterator<E> by iter {
      override fun remove() = throw UnsupportedOperationException("sorry")
    }
  }

  override fun add(element: E): Boolean {
    add(element, 1)
    return true
  }

  override val elements by map::keys

  override fun count(element: E) = map[element] ?: 0

  override fun setCount(element: E, newCount: Int): Int /*old count*/ {
    require(newCount >= 0) { "tried to set count of some component to $newCount" }
    val old = count(element)
    if (newCount == 0) {
      map.remove(element)
    } else {
      map[element] = newCount
    }
    return old
  }

  override fun add(element: E, occurrences: Int): Int /*new count*/ {
    require(occurrences >= 0)
    val newCount = count(element) + occurrences
    setCount(element, newCount)
    return newCount
  }

  override fun addAll(elements: Collection<E>): Boolean {
    if (elements is MutableMultiset<E>) {
      elements.elements.forEach { add(it, elements.count(it)) }
    } else {
      elements.forEach(::add)
    }
    return elements.any()
  }

  override fun clear() = map.clear()

  override fun remove(element: E) = tryRemove(element, 1) == 1

  override fun mustRemove(element: E, occurrences: Int): Int /* new count */ {
    require(occurrences >= 0)
    val newCount = count(element) - occurrences
    setCount(element, newCount)
    return newCount
  }

  override fun tryRemove(element: E, occurrences: Int): Int /* how many removed */ {
    val count = count(element)
    val countToRemove = min(count, occurrences)
    mustRemove(element, countToRemove)
    return countToRemove
  }

  override fun removeAll(elements: Collection<E>) =
      map.keys.removeAll(
          if (elements is MutableMultiset<E>) {
            elements.elements
          } else {
            elements.toSet()
          })

  override fun retainAll(elements: Collection<E>) =
      map.keys.retainAll(
          if (elements is MutableMultiset<E>) {
            elements.elements
          } else {
            elements.toSet()
          })
}
