package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HeadStartTest : CardTest() {
  @Test
  internal fun `Head Start grants two mandatory actions`() {
    newGame(PreludeExpansion, PromoCardPack)
    engine.phase("Prelude")
    p1.manual("4 MC, 10 ProjectCard, PreludeCard")
    p1.playPrelude(HeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")

      doTask("UseAction<PowerPlantSP, Action1>")
      doTask("11 Pay<Class<MC>> FROM MC")
      doTask("UseAction<PowerPlantSP, Action1>")
      doTask("11 Pay<Class<MC>> FROM MC")

      p1.assertCounts(2 to "MC")
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
      doTask("UseAction<HandleMandates, Action1>")
      doTask("PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>")
      doTask("UseAction<PowerPlantSP, Action1>")
      doTask("11 Pay<Class<MC>> FROM MC")
    }
  }
}
