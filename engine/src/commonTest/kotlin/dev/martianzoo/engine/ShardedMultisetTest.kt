package dev.martianzoo.engine

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ShardedMultisetTest {
  @Test
  fun satisfiesMultisetContractWhileRestrictingQueriesToSelectedShards() {
    val multiset =
        ShardedMultiset<String, Set<Int>, Int>(
            shardFor = String::length,
            queryShardsFor = { it },
        )

    multiset.add("one", 2)
    multiset.add("four", 1)
    multiset.add("five", 3)

    multiset.size shouldBe 6
    multiset.count("one") shouldBe 2
    multiset.count("absent") shouldBe 0
    multiset.elements.shouldContainExactlyInAnyOrder("one", "four", "five")
    multiset.toList().shouldContainExactlyInAnyOrder("one", "one", "four", "five", "five", "five")
    multiset.filter { it.startsWith("f") }.size shouldBe 4
    multiset.filter(setOf(3)) { true }.elements.shouldContainExactlyInAnyOrder("one")
    multiset.queryElements(setOf(4)).toList().shouldContainExactlyInAnyOrder("four", "five")

    multiset.mustRemove("five", 2) shouldBe 1
    multiset.size shouldBe 4
    multiset.count("five") shouldBe 1
  }
}
