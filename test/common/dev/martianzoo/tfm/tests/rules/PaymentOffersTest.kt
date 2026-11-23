package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class PaymentOffersTest : TfmSandboxTest() {

  @Test
  internal fun `A card made free by an earlier tag needs no titanium or graphene decision`() {
    newTestGame(kimCorporation = ThorGate)
    kim.exMachina("$CarbonNanosystems, Graphene<$CarbonNanosystems>, Titanium")

    kim.inTurn {
          // Thorgate makes the card free even with several alternative payment sources.
          doTasks(*playCardTasks("$SpaceMirrors"))
        }
        .expect("0 MC, 0 Titanium, 0 Graphene")
  }

  @Test
  internal fun `A fully discounted building needs no steel or heat decision`() {
    newTestGame(addOptions = "fakes:cards")
    kim.exMachina("$FakeHelion, $EarthCatapult, $AntiGravityTechnology, Steel, Heat")

    kim.inTurn {
          doTasks(*playCardTasks("$Mine"), "PROD[Steel]")
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
              *playCardTasks("$SpaceElevator"),
              "7 Pay<Class<Steel>> FROM Steel",
              "5 Pay<Class<Titanium>> FROM Titanium",
              "Ok",
              "PROD[Titanium]",
          )
        }
        .expect("0 MC")
  }

  @Test
  internal fun `A card with both metal tags needs no metal decisions when only another player has metals`() {
    newTestGame()
    stan.exMachina("Steel, Titanium")

    kim.inTurn {
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "27 Pay<> FROM MC",
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
          *playCardTasks("$SpaceElevator"),
          "Pay<Class<Steel>> FROM Steel",
          "25 Pay<> FROM MC",
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
      doTasks(
          *playCardTasks("$SpaceElevator"),
          "9 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // Nothing remains to pay in MC.
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
          *playCardTasks("$SpaceElevator"),
          "Pay<Class<Steel>> FROM Steel",
          "Pay<Class<Titanium>> FROM Titanium",
          "21 Pay<> FROM MC",
          "PROD[Titanium]",
      )
    }
  }

  @Test
  internal fun `Cash waits for steel and must pay the exact remainder`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
      doTasks(*playCardTasks("$Mine"))
      shouldThrow<TaskException> { doTask("4 Pay<> FROM MC") }
      doTask("Pay<Class<Steel>> FROM Steel")
      shouldThrow<TaskException> { doTask("Pay<> FROM MC") }
      shouldThrow<TaskException> { doTask("3 Pay<> FROM MC") }
      shouldThrow<TaskException> { doTask("Ok") }
      doTasks(
          "2 Pay<> FROM MC",
          "PROD[Steel]",
      )
    }
  }

  @Test
  internal fun `Declining steel leaves the full exact cash payment`() {
    newTestGame()
    kim.exMachina("Steel")

    kim.inTurn {
          doTasks(*playCardTasks("$Mine"))
          doTask("Ok") // Keep the steel.
          doTasks("4 Pay<> FROM MC", "PROD[Steel]")
        }
        .expect("0 Steel")
  }

  @Test
  internal fun `Steel covering the full cost leaves only acknowledgement before the card`() {
    newTestGame()
    kim.exMachina("2 Steel")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks(*playCardTasks("$Mine"), "2 Pay<Class<Steel>> FROM Steel")
          kim.count("$Mine") shouldBe 0
          doTask("Ok") // Nothing remains to pay in MC.
          kim.count("$Mine") shouldBe 1
          doTasks("PROD[Steel]")
        }
        .expect("0 MC")
  }

  @Test
  internal fun `Both metal choices finish before the cash remainder is paid`() {
    newTestGame()
    kim.exMachina("3 Steel, 2 Titanium")
    kim.setToExMachina(15, "MC")

    kim.inTurn {
      doTasks(*playCardTasks("$SpaceElevator"), "3 Pay<Class<Steel>> FROM Steel")
      shouldThrow<TaskException> { doTask("15 Pay<> FROM MC") }
      doTasks(
          "2 Pay<Class<Titanium>> FROM Titanium",
          "15 Pay<> FROM MC",
          "PROD[Titanium]",
      )
    }
  }
}
