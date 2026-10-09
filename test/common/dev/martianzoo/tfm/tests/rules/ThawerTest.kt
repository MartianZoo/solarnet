package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.WorldGovernmentAdvisor
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ThawerTest : CardTest() {
  @Test
  internal fun `Thawer credits each player step but not other players or Admin`() {
    newGame(GameConfig("Thawer, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC, 4 TemperatureStep")
    requireP2().runOperation("TemperatureStep")
    admin.runOperation("TemperatureStep")
    admin.phase("Action")
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Thawer")) }
    p1.runOperation("TemperatureStep")
    p1.claimMilestone(cn("Thawer")).expect("-8 MC, Thawer")
  }

  @Test
  internal fun `Snow Cover does not undo a player's previous temperature increases`() {
    newGame(GameConfig("TurmoilExpansion, Thawer, Builder, Engineer", "Player1", "Player2"))
    p1.runOperation("8 MC, 5 TemperatureStep")

    admin.runOperation("SnowCover")
    admin.runOperation("ResolveGlobalEvent<Class<SnowCover>>")
    admin.count("TemperatureStep") shouldBe 3
    admin.phase("Action")

    p1.claimMilestone(cn("Thawer")).expect("-8 MC, Thawer")
  }

  @Test
  internal fun `World Government Advisor does not give its owner Thawer credit`() {
    newGame(
        GameConfig(
            "PreludeExpansion, Prelude2CardPack, Thawer, Builder, Engineer",
            "Player1",
            "Player2",
        )
    )
    p1.runOperation("8 MC, 4 TemperatureStep, $WorldGovernmentAdvisor")
    admin.phase("Action")

    p1.cardAction1(WorldGovernmentAdvisor) { doTask("TemperatureStep BY Admin") }

    shouldThrow<RequirementException> { p1.claimMilestone(cn("Thawer")) }
  }
}
