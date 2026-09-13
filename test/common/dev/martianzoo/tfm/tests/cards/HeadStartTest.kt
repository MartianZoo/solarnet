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
    newGame(PreludeExpansion, FakeStuffBundle)
    admin.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard")
    p1.playPrelude(FakeHeadStart) {
      p1.assertCounts(2 to "Steel", 24 to "MC")

      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("11 Pay<Class<MC>> FROM MC")
      doTask("UseAction<UseStandardProjectAction, Action1>")
      doTask("UseAction<PowerPlantProject, Action1>")
      doTask("11 Pay<Class<MC>> FROM MC")

      p1.assertCounts(2 to "MC")
      p1.production(cn("Energy")) shouldBe 2
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newGame(PreludeExpansion, FakeStuffBundle, retainedStartingProjects = 5)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.runOperation("10 ProjectCard, PreludeCard")

    p1.playPrelude(FakeHeadStart) {
      doTask("UseAction<DoRequiredActionsAction, Action1>")
      p1.playPrelude(MartianIndustries) {
        doTask("UseAction<UseStandardProjectAction, Action1>")
        doTask("UseAction<PowerPlantProject, Action1>")
        doTask("11 Pay<Class<MC>> FROM MC")
      }
    }
  }
}
