package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MassiveDiscountsTest : TfmSandboxTest() {

  @Test
  internal fun `Stacks with other card discounts`() {
    newTestGame()

    kim.exMachina(
        "Steel, Titanium, $AntiGravityTechnology, $EarthCatapult, " +
            "$ResearchOutpost, $MassConverter, $QuantumExtractor, $Shuttles, $SpaceStation, " +
            "$AdvancedAlloys, $PhoboLog, $MercurianAlloys, $RegoPlastics"
    )
    kim.setToExMachina(4, "MC")
    kim.setToExMachina(1, "Titanium")

    kim.playProject(SpaceElevator, 4, steel = 1, titanium = 1).expect("-4 MC, -Steel, -Titanium")
  }

  @Test
  internal fun `A discount applies once for each matching printed tag`() {
    newTestGame()
    kim.exMachina("$EarthOffice, $AcquiredCompany, $MediaGroup")

    kim.playProject(LunaGovernor, 0).expect("PROD[2 MC]")
  }
}
