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
      doTasks("PlayProject<Class<PublicPlans>>", "-7 MC", "PublicPlans FROM ProjectCard")
      selectOnlyTask()
      doTask("2 ProjectCard<Revealed FROM Hand>")
      selectOnlyTask()
      doTask("2 MC")
    }
  }

  @Test
  internal fun `Can reveal only one of several other cards`() {
    kim.setToExMachina(3, "ProjectCard")

    kim.inTurn {
      doTasks("PlayProject<Class<PublicPlans>>", "-7 MC", "PublicPlans FROM ProjectCard")
      selectOnlyTask()
      doTask("ProjectCard<Revealed FROM Hand>")
      selectOnlyTask()
      doTask("MC")
    }
  }

  @Test
  internal fun `Cannot be played as the last card in hand`() {
    kim.setToExMachina(1, "ProjectCard")

    shouldThrow<NotNowException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<PublicPlans>>")
        selectTask("-X MC?")
        doTasks("-7 MC", "PublicPlans FROM ProjectCard")
        selectOnlyTask()
        doTask("ProjectCard<Revealed FROM Hand>")
      }
    }
  }
}
