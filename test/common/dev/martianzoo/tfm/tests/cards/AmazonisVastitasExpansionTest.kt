package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestOption.Amazonis
import dev.martianzoo.tfm.tests.TestOption.Vastitas
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AmazonisVastitasExpansionTest : CardTest() {
  @Test
  internal fun `Amazonis defaults prefer its Merchant variant and reuse matching goals`() {
    val table = newGame(Amazonis).classTable

    table.isActive(cn("Merchant3")) shouldBe true
    table.isActive(cn("Merchant")) shouldBe false
    table.isActive(cn("Manufacturer")) shouldBe true
    table.isActive(cn("Manufacturer2")) shouldBe false
    table.isActive(cn("Terran")) shouldBe true
    table.isActive(cn("Collector")) shouldBe true
  }

  @Test
  internal fun `Amazonis Merchant needs three of each resource after paying the claim cost`() {
    newGame(Amazonis)
    p1.manual("10 M, 3 S, 3 T, 3 P, 3 E, 3 H")
    engine.phase("Action")

    shouldThrow<RequirementException> {
      p1.stdAction("ClaimMilestone") { doTask("Merchant3") }
    }

    p1.manual("M")
    p1.stdAction("ClaimMilestone") { doTask("Merchant3") }
    p1.count("Merchant3") shouldBe 1
  }

  @Test
  internal fun `Amazonis Manufacturer uses the corrected production metric`() {
    newGame(Amazonis)
    val p2 = requireP2()
    p1.manual("Manufacturer, PROD[3 Steel, 2 Heat]")
    p2.manual("10 Steel, 10 Heat, PROD[2 Steel, 2 Heat]")

    engine.manual("End FROM Phase")

    p1.count("AwardTally<Player1, Manufacturer>") shouldBe 5
    p2.count("AwardTally<Player2, Manufacturer>") shouldBe 4
  }

  @Test
  internal fun `Amazonis card and wild resource bonuses work while delegates are inert`() {
    newGame(Amazonis)

    p1.manual("CityTile<Amazonis_01_04>")
    p1.manual("CityTile<Amazonis_05_03>") { doTask("Titanium") }
    p1.manual("CityTile<Amazonis_02_02>")

    p1.count("ProjectCard") shouldBe 1
    p1.count("Titanium") shouldBe 1
  }

  @Test
  internal fun `Vastitas Geologist counts owned tiles with owned neighbors`() {
    newGame(Vastitas)
    p1.manual("CommercialDistrict_SpecialTile<Vastitas_4_1>, GreeneryTile<Vastitas_3_1>")

    shouldThrow<RequirementException> { p1.manual("Geologist") }

    p1.manual("NaturalPreserve_SpecialTile<Vastitas_4_2>")
    p1.manual("Geologist")
    p1.count("Geologist") shouldBe 1
  }

  @Test
  internal fun `Vastitas Landscaper counts the largest contiguous map group`() {
    val game = newGame(Vastitas)
    game.classTable.isActive(cn("Landscaper")) shouldBe true
    val p2 = requireP2()
    p1.manual(
        "CommercialDistrict_SpecialTile<Vastitas_6_2>, GreeneryTile<Vastitas_6_3>, " +
            "NaturalPreserve_SpecialTile<Vastitas_6_4>, CityTile<Vastitas_1_1>"
    )
    p2.manual("CityTile<Vastitas_8_7>, GreeneryTile<Vastitas_8_6>")

    p1.count("OwnedTile") shouldBe 4
    p2.count("OwnedTile") shouldBe 2
    p1.count("TileInLargestGroup") shouldBe 3
    p2.count("TileInLargestGroup") shouldBe 2

    p1.manual("8 M")
    engine.phase("Action")
    p1.stdAction("FundAward") { doTask("Landscaper") }

    p1.count("Landscaper") shouldBe 1
  }

  @Test
  internal fun `Vastitas defaults reuse its supported printed goals`() {
    val table = newGame(Vastitas).classTable

    table.isActive(cn("Engineer")) shouldBe true
    table.isActive(cn("Geologist")) shouldBe true
    table.isActive(cn("Traveller")) shouldBe true
    table.isActive(cn("Promoter")) shouldBe true
  }

  @Test
  internal fun `Vastitas north pole costs four MC and raises temperature`() {
    newGame(Vastitas)
    p1.manual("4 MC")

    p1.manual("CityTile<Vastitas_5_5>")

    p1.count("MC") shouldBe 0
    p1.temperatureC() shouldBe -28
  }
}
