package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TestOption.Cimmeria
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class MiningAreaTest : CardTest() {
  @Test
  internal fun `Can be placed adjacent to a steel area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    p1.manual("$MiningArea") { placeTile(1, 1) }.expect("2 Steel, PROD[Steel]")
  }

  @Test
  internal fun `Can be placed adjacent to a titanium area`() {
    newGame()
    p1.manual("CityTile<Tharsis_7_9>")
    p1.manual("$MiningArea") { placeTile(8, 9) }.expect("Titanium, PROD[Titanium]")
  }

  @Test
  internal fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    newGame(Cimmeria)
    p1.manual("CityTile<Cimmeria_5_4>")
    p1.manual("$MiningArea") {
          placeTile(6, 4)
          doTask("PROD[Steel]")
        }
        .expect("Titanium, 2 Steel, PROD[Steel]")

    val manual = p1.also { it.autoExecMode = NONE }
    manual.beginManual("$RoboticWorkforce")
    manual.selectTask(
        "CopyProductionBox<CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))>"
    )
    manual.narrowTask("CopyProductionBox<$MiningArea>")
    manual.finish { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  internal fun `Cannot be played without an adjacent owned tile`() {
    newGame()
    shouldThrow<DependencyException> { p1.manual("$MiningArea") { placeTile(1, 1) } }
  }

  @Test
  internal fun `Cannot select a card-bonus area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NotNowException> { p1.manual("$MiningArea") { placeTile(3, 2) } }
  }
}
