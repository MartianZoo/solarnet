package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesRulesTest : TfmSandboxTest() {
  @Test
  internal fun `A second owners colony raises the trade track above the first colony`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Luna>",
          "PROD[2 MC]",
      )
    }

    stan
        .inTurn {
          doTasks(
              *standardProjectTasks("BuildColonyProject"),
              "17 Pay<> FROM MC",
              "Colony<Luna>",
              "PROD[2 MC]",
          )
        }
        .expect("ColonyProduction<Luna>")
  }

  @Test
  internal fun `A card-resource colony bonus goes to the colony owner`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Enceladus", playerCount = 2)
    stan.inTurn {
      doTasks(
          *playCardTasks("$RegolithEaters"),
          "13 Pay<> FROM MC",
          "$RegolithEaters FROM ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$NitriteReducingBacteria"),
          "11 Pay<> FROM MC",
          "$NitriteReducingBacteria FROM ProjectCard",
          "3 Microbe<$NitriteReducingBacteria>",
      )
    }
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Enceladus>",
          "3 Microbe<$NitriteReducingBacteria>",
      )
    }

    stan.inTurn {
      doTasks("UseAction<TradeAction, Action1>", "9 Pay<> FROM MC")
      run {
        doTasks("Trade<Enceladus>", "Microbe<$RegolithEaters>")
        shouldThrow<TaskException> { kim.doTask("Microbe<$NitriteReducingBacteria>") }
        // Stan chooses the timing; Kim chooses which card receives her colony bonus.
        doTask("NOW Kim { Microbe<Kim>* }")
        shouldThrow<TaskException> { doTask("Microbe<$NitriteReducingBacteria<Kim>>") }
        kim.doTask("Microbe<$NitriteReducingBacteria>")
      }
    }

    kim.count("Microbe<$NitriteReducingBacteria>") shouldBe 7
    stan.count("Microbe<$RegolithEaters>") shouldBe 1
  }

  @Test
  internal fun `Pluto draws a card before requiring its discard`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Pluto>",
          "2 ProjectCard<Hand>",
      )
    }
    kim.setToExMachina(0, "ProjectCard")
    stan.exMachina("3 Energy")
    stan.setToExMachina(0, "ProjectCard")

    stan.inTurn {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "ProjectCard<Hand>",
      )
      kim.count("PlutoLock") shouldBe 0
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "ProjectCard<Kim, Hand>")
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "-ProjectCard<Kim, Hand>")
    }

    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Pluto bonuses finish one at a time per owner`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    stan.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Pluto>",
          "2 ProjectCard",
      )
    }
    stan.setToExMachina(0, "ProjectCard")
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Pluto>",
          "2 ProjectCard<Hand>",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$ResearchColony"),
          "20 Pay<> FROM MC",
          "$ResearchColony FROM ProjectCard",
          "2 ProjectCard<Hand>",
          "Colony<Pluto>",
          "2 ProjectCard<Hand>",
      )
    }
    kim.setToExMachina(0, "ProjectCard")

    stan.inTurn {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "2 ProjectCard",
      )
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "PlutoLock!", "ProjectCard<Kim>")
      shouldThrow<TaskException> { doTask("ProjectCard<Kim>") }
      kim.count("ProjectCard") shouldBe 1
      kim.count("PlutoLock") shouldBe 1
      shouldThrow<NotNowException> {
        doTask("NOW Kim { PlutoLock! }")
      }
      kim.count("ProjectCard") shouldBe 1
      kim.count("PlutoLock") shouldBe 1
      giveTask(stan, "ProjectCard<Stan>")
      stan.count("ProjectCard") shouldBe 3
      doTask("-ProjectCard")
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "-ProjectCard", "-ProjectCard<Kim>")
      kim.count("PlutoLock") shouldBe 0
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "ProjectCard<Kim>")
      kim.count("ProjectCard") shouldBe 1
      // Stan chooses when Kim handles this colony task.
      giveTask(kim, "-ProjectCard<Kim>")
    }

    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 2
    admin.count("PlutoLock<Anyone>") shouldBe 0
  }

  @Test
  internal fun `Europa can be colonized after the last ocean is placed`() {
    newTestGame(addOptions = "Europa, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    val waterAreas = kim.list("WaterArea").take(8)
    kim.exMachina(waterAreas.joinToString { "OceanTile<$it>" })
    kim.inTurn {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Titanium",
      )
    }

    kim.inTurn {
          doTasks(
              *standardProjectTasks("BuildColonyProject"),
              "17 Pay<> FROM MC",
              "Colony<Europa>",
          )
          // Resolve the ocean offer separately from other pending placement effects.
          doTask("OceanTile<WaterArea(HAS MAX 0 Tile)>*")
        }
        .expect("0 OceanTile, 0 TerraformRating")

    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `A player can trade with Enceladus without a card that stores microbes`() {
    newTestGame(addOptions = "Enceladus, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    kim.inTurn {
      doTasks(
          *playCardTasks("$RegolithEaters"),
          "13 Pay<> FROM MC",
          "$RegolithEaters FROM ProjectCard",
      )
    }

    stan
        .inTurn {
          doTasks(
              "UseAction<TradeAction, Action2>",
              "3 Pay<Class<Energy>> FROM Energy",
              "Trade<Enceladus>",
          )
          // Resolve the trade reward: this player has no card that can hold microbes.
          doTask("Microbe")
        }
        .expect("0 Microbe<Anyone>")

    stan.count("Trade<Enceladus>") shouldBe 1
  }

  @Test
  internal fun `Two Titan colonies can put their bonus floaters on different cards`() {
    newTestGame(addOptions = "Titan, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    kim.setToExMachina(66, "MC")
    stan.exMachina("3 Energy")
    kim.inTurn {
      doTasks(
          *playCardTasks("$ForcedPrecipitation"),
          "8 Pay<> FROM MC",
          "$ForcedPrecipitation FROM ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
    }
    kim.inTurn {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Titan>",
          "3* Floater<$ForcedPrecipitation>",
      )
    }
    kim.inTurn {
      doTasks(
          *playCardTasks("$ResearchColony"),
          "20 Pay<> FROM MC",
          "$ResearchColony FROM ProjectCard",
          "2 ProjectCard<Hand>",
          "Colony<Titan>",
          "3* Floater<$ExtractorBalloons>",
      )
    }
    stan.inTurn {
      doTasks(
          *playCardTasks("$Dirigibles"),
          "11 Pay<> FROM MC",
          "$Dirigibles FROM ProjectCard",
      )
    }

    stan
        .inTurn {
          doTasks(
              "UseAction<TradeAction, Action2>",
              "3 Pay<Class<Energy>> FROM Energy",
              "Trade<Titan>",
              "Floater<$Dirigibles>",
          )
          // Stan chooses when Kim handles this colony task.
          giveTask(kim, "Floater<Kim>*", "Floater<$ForcedPrecipitation>*")
          // Stan chooses when Kim handles this colony task.
          giveTask(kim, "Floater<$ExtractorBalloons>*")
        }
        .expect("Floater<$ForcedPrecipitation<Kim>>, Floater<$ExtractorBalloons<Kim>>")
  }
}
