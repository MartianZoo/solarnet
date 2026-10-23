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

    kim.inTurn {
          doTasks(
              "UseStandardProject<AquiferProject>",
              "18 Pay<> FROM MC",
              "OceanTile<Tharsis_4_8>",
              "TerraformRating",
              "2 Plant",
          )
          doTasks(
              "UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>",
              "2 Pay<Class<Steel>> FROM Steel",
              "1 Pay<> FROM MC",
              "PROD[Energy]",
          )
          doTask("Hydroelectric<NeptunianPowerConsultants>")
        }
        .expect("Hydroelectric")
  }

  @Test
  internal fun `Owner chooses and pays when an opponent places the ocean`() {
    kim.exMachina("$NeptunianPowerConsultants")
    stan
        .inTurn {
          doTasks(
              "UseStandardProject<AquiferProject>",
              "18 Pay<> FROM MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
          )
          kim.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
          doTask("2 Steel")
          kim.doTask("5 Pay<> FROM MC")
          // Handle Admin's bill closure after Kim's cross-player payment.
          autoExecNow()
          kim.doTask("PROD[Energy]")
          kim.doTask("Hydroelectric<NeptunianPowerConsultants>")
        }
        .expect("-5 MC<Kim>, Hydroelectric<Kim>, PROD[Energy<Kim>]")
  }

  @Test
  internal fun `Unaffordable bonus does not block an opponent's ocean`() {
    kim.exMachina("$NeptunianPowerConsultants")
    kim.setToExMachina(0, "MC")

    stan
        .inTurn {
          // Decline the unaffordable Neptunian bonus.
          doTasks(
              "UseStandardProject<AquiferProject>",
              "18 Pay<> FROM MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
          )
          kim.doTask("0 UseAction<NeptunianOption<NeptunianPowerConsultants>>")
          doTask("2 Steel")
        }
        .expect("0 Hydroelectric<Kim>")
  }
}
