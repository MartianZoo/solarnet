package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PsychrophilesTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Psychrophiles")

  @Test
  internal fun `Can decline to spend a microbe on a plant-tag card`() {
    kim.exMachina("$Psychrophiles, Microbe<$Psychrophiles>")
    kim.setToExMachina(9, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<AdaptedLichen>>",
              "-9 MC",
              "AdaptedLichen FROM ProjectCard",
              "PROD[Plant]",
          )
          doTask("Ok")
        }
        .expect("0 Microbe<$Psychrophiles>")
  }

  @Test
  internal fun `Can spend five microbes toward a nine-cost card`() {
    kim.exMachina("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<AdaptedLichen>>",
          "-5 Microbe<Psychrophiles>",
          "AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("Ok" /* No MC payment. */)
    }
  }
}
