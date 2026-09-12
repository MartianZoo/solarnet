package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilProjectCardsTest : CardTest() {
  @Test
  internal fun `Cultural Metropolis cannot be played with only one delegate available`() {
    newGame(TurmoilExpansion)
    repeat(6) { p1.runOperation("PartyDelegate<MarsFirst>") }
    admin.runOperation("Ruling<Unity> FROM Ruling<Greens>")
    p1.runOperation("PROD[Energy], 20 MC, ProjectCard")
    admin.phase("Action")

    shouldThrow<DeadEndException> {
      p1.playProject(CulturalMetropolis, 20) {
        placeTile(4, 2)
        doTask("2 PartyDelegate<Scientists>")
      }
    }

    p1.count("PartyDelegate OR Chairman") shouldBe 6
    p1.count("CityTile") shouldBe 0
  }

  @Test
  internal fun `Cultural Metropolis places two of the seven delegates`() {
    newGame(TurmoilExpansion)
    repeat(5) { p1.runOperation("PartyDelegate<MarsFirst>") }
    admin.runOperation("Ruling<Unity> FROM Ruling<Greens>")
    p1.runOperation("PROD[Energy], 20 MC, ProjectCard")
    admin.phase("Action")

    p1.playProject(CulturalMetropolis, 20) {
      placeTile(4, 2)
      doTask("2 PartyDelegate<Scientists>")
    }

    p1.count("PartyDelegate OR Chairman") shouldBe 7
    p1.count("LobbyActionAvailable") shouldBe 0
    p1.count("PartyDelegate") shouldBe 7
  }

  @Test
  internal fun `Event Analysts contributes exactly one additional influence`() {
    newGame(TurmoilExpansion)
    p1.runOperation("$EventAnalysts")
    p1.runOperation("PartyDelegate<Greens>, PartyDelegate<Greens>")

    admin.runOperation("MeasureInfluence<Player1>")

    p1.count("EventAnalystsInfluence") shouldBe 1
    p1.count("Influence") shouldBe 3
  }

  @Test
  internal fun `GMO Contract pays for every matching tag on the played card`() {
    newGame(TurmoilExpansion)

    p1.runOperation("$GmoContract")
    val moneyAfterContract = p1.count("MC")
    p1.runOperation("AnimalTag<$GmoContract>, PlantTag<$GmoContract>")

    moneyAfterContract shouldBe 2
    p1.count("MC") shouldBe moneyAfterContract + 4
  }

  @Test
  internal fun `Recruitment exchanges a neutral non-leader for an available owned delegate`() {
    newGame(TurmoilExpansion)
    admin.phase("Action")
    p1.runOperation("2 MC, ProjectCard")

    shouldThrow<RequirementException> { p1.playProject(Recruitment, 2) }

    admin.runOperation(
        "PartyDelegate<MarsFirst, Neutral>, " +
            "PartyDelegate<Unity, Neutral>, PartyDelegate<Unity, Neutral>"
    )
    admin.count("Dominant<MarsFirst>") shouldBe 1
    val playerDelegatesBefore = p1.count("PartyDelegate OR Chairman")
    val neutralDelegatesBefore = admin.count("PartyDelegate<Neutral> OR Chairman<Neutral>")

    p1.playProject(Recruitment, 2) {
      doTask("RecruitmentExchange<MarsFirst>")
      doTask("-PartyDelegate<MarsFirst, Neutral>")
    }

    p1.count("PartyDelegate<MarsFirst>") shouldBe 1
    p1.count("PartyDelegate OR Chairman") shouldBe playerDelegatesBefore + 1
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyLeader<MarsFirst, Neutral>") shouldBe 1
    admin.count("PartyDelegate<Neutral> OR Chairman<Neutral>") shouldBe neutralDelegatesBefore - 1
    admin.count("Dominant<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `Recruitment cannot be played without an available owned delegate`() {
    newGame(TurmoilExpansion)
    admin.runOperation("PartyDelegate<MarsFirst, Neutral>")
    repeat(7) { p1.runOperation("PartyDelegate<Unity>") }
    admin.phase("Action")
    p1.runOperation("2 MC, ProjectCard")

    shouldThrow<DeadEndException> {
      p1.playProject(Recruitment, 2) {
        doTask("RecruitmentExchange<MarsFirst>")
        doTask("-PartyDelegate<MarsFirst, Neutral>")
      }
    }

    p1.count("PartyDelegate OR Chairman") shouldBe 7
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 2
  }

  @Test
  internal fun `Martian Media Center action requires and places an available owned delegate`() {
    newGame(TurmoilExpansion)
    p1.runOperation("$MartianMediaCenter, 3 MC")
    admin.phase("Action")

    p1.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }

    p1.count("PartyDelegate<Greens>") shouldBe 1
    p1.count("PartyDelegate OR Chairman") shouldBe 1
    p1.count("MC") shouldBe 0
  }

  @Test
  internal fun `Martian Media Center action cannot be used without an available delegate`() {
    newGame(TurmoilExpansion)
    p1.runOperation("$MartianMediaCenter, 3 MC")
    repeat(7) { p1.runOperation("PartyDelegate<Unity>") }
    admin.phase("Action")

    shouldThrow<DeadEndException> {
      p1.cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
    }

    p1.count("MC") shouldBe 3
    p1.count("PartyDelegate<Greens>") shouldBe 0
  }

  @Test
  internal fun `Vote of No Confidence can appoint the final available delegate as chairman`() {
    newGame(TurmoilExpansion)
    repeat(6) { p1.runOperation("PartyDelegate<Greens>") }
    admin.phase("Action")
    p1.runOperation("5 MC, ProjectCard")
    val neutralDelegatesBefore = admin.count("PartyDelegate<Neutral> OR Chairman<Neutral>")
    val ratingBefore = p1.count("TerraformRating")

    p1.playProject(VoteOfNoConfidence, 5)

    admin.count("Chairman<Neutral>") shouldBe 0
    p1.count("Chairman") shouldBe 1
    p1.count("PartyDelegate OR Chairman") shouldBe 7
    p1.count("LobbyActionAvailable") shouldBe 0
    admin.count("PartyDelegate<Neutral> OR Chairman<Neutral>") shouldBe neutralDelegatesBefore - 1
    p1.count("TerraformRating") shouldBe ratingBefore + 1
  }

  @Test
  internal fun `Vote of No Confidence cannot be played without an available delegate`() {
    newGame(TurmoilExpansion)
    repeat(7) { p1.runOperation("PartyDelegate<Greens>") }
    admin.phase("Action")
    p1.runOperation("5 MC, ProjectCard")

    shouldThrow<DeadEndException> { p1.playProject(VoteOfNoConfidence, 5) }

    admin.count("Chairman<Neutral>") shouldBe 1
    p1.count("Chairman") shouldBe 0
  }
}
