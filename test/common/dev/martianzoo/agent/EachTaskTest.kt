package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class EachTaskTest {
  @Test
  internal fun opensTheGroupWithoutChoosingItsChildren() {
    val player =
        Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }[PLAYER1]
    player.addTasks("EACH Player { Coin OR Marker }")
    player.doTask("EACH")
    player.count("Coin") shouldBe 0
    player.count("Marker") shouldBe 0
    player.doTask("Marker")
    player.count("Marker") shouldBe 1
  }

  @Test
  internal fun rejectsAmbiguityButRespectsAnExplicitSelection() {
    val game = Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))
    val player = Agents(game) { NONE }[PLAYER1]
    player.addTasks("EACH Player { Coin }, EACH Player { Marker }")
    val before = game.events.nextOrdinal
    shouldThrow<TaskException> { player.doTask("EACH") }
    game.events.nextOrdinal shouldBe before
    player.selectTask("EACH Player { Coin }")
    player.doTasks("EACH", "Coin", "EACH", "Marker")
    player.count("Coin") shouldBe 1
    player.count("Marker") shouldBe 1
  }

  @Test
  internal fun cannotBypassAnotherSelectedTaskOrRunDeferredWork() {
    val player =
        Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }[PLAYER1]
    player.addTasks("Coin, LATER { EACH Player { Marker } }")
    shouldThrow<TaskException> { player.doTask("EACH") }
    player.doTask("Coin")
    player.addTasks("Coin")
    player.selectTask("Coin")
    shouldThrow<TaskException> { player.doTask("EACH") }
    player.count("Coin") shouldBe 1
    player.doTasks("Coin", "EACH", "Marker")
    player.count("Coin") shouldBe 2
    player.count("Marker") shouldBe 1
  }

  @Test
  internal fun doesNotTakeAnotherActorsEachTask() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Coin", players = 2))) { NONE }
    agents[PLAYER2].addTasks("EACH Player { Coin }")
    shouldThrow<TaskException> { agents[PLAYER1].doTask("EACH") }
    agents[PLAYER2].doTasks("EACH", "2 Coin")
    agents[PLAYER2].count("Coin") shouldBe 2
  }

  @Test
  internal fun anExplicitIdCanIdentifyAnEachAmongSeveral() {
    val player =
        Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }[PLAYER1]
    val coin = player.addTasks("EACH Player { Coin }").single()
    val marker = player.addTasks("EACH Player { Marker }").single()
    player.doTask("EACH", coin)
    player.doTask("Coin")
    player.count("Coin") shouldBe 1
    player.count("Marker") shouldBe 0
    player.addTasks("EACH Player { Coin }")
    player.tryTask("EACH", marker)
    player.doTask("Marker")
    player.count("Marker") shouldBe 1
  }

  @Test
  internal fun doesNotRematchAgainstADeferredTwin() {
    val player = Agents(Engine.newGame(testGamePremise("CLASS Coin"))) { NONE }[PLAYER1]
    player.addTasks("EACH Player { Coin }, LATER { EACH Player { Coin } }")
    player.doTasks("EACH", "Coin", "EACH", "Coin")
    player.count("Coin") shouldBe 2
  }

  @Test
  internal fun doesNotRematchAgainstAnEachInsideAnAlternative() {
    val player =
        Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }[PLAYER1]
    player.addTasks("EACH Player { Coin }, EACH Player { Coin } OR Marker")
    player.doTasks("EACH", "Coin")
    player.addTasks("EACH Player { Coin }")
    player.selectTask("EACH")
    player.tryTask("EACH")
    player.doTask("Coin")
    shouldThrow<TaskException> { player.doTask("EACH") }
    player.doTask("Marker")
    player.count("Coin") shouldBe 2
    player.count("Marker") shouldBe 1
  }

  @Test
  internal fun aFormCanIdentifyAnEachAmongSeveral() {
    val player =
        Agents(Engine.newGame(testGamePremise("CLASS Coin\nCLASS Marker"))) { NONE }[PLAYER1]
    val coin = player.addTasks("EACH Player { Coin }").single()
    player.addTasks("EACH Player { Marker }")
    player.fillInTask(coin).apply { narrow("EACH") }.commit()
    player.doTasks("EACH", "Coin")
    player.count("Coin") shouldBe 1
    player.count("Marker") shouldBe 0
  }
}
