package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class LocalHeatTrappingTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose unavailable animals instead of plants`() {
    kim.setToExMachina(5, "Heat")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<LocalHeatTrapping>>",
              "-1 MC",
              "LocalHeatTrapping FROM ProjectCard",
              "-5 Heat",
          )
          selectOnlyTask()
          // Decline gaining plants by choosing animals when no animal holder exists.
          doTask("Ok")
        }
        .expect("0 Plant")
  }

  @Test
  internal fun `Cannot decline animals when an eligible holder exists`() {
    kim.setToExMachina(5, "Heat")
    kim.exMachina("$Pets")

    shouldThrow<NarrowingException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("PlayProject<Class<LocalHeatTrapping>>")
        selectTask("-X MC?")
        doTasks("-1 MC", "LocalHeatTrapping FROM ProjectCard")
        selectTask("-5 Heat!")
        selectOnlyTask()
        doTask("Ok")
      }
    }
  }
}
