package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.cardnames.AirRaid
import dev.martianzoo.tfm.tests.cards.cardnames.AtmoCollectors
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AirRaidTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Steals all five MC`() {
    kim.exMachina("$AtmoCollectors, 2 Floater<$AtmoCollectors>")
    rob.setToExMachina(5, "MC")

    kim.playProject(AirRaid, 0) {
          doTask("5 MC FROM MC<Rob>")
        }
        .expect("-Floater<$AtmoCollectors>, 5 MC<Kim>, -5 MC<Rob>")
  }

  @Test
  internal fun `Cannot steal from yourself`() {
    kim.exMachina("$AtmoCollectors, 2 Floater<$AtmoCollectors>")
    kim.setToExMachina(5, "MC")
    rob.setToExMachina(5, "MC")

    shouldThrow<ExpressionException> {
      kim.playProject(AirRaid, 0) { doTask("5 MC FROM MC<Kim>") }
    }
  }

  @Test
  internal fun `Cannot be played when only its owner has five MC`() {
    kim.exMachina("$AtmoCollectors, 2 Floater<$AtmoCollectors>")
    kim.setToExMachina(5, "MC")
    stan.setToExMachina(4, "MC")
    rob.setToExMachina(4, "MC")

    shouldThrow<LimitsException> {
      kim.playProject(AirRaid, 0) { doTask("5 MC FROM MC<Stan>") }
    }
  }
}
