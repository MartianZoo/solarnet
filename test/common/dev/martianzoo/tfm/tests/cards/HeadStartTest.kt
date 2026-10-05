package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HeadStartTest : CardTest() {
  @Test
  internal fun `Head Start grants two mandatory actions`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    admin.phase("Prelude")
    p1.runOperation("4 MC, 10 ProjectCard, PreludeCard")
    p1.turn {
      playPrelude(FakeHeadStart) {
        p1.assertCounts(2 to "Steel", 24 to "MC")

        useStdProject("PowerPlantProject")
        useStdProject("PowerPlantProject")

        p1.assertCounts(2 to "MC")
        p1.production(cn("Energy")) shouldBe 2
      }
    }
  }

  @Test
  internal fun `Head Start must use its first granted action to perform a required action`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.runOperation("10 ProjectCard, PreludeCard")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("DoRequiredActionsAction", payment = {}) {
          p1.playPrelude(MartianIndustries) {
            useStdProject("PowerPlantProject")
          }
        }
      }
    }
  }

  @Test
  internal fun `Fake Head Start allows money and steel after both actions without splitting steel`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    admin.phase("Prelude")
    p1.runOperation("2 ProjectCard")
    p1.autoExecPolicy = CONCRETE

    p1.playPrelude(FakeHeadStart) {
          shouldThrow<TaskException> { doTask("Steel") }
          doTask("UseAction<UseStandardProjectAction, Action1>")
          doTask("UseAction<SellPatentsProject, Action1>")
          doTask("MC FROM ProjectCard")
          doTask("UseAction<UseStandardProjectAction, Action1>")
          doTask("UseAction<SellPatentsProject, Action1>")
          doTask("MC FROM ProjectCard")
          doTask("2 MC / ProjectCard")
        }
        .expect("2 MC, 2 Steel, -2 ProjectCard")
  }
}
