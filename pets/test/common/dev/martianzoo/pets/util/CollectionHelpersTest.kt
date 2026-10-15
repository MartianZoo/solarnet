package dev.martianzoo.pets.util

import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CollectionHelpersTest {
  @Test
  internal fun strictCollectionsRejectDuplicateKeys() {
    shouldThrow<IllegalArgumentException> { listOf("same", "same").toSetStrict() }
    shouldThrow<IllegalArgumentException> { listOf("one", "two").associateByStrict { it.length } }
  }

  @Test
  internal fun anEmptySequenceHasNoRandomElement() {
    shouldThrow<IllegalStateException> { emptySequence<Int>().random() }
  }
}
