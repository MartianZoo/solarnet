package dev.martianzoo.tfm.tests

import dev.martianzoo.engine.*
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TestHelpersTest : TfmSandboxTest() {
  @Test
  internal fun `Net-change expectations reject empty argument lists`() {
    newTestGame()
    val result = kim.stdProject("GreeneryProject") { placeTile(6, 6) }

    result.expect("GreeneryTile")
    shouldThrow<IllegalArgumentException> { result.expect("GreeneryTile<>") }.message shouldBe
        "empty argument lists are not allowed in net-change expectations; write `GreeneryTile` instead of `GreeneryTile<>`"
  }
}
