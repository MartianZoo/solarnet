package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.AbstractException
import dev.martianzoo.tfm.tests.TestOption.FakeCardsCardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.FakeEstablishedMethods
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class FakeEstablishedMethodsBugsTest : CardTest() {
  @Test
  internal fun `Established Methods without its note dead-ends when no second project is affordable`() {
    newGame(PreludeExpansion, FakeCardsCardPack)
    p1.phase("Prelude")
    p1.manual("PreludeCard")

    val deadEnd =
        shouldThrow<AbstractException> {
          p1.playPrelude(FakeEstablishedMethods) {
            p1.manual("-20 MC")
            doTask("UseAction<GreenerySP, Action1>")
            p1.autoExecNow()
          }
        }
    deadEnd.message shouldContain "$FakeEstablishedMethods"
  }

  @Test
  internal fun `Nested standard projects preserve pending payment offer positions`() {
    newGame(PreludeExpansion, FakeCardsCardPack)
    p1.manual("PreludeCard")
    admin.phase("Prelude")
    p1.startTurn()

    p1.playPrelude(FakeEstablishedMethods) {
      val offers = standardActionOfferIds()
      offers.size shouldBe 2

      repeat(2) { projectIndex ->
        doTask("UseAction<PowerPlantSP, Action1>")

        tasks
            .extract { it }
            .any { "Pay" in "${it.instruction}" && "MC" in "${it.instruction}" } shouldBe true
        p1.count("Owed<>") shouldBe 11

        p1.pay(11)

        standardActionOfferIds() shouldBe offers.drop(projectIndex + 1)
      }
    }
  }

  private fun standardActionOfferIds() =
      game.tasks
          .extract { it }
          .filter {
            val instruction = it.instruction.toString()
            it.assignee == p1.actor && "UseAction" in instruction && "StandardAction" in instruction
          }
          .map { it.id }
}
