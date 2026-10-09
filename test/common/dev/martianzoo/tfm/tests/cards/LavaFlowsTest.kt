package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class LavaFlowsTest : ProjectCardTest() {
  @Test
  internal fun `Can place its tile on Tharsis`() {
    newTestGame()
    kim.setToExMachina(18, "MC")

    kim.playProject(LavaFlows, 18) { placeTile(2, 2) }
        .expect("2 TemperatureStep, LavaFlows_SpecialTile")
  }

  @Test
  internal fun `Can place its tile on Hellas`() {
    newTestGame(addOptions = "HellasMap")
    kim.setToExMachina(18, "MC")

    kim.playProject(LavaFlows, 18) { placeTile(1, 5) }
        .expect("2 TemperatureStep, LavaFlows_SpecialTile")
  }

  @Test
  internal fun `An occupied volcanic placement rolls back the card and temperature increase`() {
    newTestGame()
    kim.setToExMachina(18, "MC")
    kim.exMachina(
        "GreeneryTile<Tharsis_2_2>, GreeneryTile<Tharsis_3_1>, " +
            "GreeneryTile<Tharsis_4_1>, GreeneryTile<Tharsis_5_1>"
    )
    val cardsBefore = kim.count("ProjectCard")

    shouldThrow<LimitsException> { kim.playProject(LavaFlows, 18) { placeTile(2, 2) } }

    kim.count("MC") shouldBe 18
    kim.count("ProjectCard") shouldBe cardsBefore
    kim.count("Tile<Tharsis_2_2>") shouldBe 1
    kim.count("$LavaFlows") shouldBe 0
    kim.temperatureC() shouldBe -30
  }
}
