package dev.martianzoo.util

interface Multiset<E> : Collection<E> {
  val elements: MutableSet<E>
  fun count(element: E): Int
}

// This is actually kinda hopeless

public fun <E : Any, T : Any> Multiset<E>.map(thing: (E) -> T): Multiset<T> {
  val result = HashMultiset<T>()
  elements.forEach { result.add(thing(it), count(it)) }
  return result
}

public fun <E : Any> Multiset<E>.filter(thing: (E) -> Boolean): Multiset<E> {
  val result = HashMultiset<E>()
  elements.filter(thing).forEach { result.add(it, count(it)) }
  return result
}
