package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HeadStartTest : CardTest() {
  @Test
  internal fun `Head Start grants two mandatory actions`() {
    newGame(PreludeExpansion, PromoCardPack)
    engine.phase("Prelude")
    p1.manual("4, 10 ProjectCard, PreludeCard")
    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "Megacredit")

      doTask("UseAction<PowerPlantSP, First>")
      doTask("11 Pay<Class<Megacredit>> FROM Megacredit")
      doTask("UseAction<PowerPlantSP, First>")
      doTask("11 Pay<Class<Megacredit>> FROM Megacredit")

      p1.assertCounts(2 to "Megacredit")
      p1.production(cn("Energy")) shouldBe 2
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to resolve a mandate`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(ValleyTrust, 5)
    engine.phase("Prelude")
    p1.manual("10 ProjectCard, PreludeCard")

    p1.playPrelude(HeadStart) {
      doTask("UseAction<HandleMandates, First>")
      doTask("PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>")
      doTask("UseAction<PowerPlantSP, First>")
      doTask("11 Pay<Class<Megacredit>> FROM Megacredit")
    }
  }
}
