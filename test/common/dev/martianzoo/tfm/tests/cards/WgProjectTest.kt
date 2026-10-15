package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class WgProjectTest : TfmSandboxTest() {
  @Test
  internal fun `Plays the selected Prelude immediately without adding the others to the hand`() {
    newTestGame("PreludeExpansion, WgProject, HighCircles, TurmoilExpansion")
    advanceUntilActionPhase()
    kim.exMachina("-Chairman<Neutral>, Chairman")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<WgProject>>",
              "9 Pay<> FROM MC",
              "WgProject FROM ProjectCard",
          )
          doTask("3 PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<HighCircles>>")
          doTask("HighCircles FROM PreludeCard<Selecting>")
          doTask("TerraformRating")
          doTask("SearchForCard<Kim, ReferenceFilter<Class<Ruling>>>!")
          doTask("2 PartyDelegate<Unity>")
        }
        .expect("$HighCircles, 0 PreludeCard, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `Refunds fifteen MC when its selected Prelude cannot be afforded`() {
    newTestGame("PreludeExpansion, WgProject, IndustrialComplex, TurmoilExpansion")
    advanceUntilActionPhase()
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(19, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<WgProject>>")
        doTasks("9 Pay<> FROM MC", "WgProject FROM ProjectCard")
        doTask("3 PreludeCard<Kim, Selecting>")
        doTask("PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>")
        doTask("IndustrialComplex FROM PreludeCard<Selecting>")
        doTask("-18 MC")
      }
    }
    kim.inTurn {
          doTasks(
              "PlayProject<Class<WgProject>>",
              "9 Pay<> FROM MC",
              "WgProject FROM ProjectCard",
          )
          doTask("3 PreludeCard<Kim, Selecting>")
          doTask("-PreludeCard<Selecting>")
          // Hand the fizzle audit to Admin before receiving compensation.
          selectTask("Audit!")
          doTask("15 MC")
        }
        .expect("0 $IndustrialComplex, 0 PreludeCard, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `Drawn Prelude is playable without Prelude rules`() {
    playDonationWithoutPreludeRules().expect("12 MC, 0 PreludeCard")
  }

  private fun playDonationWithoutPreludeRules(): TaskResult {
    newTestGame("WgProject, TurmoilExpansion, prelude:cards")
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(9, "MC")
    kim.setToExMachina(1, "ProjectCard")
    return kim.inTurn {
      doTasks(
          "PlayProject<Class<WgProject>>",
          "9 Pay<> FROM MC",
          "WgProject FROM ProjectCard",
      )
      doTask("3 PreludeCard<Kim, Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      doTask("Donation<Kim> FROM PreludeCard<Kim, Selecting>!")
      doTask("21 MC")
    }
  }
}
