package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.generated.Class
import dev.martianzoo.generated.CorporateEraExpansion
import dev.martianzoo.generated.ElysiumMap
import dev.martianzoo.generated.QuickStartVariant
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class QuickStartVariantTest : CardTest() {
  @Test
  internal fun `Quick Start gives every player one standard-resource production`() {
    val game =
        newGame(
            gameConfig(
                extra = "-CorporateEraExpansion",
                playerNames = listOf("Player1", "Player2", "Player3", "Player4", "Player5"),
            )
        )

    Player.players(5)
        .map { game.testTfm(it) }
        .forEach { player ->
          player.assertProds(
              1 to "MC",
              1 to "Steel",
              1 to "Titanium",
              1 to "Plant",
              1 to "Energy",
              1 to "Heat",
          )
        }
  }

  @Test
  internal fun `Quick Start can be explicitly disabled or combined with Corporate Era`() {
    newGame(
        gameConfig(
            modules = listOf(Class.of(CorporateEraExpansion), Class.of(QuickStartVariant)),
            playerNames = listOf("Player1", "Player2"),
        )
    )
    p1.assertProds(
        1 to "MC",
        1 to "Steel",
        1 to "Titanium",
        1 to "Plant",
        1 to "Energy",
        1 to "Heat",
    )

    newGame(
        gameConfig(
            extra = "-CorporateEraExpansion, -QuickStartVariant",
            playerNames = listOf("Player1", "Player2"),
        )
    )
    p1.assertProds(
        0 to "MC",
        0 to "Steel",
        0 to "Titanium",
        0 to "Plant",
        0 to "Energy",
        0 to "Heat",
    )
  }

  @Test
  internal fun `Elysium uses Generalist2 only in a Quick Start game`() {
    val quickStart =
        newGame(
            gameConfig(
                modules = listOf(Class.of(ElysiumMap)),
                extra = "-CorporateEraExpansion",
                playerNames = listOf("Player1", "Player2"),
            )
        )
    quickStart.classTable.isInhabited(cn("Generalist")) shouldBe false
    quickStart.classTable.isInhabited(cn("Generalist2")) shouldBe true
    p1.runOperation("8 MC")
    admin.phase("Action")

    shouldThrow<RequirementException> {
      p1.stdAction("ClaimMilestoneAction") { doTask("Generalist2") }
    }

    p1.runOperation("PROD[1 MC, Steel, Titanium, Plant, Energy, Heat]")
    p1.stdAction("ClaimMilestoneAction") { doTask("Generalist2") }
    p1.count("Milestone") shouldBe 1

    val corporateEra =
        newGame(
            gameConfig(
                modules = listOf(Class.of(ElysiumMap)),
                playerNames = listOf("Player1", "Player2"),
            )
        )
    corporateEra.classTable.isInhabited(cn("Generalist")) shouldBe true
    corporateEra.classTable.isInhabited(cn("Generalist2")) shouldBe false
  }
}
