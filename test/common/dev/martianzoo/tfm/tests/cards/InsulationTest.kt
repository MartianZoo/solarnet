package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
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
          "-2 MC",
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

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<Insulation>>")
        selectTask("-X MC?")
        doTasks("-2 MC", "Insulation FROM ProjectCard")
        selectOnlyTask()
        doTask("Ok")
      }
    }
  }
}
