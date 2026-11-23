package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class StandardActionTest : TfmSandboxTest() {
  @Test
  internal fun `the standard card action offers the card play and then payment`() {
    newTestGame()
    kim.setToExMachina(20, "MC")
    kim.exMachina("ProjectCard<Selecting>, ProjectCard<Revealed>")

    kim.inTurn {
      doTask("UseAction<PlayCardAction, Action1>")
      shouldThrow<TaskException> {
        doTask("PlayCard<Class<ProjectCard>, Class<$Mine>, Selecting>")
      }
      shouldThrow<TaskException> {
        doTask("PlayCard<Class<ProjectCard>, Class<$Mine>, Revealed>")
      }
      kim.count("MC") shouldBe 20
      doTask("PlayCard<Class<ProjectCard>, Class<$Mine>, Hand>")
      kim.count("$Mine") shouldBe 0
      val productionBefore = kim.count("PROD[Steel]")
      val paymentStart = game.events.nextOrdinal
      doTask("4 Pay<> FROM MC")
      kim.count("$Mine") shouldBe 1
      kim.count("PROD[Steel]") shouldBe productionBefore
      game.events
          .changesSince(paymentStart)
          .single {
            it.change.gaining?.expression?.className == Mine
          }
          .actor shouldBe kim.actor
      doTask("PROD[Steel]")
    }
    game.isIdle() shouldBe true
  }
}
