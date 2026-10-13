package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MonsInsuranceTest : TfmSandboxTest() {
  @Test
  internal fun `Starting production loss reaches every opponent but not its owner`() {
    newTestGameBeforeCorporationPlay()

    kim.inTurn {
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<MonsInsurance>>",
              "MonsInsurance FROM CorporationCard",
              "48 MC",
              "PROD[4 MC]",
          )
          doTask(
              "EACH Other@Player(NOT Kim) { -2! Production<Other@Player, Class<MC>> BY Other@Player }"
          )
          selectTask("-2! Production<Stan, Class<MC>> BY Stan")
          stan.selectOnlyTask()
          selectTask("-2! Production<Rob, Class<MC>> BY Rob")
          rob.selectOnlyTask()
          doTasks("-15 MC", "5 ProjectCard<Hand FROM Selecting>")
        }
        .expect("PROD[-2 MC<Stan>, -2 MC<Rob>]")
  }

  @Test
  internal fun `Starting production loss does not target the solo opponent`() {
    newTestGameBeforeCorporationPlay(playerCount = 1)

    kim.inTurn {
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<MonsInsurance>>",
              "MonsInsurance FROM CorporationCard",
              "48 MC",
              "PROD[4 MC]",
          )
          doTask(
              "EACH Other@Player(NOT Kim) { -2! Production<Other@Player, Class<MC>> BY Other@Player }"
          )
          doTasks("-15 MC", "5 ProjectCard<Hand FROM Selecting>")
        }
        .expect("PROD[0 MC<SoloOpponent>]")
  }

  @Test
  internal fun `Merger after Manutech pays for its own production gain only`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = Manutech)

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          selectTask("4! CorporationCard<Kim, Selecting>")
          selectTask("-3! CorporationCard<Kim, Selecting>")
          doTask("PlayCard<Class<CorporationCard>, Class<MonsInsurance>>")
          doTask("MonsInsurance<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTasks("48 MC", "-42 MC", "PROD[4 MC]")
          selectTask(
              "EACH Other@Player(NOT Kim) { -2! Production<Other@Player, Class<MC>> BY Other@Player }"
          )
          doTask("4 MC")
          selectTask("-2! Production<Stan, Class<MC>> BY Stan")
          stan.selectOnlyTask()
          selectTask("-2! Production<Rob, Class<MC>> BY Rob")
          rob.selectOnlyTask()
        }
        .expect("PROD[-2 MC<Stan>, -2 MC<Rob>]")
  }

  @Test
  internal fun `Hired Raiders compensates the victim after transferring stolen steel`() {
    newTestGame(addOptions = "-promos:cards, MonsInsurance")
    kim.exMachina("$MonsInsurance")
    rob.exMachina("4 Steel")

    stan.inTurn {
      doTasks(
          "PlayProject<Class<HiredRaiders>>",
          "-1 MC",
          "HiredRaiders FROM ProjectCard",
          "2 Steel<Stan FROM Rob>",
      )
      kim.doTask("3 MC<Rob FROM Kim>")
      doTask("PlayedEvent<Class<$HiredRaiders>> FROM $HiredRaiders")
    }
  }

  @Test
  internal fun `An attack during Preludes still requires compensation`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant")

    with(kim) {
      inTurn {
            doTasks(
                "PlayCard<Class<PreludeCard>, Class<ExcentricSponsor>>",
                "ExcentricSponsor FROM PreludeCard",
                "PlayCard<Class<ProjectCard>, Class<AsteroidCard>>",
            )
            selectTask("-25* Owed")
            doTasks(
                "AsteroidCard FROM ProjectCard",
            )
            selectTask("TemperatureStep*")
            doTasks("2 Titanium", "TerraformRating")
            selectTask("-X? MC")
            doTask("Ok")
            selectTask("-X? Titanium")
            doTasks("Ok", "-Plant<Stan>")
            selectOnlyTask()
            doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
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

    kim.inTurn {
          doTasks(
              "PlayProject<Class<Birds>>",
              "-10 MC",
              "Birds FROM ProjectCard",
              "PROD[-2 Plant<Stan>]",
          )
          selectOnlyTask()
        }
        .expect("-13 MC<Kim>, 3 MC<Stan>")
  }

  @Test
  internal fun `Consuming ones own plants causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant, OceanTile<Tharsis_1_2>, OceanTile<Tharsis_1_4>, OceanTile<Tharsis_1_5>")

    stan
        .inTurn {
          doTasks(
              "PlayProject<Class<Moss>>",
              "-4 MC",
              "Moss FROM ProjectCard",
              "PROD[Plant]",
              "-Plant",
          )
        }
        .expect("0 MC<Kim>")
  }

  @Test
  internal fun `Compensation is limited to the insurer's remaining money`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    kim.setToExMachina(2, "MC")
    rob.exMachina("2 Steel")

    stan.inTurn {
      doTasks(
          "PlayProject<Class<$HiredRaiders>>",
          "-MC",
          "$HiredRaiders FROM ProjectCard",
          "2 Steel<Stan FROM Rob>",
      )
      kim.doTask("2 MC<Rob FROM Kim>")
      doTask("PlayedEvent<Class<$HiredRaiders>> FROM $HiredRaiders")
    }
  }

  @Test
  internal fun `Pharmacy Union's disease loss causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("$PharmacyUnion")

    kim.inTurn {
          doTasks("PlayProject<Class<IndustrialMicrobes>>", "-12 MC")
          selectTask("-X? Steel")
          doTasks("Ok", "IndustrialMicrobes FROM ProjectCard", "PROD[Energy]", "PROD[Steel]")
          stan.selectTask("Disease<PharmacyUnion<Stan>>! OR (MAX 0 PharmacyUnion<Stan>: Ok)")
          stan.selectOnlyTask()
        }
        .expect("-4 MC<Stan>, Disease<$PharmacyUnion<Stan>>")
  }

  @Test
  internal fun `Declining optional plant removal avoids compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    stan.exMachina("Plant")

    kim.inTurn {
          doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
          selectTask("-X? Titanium")
          doTasks(
              "Ok",
              "AsteroidCard FROM ProjectCard",
          )
          selectTask("TemperatureStep*")
          doTasks("2 Titanium", "TerraformRating")
          selectTask("-3? Plant<Anyone>")
          doTask("Ok")
          doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
        }
        .expect("0 Plant<Stan>, 0 MC<Stan>")
  }

  @Test
  internal fun `Solo steals compensate the general supply`() {
    newTestGame(playerCount = 1)
    kim.exMachina("$MonsInsurance")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<HiredRaiders>>",
              "-1 MC",
              "HiredRaiders FROM ProjectCard",
              "3 MC<Kim FROM SoloOpponent>",
          )
          selectTask("3! MC<SoloOpponent> BY Admin")
          selectOnlyTask()
          selectOnlyTask()
          doTask("PlayedEvent<Class<$HiredRaiders>> FROM $HiredRaiders")
        }
        .expect("-1 MC<Kim>")
  }

  @Test
  internal fun `An attack on the insurer causes no compensation`() {
    newTestGame()
    kim.exMachina("$MonsInsurance")
    kim.exMachina("Plant")

    stan
        .inTurn {
          doTasks("PlayProject<Class<AsteroidCard>>", "-14 MC")
          selectTask("-X? Titanium")
          doTasks(
              "Ok",
              "AsteroidCard FROM ProjectCard",
          )
          selectTask("TemperatureStep*")
          doTasks("2 Titanium", "TerraformRating", "-Plant<Kim>")
          doTask("PlayedEvent<Class<AsteroidCard>> FROM AsteroidCard")
        }
        .expect("0 MC<Kim>")
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

    stan.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Recession>>", "Recession FROM PreludeCard")
      doTask("10 MC")
      selectOnlyTask()
      if (compensateFirst) {
        doTask("-5 MC<Rob>")
        kim.doTask("3* MC<Rob FROM Kim>")
        selectTask("-5* MC<Kim>")
      } else {
        doTasks("-5 MC<Kim>", "-5 MC<Rob>")
        kim.doTask("3* MC<Rob FROM Kim>")
      }
      doTasks("PROD[-MC<Kim>]", "PROD[-MC<Rob>]")
      kim.doTask("3* MC<Rob FROM Kim>")
    }

    rob.count("MC") shouldBe if (compensateFirst) 3 else 0
    kim.count("MC") shouldBe 0
  }
}
