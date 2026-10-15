package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class PsychrophilesTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame(addOptions = "Psychrophiles")

  @Test
  internal fun `An empty holder needs no payment decision even with microbes on another card`() {
    kim.exMachina("$Psychrophiles, $RegolithEaters, Microbe<$RegolithEaters>")
    kim.setToExMachina(9, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<AdaptedLichen>>",
              "9 Pay<> FROM MC",
              "AdaptedLichen FROM ProjectCard",
              "PROD[Plant]",
          )
        }
        .expect("0 Microbe")
  }

  @Test
  internal fun `Can decline to spend a microbe on a plant-tag card`() {
    kim.exMachina("$Psychrophiles, Microbe<$Psychrophiles>")
    kim.setToExMachina(9, "MC")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<AdaptedLichen>>",
              "Ok", // Keep the microbes.
              "9 Pay<> FROM MC",
              "AdaptedLichen FROM ProjectCard",
              "PROD[Plant]",
          )
        }
        .expect("0 Microbe<$Psychrophiles>")
  }

  @Test
  internal fun `Can spend five microbes toward a nine-cost card`() {
    kim.exMachina("$Psychrophiles, 5 Microbe<$Psychrophiles>")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTask("PlayProject<Class<AdaptedLichen>>")
      // Choose to spend microbes while leaving their amount unresolved.
      selectTask("X PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>")
      doTasks(
          "5 PayFromCard<Psychrophiles> FROM Microbe<Psychrophiles>",
          "Ok", // Nothing remains to pay in MC.
          "AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
    }
  }
}
