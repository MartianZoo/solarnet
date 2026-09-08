package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

private val rulingBonusProbeDeclarations =
    parseClasses("CLASS RulingBonusProbe : TagHolder { HAS MAX 1 This }").toSet()

internal class TurmoilGovernmentTest :
    CardTest(additionalClassDeclarations = rulingBonusProbeDeclarations) {
  @Test
  internal fun `new government resolves the complete delegate and chairman sequence`() {
    newGame(TurmoilExpansion)
    val p2 = requireP2()
    clearSetupPolitics()
    p1.manual("RulingBonusProbe, 2 BuildingTag<RulingBonusProbe>")
    admin.manual("ReserveDelegate<Neutral> FROM Chairman<Neutral>")
    p2.manual("Chairman FROM ReserveDelegate")
    p1.manual("PartyDelegate<MarsFirst> FROM LobbyDelegate")
    p1.manual("PartyDelegate<MarsFirst> FROM ReserveDelegate")
    p2.manual("PartyDelegate<MarsFirst> FROM LobbyDelegate")
    admin.manual("PartyDelegate<Kelvinists, Neutral> FROM ReserveDelegate<Neutral>")
    admin.manual("PartyDelegate<Reds, Neutral> FROM ReserveDelegate<Neutral>")

    admin.manual("FormGovernment")

    admin.count("Ruling<MarsFirst>") shouldBe 1
    p1.count("MC") shouldBe 2
    p1.count("PartyDelegate<MarsFirst>") shouldBe 0
    p2.count("PartyDelegate<MarsFirst>") shouldBe 0
    admin.count("PartyLeader<MarsFirst>") shouldBe 0
    admin.count("Chairman<Neutral>") shouldBe 0
    p1.count("Chairman") shouldBe 1
    p2.count("Chairman") shouldBe 0
    p1.count("TerraformRating") shouldBe 21
    admin.count("Dominant<Kelvinists>") shouldBe 1
    p1.count("LobbyDelegate") shouldBe 1
    p1.count("ReserveDelegate") shouldBe 5
    p2.count("LobbyDelegate") shouldBe 1
    p2.count("ReserveDelegate") shouldBe 6
    admin.count("ReserveDelegate<Neutral>") shouldBe 12
    admin.count("DominancePriority") shouldBe 0
  }

  @Test
  internal fun `government dominance ties follow clockwise party order`() {
    val clockwise =
        listOf(
            "MarsFirst" to "Kelvinists",
            "Kelvinists" to "Reds",
            "Reds" to "Greens",
            "Greens" to "Unity",
            "Unity" to "Scientists",
            "Scientists" to "MarsFirst",
        )
    val parties = clockwise.map { it.first }

    clockwise.forEach { (former, expected) ->
      newGame(TurmoilExpansion)
      clearSetupPolitics()
      sendNeutralDelegate(former)
      sendNeutralDelegate(former)
      parties.filterNot { it == former }.forEach(::sendNeutralDelegate)

      admin.manual("FormGovernment")

      admin.count("Dominant<$expected>") shouldBe 1
    }
  }

  @Test
  internal fun `four parties pay every player for the matching tag families`() {
    newGame(TurmoilExpansion)
    p1.manual("RulingBonusProbe")
    p1.manual("2 BuildingTag<RulingBonusProbe>, 3 ScienceTag<RulingBonusProbe>")
    p1.manual("2 EarthTag<RulingBonusProbe>, JovianTag<RulingBonusProbe>")
    p1.manual("PlantTag<RulingBonusProbe>, 2 MicrobeTag<RulingBonusProbe>")
    p1.manual("3 AnimalTag<RulingBonusProbe>")

    admin.manual("ApplyRulingBonus<MarsFirst>")
    p1.count("MC") shouldBe 2
    admin.manual("ApplyRulingBonus<Scientists>")
    p1.count("MC") shouldBe 5
    admin.manual("ApplyRulingBonus<Unity>")
    p1.count("MC") shouldBe 8
    admin.manual("ApplyRulingBonus<Greens>")
    p1.count("MC") shouldBe 14
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `kelvinists pay for heat production`() {
    newGame(TurmoilExpansion)
    p1.manual("PROD[3 Heat]")

    admin.manual("ApplyRulingBonus<Kelvinists>")

    p1.count("MC") shouldBe 3
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `reds raise every tied lowest multiplayer rating`() {
    newGame(TurmoilExpansion)

    admin.manual("ApplyRulingBonus<Reds>")

    p1.count("TerraformRating") shouldBe 21
    requireP2().count("TerraformRating") shouldBe 21
  }

  @Test
  internal fun `reds raise only the lowest multiplayer rating`() {
    newGame(TurmoilExpansion)
    p1.manual("2 TerraformRating")

    admin.manual("ApplyRulingBonus<Reds>")

    p1.count("TerraformRating") shouldBe 22
    requireP2().count("TerraformRating") shouldBe 21
  }

  @Test
  internal fun `reds solo bonus applies at twenty but not above twenty`() {
    newGame(TurmoilExpansion, players = 1)
    p1.manual("6 TerraformRating")

    admin.manual("ApplyRulingBonus<Reds>")
    p1.count("TerraformRating") shouldBe 21
    admin.manual("ApplyRulingBonus<Reds>")
    p1.count("TerraformRating") shouldBe 21
  }

  private fun sendNeutralDelegate(party: String) {
    admin.manual("PartyDelegate<$party, Neutral> FROM ReserveDelegate<Neutral>")
  }

  private fun clearSetupPolitics() {
    listOf("MarsFirst", "Reds").forEach { party ->
      admin.manual("ReserveDelegate<Neutral> FROM PartyDelegate<$party, Neutral>")
      admin.manual("-PartyLeader<$party, Neutral>!")
    }
    admin.manual("-Dominant!")
  }
}
