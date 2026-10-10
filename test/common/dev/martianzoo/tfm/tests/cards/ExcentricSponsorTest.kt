package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class ExcentricSponsorTest : TfmSandboxTest() {
  @BeforeTest
  fun initializeGame() {
    newTestGame(addOptions = "PreludeExpansion")
  }

  @Test
  internal fun `Can apply its full discount to the next card`() {
    with(kim) {
      playPrelude(ExcentricSponsor) { playProject(NitrogenRichAsteroid, 6) }
          .expect("-6 MC, PROD[Plant], 3 TerraformRating")
    }
  }

  @Test
  internal fun `Can play a card costing less than its full discount`() {
    with(kim) {
      playPrelude(ExcentricSponsor) { playProject(GhgImportFromVenus, 0) }
          .expect("PROD[3 Heat], TerraformRating")
    }
  }

  @Test
  internal fun `Double Down copies its project play and discount`() {
    newTestGame(addOptions = "PreludeExpansion")

    with(kim) {
      playPrelude(ExcentricSponsor) { playProject(DustSeals, 0) }
      playPrelude(DoubleDown) {
            doTask("CopyPrelude<$ExcentricSponsor>")
            playProject(Mine, 0)
          }
          .expect("$Mine, PROD[Steel], 0 MC")
    }
  }
}
