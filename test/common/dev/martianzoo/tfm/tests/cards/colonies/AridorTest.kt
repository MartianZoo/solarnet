package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Aridor
import dev.martianzoo.tfm.tests.cards.cardnames.BribedCommittee
import dev.martianzoo.tfm.tests.cards.cardnames.DevelopmentCenter
import dev.martianzoo.tfm.tests.cards.cardnames.EarthCatapult
import dev.martianzoo.tfm.tests.cards.cardnames.LunaGovernor
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchCoordination
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import dev.martianzoo.tfm.tests.cards.cardnames.TitaniumMine
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AridorTest : CardTest() {
  @Test
  internal fun `required action adds one selected colony tile`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Aridor, 0).expect("40 MC")
    p1.assertCounts(1 to "RequiredAction")

    engine.phase("Action")
    p1.stdAction("DoRequiredActions") { doTask("Europa") }.expect("Europa, ColonyProduction")
    p1.assertCounts(0 to "RequiredAction")
  }

  @Test
  internal fun `delayed selection enters play immediately when its resource card already exists`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Aridor, 0)
    p1.manual("$TitanShuttles")
    p1.manual("Floater<$TitanShuttles>")

    engine.phase("Action")
    p1.stdAction("DoRequiredActions") { doTask("DelayedTitan") }.expect("Titan, ColonyProduction")
    engine.assertCounts(1 to "Titan", 0 to "DelayedTitan")
  }

  @Test
  internal fun `production rises once for each new printed non-event tag class`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Aridor, 0)
    val initialProduction = p1.count("PROD[MC]")

    requireP2().manual("$Mine")
    p1.count("PROD[MC]") shouldBe initialProduction

    p1.manual("$EarthCatapult")
    p1.count("PROD[MC]") shouldBe initialProduction + 1

    p1.manual("$DevelopmentCenter")
    p1.count("PROD[MC]") shouldBe initialProduction + 3

    p1.manual("$TitaniumMine")
    p1.count("PROD[MC]") shouldBe initialProduction + 3
  }

  @Test
  internal fun `an event with an already unique tag does not reward production`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Aridor, 0)
    p1.manual("$EarthCatapult, ProjectCard")
    val initialProduction = p1.count("PROD[MC]")
    engine.phase("Action")

    p1.stdAction("DoRequiredActions") { doTask("Europa") }
    p1.playProject(BribedCommittee, 5)

    p1.count("PROD[MC]") shouldBe initialProduction
  }

  @Test
  internal fun `two copies of one new tag on a card reward only once`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    p1.playCorp(Aridor, 0)
    val initialProduction = p1.count("PROD[MC]")

    p1.manual("$LunaGovernor")

    // Luna Governor produces two steps itself; its two Earth icons are one new tag class.
    p1.count("PROD[MC]") shouldBe initialProduction + 3
  }

  @Test
  internal fun `an action scoped wild tag is not a new printed tag`() {
    newGame(
        ColoniesExpansion,
        PreludeExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.playCorp(Aridor, 0)
    p1.manual("$ResearchCoordination, ProjectCard")
    val initialProduction = p1.count("PROD[MC]")
    engine.phase("Action")
    p1.stdAction("DoRequiredActions") { doTask("Europa") }

    p1.stdAction(
        "SellPatentsSP",
        beforeAction = {
          doTask("ScienceTag<WildTagUse<$ResearchCoordination>>")
          p1.count("PROD[MC]") shouldBe initialProduction
        },
    ) {
      doTask("1 MC FROM ProjectCard<Hand>!")
    }

    p1.count("PROD[MC]") shouldBe initialProduction
  }

  @Test
  internal fun `Venus is watched only when its tag class is active`() {
    newGame(
        ColoniesExpansion,
        VenusNextExpansion,
        colonyTiles = testColonyTiles(2),
    )
    p1.playCorp(Aridor, 0)
    val initialProduction = p1.count("PROD[MC]")

    p1.manual("AerialMappers")

    p1.count("PROD[MC]") shouldBe initialProduction + 1
  }
}
