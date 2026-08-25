package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Utopia
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CapitalTest : CardTest() {
  @Test
  internal fun `Requires at least four oceans`() {
    newGame()
    p1.manual(
        "26 Megacredit, ProjectCard, PROD[2 Energy], " +
            "OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>"
    )
    engine.manual("OceanTile<Tharsis_6_8>")
    engine.phase("Action")

    shouldThrow<RequirementException> {
      p1.playProject(Capital, 26) { placeTile(3, 3) }
    }
  }

  @Test
  internal fun `Places a city under normal restrictions and scores adjacent oceans`() {
    newGame()
    p1.manual(
        "26 Megacredit, ProjectCard, PROD[2 Energy], " +
            "OceanTile<Tharsis_3_2>, OceanTile<Tharsis_4_3>"
    )
    engine.manual("OceanTile<Tharsis_6_8>, OceanTile<Tharsis_9_9>")
    engine.phase("Action")

    p1.playProject(Capital, 26) {
      placeTile(3, 3)
    }

    p1.assertCounts(1 to "CityTile<Tharsis_3_3>")
    p1.manual("GreeneryTile<Tharsis_2_3>")
    engine.phase("End")
    p1.assertCounts(27 to "VictoryPoint")
  }

  @Test
  internal fun `Does not count toward the Manager milestone`() {
    newGame(Utopia)
    p1.manual("8, PROD[2 Energy]")
    p1.manual("EcologicalZone_SpecialTile<Utopia_2_2>, NaturalPreserve_SpecialTile<Utopia_3_3>")
    p1.manual("$Capital") { placeTile(1, 1) }
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.stdAction("ClaimMilestoneSA") { doTask("Manager") } }
  }
}
