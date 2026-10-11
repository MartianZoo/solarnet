package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
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
        selectOnlyTask()
        doTasks("PlayProject<Class<AiCentral>>", "-21 MC")
        selectTask("-X Steel?")
        selectTask("Ok")
        doTask("AiCentral FROM ProjectCard")
        selectOnlyTask()
      }
    }
  }

  @Test
  internal fun `Cannot use its action twice in one generation`() {
    kim.exMachina("$AiCentral")
    kim.inTurn {
      doTask("UseCardAction<AiCentral, Action1>")
      selectTask("2 ProjectCard<Kim, Hand>!")
    }

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<AiCentral, Action1>")
      }
    }
  }
}
