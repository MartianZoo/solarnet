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
}
