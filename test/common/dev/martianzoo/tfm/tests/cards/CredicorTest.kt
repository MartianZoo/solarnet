package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CrediCorTest : TfmSandboxTest() {
  @Test
  internal fun `Uses printed cost for its rebate even when discounts bring payment below twenty MC`() {
    newTestGame(kimCorporation = CrediCor)

    kim.exMachina("$EarthOffice, $ResearchOutpost")

    kim.playProject(EarthCatapult, 19).expect("-15 MC")
  }

  @Test
  internal fun `Stacks its standard-project rebate with Standard Technology`() {
    newTestGame(kimCorporation = CrediCor)

    kim.exMachina("$StandardTechnology")

    kim.stdProject("CityProject") { placeTile(2, 1) }.expect("-18 MC")
  }
}
