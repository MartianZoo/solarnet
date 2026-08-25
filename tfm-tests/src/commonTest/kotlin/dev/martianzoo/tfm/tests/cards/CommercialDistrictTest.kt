package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.ExpressionException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CommercialDistrictTest : CardTest() {
  @Test
  internal fun `Can be placed between two cities`() {
    newGame()
    val p2 = requireP2()

    p1.manual("PROD[Energy], CityTile<Tharsis_3_2>")
    p1.manual("$CommercialDistrict") { placeTile(3, 3) }
    p2.manual("CityTile<Tharsis_3_4>")

    engine.phase("End")
    p1.assertCounts(22 to "VictoryPoint")
    p2.assertCounts(20 to "VictoryPoint")
  }

  @Test
  internal fun `Cannot be placed on a water area`() {
    newGame()
    p1.manual("PROD[Energy]")

    p1.manual("$CommercialDistrict") {
      shouldThrow<NarrowingException> {
        placeTile(1, 2)
      }
      abort()
    }

    p1.count("CommercialDistrict_SpecialTile") shouldBe 0
  }

  @Test
  internal fun `Cannot be placed in a nonadjacent area`() {
    newGame()
    p1.manual("PROD[Energy]")

    p1.manual("$CommercialDistrict") {
      shouldThrow<ExpressionException> {
        doTask("CommercialDistrict_SpecialTile<GanymedeColony_RemoteArea>")
      }
      abort()
    }

    p1.count("CommercialDistrict_SpecialTile") shouldBe 0
  }
}
