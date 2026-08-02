package dev.martianzoo.engine

import dev.martianzoo.util.HashMultiset
import dev.martianzoo.util.Multiset
import kotlin.collections.Map.Entry

/** A multiset partitioned into disjoint storage shards, with routing for broader query keys. */
internal class ShardedMultiset<E, Q, S>(
    private val shardFor: (E) -> S,
    private val queryShardsFor: (Q) -> Iterable<S>,
) : Multiset<E> {
  private val shards = mutableMapOf<S, HashMultiset<E>>()
  private var totalSize = 0

  override val size: Int
    get() = totalSize

  override val elements: Set<E>
    get() = distinctElements().toSet()

  override val entries: Set<Entry<E, Int>>
    get() = shards.values.flatMapTo(linkedSetOf()) { it.entries }

  override fun isEmpty(): Boolean = totalSize == 0

  override fun contains(element: E): Boolean = count(element) > 0

  override fun containsAll(elements: Collection<E>): Boolean = elements.all(::contains)

  override fun iterator(): Iterator<E> =
      shards.values.asSequence().flatMap { it.asSequence() }.iterator()

  override fun count(element: E): Int = shards[shardFor(element)]?.count(element) ?: 0

  internal fun add(element: E, occurrences: Int): Int {
    require(occurrences >= 0)
    if (occurrences == 0) return count(element)
    totalSize += occurrences
    return shards.getOrPut(shardFor(element), ::HashMultiset).add(element, occurrences)
  }

  internal fun mustRemove(element: E, occurrences: Int): Int {
    require(occurrences >= 0)
    if (occurrences == 0) return count(element)
    val shard = shardFor(element)
    val multiset = shards[shard] ?: error("tried to remove absent element $element")
    val result = multiset.mustRemove(element, occurrences)
    totalSize -= occurrences
    if (multiset.isEmpty()) shards.remove(shard)
    return result
  }

  internal fun queryElements(query: Q): Sequence<E> =
      queryShardsFor(query).asSequence().distinct().mapNotNull(shards::get).flatMap { it.elements }

  internal fun queryEntries(query: Q): Sequence<Entry<E, Int>> =
      queryShardsFor(query).asSequence().distinct().mapNotNull(shards::get).flatMap { it.entries }

  internal fun filter(query: Q, predicate: (E) -> Boolean): Multiset<E> =
      HashMultiset<E>().also { result ->
        queryEntries(query).forEach { (element, count) ->
          if (predicate(element)) result.add(element, count)
        }
      }

  internal fun copy(): HashMultiset<E> =
      HashMultiset<E>().also { result -> shards.values.forEach(result::addAll) }

  internal fun distinctElements(): Sequence<E> = shards.values.asSequence().flatMap { it.elements }
}
