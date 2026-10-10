package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class BoardOfDirectorsTest : TfmSandboxTest() {
  @Test
  internal fun `Remains available after Prelude phase and spends a director to play another Prelude`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Donation)
        }
        .expect("-Director<$BoardOfDirectors>, 0 $BoardOfDirectors, $Donation, 9 MC")
  }

  @Test
  internal fun `Still spends its money and director when the selected Prelude fizzles`() {
    newTestGame("PreludeExpansion, BoardOfDirectors, IndustrialComplex")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()
    kim.setToExMachina(24, "MC")

    shouldThrow<LimitsException> {
      kim.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        kim.playPrelude(IndustrialComplex)
      }
    }
    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          doTask("-PreludeCard")
        }
        .expect("3 MC, -Director<$BoardOfDirectors>, 0 $IndustrialComplex")
  }

  @Test
  internal fun `A corporation acquired through Merger requires its first action`() {
    newTestGame("PreludeExpansion, BoardOfDirectors")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          kim.playPrelude(Merger) { kim.playCorp(Inventrix) }
        }
        .expect("$Inventrix")
    shouldThrow<NarrowingException> { kim.stdProject("PowerPlantProject") }
    kim.stdAction("RequiredActionsSignal").expect("3 ProjectCard")
    kim.stdProject("PowerPlantProject").expect("PROD[Energy]")
  }

  @Test
  internal fun `Sky Docks discounts the project played through Ecology Experts`() {
    newTestGame("PreludeExpansion, BoardOfDirectors, EcologyExperts, Unsafe")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()
    kim.exMachina("$SkyDocks")
    kim.setToExMachina(13, "MC")

    kim.cardAction1(BoardOfDirectors) {
          doTask("-12 MC")
          with(kim) { playPrelude(EcologyExperts) { playProject(DustSeals, 1) } }
        }
        .expect("-13 MC, $EcologyExperts, $DustSeals")
  }
}
