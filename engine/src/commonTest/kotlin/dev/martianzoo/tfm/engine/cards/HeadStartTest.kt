package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class HeadStartTest : CardTest() {
  @Test
  fun `Head Start grants two mandatory actions`() {
    newGame(PreludeExpansion, PromoCardPack)
    engine.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard")
    p1.playPrelude("HeadStart") {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")

      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")
      doTask("UseAction1<UseStandardProjectSA>")
      doTask("UseAction1<PowerPlantSP>")

      p1.assertCounts(2 to "Megacredit")
      p1.production(cn("Energy")) shouldBe 2
    }
  }
}
