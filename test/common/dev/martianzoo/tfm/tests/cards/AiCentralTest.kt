package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AiCentralTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot be played without energy production`() {
    kim.exMachina("$SearchForLife, $InventorsGuild, $DesignedMicroorganisms")
    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(21, "MC")

    shouldThrow<LimitsException> { kim.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    kim.exMachina("$AiCentral")
    kim.cardAction1(AiCentral)

    shouldThrow<LimitsException> { kim.cardAction1(AiCentral) }
  }
}
