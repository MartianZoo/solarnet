package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.VenusNextExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.Dirigibles
import dev.martianzoo.tfm.tests.cards.cardnames.ExtractorBalloons
import dev.martianzoo.tfm.tests.cards.cardnames.ForcedPrecipitation
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Passing characterizations of known incorrect behavior in Terraforming Mars rules. */
internal class BugsTest : CardTest() {
  // BGG two-player award rules discussion:
  // https://boardgamegeek.com/thread/1762387/article/25616311#25616311
  @Test
  internal fun `SecondPlace incorrectly remains active with only two players`() {
    val twoPlayers = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2")))
    val threePlayers =
        Engine.newGame(Canon.gamePremise(GameConfig("", "Player1", "Player2", "Player3")))

    twoPlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
    threePlayers.classTable.allClassNames.shouldContain(cn("SecondPlace"))
  }

  @Test
  internal fun `Two Pluto colonies incorrectly draw both cards before either discard`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(players = 2, "Pluto"))
    val p2 = requireP2()
    p1.runOperation("37 MC, ProjectCard")
    p2.runOperation("3 Energy")
    admin.phase("Action")
    p1.stdProject("BuildColonyProject") { doTask("Colony<Pluto>") }
    p1.playProject(ResearchColony, 20) { doTask("Colony<Pluto>") }
    p1.runOperation("-${p1.count("ProjectCard")} ProjectCard")
    p1.count("ProjectCard") shouldBe 0
    p1.autoExecPolicy = NONE

    p2.stdAction("TradeAction", 2) {
      doWithoutAutoExec(p2) {
        doTask("Trade<Pluto>")
        doTask("-TradeBarrier<Pluto>")
        doTask("2 ProjectCard")
        doTask("2 ProjectCard<Player1>")
        p1.count("ProjectCard") shouldBe 2
        doTask("-2 ProjectCard<Player1>")
      }
    }

    p1.count("ProjectCard") shouldBe 0
    p2.count("ProjectCard") shouldBe 2
  }

  @Test
  internal fun `Two Titan colonies incorrectly require both bonus floaters on one card`() {
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
        doTask("-TradeBarrier<Titan>")
        doTask("Floater<$Dirigibles>")
        shouldThrow<TaskException> { doTask("Floater<Player1>") }
        doTask("2 Floater<Player1>")
        p1.addCardResources(ForcedPrecipitation, 2)
      }
    }

    p1.count("Floater<$ForcedPrecipitation>") shouldBe precipitationBefore + 2
    p1.count("Floater<$ExtractorBalloons>") shouldBe balloonsBefore
  }
}
