package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class LavaFlowsTest : CardTest() {
  @Test
  fun `on Tharsis Map, resolves Lava Flows`() {
    newGame()
    p1.manual("LavaFlows") { doTask("LfTile<Tharsis_2_2>") }.expect("2 TemperatureStep")
  }

  @Test
  fun `on Hellas, resolves Lava Flows`() {
    newGame("TerraformingMars,HellasMapOption")
    p1.manual("LavaFlows") { doTask("LfTile<Hellas_1_5>") }.expect("2 TemperatureStep")
  }

  @Test
  fun `with every volcanic area occupied, tries to place Lava Flows`() {
    newGame()
    p1.manual(
        "GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_3_1>, " +
            "GreeneryTile<Tharsis_4_1>, GreeneryTile<Tharsis_5_1>"
    )

    p1.manual("LavaFlows") {
      shouldThrow<NarrowingException> { doTask("LfTile<Tharsis_2_3>") }
      abort()
    }

    p1.assertCounts(0 to "LavaFlows", 0 to "LfTile")
    p1.temperatureC() shouldBe -30
  }
}
