package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DiversitySupportTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Checks its ninth resource type before spending the last MC`() {
    kim.setToExMachina(1, "MC")
    kim.exMachina(
        "Steel, Titanium, Plant, Energy, Heat, $Pets, $Decomposers, $ForcedPrecipitation, " +
            "Animal<$Pets>, Microbe<$Decomposers>, Floater<$ForcedPrecipitation>"
    )

    kim.playProject(DiversitySupport, 1).expect("-MC, TerraformRating")
  }

  @Test
  internal fun `Cannot be played with eight resource types`() {
    kim.exMachina(
        "Steel, Titanium, Plant, Energy, Heat, " +
            "$Pets, $Decomposers, Animal<$Pets>, Microbe<$Decomposers>"
    )

    shouldThrow<RequirementException> { kim.playProject(DiversitySupport, 1) }
  }
}
