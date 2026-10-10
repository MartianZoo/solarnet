package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MonsInsuranceTest : TfmSandboxTest() {
  @Test
  internal fun `Starting production loss reaches every opponent but not its owner`() {
    newTestGame(kimCorporation = MonsInsurance, startAtCorporation = true)

    kim.playCorp(MonsInsurance) {
          doTask(
              "EACH Other@Player(NOT Kim) { -2 Production<Other@Player, Class<MC>>! BY Other@Player }"
          )
        }
        .expect("18 MC, PROD[4 MC<Kim>, -2 MC<Stan>, -2 MC<Rob>]")
  }

  @Test
  internal fun `Starting production loss does not target the solo opponent`() {
    newTestGame(playerCount = 1, kimCorporation = MonsInsurance, startAtCorporation = true)

    kim.playCorp(MonsInsurance) {
          doTask(
              "EACH Other@Player(NOT Kim) { -2 Production<Other@Player, Class<MC>>! BY Other@Player }"
          )
        }
        .expect("PROD[4 MC<Kim>, 0 MC<SoloOpponent>]")
  }

  @Test
  internal fun `Merger after Manutech pays for its own production gain only`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = Manutech)

    kim.playPrelude(Merger) { kim.playCorp(MonsInsurance) }
        .expect("10 MC<Kim>, PROD[4 MC<Kim>, -2 MC<Stan>, -2 MC<Rob>]")
  }

  @Test
  internal fun `Hired Raiders compensates the victim after transferring stolen steel`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    rob.exMachina("4 Steel")

    stan
        .playProject(HiredRaiders, 1) { doTask("2 Steel<Stan> FROM Steel<Rob>") }
        .expect("2 Steel<Stan>, -2 Steel<Rob>, -3 MC<Kim>, 3 MC<Rob>")
  }

  @Test
  internal fun `An attack during Preludes still requires compensation`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant")

    with(kim) {
      playPrelude(ExcentricSponsor) {
            playProject(AsteroidCard, 0) { doTask("-Plant<Stan>") }
          }
          .expect("-Plant<Stan>, -3 MC<Kim>, 3 MC<Stan>")
    }
  }

  @Test
  internal fun `A multi-step production attack compensates its victim once`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    kim.setToExMachina(13, "OxygenStep")
    stan.setToExMachina(3, "PROD[Plant]")

    kim.playProject(Birds, 10) { doTask("PROD[-2 Plant<Stan>]") }
        .expect("PROD[-2 Plant<Stan>], -13 MC<Kim>, 3 MC<Stan>")
  }

  @Test
  internal fun `Consuming ones own plants causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant, OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")

    stan.playProject(Moss, 4).expect("-Plant<Stan>, -4 MC<Stan>, 0 MC<Kim>")
  }

  @Test
  internal fun `Compensation is limited to the insurer's remaining money`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    kim.setToExMachina(2, "MC")
    rob.exMachina("2 Steel")

    stan
        .playProject(HiredRaiders, 1) { doTask("2 Steel<Stan> FROM Steel<Rob>") }
        .expect("-2 MC<Kim>, 2 MC<Rob>")
  }

  @Test
  internal fun `Pharmacy Union's disease loss causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("$PharmacyUnion")

    kim.playProject(IndustrialMicrobes, 12)
        .expect("-12 MC<Kim>, -4 MC<Stan>, Disease<$PharmacyUnion<Stan>>")
  }

  @Test
  internal fun `Declining optional plant removal avoids compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant")

    kim.playProject(AsteroidCard, 14) { declineTask() }
        .expect("0 Plant<Stan>, -14 MC<Kim>, 0 MC<Stan>")
  }

  @Test
  internal fun `Solo steals compensate the general supply`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$MonsInsurance")

    kim.playProject(HiredRaiders, 1) { doTask("3 MC<Kim> FROM MC<SoloOpponent>") }
        .expect("-1 MC<Kim>")
  }

  @Test
  internal fun `An attack on the insurer causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    kim.exMachina("Plant")

    stan.playProject(AsteroidCard, 14) { doTask("-Plant<Kim>") }.expect("-Plant<Kim>, 0 MC<Kim>")
  }

  @Test
  internal fun `Recession can compensate Rob before exhausting the insurers money`() {
    recessionLossOrder(compensateFirst = true)
  }

  @Test
  internal fun `Recession can exhaust the insurers money before compensating Rob`() {
    recessionLossOrder(compensateFirst = false)
  }

  private fun recessionLossOrder(compensateFirst: Boolean) {
    newTestGame(addOptions = "PreludeExpansion, Recession")
    kim.exMachina("$MonsInsurance")
    kim.setToExMachina(5, "MC")
    rob.setToExMachina(5, "MC")
    players.forEach { it.autoExecPolicy = CONCRETE }

    stan.playPrelude(Recession) {
      doTask("10 MC")
      if (compensateFirst) {
        doTask("-5 MC<Rob>")
        stan.selectTask("3 MC<Rob FROM Kim>.")
        autoExecNow()
        doTask("-2 MC<Kim>")
      } else {
        doTask("-5 MC<Kim>")
        doTask("-5 MC<Rob>")
        stan.selectTask("3 MC<Rob FROM Kim>.")
        autoExecNow()
      }
      doTask("PROD[-MC<Kim>]")
    }

    rob.count("MC") shouldBe if (compensateFirst) 3 else 0
    kim.count("MC") shouldBe 0
  }
}
