package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MilestonesAwardsExpansionTest : CardTest() {
  @Test
  internal fun `Geologist counts owned tiles with owned neighbors`() {
    newGame(GameConfig("Geologist", "Player1", "Player2"))
    p1.manual("CommercialDistrict_SpecialTile<Tharsis_2_2>, GreeneryTile<Tharsis_2_1>")

    shouldThrow<RequirementException> { p1.manual("Geologist") }

    p1.manual("NaturalPreserve_SpecialTile<Tharsis_2_3>")
    p1.manual("Geologist")
    p1.count("Geologist") shouldBe 1
  }

  @Test
  internal fun `Landscaper counts the largest contiguous map group and ignores remote tiles`() {
    val game = newGame(GameConfig("Landscaper", "Player1", "Player2"))
    game.classTable.isActive(cn("Landscaper")) shouldBe true
    val p2 = requireP2()
    p1.manual(
        "CommercialDistrict_SpecialTile<Tharsis_2_2>, GreeneryTile<Tharsis_2_1>, NaturalPreserve_SpecialTile<Tharsis_2_3>, " +
            "CityTile<Tharsis_4_6>, CityTile<GanymedeColony_RemoteArea>"
    )
    p2.manual("CityTile<Tharsis_8_7>, GreeneryTile<Tharsis_8_6>")

    p1.count("OwnedTile") shouldBe 5
    p2.count("OwnedTile") shouldBe 2
    p1.count("OwnedTile<Tharsis_2_2>") shouldBe 1
    p1.count("TileInLargestGroup") shouldBe 3
    p2.count("TileInLargestGroup") shouldBe 2

    p1.manual("8 M")
    engine.phase("Action")
    p1.stdAction("FundAwardSA")

    p1.count("Landscaper") shouldBe 1
    p1.count("OwnedTile") shouldBe 5
  }

  @Test
  internal fun `Merchant checks resources after the normal claim cost`() {
    val game = newGame(GameConfig("Merchant", "Player1", "Player2"))
    game.classTable.isActive(cn("Merchant")) shouldBe true
    p1.manual("10 M, 2 S, 2 T, 2 P, 2 E, 2 H")
    engine.phase("Action")

    p1.stdAction("ClaimMilestoneSA")

    p1.count("Merchant") shouldBe 1
  }

  @Test
  internal fun `Producer22 requires combined production of twenty two`() {
    newGame(GameConfig("Producer22, -CorporateEraExpansion", "Player1", "Player2"))
    p1.manual("8 M")
    engine.phase("Action")

    shouldThrow<RequirementException> { p1.manual("Producer22") }

    p1.manual("PROD[6 Megacredit, Steel, Titanium, Plant, Energy, Heat]")
    p1.count("PROD[StandardResource]") shouldBe 22
    p1.stdAction("ClaimMilestoneSA")
    p1.count("Milestone") shouldBe 1
  }

  @Test
  internal fun `Producer versions belong to opposite Quick Start modes`() {
    shouldThrow<LimitsException> {
      newGame(GameConfig("Producer, -CorporateEraExpansion", "Player1", "Player2"))
    }
    shouldThrow<IllegalArgumentException> {
      newGame(GameConfig("Producer22", "Player1", "Player2"))
    }
  }
}
