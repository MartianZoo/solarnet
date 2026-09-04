package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesAwardsExpansionTest : CardTest() {
  @Test
  internal fun `Philantropist counts victory point gains but not Vitor's reference`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Philantropist, Builder, Engineer",
            "Player1",
            "Player2",
        )
    )
    p1.manual("$Vitor, $SearchForLife, $Tardigrades, $ColonizerTrainingCamp, $DustSeals")

    shouldThrow<RequirementException> { p1.manual("Philantropist") }

    p1.manual("$SpaceElevator")
    p1.manual("Philantropist")
    p1.count("Philantropist") shouldBe 1
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    val game =
        newGame(
            GameConfig(
                "Merchant, Builder, Engineer",
                "Player1",
                "Player2",
            )
        )
    game.classTable.isActive(cn("Merchant")) shouldBe true
    p1.manual("10 M, 2 S, 2 T, 2 P, 2 E, 2 H")
    engine.phase("Action")

    p1.stdAction("ClaimMilestone") { doTask("Merchant") }

    p1.count("Merchant") shouldBe 1
  }

  // Producer wants 16 printed production, and Producer22 wants 22 because QuickStartVariant hands
  // you 6 at setup. Both start one short of their threshold after these grants.
  private fun claimProducerOneProductionShortOfThreshold(milestone: String, modules: String) {
    newGame(GameConfig("$milestone, Builder, Engineer$modules", "Player1", "Player2"))
    p1.manual("8 M")
    p1.manual("PROD[5 Steel, 5 Titanium, 5 Plant]")
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.manual(milestone) }

    p1.manual("PROD[Energy]")
    p1.stdAction("ClaimMilestone") { doTask(milestone) }

    p1.count(milestone) shouldBe 1
  }

  @Test
  internal fun `Producer requires sixteen printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer", "")

  @Test
  internal fun `Producer22 requires twenty two printed production`() =
      claimProducerOneProductionShortOfThreshold("Producer22", ", -CorporateEraExpansion")

  @Test
  internal fun `Producer versions belong to opposite Quick Start modes`() {
    shouldThrow<LimitsException> {
      newGame(
          GameConfig(
              "Producer, Builder, Engineer, -CorporateEraExpansion",
              "Player1",
              "Player2",
          )
      )
    }
    shouldThrow<IllegalArgumentException> {
      newGame(
          GameConfig(
              "Producer22, Builder, Engineer",
              "Player1",
              "Player2",
          )
      )
    }
  }
}
