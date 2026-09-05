package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AiCentralTest : CardTest() {
  @Test
  internal fun `Can be played with three science tags`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    establishScienceTags(3)
    p1.stdProject("PowerPlantSP")

    p1.playProject(AiCentral, 21).expect("PROD[-Energy]")
  }

  @Test
  internal fun `Can use its action`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    playAiCentral()

    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

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
  internal fun `Cannot be played with only two science tags`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    establishScienceTags(2)
    p1.stdProject("PowerPlantSP")

    shouldThrow<RequirementException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot be played without energy production`() {
    newGameWithAutoWorkflow()
    playUntilFirstActionPhase()
    establishScienceTags(3)

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

  private fun establishScienceTags(count: Int) {
    require(count in 2..3)
    p1.turn {
      playProject(SearchForLife, 3)
      playProject(InventorsGuild, 9)
    }
    requireP2().pass()
    if (count == 3) p1.playProject(DesignedMicroorganisms, 16)
  }

  private fun playAiCentral() {
    establishScienceTags(3)
    p1.stdProject("PowerPlantSP")
    p1.playProject(AiCentral, 21)
  }
}
