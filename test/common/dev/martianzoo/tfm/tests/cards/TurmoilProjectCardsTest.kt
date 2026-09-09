package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilProjectCardsTest : CardTest() {
  @Test
  internal fun `Banned Delegate returns a non-leader delegate to its owner's reserve`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    p2.manual(
        "PartyDelegate<MarsFirst> FROM ReserveDelegate, " +
            "PartyDelegate<MarsFirst> FROM ReserveDelegate"
    )
    val reserveBefore = p2.count("ReserveDelegate")

    p1.manual("$BannedDelegate") {
      doTask("BannedDelegateRemoval<Player1, MarsFirst, Player2>")
      doTask("ReserveDelegate<Player2> FROM PartyDelegate<MarsFirst, Player2>")
    }

    p2.count("PartyDelegate<MarsFirst>") shouldBe 1
    p2.count("PartyLeader<MarsFirst>") shouldBe 1
    p2.count("ReserveDelegate") shouldBe reserveBefore + 1
  }

  @Test
  internal fun `Event Analysts contributes exactly one additional influence`() {
    newGame(TurmoilExpansion)
    p1.manual("$EventAnalysts")
    p1.manual(
        "PartyDelegate<Greens> FROM ReserveDelegate, " +
            "PartyDelegate<Greens> FROM ReserveDelegate"
    )

    admin.manual("MeasureInfluence<Player1>")

    p1.count("EventAnalystsInfluence") shouldBe 1
    p1.count("Influence") shouldBe 3
  }

  @Test
  internal fun `GMO Contract pays for every matching tag on the played card`() {
    newGame(TurmoilExpansion)

    p1.manual("$GmoContract")
    val moneyAfterContract = p1.count("MC")
    p1.manual("AnimalTag<$GmoContract>, PlantTag<$GmoContract>")

    moneyAfterContract shouldBe 2
    p1.count("MC") shouldBe moneyAfterContract + 4
  }

  @Test
  internal fun `Recruitment exchanges a neutral non-leader for an owned reserve delegate`() {
    newGame(TurmoilExpansion)
    admin.phase("Action")
    p1.manual("2 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(Recruitment, 2) }

    admin.manual("PartyDelegate<MarsFirst, Neutral> FROM ReserveDelegate<Neutral>")
    val playerReserveBefore = p1.count("ReserveDelegate")
    val neutralReserveBefore = admin.count("ReserveDelegate<Neutral>")

    p1.playProject(Recruitment, 2) {
      doTask("RecruitmentExchange<MarsFirst>")
      doTask("ReserveDelegate<Neutral> FROM PartyDelegate<MarsFirst, Neutral>")
    }

    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
    p1.count("ReserveDelegate") shouldBe playerReserveBefore - 1
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
    admin.count("ReserveDelegate<Neutral>") shouldBe neutralReserveBefore + 1
  }

  @Test
  internal fun `Vote of No Confidence replaces the neutral chairman and raises rating`() {
    newGame(TurmoilExpansion)
    p1.manual("PartyDelegate<Greens> FROM ReserveDelegate")
    admin.phase("Action")
    p1.manual("5 MC, ProjectCard")
    val playerReserveBefore = p1.count("ReserveDelegate")
    val neutralReserveBefore = admin.count("ReserveDelegate<Neutral>")
    val ratingBefore = p1.count("TerraformRating")

    p1.playProject(VoteOfNoConfidence, 5)

    admin.count("Chairman<Neutral>") shouldBe 0
    p1.count("Chairman") shouldBe 1
    p1.count("ReserveDelegate") shouldBe playerReserveBefore - 1
    admin.count("ReserveDelegate<Neutral>") shouldBe neutralReserveBefore + 1
    p1.count("TerraformRating") shouldBe ratingBefore + 1
  }
}
