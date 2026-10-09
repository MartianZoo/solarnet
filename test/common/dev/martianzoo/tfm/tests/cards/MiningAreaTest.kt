package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.GameplayException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MiningAreaTest : ProjectCardTest() {
  @Test
  internal fun `Can be placed adjacent to a titanium area`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Kim, Tharsis_7_9>")

    kim.playProject(MiningArea, 4) { placeTile(8, 9) }.expect("-4 MC, Titanium, PROD[Titanium]")
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    // Resolved FAQ: copying allows any originally available metal, regardless of the first choice.
    newTestGame(addOptions = "CimmeriaMap")
    kim.exMachina("NormalCityTile<Kim, Cimmeria_5_4>")

    kim.playProject(MiningArea, 4) {
          placeTile(6, 4)
          doTask("PROD[Steel]")
        }
        .expect("-4 MC, Titanium, 2 Steel, PROD[Steel]")

    kim.playProject(RoboticWorkforce, 9) {
          doTask("CopyProductionBox<$MiningArea>")
          doTask("PROD[Titanium]")
        }
        .expect("-9 MC, PROD[Titanium]")
  }

  @Test
  internal fun `Cannot select an area without a metal placement bonus`() {
    newTestGame()
    kim.exMachina("NormalCityTile<Kim, Tharsis_2_1>")

    shouldThrow<GameplayException> { kim.playProject(MiningArea, 4) { placeTile(3, 2) } }
  }
}
