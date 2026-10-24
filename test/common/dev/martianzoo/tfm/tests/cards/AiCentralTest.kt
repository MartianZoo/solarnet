package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class AiCentralTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Cannot be played without energy production`() {
    kim.exMachina("$SearchForLife, $InventorsGuild, $DesignedMicroorganisms")
    kim.setToExMachina(0, "PROD[Energy]")
    kim.setToExMachina(21, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(*playCardTasks("AiCentral"), "21 Pay<> FROM MC")
        doTask("AiCentral FROM ProjectCard")
        doTask("PROD[-Energy]")
      }
    }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    kim.exMachina("$AiCentral")
    kim.inTurn {
      doTasks(*cardActionTasks("AiCentral"))
      doTask("2 ProjectCard<Kim, Hand>")
    }

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(*cardActionTasks("AiCentral"), "2 ProjectCard")
      }
    }
  }

  @Test
  internal fun `Cannot use a second action that AI Central does not have`() {
    shouldThrow<DeadEndException> { useSecondAction() }
    kim.inTurn { doTasks(*cardActionTasks("AiCentral"), "2 ProjectCard") }
  }

  private fun useSecondAction() {
    kim.exMachina("$AiCentral")
    kim.inTurn { doTasks(*cardActionTasks("AiCentral", "Action2")) }
  }
}
