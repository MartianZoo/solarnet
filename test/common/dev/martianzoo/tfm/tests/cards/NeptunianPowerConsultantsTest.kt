package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.NeptunianPowerConsultants
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class NeptunianPowerConsultantsTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Ocean bonus can be paid with steel`() {
    kim.exMachina("$NeptunianPowerConsultants")
    kim.setToExMachina(19, "MC")
    kim.setToExMachina(2, "Steel")

    kim.stdProject("AquiferProject") {
          placeTile(4, 8)
          doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
          kim.pay(mc = 1, steel = 2)
        }
        .expect("-2 Steel, 2 Plant, Hydroelectric, PROD[Energy]")
  }

  @Test
  internal fun `Owner chooses and pays when an opponent places the ocean`() {
    kim.exMachina("$NeptunianPowerConsultants")
    stan
        .stdProject("AquiferProject") {
          placeTile(1, 2)
          kim.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
          kim.pay(5)
        }
        .expect("-5 MC<Kim>, Hydroelectric<Kim>, PROD[Energy<Kim>]")
  }

  @Test
  internal fun `Unaffordable bonus does not block an opponent's ocean`() {
    kim.exMachina("$NeptunianPowerConsultants")
    kim.setToExMachina(0, "MC")

    stan
        .stdProject("AquiferProject") {
          placeTile(1, 2)
          // Decline the unaffordable Neptunian bonus.
          kim.declineTask()
        }
        .expect("OceanTile, TerraformRating<Stan>, 0 Hydroelectric<Kim>")
  }
}
