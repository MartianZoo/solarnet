package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.*
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Dirigibles
import dev.martianzoo.tfm.tests.cards.cardnames.ExtractorBalloons
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import dev.martianzoo.tfm.tests.cards.cardnames.NitriteReducingBacteria
import dev.martianzoo.tfm.tests.cards.cardnames.RegolithEaters
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ColoniesRulesTest : CardTest() {
  @Test
  internal fun `A card-resource colony bonus goes to the colony owner`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = setOf("Luna", "Ceres", "Triton", "Ganymede", "Enceladus").map(::cn).toSet(),
    )
    val p2 = requireP2()
    p1.runOperation("100 MC, 5 ProjectCard")
    p2.runOperation("100 MC, 5 ProjectCard")
    admin.phase("Action")
    p2.playProject(RegolithEaters, 13)
    p1.playProject(NitriteReducingBacteria, 11)
    p1.stdProject("BuildColonyProject") {
      doTask("Colony<Enceladus>")
      doTask("3 Microbe<$NitriteReducingBacteria>")
    }

    p2.stdAction("TradeAction", 1) {
      doWithoutAutoExec(p2) {
        doTask("Trade<Enceladus>")
        doTask("-TradeBarrier")
        doTask("Microbe<$RegolithEaters>")
        shouldThrow<TaskException> { p1.doTask("Microbe<$NitriteReducingBacteria>") }
        p2.selectTask("Microbe<Player1>.")
        p1.doTask("Microbe<$NitriteReducingBacteria>")
      }
    }

    p1.count("Microbe<$NitriteReducingBacteria>") shouldBe 7
    p2.count("Microbe<$RegolithEaters>") shouldBe 1
  }

  @Test
  internal fun `Pluto draws a card before requiring its discard`() {
    newGame(
        ColoniesExpansion,
        colonyTiles = testColonyTiles(players = 2, "Pluto"),
    )
    val p2 = requireP2()
    p1.runOperation("Colony<Pluto>")
    p1.runOperation("-2 ProjectCard")
    p2.runOperation("3 Energy")
    admin.phase("Action")

    p2.stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }

    p1.count("ProjectCard") shouldBe 0
    p2.count("ProjectCard") shouldBe 1
  }

  @Test
  internal fun `Pluto bonuses finish one at a time per owner`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(players = 2, "Pluto"))
    val p2 = requireP2()
    p1.runOperation("37 MC, ProjectCard")
    p2.runOperation("3 Energy, Colony<Pluto>")
    p2.runOperation("-2 ProjectCard")
    admin.phase("Action")
    p1.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    p1.playProject(ResearchColony, 20) { doTask("Colony<Pluto>") }
    p1.runOperation("-${p1.count("ProjectCard")} ProjectCard")
    p1.count("ProjectCard") shouldBe 0
    p1.autoExecPolicy = NONE

    p2.stdAction("TradeAction", 2) {
      doWithoutAutoExec(p2) {
        doTask("Trade<Pluto>")
        doTask("-TradeBarrier")
        doTask("2 ProjectCard")
        doTask("PlutoLock<Player1>!")
        doTask("ProjectCard<Player1>")
        shouldThrow<LimitsException> { doTask("PlutoLock<Player1>!") }
        shouldThrow<TaskException> { doTask("ProjectCard<Player1>") }
        p1.count("ProjectCard") shouldBe 1

        // Another owner's bonus remains available while Player1 must discard.
        doTask("PlutoLock<Player2>!")
        doTask("ProjectCard<Player2>")
        p2.count("ProjectCard") shouldBe 3
        doTask("-ProjectCard<Player2>")
        doTask("-PlutoLock<Player2>!")

        doTask("-ProjectCard<Player1>")
        doTask("-PlutoLock<Player1>!")
        doTask("PlutoLock<Player1>!")
        doTask("ProjectCard<Player1>")
        p1.count("ProjectCard") shouldBe 1
        doTask("-ProjectCard<Player1>")
        doTask("-PlutoLock<Player1>!")
      }
    }

    p1.count("ProjectCard") shouldBe 0
    p2.count("ProjectCard") shouldBe 2
    admin.count("PlutoLock<Anyone>") shouldBe 0
  }

  @Test
  internal fun `Two Pluto bonuses complete with normal autoexecution`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(players = 2, "Pluto"))
    val p2 = requireP2()
    p1.runOperation("37 MC, ProjectCard")
    p2.runOperation("3 Energy")
    admin.phase("Action")
    p1.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    p1.playProject(ResearchColony, 20) { doTask("Colony<Pluto>") }

    p2.stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }
        .expect("0 ProjectCard<Player1>, 2 ProjectCard<Player2>, 0 PlutoLock<Anyone>")
  }

  @Test
  internal fun `Europa can be colonized after the last ocean is placed`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(players = 2, "Europa"))
    p1.runOperation("17 MC")
    val waterAreas = p1.list("WaterArea").take(9)
    p1.runOperation(waterAreas.joinToString { "OceanTile<$it>" })
    admin.phase("Action")
    val ratingBefore = p1.count("TerraformRating")

    p1.stdProject("BuildColonyProject") { doTask("Colony<Europa>") }
        .expect("Colony<Europa>, 0 OceanTile")

    p1.count("TerraformRating") shouldBe ratingBefore
    admin.count("OceanTile") shouldBe 9
  }

  @Test
  internal fun `A player can trade with Enceladus without a card that stores microbes`() {
    newGame(
        ColoniesExpansion,
        PromoCardPack,
        colonyTiles = testColonyTiles(players = 2, "Enceladus"),
    )
    val p2 = requireP2()
    p1.runOperation("13 MC, ProjectCard")
    p2.runOperation("3 Energy")
    admin.phase("Action")
    p1.playProject(RegolithEaters, 13)

    p2.stdAction("TradeAction", 2) { doTask("Trade<Enceladus>") }.expect("0 Microbe<Anyone>")

    p2.count("Trade<Enceladus>") shouldBe 1
  }

  @Test
  internal fun `Two Titan colonies can put their bonus floaters on different cards`() {
    newGame(
        ColoniesExpansion,
        VenusNextExpansion,
        colonyTiles = testColonyTiles(players = 2, "Titan"),
    )
    val p2 = requireP2()
    p1.runOperation("66 MC, 3 ProjectCard")
    p2.runOperation("11 MC, ProjectCard, 3 Energy")
    admin.phase("Action")
    p1.playProject(ForcedPrecipitation, 8)
    p1.playProject(ExtractorBalloons, 21)
    p1.stdProject("BuildColonyProject") {
      doTask("Colony<Titan>")
      addCardResources(ForcedPrecipitation, 3)
    }
    p1.playProject(ResearchColony, 20) {
      doTask("Colony<Titan>")
      addCardResources(ExtractorBalloons, 3)
    }
    p2.playProject(Dirigibles, 11)
    val precipitationBefore = p1.count("Floater<$ForcedPrecipitation>")
    val balloonsBefore = p1.count("Floater<$ExtractorBalloons>")

    p2.stdAction("TradeAction", 2) {
      doWithoutAutoExec(p2) {
        doTask("Trade<Titan>")
        doTask("-TradeBarrier")
        doTask("Floater<$Dirigibles>")
        doTask("Floater<Player1>")
        p1.addCardResources(ForcedPrecipitation)
        doTask("Floater<Player1>")
        p1.addCardResources(ExtractorBalloons)
      }
    }

    p1.count("Floater<$ForcedPrecipitation>") shouldBe precipitationBefore + 1
    p1.count("Floater<$ExtractorBalloons>") shouldBe balloonsBefore + 1
  }
}
