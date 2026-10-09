package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class MergerTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(
        VenusNextExpansion,
        PreludeExpansion,
        PromoCardPack,
    )
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.playPrelude(UnmiContractor)
    p1.playPrelude(Merger) {
      p1.playCorp(Celestic)
    }
  }

  @Test
  internal fun `Can choose Celestic after Valley Trust`() {
    p1.assertCounts(0 to "PreludeCard", 6 to "ProjectCard")
  }

  @Test
  internal fun `Resolves both corporations' starting benefits`() {
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") {
      p1.assertCounts(8 to "ProjectCard", 0 to "PreludeCard")
      p1.assertProds(
          0 to "MC",
          0 to "Steel",
          0 to "Titanium",
          0 to "Plant",
          0 to "Energy",
          0 to "Heat",
      )

      p1.playPrelude(SocietySupport)
      p1.assertProds(
          -1 to "MC",
          0 to "Steel",
          0 to "Titanium",
          1 to "Plant",
          1 to "Energy",
          1 to "Heat",
      )
    }
  }

  @Test
  internal fun `Can resolve Merger payment and the second corporation`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    val result =
        p1.playPrelude(Merger) {
          p1.playCorp(Celestic)
        }

    p1.assertCounts(1 to "$Celestic")
    result.changes
        .filter { it.change.gaining?.type == p1.resolve("CorporationCard<Selecting>") }
        .sumOf { it.change.count } shouldBe 4
    p1.assertCounts(0 to "CorporationCard<Selecting>")
  }

  @Test
  internal fun `New Partner can play Merger while both card families are being selected`() {
    newGame(VenusNextExpansion, PreludeExpansion, PromoCardPack)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")

    p1.playPrelude(NewPartner) {
      p1.playPrelude(Merger) {
        p1.playCorp(Celestic)
      }
    }

    p1.assertCounts(
        1 to "$Merger",
        1 to "$Celestic",
    )
  }

  @Test
  internal fun `Polyphemos then Merger into TerraLabs still buys cards for three`() {
    newGame(
        ColoniesExpansion,
        TurmoilExpansion,
        PreludeExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(2),
    )
    playCorporationWithoutStartingProjects(p1, Polyphemos)
    admin.phase("Prelude")
    p1.playPrelude(Merger) {
      p1.playCorp(TerraLabsResearch)
    }

    p1.runOperation("ProjectCard<Selecting> THEN BuySelectedCards") { p1.pay(3) }
        .expect("ProjectCard, -3 MC")
  }

  @Test
  internal fun `New corporation cash alone can fund the Merger payment`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(PhoboLog, 0)
    p1.runOperation("-${p1.count("MC")} MC")
    admin.phase("Prelude")
    p1.count("MC") shouldBe 0

    p1.playPrelude(Merger) { p1.playCorp(PharmacyUnion) }.expect("4 MC, 2 Disease<$PharmacyUnion>")
    p1.assertCounts(1 to "$PharmacyUnion", 4 to "MC")
  }

  @Test
  internal fun `Merger makes Pharmacy Union starting money available for its diseases`() {
    newGame(PreludeExpansion, PromoCardPack, CorporateEraExpansion)
    p1.playCorp(SaturnSystems, 0)
    admin.phase("Prelude")

    p1.playPrelude(Merger) { p1.playCorp(PharmacyUnion) }.expect("4 MC, 2 Disease<$PharmacyUnion>")
  }

  @Test
  internal fun `Pharmacy Union loss makes Board Merger Recyclon unaffordable and rolls back`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(PharmacyUnion, 0) {
      doTask("Disease<$PharmacyUnion>")
      doTask("Disease<$PharmacyUnion>")
    }
    admin.phase("Prelude")
    p1.playPrelude(BoardOfDirectors)
    p1.runOperation("-${p1.count("MC") - 17} MC")
    admin.phase("Action")
    p1.count("MC") shouldBe 17

    val previousPolicy = p1.autoExecPolicy
    try {
      shouldThrow<LimitsException> {
        p1.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          p1.playPrelude(Merger) {
            p1.autoExecPolicy = NONE
            p1.playCorp(Recyclon) {
              doTask("Owed<> / $Recyclon.cost")
              doTask("CardBilling")
              doTask("$Recyclon FROM CorporationCard<Selecting>")
              doTask("38 MC")
              // Choose the disease loss before Merger's payment; both are queued.
              doTask("-4 MC.")
              doTask("-42 MC")
            }
          }
        }
      }
    } finally {
      p1.autoExecPolicy = previousPolicy
    }
    p1.count("MC") shouldBe 17
    p1.count("Disease<$PharmacyUnion>") shouldBe 2
    p1.count("Director<$BoardOfDirectors>") shouldBe 4
    p1.count("$Recyclon") shouldBe 0
    p1.count("$Merger") shouldBe 0
  }
}
