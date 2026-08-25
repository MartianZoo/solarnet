package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Aphrodite
import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentRulesTest : CardTest() {
  @Test
  internal fun `A completed parameter is not a legal World Government choice`() {
    newGame(VenusNextExpansion)
    p1.manual("15 VenusStep")
    TfmWorkflow.Manual(game).solarPhase()

    shouldThrow<LimitsException> { p1.doTask("VenusStep! BY Engine") }
    p1.doTask("TemperatureStep! BY Engine")
  }

  @Test
  internal fun `Engine terraforming triggers Aphrodite without granting terraform rating`() {
    newGame(VenusNextExpansion, PromoCardPack)
    p1.manual("$Aphrodite")
    val moneyBefore = p1.count("Megacredit")
    val ratingBefore = p1.count("TerraformRating")
    TfmWorkflow.Manual(game).solarPhase()

    p1.doTask("VenusStep! BY Engine")

    p1.count("Megacredit") shouldBe moneyBefore + 2
    p1.count("TerraformRating") shouldBe ratingBefore
  }

  @Test
  internal fun `Engine terraforming does not trigger an owner-only effect`() {
    newGame(VenusNextExpansion, PromoCardPack)
    p1.manual("$HomeostasisBureau")
    TfmWorkflow.Manual(game).solarPhase()

    p1.doTask("TemperatureStep! BY Engine")

    p1.count("Megacredit") shouldBe 0
  }

  @Test
  internal fun `World Government is absent when unselected or disabled in Venus`() {
    newGame()
    TfmWorkflow.Manual(game).solarPhase()
    game.isIdle() shouldBe true

    newGame(
        GameConfig(
            "VenusNextExpansion, -WorldGovernmentOption",
            "Player1",
            "Player2",
        )
    )
    TfmWorkflow.Manual(game).solarPhase()

    game.isIdle() shouldBe true
  }

  @Test
  internal fun `World Government can be selected without Venus`() {
    newGame(GameConfig("WorldGovernmentOption", "Player1", "Player2"))

    TfmWorkflow.Manual(game).solarPhase()
    p1.doTask("TemperatureStep! BY Engine")

    p1.count("TemperatureStep") shouldBe 1
  }
}
