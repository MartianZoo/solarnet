package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.ResearchColony
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
  internal fun `Two Pluto colonies incorrectly allow both draws before either discard`() {
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
        doTask("ProjectCard<Player1>")
        doTask("ProjectCard<Player1>")
        p1.count("ProjectCard") shouldBe 2
        doTask("-ProjectCard<Player1>")
        doTask("-ProjectCard<Player1>")
      }
    }

    p1.count("ProjectCard") shouldBe 0
    p2.count("ProjectCard") shouldBe 2
  }
}
