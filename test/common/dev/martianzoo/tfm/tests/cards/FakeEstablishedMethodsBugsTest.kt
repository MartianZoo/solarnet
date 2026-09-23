package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.tfm.tests.TestOption.FakeStuffBundle
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeEstablishedMethods
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class FakeEstablishedMethodsBugsTest : CardTest() {
  @Test
  internal fun `Established Methods without its note incorrectly dead-ends when no second project is affordable`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("PreludeCard")

    val deadEnd =
        shouldThrow<NotFullySpecifiedException> {
          p1.playPrelude(FakeEstablishedMethods) {
            p1.runOperation("-20 MC")
            doTask("UseAction<UseStandardProjectAction, Action1>")
            doTask("UseAction<GreeneryProject, Action1>")
            p1.autoExecNow()
          }
        }
    deadEnd.message shouldContain "$FakeEstablishedMethods"
  }

  @Test
  internal fun `Fake Established Methods incorrectly permits Sell Patents`() {
    newGame(PreludeExpansion, FakeStuffBundle)
    p1.phase("Prelude")
    p1.runOperation("2 ProjectCard, PreludeCard")

    p1.playPrelude(FakeEstablishedMethods) {
      repeat(2) {
        doTask("UseAction<UseStandardProjectAction, Action1>")
        doTask("UseAction<SellPatentsProject, Action1>")
        doTask("MC FROM ProjectCard!")
      }
    }

    p1.count("ProjectCard") shouldBe 0
    p1.count("MC") shouldBe 32
  }
}
