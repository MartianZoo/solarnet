package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.ExtractorBalloons
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import dev.martianzoo.tfm.tests.cards.cardnames.NitriteReducingBacteria
import dev.martianzoo.tfm.tests.cards.cardnames.RegolithEaters
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesRulesTest : TfmSandboxTest() {
  @Test
  internal fun `A second owners colony raises the trade track above the first colony`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.inTurn {
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Luna>", "PROD[2 MC]")
    }

    stan
        .inTurn {
          doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Luna>", "PROD[2 MC]")
        }
        .expect("ColonyProduction<Luna>")
  }

  @Test
  internal fun `A card-resource colony bonus goes to the colony owner`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Enceladus", playerCount = 2)
    stan.inTurn {
      doTasks("PlayProject<Class<RegolithEaters>>", "-13 MC", "RegolithEaters FROM ProjectCard")
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<NitriteReducingBacteria>>",
          "-11 MC",
          "NitriteReducingBacteria FROM ProjectCard",
      )
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Enceladus>")
      selectTask("3 Microbe.")
      doTask("3 Microbe<NitriteReducingBacteria>")
    }

    stan.inTurn {
      doTasks("TradeAction<Action1>", "-9 MC")
      run {
        doTasks("Trade<Enceladus>", "LATER { FinishTrade<Enceladus> }", "Microbe<$RegolithEaters>")
        kim.selectTask("Microbe<Kim>.")
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
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Pluto>")
      selectTask("2 ProjectCard<Kim, Hand>!")
    }
    kim.setToExMachina(0, "ProjectCard")
    stan.exMachina("3 Energy")
    stan.setToExMachina(0, "ProjectCard")

    stan.inTurn {
      doTasks("TradeAction<Action2>", "-Energy / Owed<Class<Energy>>")
      selectOnlyTask()
      doTasks("Trade<Pluto>", "LATER { FinishTrade<Pluto> }")
      selectTask("ProjectCard<Stan, Hand>!")
      kim.selectTask("ProjectCard<Kim, Hand>!")
      kim.selectTask("-ProjectCard<Kim, Hand>!")
    }

    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Pluto bonuses finish one at a time per owner`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    stan.inTurn {
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Pluto>")
      selectOnlyTask()
    }
    stan.setToExMachina(0, "ProjectCard")
    kim.inTurn {
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Pluto>")
      selectTask("2 ProjectCard<Kim, Hand>!")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<ResearchColony>>", "-20 MC")
      selectTask("-X Titanium?")
      doTasks("Ok", "ResearchColony FROM ProjectCard")
      selectTask("2 ProjectCard<Kim, Hand>!")
      selectOnlyTask()
      doTask("Colony<Pluto>")
      selectTask("2 ProjectCard<Kim, Hand>!")
    }
    kim.setToExMachina(0, "ProjectCard")

    stan.beginOperation("NewTurn")
    stan.doTasks(
        "TradeAction<Action2>",
        "-3 Energy",
        "Trade<Pluto>",
        "LATER { FinishTrade<Pluto> }",
        "2 ProjectCard",
    )
    kim.doTask("ProjectCard<Kim>")
    shouldThrow<TaskException> { stan.doTask("ProjectCard<Kim>") }
    kim.count("ProjectCard") shouldBe 1
    stan.doTask("ProjectCard<Stan>")
    stan.count("ProjectCard") shouldBe 3
    stan.doTask("-ProjectCard<Stan>")
    kim.doTask("-ProjectCard<Kim>")
    kim.doTask("ProjectCard<Kim>")
    kim.count("ProjectCard") shouldBe 1
    kim.doTask("-ProjectCard<Kim>")
    stan.completeOperation {}

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
          "UseStandardProject<AquiferProject>",
          "-18 MC",
          "OceanTile<Tharsis_9_9>",
          "TerraformRating",
          "2 Titanium",
      )
    }
    val ratingBefore = kim.count("TerraformRating")

    kim.inTurn {
          doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Europa>")
          selectTask("OceanTile<WaterArea(HAS MAX 0 Tile)>.")
        }
        .expect("0 OceanTile")

    kim.count("TerraformRating") shouldBe ratingBefore
    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `A player can trade with Enceladus without a card that stores microbes`() {
    newTestGame(addOptions = "Enceladus, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    kim.inTurn {
      doTasks("PlayProject<Class<RegolithEaters>>", "-13 MC", "RegolithEaters FROM ProjectCard")
    }

    stan
        .inTurn {
          doTasks("TradeAction<Action2>", "-Energy / Owed<Class<Energy>>")
          selectOnlyTask()
          doTasks("Trade<Enceladus>", "LATER { FinishTrade<Enceladus> }")
          selectOnlyTask()
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
          "PlayProject<Class<ForcedPrecipitation>>",
          "-8 MC",
          "ForcedPrecipitation FROM ProjectCard",
      )
    }
    kim.inTurn {
      doTasks(
          "PlayProject<Class<ExtractorBalloons>>",
          "-21 MC",
          "ExtractorBalloons FROM ProjectCard",
      )
      selectOnlyTask()
    }
    kim.inTurn {
      doTasks("UseStandardProject<BuildColonyProject>", "-17 MC", "Colony<Titan>")
      selectOnlyTask()
      doTask("3 Floater<ForcedPrecipitation>.")
    }
    kim.inTurn {
      doTasks("PlayProject<Class<ResearchColony>>", "-20 MC")
      selectTask("-X Titanium?")
      doTasks("Ok", "ResearchColony FROM ProjectCard")
      selectTask("2 ProjectCard<Kim, Hand>!")
      selectOnlyTask()
      doTask("Colony<Titan>")
      selectOnlyTask()
      doTask("3 Floater<ExtractorBalloons>.")
    }
    stan.inTurn {
      doTasks("PlayProject<Class<Dirigibles>>", "-11 MC", "Dirigibles FROM ProjectCard")
    }
    val precipitationBefore = kim.count("Floater<$ForcedPrecipitation>")
    val balloonsBefore = kim.count("Floater<$ExtractorBalloons>")

    stan.inTurn {
      doTasks("TradeAction<Action2>", "-Energy / Owed<Class<Energy>>")
      selectOnlyTask()
      doTasks("Trade<Titan>", "LATER { FinishTrade<Titan> }", "Floater<Dirigibles>")
      kim.selectTask("Floater<Kim>.")
      kim.doTask("Floater<ForcedPrecipitation>.")
      kim.selectTask("Floater<Kim>.")
      kim.doTask("Floater<ExtractorBalloons>.")
    }

    kim.count("Floater<$ForcedPrecipitation>") shouldBe precipitationBefore + 1
    kim.count("Floater<$ExtractorBalloons>") shouldBe balloonsBefore + 1
  }
}
