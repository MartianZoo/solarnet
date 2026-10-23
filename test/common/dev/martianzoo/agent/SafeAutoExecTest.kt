package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SafeAutoExecTest {
  @Test
  internal fun adminHandlingCannotBeDisabledByPolicyOrScopedOverride() {
    val agents = Agents(Engine.newGame(testGamePremise("CLASS Token"))) { NONE }
    val admin = agents[ADMIN]
    shouldThrow<IllegalArgumentException> { admin.autoExecPolicy = NONE }
    shouldThrow<IllegalArgumentException> { admin.autoExecNow(NONE) }
    admin.addTasks("Token")
    admin.count("Token") shouldBe 1
  }

  @Test
  internal fun constructionSetsActorPoliciesWithoutExecutingPendingWork() {
    val game =
        Engine.newGame(testGamePremise("CLASS Token<Owner>\nCLASS Notice : System", players = 2))
    val manual = Agents(game) { NONE }
    manual[ADMIN].addTasks("Notice!")
    manual[PLAYER1].addTasks("Token<Player1>!, 2! Token<Player1>")
    manual[PLAYER2].addTasks("Token<Player2>!")
    val before = game.events.nextOrdinal

    val agents = Agents(game) { if (it == ADMIN) EAGER else CONCRETE }

    game.events.nextOrdinal shouldBe before
    agents[PLAYER1].autoExecNow()
    agents[ADMIN].count("Notice") shouldBe 1
    agents[PLAYER2].count("Token<Player2>") shouldBe 1
    agents[PLAYER1].count("Token<Player1>") shouldBe 0

    agents[PLAYER1].autoExecNow(EAGER)
    agents[PLAYER1].count("Token<Player1>") shouldBe 3
    agents[PLAYER1].autoExecPolicy shouldBe CONCRETE
    game.isIdle() shouldBe true
  }

  @Test
  internal fun automaticPlayerWaitsForAManualPlayerToEnableItsTask() {
    val game = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward", players = 2))
    val agents = Agents(game)
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val p2 = agents[PLAYER2].also { it.autoExecPolicy = NONE }
    p1.addTasks("Token: Reward")
    val enablingTask = p2.addTasks("Token").single()
    p2.canSelectTask(enablingTask) shouldBe true

    p1.autoExecPolicy = EAGER
    p1.count("Reward") shouldBe 0
    p2.count("Token") shouldBe 0
    p2.canSelectTask(enablingTask) shouldBe true

    p2.doTask("Token", enablingTask)
    p1.count("Reward") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  internal fun adminDoesNotLockAnAbstractTaskWhileAManualPlayerCanAct() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Token
                ABSTRACT CLASS Choice : System {
                  CLASS Left
                  CLASS Right
                }
                """
                    .trimIndent()
            )
        )
    val agents = Agents(game)
    val player = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val admin = agents[ADMIN]
    player.addTasks("Token!")
    admin.addTasks("Choice!")

    player.autoExecNow()

    game.tasks.selectedTask() shouldBe null
    player.doTasks("Token")
    player.count("Token") shouldBe 1
    admin.doTasks("Left")
    admin.count("Left") shouldBe 1
    game.isIdle() shouldBe true
  }

  @Test
  internal fun adminRetriesANegativeSystemAmountAfterTheLaterGain() {
    val game = Engine.newGame(testGamePremise("CLASS Point : System"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }
    val before = game.events.nextOrdinal
    player.addTasks("-2! Point, 5! Point")

    player.doTasks()

    player.count("Point") shouldBe 3
    game.events.changesSince(before).map { it.actor } shouldBe listOf(ADMIN, ADMIN)
    game.isIdle() shouldBe true
    player.autoExecPolicy shouldBe NONE
  }

  @Test
  internal fun unsuccessfulAdminPassThrowsAndRestoresTheCall() {
    val game = Engine.newGame(testGamePremise("CLASS Point : System"))
    val player = Agents(game) { NONE }[PLAYER1]
    val before = game.events.nextOrdinal

    shouldThrow<LimitsException> { player.addTasks("-2 Point") }

    player.count("Point") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.events.nextOrdinal shouldBe before
  }

  @Test
  internal fun rejectedAdminEffectsRestoreTheWholeCallAfterOtherWorkSucceeds() {
    val game =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Rejected : System {
                  This:: Notice!
                  This: Die
                }
                CLASS Notice
                CLASS Reward : System
                """
            )
        )
    val player = Agents(game) { NONE }[PLAYER1]
    val before = game.events.nextOrdinal

    shouldThrow<DeadEndException> { player.addTasks("Rejected, Reward") }

    player.count("Reward") shouldBe 0
    player.count("Rejected") shouldBe 0
    player.count("Notice") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.events.nextOrdinal shouldBe before
  }

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
    val taskId = player.addTasks("2! Token").single()
    val task = game.tasks.getTaskData(taskId)
    val before = game.events.nextOrdinal

    player.autoExecNow()
    game.events.nextOrdinal shouldBe before

    shouldThrow<LimitsException> { player.autoExecNow(CONCRETE) }

    game.events.nextOrdinal shouldBe before
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
  internal fun scopedPolicyExecutesWorkWithoutChangingTheConfiguredPolicyInsideTheBlock() {
    val game = Engine.newGame(testGamePremise("CLASS Token"))
    val player = Agents(game)[PLAYER1].also { it.autoExecPolicy = NONE }

    player.addTasks("Token!")
    player.completeOperation {
      player.count("Token") shouldBe 0
      doWithAutoExecPolicy(EAGER) {
        player.autoExecPolicy shouldBe NONE
        player.count("Token") shouldBe 1
      }
    }

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
