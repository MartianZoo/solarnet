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
      kim.doTask("UseAction<PlayCardAction, Action1>")
      shouldThrow<TaskException> {
        kim.doTask("PlayCard<Class<ProjectCard>, Class<Mine>, Selecting>")
      }
      shouldThrow<TaskException> {
        kim.doTask("PlayCard<Class<ProjectCard>, Class<Mine>, Revealed>")
      }
      kim.count("MC") shouldBe 20
      kim.doTasks(
          "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
          "4 Pay<> FROM MC",
          "Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }
    game.isIdle() shouldBe true
  }
}
