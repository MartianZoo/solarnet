package dev.martianzoo.tfm.engine

import dev.martianzoo.tfm.engine.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TestHelpersTest : CardTest() {
  @Test
  internal fun `Net-change expectations reject empty argument lists`() {
    newGame()
    val result = p1.manual("GreeneryTile<Tharsis_6_6>")

    result.expect("GreeneryTile")
    shouldThrow<IllegalArgumentException> { result.expect("GreeneryTile<>") }.message shouldBe
        "empty argument lists are not allowed in net-change expectations; write `GreeneryTile` instead of `GreeneryTile<>`"
  }
}
