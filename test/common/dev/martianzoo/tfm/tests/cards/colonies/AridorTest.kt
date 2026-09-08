package dev.martianzoo.tfm.tests.cards.colonies

import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Aridor
import dev.martianzoo.tfm.tests.cards.cardnames.BribedCommittee
import dev.martianzoo.tfm.tests.cards.cardnames.CryoSleep
import dev.martianzoo.tfm.tests.cards.cardnames.Decomposers
import dev.martianzoo.tfm.tests.cards.cardnames.DevelopmentCenter
import dev.martianzoo.tfm.tests.cards.cardnames.EarthCatapult
import dev.martianzoo.tfm.tests.cards.cardnames.LunaGovernor
import dev.martianzoo.tfm.tests.cards.cardnames.Mine
import dev.martianzoo.tfm.tests.cards.cardnames.PharmacyUnion
import dev.martianzoo.tfm.tests.cards.cardnames.TitanShuttles
import dev.martianzoo.tfm.tests.cards.cardnames.TitaniumMine
import dev.martianzoo.tfm.tests.cards.cardnames.UrbanDecomposers
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AridorTest : CardTest() {
  @Test
  internal fun `required action adds one selected colony tile`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Aridor).expect("40 MC")
    p1.assertCounts(1 to "RequiredAction")

    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("Europa") }.expect("Europa, ColonyProduction")
    p1.assertCounts(0 to "RequiredAction")
  }

  @Test
  internal fun `delayed selection enters play immediately when its resource card already exists`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Aridor)
    p1.manual("$TitanShuttles")
    p1.manual("Floater<$TitanShuttles>")

    admin.phase("Action")
    p1.stdAction("DoRequiredActionsAction") { doTask("DelayedTitan") }
        .expect("Titan, ColonyProduction")
    admin.assertCounts(1 to "Titan", 0 to "DelayedTitan")
  }

  @Test
  internal fun `production rises once for each new printed non-event tag class`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Aridor)
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
  internal fun `existing tag classes are not rewarded when Aridor enters`() {
    newGame(ColoniesExpansion, PromoCardPack, colonyTiles = testColonyTiles(2))
    p1.manual("$PharmacyUnion, $EarthCatapult")
    val initialProduction = p1.count("PROD[MC]")

    p1.manual("$Aridor")

    p1.count("PROD[MC]") shouldBe initialProduction

    p1.manual("$DevelopmentCenter")

    p1.count("PROD[MC]") shouldBe initialProduction + 2
  }

  @Test
  internal fun `a tag lost with Pharmacy Union can be rewarded again`() {
    newGame(ColoniesExpansion, PromoCardPack, colonyTiles = testColonyTiles(2))
    p1.manual("$Aridor")
    val initialProduction = p1.count("PROD[MC]")
    p1.manual("$PharmacyUnion")
    p1.count("PROD[MC]") shouldBe initialProduction + 1

    p1.manual("-2 Disease<$PharmacyUnion>")
    p1.manual("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
    p1.count("PROD[MC]") shouldBe initialProduction + 1

    p1.manual("$CryoSleep")
    p1.count("PROD[MC]") shouldBe initialProduction + 2

    p1.manual("$Decomposers")

    p1.count("PROD[MC]") shouldBe initialProduction + 3
  }

  @Test
  internal fun `a remaining microbe tag prevents another reward`() {
    newGame(ColoniesExpansion, PromoCardPack, colonyTiles = testColonyTiles(2))
    p1.manual("$Aridor")
    val initialProduction = p1.count("PROD[MC]")
    p1.manual("$PharmacyUnion, $Decomposers")

    p1.manual("-3 Disease<$PharmacyUnion>")
    p1.manual("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")

    p1.count("PROD[MC]") shouldBe initialProduction + 1
    p1.manual("$UrbanDecomposers") { doTask("2 Microbe<$Decomposers>") }
    p1.count("PROD[MC]") shouldBe initialProduction + 1
  }

  @Test
  internal fun `an event with an already unique tag does not reward production`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Aridor)
    p1.manual("$EarthCatapult, ProjectCard")
    val initialProduction = p1.count("PROD[MC]")
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") { doTask("Europa") }
    p1.playProject(BribedCommittee, 5)

    p1.count("PROD[MC]") shouldBe initialProduction
  }

  @Test
  internal fun `two copies of one new tag on a card reward only once`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Aridor)
    val initialProduction = p1.count("PROD[MC]")

    p1.manual("$LunaGovernor")

    // Luna Governor produces two steps itself; its two Earth icons are one new tag class.
    p1.count("PROD[MC]") shouldBe initialProduction + 3
  }

  @Test
  internal fun `Venus is watched only when its tag class is active`() {
    newGame(
        ColoniesExpansion,
        VenusNextExpansion,
        colonyTiles = testColonyTiles(2),
    )
    playCorporationWithoutStartingProjects(p1, Aridor)
    val initialProduction = p1.count("PROD[MC]")

    p1.manual("AerialMappers")

    p1.count("PROD[MC]") shouldBe initialProduction + 1
  }
}
