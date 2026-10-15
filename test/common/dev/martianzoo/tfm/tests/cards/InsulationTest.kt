package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class InsulationTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can convert two of three heat production`() {
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(0, "PROD[MC]")
    kim.setToExMachina(3, "PROD[Heat]")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<Insulation>>",
          "2 Pay<> FROM MC",
          "Insulation FROM ProjectCard",
          "PROD[2 MC FROM Heat]",
      )
    }
  }

  @Test
  internal fun `Cannot skip its production conversion`() {
    kim.setToExMachina(2, "MC")
    kim.setToExMachina(0, "PROD[MC]")
    kim.setToExMachina(3, "PROD[Heat]")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("PlayProject<Class<Insulation>>")
        doTasks("2 Pay<> FROM MC", "Insulation FROM ProjectCard")
        doTask("Ok")
      }
    }
  }
}
