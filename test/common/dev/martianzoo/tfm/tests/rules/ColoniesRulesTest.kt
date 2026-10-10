package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Dirigibles
import dev.martianzoo.tfm.tests.cards.cardnames.ExtractorBalloons
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import dev.martianzoo.tfm.tests.cards.cardnames.NitriteReducingBacteria
import dev.martianzoo.tfm.tests.cards.cardnames.RegolithEaters
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesRulesTest : TfmSandboxTest() {
  @Test
  internal fun `A second owners colony raises the trade track above the first colony`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", playerCount = 2)
    kim.stdProject("BuildColonyProject") { doTask("Colony<Luna>") }

    stan
        .stdProject("BuildColonyProject") { doTask("Colony<Luna>") }
        .expect("ColonyProduction<Luna>")
  }

  @Test
  internal fun `A card-resource colony bonus goes to the colony owner`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Enceladus", playerCount = 2)
    stan.playProject(RegolithEaters, 13)
    kim.playProject(NitriteReducingBacteria, 11)
    kim.stdProject("BuildColonyProject") {
      doTask("Colony<Enceladus>")
      doTask("3 Microbe<$NitriteReducingBacteria>")
    }

    stan.stdAction("TradeAction<Action1>") {
      doWithoutAutoExec(stan) {
        doTask("Trade<Enceladus>")
        doTask("-TradeBarrier")
        doTask("Microbe<$RegolithEaters>")
        shouldThrow<TaskException> { kim.doTask("Microbe<$NitriteReducingBacteria>") }
        stan.selectTask("Microbe<Kim>.")
        kim.doTask("Microbe<$NitriteReducingBacteria>")
      }
    }

    kim.count("Microbe<$NitriteReducingBacteria>") shouldBe 7
    stan.count("Microbe<$RegolithEaters>") shouldBe 1
  }

  @Test
  internal fun `Pluto draws a card before requiring its discard`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    kim.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    kim.setToExMachina(0, "ProjectCard")
    stan.exMachina("3 Energy")
    stan.setToExMachina(0, "ProjectCard")

    stan.stdAction("TradeAction<Action2>") { doTask("Trade<Pluto>") }

    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Pluto bonuses finish one at a time per owner`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    stan.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    stan.setToExMachina(0, "ProjectCard")
    kim.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    kim.playProject(ResearchColony, 20) { doTask("Colony<Pluto>") }
    kim.setToExMachina(0, "ProjectCard")
    kim.autoExecPolicy = NONE

    stan.stdAction("TradeAction<Action2>") {
      doWithoutAutoExec(stan) {
        fun performForKim(instruction: String) {
          stan.selectTask(instruction)
          kim.doTask(instruction)
        }

        doTask("Trade<Pluto>")
        doTask("-TradeBarrier")
        doTask("2 ProjectCard")
        performForKim("PlutoLock<Kim>!")
        performForKim("ProjectCard<Kim>")
        shouldThrow<TaskException> { doTask("ProjectCard<Kim>") }
        kim.count("ProjectCard") shouldBe 1

        // Another owner's bonus remains available while Kim must discard.
        doTask("PlutoLock<Stan>!")
        doTask("ProjectCard<Stan>")
        stan.count("ProjectCard") shouldBe 3
        doTask("-ProjectCard<Stan>")
        doTask("-PlutoLock<Stan>!")

        performForKim("-ProjectCard<Kim>")
        performForKim("-PlutoLock<Kim>!")
        performForKim("PlutoLock<Kim>!")
        performForKim("ProjectCard<Kim>")
        kim.count("ProjectCard") shouldBe 1
        performForKim("-ProjectCard<Kim>")
        performForKim("-PlutoLock<Kim>!")
      }
    }

    kim.count("ProjectCard") shouldBe 0
    stan.count("ProjectCard") shouldBe 2
    admin.count("PlutoLock<Anyone>") shouldBe 0
  }

  @Test
  internal fun `Two Pluto bonuses complete with normal autoexecution`() {
    newTestGame(addOptions = "Pluto, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    kim.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    kim.playProject(ResearchColony, 20) { doTask("Colony<Pluto>") }

    stan
        .stdAction("TradeAction<Action2>") { doTask("Trade<Pluto>") }
        .expect("0 ProjectCard<Kim>, 2 ProjectCard<Stan>, 0 PlutoLock<Anyone>")
  }

  @Test
  internal fun `Europa can be colonized after the last ocean is placed`() {
    newTestGame(addOptions = "Europa, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    val waterAreas = kim.list("WaterArea").take(8)
    kim.exMachina(waterAreas.joinToString { "OceanTile<$it>" })
    kim.stdProject("AquiferProject") { doTask("OceanTile<Tharsis_9_9>") }
    val ratingBefore = kim.count("TerraformRating")

    kim.stdProject("BuildColonyProject") { doTask("Colony<Europa>") }
        .expect("Colony<Europa>, 0 OceanTile")

    kim.count("TerraformRating") shouldBe ratingBefore
    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `A player can trade with Enceladus without a card that stores microbes`() {
    newTestGame(addOptions = "Enceladus, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    stan.exMachina("3 Energy")
    kim.playProject(RegolithEaters, 13)

    stan
        .stdAction("TradeAction<Action2>") { doTask("Trade<Enceladus>") }
        .expect("0 Microbe<Anyone>")

    stan.count("Trade<Enceladus>") shouldBe 1
  }

  @Test
  internal fun `Two Titan colonies can put their bonus floaters on different cards`() {
    newTestGame(addOptions = "Titan, Luna, Ceres, Triton, Ganymede", playerCount = 2)
    kim.setToExMachina(66, "MC")
    stan.exMachina("3 Energy")
    kim.playProject(ForcedPrecipitation, 8)
    kim.playProject(ExtractorBalloons, 21)
    kim.stdProject("BuildColonyProject") {
      doTask("Colony<Titan>")
      addCardResources(ForcedPrecipitation, 3)
    }
    kim.playProject(ResearchColony, 20) {
      doTask("Colony<Titan>")
      addCardResources(ExtractorBalloons, 3)
    }
    stan.playProject(Dirigibles, 11)
    val precipitationBefore = kim.count("Floater<$ForcedPrecipitation>")
    val balloonsBefore = kim.count("Floater<$ExtractorBalloons>")

    stan.stdAction("TradeAction<Action2>") {
      doWithoutAutoExec(stan) {
        doTask("Trade<Titan>")
        doTask("-TradeBarrier")
        doTask("Floater<$Dirigibles>")
        stan.selectTask("Floater<Kim>.")
        kim.addCardResources(ForcedPrecipitation)
        stan.selectTask("Floater<Kim>.")
        kim.addCardResources(ExtractorBalloons)
      }
    }

    kim.count("Floater<$ForcedPrecipitation>") shouldBe precipitationBefore + 1
    kim.count("Floater<$ExtractorBalloons>") shouldBe balloonsBefore + 1
  }
}
