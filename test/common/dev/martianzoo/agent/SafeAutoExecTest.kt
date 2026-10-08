package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SafeAutoExecTest {
  @Test
  internal fun failingSingletonRestoresItsTaskAndAutomaticEffects() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token {
                  HAS MAX 1 This
                  This:: Notice!
                }
                CLASS Notice
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val taskId = player.addTasks("2 Token!").single()
    val task = game.tasks.getTaskData(taskId)
    val before = game.timeline.checkpoint()

    player.autoExecNow()
    game.timeline.checkpoint() shouldBe before

    shouldThrow<LimitsException> { player.autoExecNow(CONCRETE) }

    game.timeline.checkpoint() shouldBe before
    game.tasks.getTaskData(taskId) shouldBe task
    player.count("Token") shouldBe 0
    player.count("Notice") shouldBe 0
    player.autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun transientPolicyRunsNowWithoutChangingTheConfiguredPolicy() {
    val game = Engine.newGame(testGamePremise("CLASS Token"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Token!")

    player.autoExecNow(CONCRETE)

    player.count("Token") shouldBe 1
    game.tasks.isEmpty() shouldBe true
    player.autoExecPolicy shouldBe NONE

    player.addTasks("Token!")
    player.autoExecNow()

    player.count("Token") shouldBe 1
    game.tasks.ids().size shouldBe 1
  }

  @Test
  internal fun safeExecutesTheAvailableTaskBeforeTheTaskItEnables() {
    val game = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    player.addTasks("Token: Reward!")
    player.addTasks("Token!")

    player.autoExecPolicy = CONCRETE

    player.count("Token") shouldBe 1
    player.count("Reward") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun safeActsOnOnlyItsOwnUnambiguousTask() {
    val game =
        Engine.newGame(testGamePremise("CLASS Token<Owner>\nCLASS Marker<Owner>", players = 2))
    val agents = Agents(game)
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val p2 = agents[PLAYER2].also { it.autoExecPolicy = NONE }
    val taskIds = p1.addTasks("Token<Player1>") + p2.addTasks("Marker<Player2>")

    p1.autoExecPolicy = CONCRETE

    p1.count("Token<Player1>") shouldBe 1
    p2.count("Marker<Player2>") shouldBe 0
    game.tasks.ids() shouldBe setOf(taskIds.last())
    game.tasks.extract { it.selected }.all { !it } shouldBe true
  }

  @Test
  internal fun safeSelectsAnAbstractSingletonWithoutChoosingItsNarrowing() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                ABSTRACT CLASS Choice {
                  CLASS Left
                  CLASS Right
                }
                """
                    .trimIndent()
            )
        )
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
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
