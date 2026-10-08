package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AiCentralTest : CardTest() {
  @Test
  internal fun `Can use its action again next generation`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    playAiCentral()
    p1.cardAction1(AiCentral)

    p1.pass()
    p1.buyCards(0)
    requireP2().buyCards(0)
    requireP2().pass()

    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  internal fun `Cannot be played without energy production`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    establishScienceTags()

    shouldThrow<LimitsException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    playAiCentral()
    p1.cardAction1(AiCentral)

    shouldThrow<LimitsException> { p1.cardAction1(AiCentral) }
  }

  private fun establishScienceTags() {
    p1.turn {
      playProject(SearchForLife, 3)
      playProject(InventorsGuild, 9)
    }
    requireP2().pass()
    p1.playProject(DesignedMicroorganisms, 16)
  }

  private fun playAiCentral() {
    establishScienceTags()
    p1.stdProject("PowerPlantProject")
    p1.playProject(AiCentral, 21)
  }
}
