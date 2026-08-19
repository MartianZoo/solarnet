package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.UtopiaPlanitiaMapOption
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class CapitalTest : CardTest() {
  @Test
  fun `places a regular city and scores its adjacent oceans`() {
    newGame()
    p1.manual(
        "26 Megacredit, ProjectCard, PROD[2 Energy], " +
            "OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>"
    )
    engine.manual("OceanTile<Tharsis_6_8>, OceanTile<Tharsis_9_9>")
    engine.phase("Action")

    p1.playProject(Capital, 26) {
      doTask("CityTile<Tharsis_3_3>")
    }

    p1.assertCounts(1 to "CityTile<Tharsis_3_3>")
    p1.manual("GreeneryTile<Tharsis_2_3>")
    engine.phase("End")
    p1.assertCounts(27 to "VictoryPoint")
  }

  @Test
  fun `does not qualify as a special tile for the Manager milestone`() {
    newGame(UtopiaPlanitiaMapOption)
    p1.manual("8, PROD[2 Energy]")
    p1.manual("Card128_SpecialTile<Utopia_2_2>, Card044_SpecialTile<Utopia_3_3>")
    p1.manual("$Capital") { doTask("CityTile<Utopia_1_1>") }
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.stdAction("ClaimMilestoneSA") { doTask("Manager") } }
  }
}
