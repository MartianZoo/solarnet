package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Player
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.Flooding
import dev.martianzoo.tfm.tests.cards.cardnames.NeptunianPowerConsultants
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Ignore
import kotlin.test.Test

internal class FloodingTest : CardTest() {
  @Test
  internal fun `Can choose between neighboring owners`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player3>!")
        }
        .expect("0 MC<Player2>, -4 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can charge no one`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) { placeTile(5, 4) }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Cannot charge anyone when no ocean area neighbors a tile`() {
    newGame(players = 4)
    val p2 = requireP2()
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")
    p2.runOperation("10 MC")

    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
      }
    }

    p2.count("MC") shouldBe 10
  }

  @Test
  internal fun `Can remove fewer than four MC from a neighboring player`() {
    arrangeFlooding()
    requireP2().runOperation("-8 MC")
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Player2>!")
        }
        .expect("-2 MC<Player2>, 0 MC<Player3>")
  }

  @Test
  internal fun `Cannot remove more than four MC`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -5 MC<Player2>!")
      }
    }
  }

  @Test
  internal fun `Cannot attack a second neighboring owner`() {
    arrangeFlooding()
    shouldThrow<TaskException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        doTask("-4 MC<Player3>")
      }
    }
  }

  @Test
  internal fun `An unrelated ocean while Flooding is pending grants no extra attack`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.runOperation("$Flooding, OceanTile<Tharsis_1_2>") { doTask("-4 MC<Player4>") }
    }
  }

  @Test
  internal fun `A later ocean grants no attack after Flooding resolves`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) { placeTile(5, 4) }
    shouldThrow<TaskException> {
      p1.runOperation("OceanTile<Tharsis_1_2>") { doTask("-4 MC<Player4>") }
    }
  }

  @Test
  internal fun `An adjacent special tile qualifies its owner`() {
    arrangeFlooding()
    requireP2().runOperation("-CityTile<Tharsis_4_3>, NaturalPreserve_SpecialTile<Tharsis_4_3>")
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("-4 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can play at the ocean limit without placing an ocean or attacking`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )
    p1.playProject(Flooding, 7)
        .expect("0 OceanTile, 0 TerraformRating, 0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Placing the last ocean still permits the attack`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, " +
            "OceanTile<Tharsis_2_6>, OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, " +
            "OceanTile<Tharsis_5_6>, OceanTile<Tharsis_6_7>"
    )
    p1.playProject(Flooding, 7) {
          p1.selectTask(tasks.ids().single())
          doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>!")
        }
        .expect("OceanTile, TerraformRating, -4 MC<Player2>, 0 MC<Player4>")
  }

  @Test
  internal fun `Cannot discard ocean restrictions by choosing the victim first`() {
    arrangeFlooding()
    admin.runOperation("OceanTile<Tharsis_1_2>")
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        p1.selectTask(tasks.ids().single())
        p1.narrowTask("OceanTile<WaterArea(HAS MAX 0 Tile)>! THEN -4 MC<Player4>!")
      }
    }
  }

  @Test
  internal fun `Further narrowing cannot switch the chosen victim`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>?")
        p1.selectTask(tasks.ids().single())
        p1.narrowTask("-4 MC<Player2>?")
        doTask("-4 MC<Player4>!")
      }
    }
  }

  @Test
  internal fun `Must place an ocean when the track is not complete`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) { declineTask() }
    }
  }

  @Test
  internal fun `An occupied water area cannot substitute an existing ocean for a new placement`() {
    arrangeFlooding()
    admin.runOperation("OceanTile<Tharsis_1_2>")
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player4>!")
      }
    }
  }

  @Test
  internal fun `Another ocean can fill the track while Flooding is pending`() {
    arrangeFlooding()
    admin.runOperation(
        "OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>, OceanTile<Tharsis_2_6>, " +
            "OceanTile<Tharsis_4_8>, OceanTile<Tharsis_5_5>, OceanTile<Tharsis_5_6>, " +
            "OceanTile<Tharsis_6_7>, OceanTile<Tharsis_6_8>"
    )
    p1.runOperation("$Flooding, OceanTile<Tharsis_1_2>")
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `Cannot charge a non-neighboring owner`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player4>!")
      }
    }
  }

  @Test
  internal fun `Cannot qualify the victim through a different ocean area`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        doTask("OceanTile<Tharsis_1_2>! THEN -4 MC<Player2>!")
      }
    }
  }

  @Test
  internal fun `Partial narrowing cannot discard the shared victim`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        p1.selectTask(tasks.ids().single())
        p1.narrowTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Anyone>?")
      }
    }
  }

  @Test
  internal fun `Selecting an unresolved attack arm cannot start the placement`() {
    arrangeFlooding()
    shouldThrow<NarrowingException> {
      p1.playProject(Flooding, 7) {
        p1.selectTask(tasks.ids().single())
        p1.narrowTask(
            "OceanTile<WaterArea(HAS MAX 0 Tile, HAS Neighbor<OwnedTile<Anyone>>)>! " +
                "THEN -4 MC<Anyone>?"
        )
      }
    }
  }

  @Test
  internal fun `Can decline the loss in a complete placement choice`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN Ok")
        }
        .expect("0 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  @Test
  internal fun `Can choose a smaller optional loss while selecting the victim`() {
    arrangeFlooding()
    p1.playProject(Flooding, 7) {
          doTask("OceanTile<Tharsis_5_4>! THEN -2 MC<Player2>?")
          doTask("-2 MC<Player2>!")
        }
        .expect("-2 MC<Player2>, 0 MC<Player3>, 0 MC<Player4>")
  }

  private fun arrangeFlooding() {
    val game = newGame(players = 4)
    val p2 = requireP2()
    val players = Player.players(4)
    val p3 = game.testTfm(players[2])
    val p4 = game.testTfm(players[3])
    admin.phase("Action")
    p1.runOperation("7 MC, ProjectCard")
    p2.runOperation("10 MC, CityTile<Tharsis_4_3>")
    p3.runOperation("10 MC, CityTile<Tharsis_5_3>")
    p4.runOperation("10 MC, CityTile<Tharsis_1_1>")
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
    newGame(PromoCardPack)
    admin.phase("Action")
    p1.runOperation("$NeptunianPowerConsultants, 16 MC, ProjectCard, CityTile<Tharsis_4_3>")
    return p1.playProject(Flooding, 7) {
      val previousPolicy = p1.autoExecPolicy
      p1.autoExecPolicy = NONE
      try {
        doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player1>?")
        doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
        p1.pay()
        p1.selectTask("-4 MC?")
        doTask("-4 MC")
        p1.count("Owed") shouldBe remainingDebt
        p1.pay(remainingDebt)
      } finally {
        p1.autoExecPolicy = previousPolicy
      }
    }
  }

  @Ignore // Acceptance releases Flooding before the bonus payment finishes.
  @Test
  internal fun `Cannot resume the loss before the accepted Neptunian bonus is paid`() {
    acceptOpponentsNeptunianBonus()
    shouldThrow<TaskException> { p1.doTask("-4 MC<Player2>!") }
    requireP2().assertCounts(5 to "MC", 0 to "Hydroelectric")
  }

  @Test
  internal fun `BUG - Can resume the loss before the accepted Neptunian bonus is paid`() {
    acceptOpponentsNeptunianBonus()
    p1.doTask("-4 MC<Player2>!")
    requireP2().assertCounts(1 to "MC", 0 to "Hydroelectric")
    requireP2().assertProds(0 to "Energy")
  }

  private fun acceptOpponentsNeptunianBonus() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.runOperation("$NeptunianPowerConsultants, CityTile<Tharsis_4_3>, 5 MC")
    admin.phase("Action")
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE

    p1.beginOperation("$Flooding")
    p1.doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>?")
    p1.selectTask("UseAction<Player2, NeptunianOption<NeptunianPowerConsultants<Player2>>>?")

    shouldThrow<TaskException> { p1.doTask("-4 MC<Player2>!") }.detail shouldContain "select-lock"
    p2.count("MC") shouldBe 5
    p2.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")
  }

  @Ignore // Flooding can consume the last cash after Steel was already spent.
  @Test
  internal fun `Cannot interrupt a partially paid Neptunian bonus`() {
    partlyPayOpponentsNeptunianBonus()
    shouldThrow<TaskException> {
      p1.selectTask("-4 MC<Player2>?")
      p1.doTask("-1 MC<Player2>!")
    }
    requireP2().assertCounts(0 to "Steel", 1 to "MC", 1 to "Owed")
  }

  @Test
  internal fun `BUG - Can interrupt a partially paid Neptunian bonus and strand spent steel`() {
    partlyPayOpponentsNeptunianBonus()
    p1.selectTask("-4 MC<Player2>?")
    p1.doTask("-1 MC<Player2>!")
    p1.selectTask("-X MC<Player2>?")
    shouldThrow<LimitsException> { requireP2().doTask("-MC!") }.detail shouldContain "MC<Player2>"
    requireP2().assertCounts(0 to "Steel", 0 to "MC", 1 to "Owed", 0 to "Hydroelectric")
    requireP2().assertProds(0 to "Energy")
  }

  private fun partlyPayOpponentsNeptunianBonus() {
    newGame(PromoCardPack)
    val p2 = requireP2()
    p2.runOperation("$NeptunianPowerConsultants, CityTile<Tharsis_4_3>, 2 Steel, 1 MC")
    admin.phase("Action")
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE

    p1.beginOperation("$Flooding")
    p1.doTask("OceanTile<Tharsis_5_4>! THEN -4 MC<Player2>?")
    p1.selectTask("UseAction<Player2, NeptunianOption<NeptunianPowerConsultants<Player2>>>?")
    p2.doTask("UseAction<NeptunianOption<NeptunianPowerConsultants>, Action1>")

    // P1 orders each billing stage, while P2 performs the work assigned to P2.
    p1.selectTask("5 Owed<Player2>!")
    p2.doTask("5 Owed<Player2>")
    p1.selectTask(
        "ActionBilling<Player2, NeptunianOption<NeptunianPowerConsultants<Player2>>, Action1>"
    )
    p2.doTask(
        "ActionBilling<Player2, NeptunianOption<NeptunianPowerConsultants<Player2>>, Action1>"
    )
    p1.selectTask("-X Steel<Player2>?")
    p2.doTask("-2 Steel")
    p2.assertCounts(0 to "Steel", 1 to "MC", 1 to "Owed")
  }
}
