package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.CorporateEraExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior in Terraforming Mars rules. */
internal class BugsTest : CardTest() {
  // BGG two-player award rules discussion:
  // https://boardgamegeek.com/thread/1762387/article/25616311#25616311
  @Test
  internal fun `SecondPlace incorrectly remains active with only two players`() {
    val twoPlayers = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2")))
    val threePlayers =
        Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2", "Player3")))

    twoPlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
    threePlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
  }

  // BGG exact question (no written designer answer):
  // https://boardgamegeek.com/thread/3512556/article/46088087#46088087
  @Test
  internal fun `Landshaper incorrectly counts Capital as two tiles`() {
    setupLandshaperCapital()
    p1.stdProject("GreeneryProject") { placeTile(5, 2) }

    p1.claimMilestone(cn("Landshaper")).expect("Landshaper")
    p1.count("OwnedTile") shouldBe 2
  }

  // BGG exact question (no written designer answer):
  // https://boardgamegeek.com/thread/3512556/article/46088087#46088087
  @Test
  internal fun `Landshaper incorrectly counts Capital twice even with two greeneries`() {
    setupLandshaperCapital()
    p1.runOperation("20 MC")
    p1.stdProject("GreeneryProject") { placeTile(5, 2) }
    p1.stdProject("GreeneryProject") { placeTile(6, 1) }

    p1.claimMilestone(cn("Landshaper")).expect("Landshaper")
    p1.count("OwnedTile") shouldBe 3
  }

  // BGG exact zero-reveal question (unsettled):
  // https://boardgamegeek.com/thread/3556036/article/46461394#46461394
  @Test
  internal fun `Public Plans incorrectly accepts zero other revealed cards`() {
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")

    p1.playProject(PublicPlans, 7)

    p1.assertCounts(0 to "MC", 0 to "ProjectCard", 1 to "PlayedEvent<Class<$PublicPlans>>")
  }

  private fun setupLandshaperCapital() {
    newGame(Amazonis, PreludeExpansion, CorporateEraExpansion)
    p1.playCorp(CrediCor, 1)
    requireP2().runOperation("72 MC")
    admin.phase("Prelude")
    p1.playPrelude(PowerGeneration)
    p1.playPrelude(Donation)
    admin.phase("Action")
    requireP2().stdProject("AquiferProject") { placeTile(2, 1) }
    requireP2().stdProject("AquiferProject") { placeTile(2, 6) }
    requireP2().stdProject("AquiferProject") { placeTile(3, 1) }
    requireP2().stdProject("AquiferProject") { placeTile(3, 6) }
    p1.playProject(Capital, 26) { placeTile(5, 1) }
    shouldThrow<RequirementException> { p1.claimMilestone(cn("Landshaper")) }
  }
}
