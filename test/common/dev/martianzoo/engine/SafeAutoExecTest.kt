package dev.martianzoo.engine

import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.engine.AutoExecMode.SAFE
import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.data.Player.Companion.PLAYER2
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SafeAutoExecTest {
  @Test
  internal fun safeLeavesAChoiceBetweenTasksPendingAcrossActors() {
    val game =
        Engine.newGame(testGamePremise("CLASS Token<Owner>\nCLASS Marker<Owner>", players = 2))
    val p1 = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val p2 = game.agent(PLAYER2).also { it.autoExecMode = NONE }
    val taskIds = p1.addTasks("Token<Player1>") + p2.addTasks("Marker<Player2>")

    p1.autoExecMode = SAFE

    p1.count("Token<Player1>") shouldBe 0
    p2.count("Marker<Player2>") shouldBe 0
    game.tasks.ids() shouldBe taskIds.toSet()
    game.tasks.extract { it.selected }.all { !it } shouldBe true
  }

  @Test
  internal fun safeSelectsAnAbstractSingletonWithoutChoosingItsNarrowing() {
    val game = Engine.newGame(testGamePremise("ABSTRACT CLASS Choice { CLASS Left, Right }"))
    val player = game.agent(PLAYER1).also { it.autoExecMode = NONE }
    val taskId = player.addTasks("Choice").single()

    player.autoExecMode = SAFE

    player.count("Left") shouldBe 0
    player.count("Right") shouldBe 0
    val task = game.tasks.extract { it }.single()
    task.id shouldBe taskId
    task.selected shouldBe true
    task.instruction.isAbstract(player.reader) shouldBe true
  }
}
