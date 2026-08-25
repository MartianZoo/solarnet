package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StormcraftIncorporatedTest : CardTest() {
  @Test
  internal fun `Starts with 48 megacredits and can add a floater to another card`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(StormcraftIncorporated, 0).expect("48")

    engine.phase("Action")
    p1.manual("$TitanShuttles")
    p1.cardAction1(StormcraftIncorporated) { doTask("Floater<$TitanShuttles>") }
        .expect("Floater<$TitanShuttles>")
  }

  @Test
  internal fun `Can spend a floater as two heat for an action cost`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    engine.phase("Action")
    p1.manual("$StormcraftIncorporated, Floater<$StormcraftIncorporated>, 6 Heat")

    p1.stdAction(
            "ConvertHeatSA",
            payment = {
              doTask("PayFromCard<$StormcraftIncorporated> FROM Floater<$StormcraftIncorporated>")
              doTask("6 Pay<Class<Heat>> FROM Heat")
            },
        )
        .expect("-Floater<$StormcraftIncorporated>, -6 Heat, TemperatureStep")
  }

  @Test
  internal fun `Can spend no floaters on Local Heat Trapping`() {
    initializeStormcraftGame(floaters = 1, heat = 5)

    p1.playProject(LocalHeatTrapping, 1) {
          doTask("4 Plant")
          doTask("Ok")
        }
        .expect("-5 Heat, 4 Plant")
    p1.count("Floater<$StormcraftIncorporated>") shouldBe 1
  }

  @Test
  internal fun `Can spend two floaters after Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 2, heat = 5)

    p1.playProject(LocalHeatTrapping, 1) {
          doTask("4 Plant")
          doTask("-2 Floater<$StormcraftIncorporated> THEN 4 Heat")
        }
        .expect("-2 Floater<$StormcraftIncorporated>, -Heat, 4 Plant")
  }

  @Test
  internal fun `Can spend three floaters before Local Heat Trapping removes heat`() {
    initializeStormcraftGame(floaters = 3, heat = 0)

    p1.playProject(LocalHeatTrapping, 1) {
          doTask("-3 Floater<$StormcraftIncorporated> THEN 5 Heat")
          doTask("4 Plant")
        }
        .expect("-3 Floater<$StormcraftIncorporated>, 4 Plant")
  }

  private fun initializeStormcraftGame(floaters: Int, heat: Int) {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    engine.phase("Action")
    val heatSetup = if (heat == 0) "" else ", $heat Heat"
    p1.manual(
        "$StormcraftIncorporated, $floaters Floater<$StormcraftIncorporated>$heatSetup, ProjectCard, 1"
    )
  }
}
