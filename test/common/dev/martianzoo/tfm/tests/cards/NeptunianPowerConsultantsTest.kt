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
              "-18 MC",
              "OceanTile<Tharsis_4_8>",
              "TerraformRating",
              "2 Plant",
          )
          selectOnlyTask()
          doTasks(
              "UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>",
              "-2 Steel",
              "-1 MC",
              "PROD[Energy]",
          )
          selectOnlyTask()
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
              "-18 MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
          )
          selectTask("UseAction<Kim, NeptunianOption<NeptunianPowerConsultants<Kim>>>?")
          kim.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
          doTask("2 Steel")
          selectTask("-X? Steel<Kim>")
          kim.selectTask("Ok")
          selectOnlyTask()
          kim.doTask("-5 MC")
          selectTask("Production<Kim, Class<Energy>>!")
          kim.selectTask("PROD[Energy]")
          selectOnlyTask()
          kim.selectOnlyTask()
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
              "-18 MC",
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
          )
          selectTask("UseAction<Kim, NeptunianOption<NeptunianPowerConsultants<Kim>>>?")
          kim.doTask("Ok")
          doTask("2 Steel")
        }
        .expect("0 Hydroelectric<Kim>")
  }
}
