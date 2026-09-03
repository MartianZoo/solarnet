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

  @Test
  internal fun `Producer22 requires combined production of twenty two`() {
    newGame(
        GameConfig(
            "Producer22, Builder, Engineer, -CorporateEraExpansion",
            "Player1",
            "Player2",
        )
    )
    p1.manual("8 M")
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.manual("Producer22") }

    p1.manual("PROD[6 MC, Steel, Titanium, Plant, Energy, Heat]")
    p1.count("PROD[StandardResource]") shouldBe 22
    p1.stdAction("ClaimMilestone") { doTask("Producer22") }
    p1.count("Milestone") shouldBe 1
  }

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
