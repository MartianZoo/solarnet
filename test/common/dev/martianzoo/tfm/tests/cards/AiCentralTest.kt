package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AiCentralTest : CardTest() {
  @Test
  internal fun `Can be played with three science tags`() {
    startActionGame()
    establishScienceTags(3)
    p1.stdProject("PowerPlantProject")

    p1.playProject(AiCentral, 21).expect("PROD[-Energy]")
  }

  @Test
  internal fun `Can use its action`() {
    startActionGame()
    playAiCentral()

    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  internal fun `Can use its action again next generation`() {
    startActionGame()
    playAiCentral()
    p1.cardAction1(AiCentral)

    admin.phase("Production")
    p1.autoExecPolicy = CONCRETE
    requireP2().autoExecPolicy = CONCRETE
    admin.phase("Research") {
      p1.buyCards(0)
      requireP2().buyCards(0)
    }
    admin.phase("Action")
    p1.autoExecPolicy = EAGER

    p1.cardAction1(AiCentral).expect("2 ProjectCard")
  }

  @Test
  internal fun `Cannot be played with only two science tags`() {
    startActionGame()
    establishScienceTags(2)
    p1.stdProject("PowerPlantProject")

    shouldThrow<RequirementException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot be played without energy production`() {
    startActionGame()
    establishScienceTags(3)

    shouldThrow<LimitsException> { p1.playProject(AiCentral, 21) }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    startActionGame()
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

  private fun startActionGame() {
    newGame()
    admin.phase("Action")
    p1.runOperation(
        "500 MC, " +
            "ProjectCard<Class<$SearchForLife>, Hand>, " +
            "ProjectCard<Class<$InventorsGuild>, Hand>, " +
            "ProjectCard<Class<$DesignedMicroorganisms>, Hand>, " +
            "ProjectCard<Class<$AiCentral>, Hand>"
    )
  }

  private fun playAiCentral() {
    establishScienceTags(3)
    p1.stdProject("PowerPlantProject")
    p1.playProject(AiCentral, 21)
  }
}
