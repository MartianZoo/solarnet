package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TestOption.TurmoilExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilPoliciesTest : CardTest() {
  @Test
  internal fun `greens policy exists only in the action phase and rewards greenery`() {
    newGame(TurmoilExpansion)

    admin.phase("Action")
    admin.count("Policy") shouldBe 1
    admin.count("GreensPolicy") shouldBe 1
    p1.manual("GreeneryTile<Tharsis_3_3>")
    p1.count("MC") shouldBe 4

    admin.phase("Production")
    admin.count("Policy") shouldBe 0
  }

  @Test
  internal fun `mars first policy rewards a tile placed on Mars`() {
    newGame(TurmoilExpansion)
    admin.manual("Ruling<MarsFirst> FROM Ruling")
    admin.phase("Action")

    p1.manual("CityTile<Tharsis_3_3>")

    admin.count("MarsFirstPolicy") shouldBe 1
    p1.count("Steel") shouldBe 1
  }

  @Test
  internal fun `scientists action draws three cards only once per generation`() {
    newGame(TurmoilExpansion)
    admin.manual("Ruling<Scientists> FROM Ruling")
    p1.manual("20 MC")
    admin.phase("Action")

    p1.turn {
      stdAction("ScientistsPolicy")
      shouldThrow<NotNowException> { stdAction("ScientistsPolicy") }
    }

    p1.count("ProjectCard") shouldBe 3
    p1.count("ScientistsUsedMarker") shouldBe 1
    p1.count("MC") shouldBe 10
  }

  @Test
  internal fun `unity policy adds one titanium payment value for every player`() {
    newGame(TurmoilExpansion)
    admin.manual("Ruling<Unity> FROM Ruling")

    admin.phase("Action")

    admin.count("UnityPolicy") shouldBe 1
    p1.count("UnityTitaniumValue") shouldBe 1
    requireP2().count("UnityTitaniumValue") shouldBe 1
    p1.count("ResourceValue<Class<Titanium>>") shouldBe 4
  }

  @Test
  internal fun `reds policy charges three mc for each player-attributed tr step`() {
    newGame(TurmoilExpansion)
    admin.manual("Ruling<Reds> FROM Ruling")
    p1.manual("9 MC")
    admin.phase("Action")

    p1.manual("2 TerraformRating")
    p1.count("MC") shouldBe 3
    p1.count("TerraformRating") shouldBe 22
    p1.manual("TerraformRating")
    p1.count("MC") shouldBe 0
    p1.count("TerraformRating") shouldBe 23
    p1.manual("2 MC")
    shouldThrow<NotNowException> { p1.manual("TerraformRating") }
    p1.count("MC") shouldBe 2
    p1.count("TerraformRating") shouldBe 23
  }

  @Test
  internal fun `kelvinists action raises heat and energy production for ten mc`() {
    newGame(TurmoilExpansion)
    admin.manual("Ruling<Kelvinists> FROM Ruling")
    p1.manual("10 MC")
    admin.phase("Action")

    p1.stdAction("KelvinistsPolicy")

    p1.count("MC") shouldBe 0
    p1.count("PROD[Heat]") shouldBe 1
    p1.count("PROD[Energy]") shouldBe 1
  }
}
