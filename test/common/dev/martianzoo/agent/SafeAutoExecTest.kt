package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SafeAutoExecTest {
  @Test
  internal fun safeLeavesAChoiceBetweenTasksPendingAcrossActors() {
    val game =
        Engine.newGame(testGamePremise("CLASS Token<Owner>\nCLASS Marker<Owner>", players = 2))
    val agents = createAgents(game)
    val p1 = agents.getValue(PLAYER1).also { it.autoExecPolicy = NONE }
    val p2 = agents.getValue(PLAYER2).also { it.autoExecPolicy = NONE }
    val taskIds = p1.addTasks("Token<Player1>") + p2.addTasks("Marker<Player2>")

    p1.autoExecPolicy = CONCRETE

    p1.count("Token<Player1>") shouldBe 0
    p2.count("Marker<Player2>") shouldBe 0
    game.tasks.ids() shouldBe taskIds.toSet()
    game.tasks.extract { it.selected }.all { !it } shouldBe true
  }

  @Test
  internal fun safeSelectsAnAbstractSingletonWithoutChoosingItsNarrowing() {
    val game = Engine.newGame(testGamePremise("ABSTRACT CLASS Choice { CLASS Left, Right }"))
    val player = createAgents(game).getValue(PLAYER1).also { it.autoExecPolicy = NONE }
    val taskId = player.addTasks("Choice").single()

    player.autoExecPolicy = CONCRETE

    player.count("Left") shouldBe 0
    player.count("Right") shouldBe 0
    val task = game.tasks.extract { it }.single()
    task.id shouldBe taskId
    task.selected shouldBe true
    task.instruction.isAbstract(player.reader) shouldBe true
  }
}
