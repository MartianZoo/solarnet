package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DevelopmentCenterTest : TfmSandboxTest() {
  @Test
  internal fun `using a card through the standard action consumes its action for the generation`() {
    newTestGame()
    kim.exMachina("$DevelopmentCenter, 2 Energy")
    kim.setToExMachina(0, "ProjectCard")

    kim.inTurn {
      doTasks(
          "UseAction<UseActionOnCardAction, Action1>",
          "UseAction<$DevelopmentCenter, Action1>",
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }
    kim.inTurn {
      shouldThrow<LimitsException> {
        kim.continueOperation {
          doTasks(
              "UseAction<UseActionOnCardAction, Action1>",
              "UseAction<$DevelopmentCenter, Action1>",
              "Pay<Class<Energy>> FROM Energy",
              "ProjectCard",
          )
        }
      }
      doTask("Pass")

      kim.count("Energy") shouldBe 1
      kim.count("ProjectCard") shouldBe 1
    }
    game.isIdle() shouldBe true
  }
}
