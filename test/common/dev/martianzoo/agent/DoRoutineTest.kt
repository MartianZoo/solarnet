package dev.martianzoo.agent

import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DoRoutineTest {
  @Test
  internal fun failureRestoresEarlierSubmissionsWithinTheRoutine() {
    val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Prize { HAS MAX 0 This }"))
    val agents =
        Agents(
            world,
            mapOf(
                "claim" to
                    Routine { _, player, _, _ ->
                      player.doTask("Token")
                      player.doTask("Prize")
                    }
            ),
        )
    val player = agents[PLAYER1]
    player.addTasks("Token!, Prize!")
    val before = world.events.nextOrdinal
    val tasksBefore = world.tasks.ids()

    shouldThrow<LimitsException> { player.doRoutine("claim") }

    player.count("Token") shouldBe 0
    world.events.nextOrdinal shouldBe before
    world.tasks.ids() shouldBe tasksBefore
  }

  @Test
  internal fun forkedDefinitionsReceiveTheForkedSessionAndActor() {
    val world = Engine.newGame(testGamePremise("CLASS Token"))
    val agents =
        Agents(
            world,
            mapOf(
                "claim" to
                    Routine { session, player, arguments, _ ->
                      session[player.actor].doTask(arguments.single())
                    }
            ),
        )
    agents[PLAYER1].addTasks("Token!")
    val fork = agents.fork()

    fork[PLAYER1].doRoutine("claim", "Token")

    fork[PLAYER1].count("Token") shouldBe 1
    fork.world.tasks.isEmpty() shouldBe true
    agents[PLAYER1].count("Token") shouldBe 0
    agents[PLAYER1].tasks.isEmpty() shouldBe false
  }
}
