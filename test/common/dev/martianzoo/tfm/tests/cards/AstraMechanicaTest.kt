package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class AstraMechanicaTest : TfmSandboxTest() {

  @Test
  internal fun `Can return two differently typed played events`() {
    newTestGame()
    kim.inTurn {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    kim.inTurn {
          doTasks(
              *playCardTasks("$AstraMechanica"),
              "7 Pay<> FROM MC",
              "$AstraMechanica FROM ProjectCard",
              "ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>",
              "ProjectCard FROM PlayedEvent<Class<$InvestmentLoan>>",
          )
        }
        .expect("ProjectCard")
  }

  @Test
  internal fun `Cannot recover an event that placed a special tile`() {
    newTestGame()
    kim.inTurn {
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "LavaFlows_SpecialTile<Tharsis_2_2>",
          "Steel",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    shouldThrow<NarrowingException> {
      kim.inTurn {
        doTasks(*playCardTasks("$AstraMechanica"))

        doTasks(
            "7 Pay<> FROM MC",
            "$AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Class<$LavaFlows>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot recover flipped Pharmacy Union`() {
    newTestGame(kimCorporation = PharmacyUnion)
    kim.inTurn {
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$PhysicsComplex"),
          "12 Pay<> FROM MC",
          "$PhysicsComplex FROM ProjectCard",
          "PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion",
          "3 TerraformRating",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*playCardTasks("$AstraMechanica"))

        doTasks(
            "7 Pay<> FROM MC",
            "$AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Class<$PharmacyUnion>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot recover another player's event`() {
    newTestGame()
    stan.inTurn {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
      )
    }

    shouldThrow<TaskException> {
      kim.inTurn {
        doTasks(*playCardTasks("$AstraMechanica"))

        doTasks(
            "7 Pay<> FROM MC",
            "$AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Stan, Class<$MineralDeposit>>",
        )
      }
    }
  }

  @Test
  internal fun `Cannot play with only one eligible event`() {
    newTestGame()
    kim.inTurn {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "5 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *playCardTasks("$AstraMechanica"),
            "7 Pay<> FROM MC",
            "$AstraMechanica FROM ProjectCard",
            "ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>",
            "ProjectCard FROM PlayedEvent<Class<$MineralDeposit>>",
        )
      }
    }
  }
}
