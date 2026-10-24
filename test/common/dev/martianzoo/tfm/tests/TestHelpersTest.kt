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
    val result = kim.inTurn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_6_6>",
      )
      doTask("OxygenStep")
      doTasks("Plant", "TerraformRating")
    }

    result.expect("GreeneryTile, 0 Steel, -0 Titanium, PROD[0 Energy]")
    shouldThrow<IllegalArgumentException> { result.expect("GreeneryTile<>") }.message shouldBe
        "empty argument lists are not allowed in net-change expectations; write `GreeneryTile` instead of `GreeneryTile<>`"
  }
}
