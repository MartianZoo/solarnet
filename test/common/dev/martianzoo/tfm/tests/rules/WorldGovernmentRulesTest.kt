package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.generated.AmazonisMap
import dev.martianzoo.generated.Class
import dev.martianzoo.generated.VenusNextExpansion
import dev.martianzoo.generated.WorldGovernmentRule
import dev.martianzoo.generated.gameConfig
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion as VenusNextExpansionOption
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Aphrodite
import dev.martianzoo.tfm.tests.cards.cardnames.HomeostasisBureau
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class WorldGovernmentRulesTest : CardTest() {
  @Test
  internal fun `A completed parameter is not a legal World Government choice`() {
    newGame(VenusNextExpansionOption)
    p1.runOperation("15 VenusStep")
    TfmWorkflow.Stepwise(agents).solarPhase()

    shouldThrow<LimitsException> { p1.doTask("VenusStep! BY Admin") }
    p1.doTask("TemperatureStep! BY Admin")
  }

  @Test
  internal fun `Admin terraforming triggers Aphrodite without granting terraform rating`() {
    newGame(VenusNextExpansionOption, PromoCardPack)
    p1.runOperation("$Aphrodite")
    val moneyBefore = p1.count("MC")
    val ratingBefore = p1.count("TerraformRating")
    TfmWorkflow.Stepwise(agents).solarPhase()

    p1.doTask("VenusStep! BY Admin")

    p1.count("MC") shouldBe moneyBefore + 2
    p1.count("TerraformRating") shouldBe ratingBefore
  }

  @Test
  internal fun `Admin terraforming does not trigger an owner-only effect`() {
    newGame(VenusNextExpansionOption, PromoCardPack)
    p1.runOperation("$HomeostasisBureau")
    TfmWorkflow.Stepwise(agents).solarPhase()

    p1.doTask("TemperatureStep! BY Admin")

    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `World Government is absent when unselected or disabled in Venus`() {
    newGame()
    TfmWorkflow.Stepwise(agents).solarPhase()
    game.isIdle() shouldBe true

    newGame(
        gameConfig(
            modules = listOf(Class.of(VenusNextExpansion)),
            extra = "-WorldGovernmentRule",
            playerNames = listOf("Player1", "Player2"),
        )
    )
    TfmWorkflow.Stepwise(agents).solarPhase()

    game.isIdle() shouldBe true
  }

  @Test
  internal fun `World Government can be selected without Venus`() {
    newGame(
        gameConfig(
            modules = listOf(Class.of(WorldGovernmentRule)),
            playerNames = listOf("Player1", "Player2"),
        )
    )

    TfmWorkflow.Stepwise(agents).solarPhase()
    p1.doTask("TemperatureStep! BY Admin")

    p1.count("TemperatureStep") shouldBe 1
  }

  @Test
  internal fun `first player places a standard-track threshold ocean for World Government`() {
    newGame(
        gameConfig(
            modules = listOf(Class.of(WorldGovernmentRule)),
            playerNames = listOf("Player1", "Player2"),
        )
    )
    admin.runOperation("14 TemperatureStep")

    TfmWorkflow.Stepwise(agents).solarPhase()
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("OceanTile<Tharsis_1_2> BY Admin")

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Tharsis_1_2>") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `first player places an extended-track threshold ocean for World Government`() {
    newGame(
        gameConfig(
            modules = listOf(Class.of(AmazonisMap), Class.of(WorldGovernmentRule)),
            playerNames = listOf("Player1", "Player2"),
        )
    )
    admin.runOperation("14 TemperatureStep")

    TfmWorkflow.Stepwise(agents).solarPhase()
    p1.doTask("TemperatureStep! BY Admin")
    p1.doTask("OceanTile<Amazonis_02_01> BY Admin")

    admin.count("TemperatureStep") shouldBe 15
    admin.count("OceanTile<Amazonis_02_01>") shouldBe 1
    p1.count("TerraformRating") shouldBe 20
  }
}
