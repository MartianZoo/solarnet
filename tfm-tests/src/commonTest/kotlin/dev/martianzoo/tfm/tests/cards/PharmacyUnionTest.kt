package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PharmacyUnionTest : CardTest() {
  @Test
  internal fun `Starting money precedes both mandatory microbe-tag losses`() {
    newGame(PromoCardPack)

    p1.manual("$PharmacyUnion").expect("46 Megacredit, ProjectCard, 2 Disease<$PharmacyUnion>")

    p1.assertCounts(0 to "Mandate")
  }

  @Test
  internal fun `A science tag must remove one disease and raise TR`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")

    p1.manual("$PhysicsComplex").expect("-Disease<$PharmacyUnion>, TerraformRating")
  }

  @Test
  internal fun `Two science tags with one disease remove it and then flip Pharmacy Union`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")
    p1.manual("-Disease<$PharmacyUnion>")
    val trBefore = p1.count("TerraformRating")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.manual("$Research") {
      doTask("TerraformRating FROM Disease<$PharmacyUnion>")
      doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
      doTask("3 TerraformRating")
      doTask("2 ProjectCard")
    }

    p1.count("TerraformRating") shouldBe trBefore + 4
    p1.assertCounts(
        0 to "Disease<$PharmacyUnion>",
        0 to "$PharmacyUnion",
        1 to "PlayedEvent<Class<$PharmacyUnion>>",
    )
  }

  @Test
  internal fun `Two science tags can flip Pharmacy Union only once`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")
    p1.manual("-2 Disease<$PharmacyUnion>")
    val trBefore = p1.count("TerraformRating")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.manual("$Research") {
      doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
      doTask("3 TerraformRating")
      // Decline the second science tag's attempt to flip Pharmacy Union again.
      declineTask()
      doTask("2 ProjectCard")
    }

    p1.count("TerraformRating") shouldBe trBefore + 3
    p1.assertCounts(0 to "$PharmacyUnion", 1 to "PlayedEvent<Class<$PharmacyUnion>>")
  }

  // FAQ: a microbe trigger that was already pending when Pharmacy Union flips still loses 4 M€,
  // but places no disease because the corporation is no longer in play.
  @Test
  internal fun `Pending disease placement becomes its explicit fallback after Pharmacy Union flips`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")
    p1.manual("-2 Disease<$PharmacyUnion>")
    val moneyBefore = p1.count("Megacredit")
    val trBefore = p1.count("TerraformRating")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.manual("$RegolithEaters") {
      doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion")
      doTask("3 TerraformRating")
      doTask("-4 Megacredit")
      // Decline placing disease after Pharmacy Union has left play.
      declineTask()
    }

    p1.count("Megacredit") shouldBe moneyBefore - 4
    p1.count("TerraformRating") shouldBe trBefore + 3
    p1.assertCounts(
        0 to "Disease<$PharmacyUnion>",
        0 to "$PharmacyUnion",
        1 to "PlayedEvent<Class<$PharmacyUnion>>",
    )
  }
}
