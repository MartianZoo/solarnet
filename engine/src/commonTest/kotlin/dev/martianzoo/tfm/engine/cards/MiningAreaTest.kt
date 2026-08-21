package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestOption.TerraCimmeriaMapOption
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class MiningAreaTest : CardTest() {
  @Test
  fun `Can be placed adjacent to a steel area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    p1.manual("$MiningArea") {
          doTask("Card064_SpecialTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")
  }

  @Test
  fun `Can be placed adjacent to a titanium area`() {
    newGame()
    p1.manual("CityTile<Tharsis_7_9>")
    p1.manual("$MiningArea") {
          doTask("Card064_SpecialTile<Tharsis_8_9>")
        }
        .expect("Titanium, PROD[Titanium]")
  }

  @Test
  fun `Robotic Workforce re-evaluates its production box instead of remembering steel`() {
    newGame(TerraCimmeriaMapOption)
    p1.manual("CityTile<Cimmeria_5_4>")
    p1.manual("$MiningArea") {
          doTask("Card064_SpecialTile<Cimmeria_6_4>")
          doTask("PROD[Steel]")
        }
        .expect("Titanium, 2 Steel, PROD[Steel]")

    val manual = p1.godMode().also { it.autoExecMode = NONE }
    manual.beginManual("$RoboticWorkforce")
    manual.reviseTask(
        "CopyProductionBox<CardFront(HAS BuildingTag OR WildTagUse(HAS BuildingTag))>",
        "CopyProductionBox<$MiningArea>",
    )
    manual.finish { doTask("PROD[Titanium]") }.expect("PROD[Titanium]")
  }

  @Test
  fun `Cannot be played without an adjacent owned tile`() {
    newGame()
    shouldThrow<DependencyException> {
      p1.manual("$MiningArea") { doTask("Card064_SpecialTile<Tharsis_1_1>") }
    }
  }

  @Test
  fun `Cannot select a card-bonus area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NotNowException> {
      p1.manual("$MiningArea") { doTask("Card064_SpecialTile<Tharsis_3_2>") }
    }
  }
}
