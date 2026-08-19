package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class CommercialDistrictTest : CardTest() {
  @Test
  fun `between two cities, places Commercial District`() {
    newGame()
    val p2 = requireP2()

    p1.manual("PROD[Energy], CityTile<Tharsis_3_2>")
    p1.manual("$CommercialDistrict") { doTask("Card085_SpecialTile<Tharsis_3_3>") }
    p2.manual("CityTile<Tharsis_3_4>")

    engine.phase("End")
    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }

  @Test
  fun `cannot place Commercial District on a water area`() {
    newGame()
    p1.manual("PROD[Energy]")

    p1.manual("$CommercialDistrict") {
      shouldThrow<NarrowingException> {
        doTask("Card085_SpecialTile<Tharsis_1_2>")
      }
      abort()
    }

    p1.count("Card085_SpecialTile") shouldBe 0
  }

  @Test
  fun `cannot place Commercial District on a remote area`() {
    newGame()
    p1.manual("PROD[Energy]")

    p1.manual("$CommercialDistrict") {
      shouldThrow<ExpressionException> {
        doTask("Card085_SpecialTile<Card081_RemoteArea>")
      }
      abort()
    }

    p1.count("Card085_SpecialTile") shouldBe 0
  }
}
