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
    p1.runOperation("RulingBonusProbe, 2 BuildingTag<RulingBonusProbe>")
    admin.runOperation("-Chairman<Neutral>")
    p2.runOperation("Chairman")
    p1.runOperation("PartyDelegate<MarsFirst>")
    p1.runOperation("PartyDelegate<MarsFirst>")
    p2.runOperation("PartyDelegate<MarsFirst>")
    admin.runOperation("PartyDelegate<Kelvinists, Neutral>")
    admin.runOperation("PartyDelegate<Reds, Neutral>")

    admin.runOperation("FormGovernment")

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
    p1.count("LobbyActionAvailable") shouldBe 1
    p1.count("Delegate") shouldBe 1
    p2.count("LobbyActionAvailable") shouldBe 1
    p2.count("Delegate") shouldBe 0
    admin.count("Delegate<Neutral>") shouldBe 2
  }

  @Test
  internal fun `government dominance ties follow party order`() {
    val partyOrder =
        listOf(
            "MarsFirst" to "Kelvinists",
            "Kelvinists" to "Reds",
            "Reds" to "Greens",
            "Greens" to "Unity",
            "Unity" to "Scientists",
            "Scientists" to "MarsFirst",
        )
    val parties = partyOrder.map { it.first }

    partyOrder.forEach { (former, expected) ->
      newGame(TurmoilExpansion)
      clearSetupPolitics()
      sendNeutralDelegate(former)
      sendNeutralDelegate(former)
      parties.filterNot { it == former }.forEach(::sendNeutralDelegate)

      admin.runOperation("FormGovernment")

      admin.count("Dominant<$expected>") shouldBe 1
    }
  }

  @Test
  internal fun `government dominance skips smaller parties in party order`() {
    newGame(TurmoilExpansion)
    clearSetupPolitics()
    repeat(3) { sendNeutralDelegate("MarsFirst") }
    repeat(2) { sendNeutralDelegate("Unity") }
    repeat(2) { sendNeutralDelegate("Reds") }

    admin.runOperation("FormGovernment")

    admin.count("Dominant<Reds>") shouldBe 1
  }

  @Test
  internal fun `government dominance advances when every party is empty`() {
    newGame(TurmoilExpansion)
    clearSetupPolitics()
    admin.runOperation("Dominant<MarsFirst>")

    admin.runOperation("FormGovernment")

    admin.count("Dominant<Kelvinists>") shouldBe 1
  }

  @Test
  internal fun `four parties pay every player for the matching tag families`() {
    newGame(TurmoilExpansion)
    p1.runOperation("RulingBonusProbe")
    p1.runOperation("2 BuildingTag<RulingBonusProbe>, 3 ScienceTag<RulingBonusProbe>")
    p1.runOperation("2 EarthTag<RulingBonusProbe>, JovianTag<RulingBonusProbe>")
    p1.runOperation("PlantTag<RulingBonusProbe>, 2 MicrobeTag<RulingBonusProbe>")
    p1.runOperation("3 AnimalTag<RulingBonusProbe>")

    admin.runOperation("ApplyRulingBonus<MarsFirst>")
    p1.count("MC") shouldBe 2
    admin.runOperation("ApplyRulingBonus<Scientists>")
    p1.count("MC") shouldBe 5
    admin.runOperation("ApplyRulingBonus<Unity>")
    p1.count("MC") shouldBe 8
    admin.runOperation("ApplyRulingBonus<Greens>")
    p1.count("MC") shouldBe 14
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `kelvinists pay for heat production`() {
    newGame(TurmoilExpansion)
    p1.runOperation("PROD[3 Heat]")

    admin.runOperation("ApplyRulingBonus<Kelvinists>")

    p1.count("MC") shouldBe 3
    requireP2().count("MC") shouldBe 0
  }

  @Test
  internal fun `reds raise every tied lowest multiplayer rating`() {
    newGame(TurmoilExpansion)

    admin.runOperation("ApplyRulingBonus<Reds>")

    p1.count("TerraformRating") shouldBe 21
    requireP2().count("TerraformRating") shouldBe 21
  }

  @Test
  internal fun `reds raise only the lowest multiplayer rating`() {
    newGame(TurmoilExpansion)
    p1.runOperation("2 TerraformRating")

    admin.runOperation("ApplyRulingBonus<Reds>")

    p1.count("TerraformRating") shouldBe 22
    requireP2().count("TerraformRating") shouldBe 21
  }

  @Test
  internal fun `reds solo bonus applies at twenty but not above twenty`() {
    newGame(TurmoilExpansion, players = 1)
    p1.runOperation("6 TerraformRating")

    admin.runOperation("ApplyRulingBonus<Reds>")
    p1.count("TerraformRating") shouldBe 21
    admin.runOperation("ApplyRulingBonus<Reds>")
    p1.count("TerraformRating") shouldBe 21
  }

  private fun sendNeutralDelegate(party: String) {
    admin.runOperation("PartyDelegate<$party, Neutral>")
  }

  private fun clearSetupPolitics() {
    listOf("MarsFirst", "Reds").forEach { party ->
      admin.runOperation("-PartyDelegate<$party, Neutral>")
    }
    admin.runOperation("-Dominant!")
  }
}
