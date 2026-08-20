package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PharmacyUnionTest : CardTest() {
  @Test
  fun `starting money precedes both mandatory microbe-tag losses`() {
    newGame(PromoCardPack)

    p1.manual("$PharmacyUnion").expect("46 Megacredit, ProjectCard, 2 Disease<$PharmacyUnion>")

    p1.assertCounts(0 to "Mandate")
  }

  @Test
  fun `a science tag must remove one disease and raise TR`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")
    val trBefore = p1.count("TerraformRating")

    p1.manual("$PhysicsComplex").expect("-Disease<$PharmacyUnion>, TerraformRating")

    p1.count("TerraformRating") shouldBe trBefore + 1
  }

  @Test
  fun `two science tags can flip Pharmacy Union only once`() {
    newGame(PromoCardPack)
    p1.manual("$PharmacyUnion")
    p1.manual("-2 Disease<$PharmacyUnion>")
    val trBefore = p1.count("TerraformRating")
    val manual = p1.godMode().also { it.autoExecMode = NONE }

    manual.manual("$Research") {
      doTask("PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion THEN 3 TerraformRating")
      doTask("Ok")
      doTask("2 ProjectCard")
    }

    p1.count("TerraformRating") shouldBe trBefore + 3
    p1.assertCounts(0 to "$PharmacyUnion", 1 to "PlayedEvent<Class<$PharmacyUnion>>")
  }
}
