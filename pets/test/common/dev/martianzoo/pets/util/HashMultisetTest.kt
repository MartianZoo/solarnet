package dev.martianzoo.pets.util

import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class HashMultisetTest {
  @Test
  internal fun rejectsNegativeCounts() {
    val multiset = HashMultiset<String>()

    shouldThrow<IllegalArgumentException> { multiset.setCount("Plant", -1) }
    shouldThrow<IllegalArgumentException> { multiset.add("Plant", -1) }
    shouldThrow<IllegalArgumentException> { multiset.mustRemove("Plant", -1) }
    shouldThrow<IllegalArgumentException> { multiset.tryRemove("Plant", -1) }
    shouldThrow<IllegalArgumentException> {
      HashMultiset(mutableMapOf("Plant" to -1)).iterator().hasNext()
    }
  }

  @Test
  internal fun iteratorDoesNotSupportRemoval() {
    val iterator = HashMultiset.of(listOf("Plant")).iterator()
    iterator.next()

    shouldThrow<UnsupportedOperationException> { iterator.remove() }
  }
}
