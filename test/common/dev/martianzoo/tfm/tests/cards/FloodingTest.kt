package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Flooding
import dev.martianzoo.tfm.tests.cards.cardnames.NeptunianPowerConsultants
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class FloodingTest : TfmSandboxTest() {
  @Test
  internal fun `Can choose between neighboring owners`() {
    arrangeFlooding()
    kim.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Rob>!")
        }
        .expect("0 MC<Stan>, -4 MC<Rob>, 0 MC<Maya>")
  }

  @Test
  internal fun `Can charge no one`() {
    arrangeFlooding()
    kim.playProject(Flooding, 7) { placeTile(5, 4) }.expect("0 MC<Stan>, 0 MC<Rob>, 0 MC<Maya>")
  }

  @Test
  internal fun `Cannot charge anyone when no ocean area neighbors a tile`() {
    newTestGame(playerCount = 4)
    stan.setToExMachina(10, "MC")

    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>!")
      }
    }

    stan.assertCounts(10 to "MC")
  }

  @Test
  internal fun `Can remove fewer than four MC from a neighboring player`() {
    arrangeFlooding()
    stan.setToExMachina(2, "MC")
    kim.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Stan>!")
        }
        .expect("-2 MC<Stan>, 0 MC<Rob>")
  }

  @Test
  internal fun `Cannot remove more than four MC`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -5 MC<Stan>!")
      }
    }
  }

  @Test
  internal fun `Cannot attack a second neighboring owner`() {
    arrangeFlooding()
    shouldThrow<TaskException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>!")
        doTask("-4 MC<Rob>")
      }
    }
  }

  @Test
  internal fun `A later ocean grants no attack after Flooding resolves`() {
    arrangeFlooding()
    kim.playProject(Flooding, 7) { placeTile(5, 4) }
    kim.setToExMachina(18, "MC")
    shouldThrow<TaskException> {
      kim.stdProject("AquiferProject") {
        placeTile(1, 2)
        doTask("-4 MC<Maya>")
      }
    }
    players[3].assertCounts(10 to "MC")
  }

  @Test
  internal fun `An adjacent special tile qualifies its owner`() {
    arrangeFlooding()
    stan.exMachina(
        "-NormalCityTile<Stan, Tharsis_4_3>, NaturalPreserve_SpecialTile<Stan, Tharsis_4_3>"
    )
    kim.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>!")
        }
        .expect("-4 MC<Stan>, 0 MC<Rob>, 0 MC<Maya>")
  }

  @Test
  internal fun `Can play at the ocean limit without placing an ocean or attacking`() {
    arrangeFlooding()
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )
    stan.setToExMachina(18, "MC")
    stan.stdProject("AquiferProject") { placeTile(6, 8) }
    kim.playProject(Flooding, 7)
        .expect("0 OceanTile, 0 TerraformRating, 0 MC<Stan>, 0 MC<Rob>, 0 MC<Maya>")
  }

  @Test
  internal fun `Placing the last ocean still permits the attack`() {
    arrangeFlooding()
    kim.exMachina(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )
    kim.playProject(Flooding, 7) {
          kim.selectTask(tasks.ids().single())
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>!")
        }
        .expect("OceanTile, TerraformRating, -4 MC<Stan>, 0 MC<Maya>")
  }

  @Test
  internal fun `Cannot discard ocean restrictions by choosing the victim first`() {
    arrangeFlooding()
    kim.exMachina("OceanTile<Tharsis_1_2>")
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        kim.selectTask(tasks.ids().single())
        kim.narrowTask("OceanTile<WaterArea(HAS MAX 0 Tile)>! THEN -4 MC<Maya>!")
      }
    }
  }

  @Test
  internal fun `Further narrowing cannot switch the chosen victim`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>?")
        kim.selectTask(tasks.ids().single())
        kim.narrowTask("-4 MC<Stan>?")
        doTask("-4 MC<Maya>!")
      }
    }
  }

  @Test
  internal fun `Must place an ocean when the track is not complete`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) { declineTask() }
    }
  }

  @Test
  internal fun `An occupied water area cannot substitute an existing ocean for a new placement`() {
    arrangeFlooding()
    kim.exMachina("OceanTile<Tharsis_1_2>")
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Maya>!")
      }
    }
  }

  @Test
  internal fun `Cannot charge a non-neighboring owner`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Maya>!")
      }
    }
  }

  @Test
  internal fun `Cannot qualify the victim through a different ocean area`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Stan>!")
      }
    }
  }

  @Test
  internal fun `Partial narrowing cannot discard the shared victim`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        kim.selectTask(tasks.ids().single())
        kim.narrowTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Anyone>?")
      }
    }
  }

  @Test
  internal fun `Selecting an unresolved attack arm cannot start the placement`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      kim.playProject(Flooding, 7) {
        kim.selectTask(tasks.ids().single())
        kim.narrowTask(
            "OceanTile<WaterArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile<Anyone>>)>! " +
                "THEN -4 MC<Anyone>?"
        )
      }
    }
  }

  @Test
  internal fun `Can decline the loss in a complete placement choice`() {
    arrangeFlooding()
    kim.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN Ok")
        }
        .expect("0 MC<Stan>, 0 MC<Rob>, 0 MC<Maya>")
  }

  @Test
  internal fun `Can choose a smaller optional loss while selecting the victim`() {
    arrangeFlooding()
    kim.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Stan>?")
          doTask("-2 MC<Stan>!")
        }
        .expect("-2 MC<Stan>, 0 MC<Rob>, 0 MC<Maya>")
  }

  private fun arrangeFlooding() {
    newTestGame(playerCount = 4)
    kim.setToExMachina(7, "MC")
    stan.setToExMachina(10, "MC")
    rob.setToExMachina(10, "MC")
    players[3].setToExMachina(10, "MC")
    stan.exMachina("NormalCityTile<Stan, Tharsis_4_3>")
    rob.exMachina("NormalCityTile<Rob, Tharsis_5_3>")
    players[3].exMachina("NormalCityTile<Maya, Tharsis_1_1>")
  }

  @Ignore // An unrelated loss by the payer is counted as payment.
  @Test
  internal fun `Self-inflicted loss does not pay an open Neptunian bill`() {
    loseMoneyWithNeptunianBill(remainingDebt = 5).expect("Hydroelectric, PROD[Energy], -16 MC")
  }

  @Test
  internal fun `BUG - Self-inflicted loss pays an open Neptunian bill`() {
    loseMoneyWithNeptunianBill(remainingDebt = 1).expect("Hydroelectric, PROD[Energy], -12 MC")
  }

  private fun loseMoneyWithNeptunianBill(remainingDebt: Int): TaskResult {
    newTestGame()
    kim.exMachina("$NeptunianPowerConsultants, NormalCityTile<Tharsis_4_3>")
    kim.setToExMachina(16, "MC")
    return kim.playProject(Flooding, 7) {
      doWithoutAutoExec(kim) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Kim>?")
        doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
        kim.pay()
        kim.selectTask("-4 MC?")
        doTask("-4 MC")
        kim.count("Owed") shouldBe remainingDebt
        kim.pay(remainingDebt)
      }
    }
  }

  @Ignore // Acceptance releases Flooding before the bonus payment finishes.
  @Test
  internal fun `Cannot resume the loss before the accepted Neptunian bonus is paid`() {
    acceptOpponentsNeptunianBonus {
      shouldThrow<TaskException> {
        kim.selectTask("-4 MC<Stan>?")
        kim.doTask("-4 MC<Stan>!")
      }
      stan.assertCounts(5 to "MC", 0 to "Hydroelectric")
    }
  }

  @Test
  internal fun `BUG - Can resume the loss before the accepted Neptunian bonus is paid`() {
    acceptOpponentsNeptunianBonus {
      kim.selectTask("-4 MC<Stan>?")
      kim.doTask("-4 MC<Stan>!").expect("-4 MC<Stan>")
      stan.assertCounts(1 to "MC", 0 to "Hydroelectric")
      stan.assertProds(0 to "Energy")
    }
  }

  private fun acceptOpponentsNeptunianBonus(assertWhileBillOpen: () -> Unit) {
    newTestGame()
    stan.setToExMachina(5, "MC")
    stan.setToExMachina(0, "PROD[Energy]")
    stan.exMachina("$NeptunianPowerConsultants, NormalCityTile<Tharsis_4_3>")

    kim.playProject(Flooding, 7) {
      kim.autoExecPolicy = NONE
      stan.autoExecPolicy = NONE
      doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>?")
      kim.selectTask("UseAction<Stan, NeptunianOption<NeptunianPowerConsultants<Stan>>>?")
      shouldThrow<TaskException> { kim.doTask("-4 MC<Stan>!") }
      stan.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
      assertWhileBillOpen()
      // The test observes the interruption while payment remains pending, then abandons the play.
      abort()
    }
  }

  @Ignore // Flooding can consume the last cash after Steel was already spent.
  @Test
  internal fun `Cannot interrupt a partially paid Neptunian bonus`() {
    partlyPayOpponentsNeptunianBonus {
      shouldThrow<TaskException> {
        kim.selectTask("-4 MC<Stan>?")
        kim.doTask("-1 MC<Stan>!")
      }
      stan.assertCounts(0 to "Steel", 1 to "MC", 1 to "Owed")
    }
  }

  @Test
  internal fun `BUG - Can interrupt a partially paid Neptunian bonus and strand spent steel`() {
    partlyPayOpponentsNeptunianBonus {
      kim.selectTask("-4 MC<Stan>?")
      kim.doTask("-1 MC<Stan>!")
      kim.selectTask("-X MC<Stan>?")
      shouldThrow<LimitsException> { stan.doTask("-MC!") }
      stan.assertCounts(0 to "Steel", 0 to "MC", 1 to "Owed", 0 to "Hydroelectric")
      stan.assertProds(0 to "Energy")
    }
  }

  private fun partlyPayOpponentsNeptunianBonus(assertWhileBillOpen: () -> Unit) {
    newTestGame()
    stan.setToExMachina(1, "MC")
    stan.setToExMachina(0, "PROD[Energy]")
    stan.exMachina("$NeptunianPowerConsultants, NormalCityTile<Tharsis_4_3>, 2 Steel")

    kim.playProject(Flooding, 7) {
      kim.autoExecPolicy = NONE
      stan.autoExecPolicy = NONE
      doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Stan>?")
      kim.selectTask("UseAction<Stan, NeptunianOption<NeptunianPowerConsultants<Stan>>>?")
      stan.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
      // Admin opens the bill; Kim selects the payment choice for Stan.
      admin.doTask("5 Owed<Stan>")
      admin.doTask("ActionBilling<Stan, NeptunianOption<NeptunianPowerConsultants<Stan>>, Action1>")
      kim.selectTask("-X Steel<Stan>?")
      stan.doTask("-2 Steel")
      assertWhileBillOpen()
      abort()
    }
  }
}
