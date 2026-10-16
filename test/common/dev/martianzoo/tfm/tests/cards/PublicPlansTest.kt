package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PublicPlansTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Rewards two revealed cards and returns them to hand`() {
    kim.setToExMachina(3, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<PublicPlans>>",
          "7 Pay<> FROM MC",
          "PublicPlans FROM ProjectCard",
      )
      doTask("2 ProjectCard<Revealed FROM Hand>")
      doTask("2 ProjectCard FROM ProjectCard<Revealed>")
      doTask("2 MC")
    }
  }

  @Test
  internal fun `Can reveal only one of several other cards`() {
    kim.setToExMachina(3, "ProjectCard")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<PublicPlans>>",
          "7 Pay<> FROM MC",
          "PublicPlans FROM ProjectCard",
      )
      doTask("ProjectCard<Revealed FROM Hand>")
      doTask("ProjectCard FROM ProjectCard<Revealed>")
      doTask("MC")
    }
  }

  @Test
  internal fun `Cannot be played as the last card in hand`() {
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<NotNowException> {
      kim.inTurn {
        doTask("PlayProject<Class<PublicPlans>>")

        doTasks(
            "7 Pay<> FROM MC",
            "PublicPlans FROM ProjectCard",
        )
        doTask("ProjectCard<Revealed FROM Hand>")
      }
    }
  }
}
