package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

class MiningAreaTest : CardTest() {
  @Test
  fun `with a steel area adjacent, plays Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    p1.manual("$MiningArea") {
          doTask("Card064_SpecialTile<Tharsis_1_1>")
        }
        .expect("2 Steel, PROD[Steel]")
  }

  @Test
  fun `with a titanium area adjacent, plays Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_7_9>")
    p1.manual("$MiningArea") {
          doTask("Card064_SpecialTile<Tharsis_8_9>")
        }
        .expect("Titanium, PROD[Titanium]")
  }

  @Test
  fun `without an adjacent tile, tries to play Mining Area`() {
    newGame()
    shouldThrow<DependencyException> {
      p1.manual("$MiningArea") { doTask("Card064_SpecialTile<Tharsis_1_1>") }
    }
  }

  @Test
  fun `with a card-bonus area selected, tries to play Mining Area`() {
    newGame()
    p1.manual("CityTile<Tharsis_2_1>")
    shouldThrow<NotNowException> {
      p1.manual("$MiningArea") { doTask("Card064_SpecialTile<Tharsis_3_2>") }
    }
  }
}
