package dev.martianzoo.tfm.data

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionDefinitionTest {
  @Test
  fun testOneFromCanon() {
    val claim = Canon.action(cn("ClaimMilestoneSA"))
    claim.shortName shouldBe cn("SAC")
    claim.bundle shouldBe "B"
    claim.project shouldBe false
    claim.actions.shouldContainExactlyInAnyOrder("8 -> Milestone")
  }
}
