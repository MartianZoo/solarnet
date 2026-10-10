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
    startActionPhase()
    kim.exMachina("-Chairman<Neutral>, Chairman")

    kim.playProject(WgProject, 9) {
          kim.playPrelude(HighCircles) { doTask("2 PartyDelegate<Unity>") }
        }
        .expect(
            "$HighCircles, TerraformRating, 2 PartyDelegate<Unity>, 0 PreludeCard, 0 PreludeCard<Selecting>"
        )
  }

  @Test
  internal fun `Refunds fifteen MC when its selected Prelude cannot be afforded`() {
    newTestGame("PreludeExpansion, WgProject, IndustrialComplex, TurmoilExpansion")
    startActionPhase()
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(19, "MC")

    shouldThrow<LimitsException> {
      kim.playProject(WgProject, 9) { kim.playPrelude(IndustrialComplex) }
    }
    kim.playProject(WgProject, 9) { doTask("-PreludeCard<Selecting>") }
        .expect("6 MC, $WgProject, 0 $IndustrialComplex, 0 PreludeCard, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `Drawn Prelude is playable without explicitly selecting its pool`() {
    playDonationWithoutPreludePool().expect("12 MC, 0 PreludeCard")
  }

  private fun playDonationWithoutPreludePool(): TaskResult {
    newTestGame("WgProject, TurmoilExpansion")
    kim.exMachina("-Chairman<Neutral>, Chairman")
    kim.setToExMachina(9, "MC")
    kim.setToExMachina(1, "ProjectCard")
    return kim.playProject(WgProject, 9) { kim.playPrelude(Donation) }
  }
}
