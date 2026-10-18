package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class PaymentOffersTest : TfmSandboxTest() {
  @Test
  internal fun `Water Import from Europa can pay its entire action cost in titanium`() {
    newTestGame()
    kim.exMachina("$WaterImportFromEuropa, 4 Titanium")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks(
              "UseCardAction<WaterImportFromEuropa, Action1>",
              "4 Pay<Class<Titanium>> FROM Titanium",
              "Ok", // Nothing remains to pay in MC.
              "OceanTile<Tharsis_1_2>",
              "TerraformRating",
              "2 Steel",
          )
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Aquifer Pumping can combine steel with the exact cash remainder`() {
    newTestGame()
    kim.exMachina("$AquiferPumping, $EarthCatapult, 3 Steel")
    kim.setToExMachina(2, "MC")

    kim.inTurn {
      doTasks(
          "UseCardAction<AquiferPumping, Action1>",
          "3 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "OceanTile<Tharsis_1_2>",
          "TerraformRating",
          "2 Steel",
      )
    }
  }

  @Test
  internal fun `A card made free by an earlier tag needs no titanium or graphene decision`() {
    newTestGame(kimCorporation = ThorGate)
    kim.exMachina("$CarbonNanosystems, Graphene<$CarbonNanosystems>, Titanium")

    kim.inTurn {
          // Thorgate makes the card free even with several alternative payment sources.
          doTasks("PlayProject<Class<SpaceMirrors>>", "SpaceMirrors FROM ProjectCard")
        }
        .expect("0 MC, 0 Titanium, 0 Graphene")
  }

  @Test
  internal fun `A fully discounted building needs no steel or heat decision`() {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeHelion, $EarthCatapult, $AntiGravityTechnology, Steel, Heat")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 MC, 0 Steel, 0 Heat")
  }

  @Test
  internal fun `Mixed-metal payment currently accepts seven steel and five titanium for Space Elevator`() {
    newTestGame()
    kim.exMachina("10 Steel, 10 Titanium")
    kim.setToExMachina(1, "ProjectCard")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<$SpaceElevator>>",
              "7 Pay<Class<Steel>> FROM Steel",
              "5 Pay<Class<Titanium>> FROM Titanium",
              "Ok",
          )
          doTasks("SpaceElevator FROM ProjectCard", "PROD[Titanium]")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `A card with both metal tags needs no metal decisions when only another player has metals`() {
    newTestGame()
    stan.exMachina("Steel, Titanium")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "27 Pay<> FROM MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Steel remains usable when there is no titanium`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "Pay<Class<Steel>> FROM Steel",
          "25 Pay<> FROM MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Titanium can pay the whole price without an empty steel decision`() {
    newTestGame()
    kim.exMachina("9 Titanium")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
      doTask("PlayProject<Class<SpaceElevator>>")
      doTasks(
          "9 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // Nothing remains to pay in MC.
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Unity raises titanium value without changing steel value`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("Steel, Titanium, Ruling<Unity FROM Greens>, UnityPolicy")

    kim.inTurn {
      doTasks(
          "PlayProject<Class<SpaceElevator>>",
          "Pay<Class<Steel>> FROM Steel",
          "Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cash waits for steel and must pay the exact remainder`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
      doTask("PlayProject<Class<Mine>>")
      shouldThrow<TaskException> { doTask("4 Pay<> FROM MC") }
      doTask("Pay<Class<Steel>> FROM Steel")
      shouldThrow<TaskException> { doTask("Pay<> FROM MC") }
      shouldThrow<TaskException> { doTask("3 Pay<> FROM MC") }
      shouldThrow<TaskException> { doTask("Ok") }
      doTask("2 Pay<> FROM MC")
      doTasks("Mine FROM ProjectCard", "PROD[Steel]")
    }
  }

  @Test
  internal fun `Declining steel leaves the full exact cash payment`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
          doTask("PlayProject<Class<Mine>>")
          doTask("Ok") // Keep the steel.
          doTasks("4 Pay<> FROM MC", "Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 Steel")
  }

  @Test
  internal fun `Steel covering the full cost leaves only acknowledgement before the card`() {
    newTestGame()
    kim.exMachina("2 Steel")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks("PlayProject<Class<Mine>>", "2 Pay<Class<Steel>> FROM Steel")
          shouldThrow<RequirementException> { doTask("Mine FROM ProjectCard") }
          doTask("Ok") // Nothing remains to pay in MC.
          doTasks("Mine FROM ProjectCard", "PROD[Steel]")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Both metal choices finish before the cash remainder is paid`() {
    newTestGame()
    kim.exMachina("3 Steel, 2 Titanium")
    kim.setToExMachina(15, "MC")

    kim.inTurn {
      doTasks("PlayProject<Class<SpaceElevator>>", "3 Pay<Class<Steel>> FROM Steel")
      shouldThrow<TaskException> { doTask("15 Pay<> FROM MC") }
      doTasks(
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "SpaceElevator FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
  }
}
