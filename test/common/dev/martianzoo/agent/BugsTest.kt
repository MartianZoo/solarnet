package dev.martianzoo.agent

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.testGamePremise
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/** Passing characterizations of known incorrect Agent behavior. */
internal class BugsTest {
  @Test
  internal fun `autoexecution incorrectly throws while another player can enable its task`() {
    val game = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward", players = 2))
    val agents = Agents(game)
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
    val p2 = agents[PLAYER2].also { it.autoExecPolicy = NONE }
    p1.addTasks("Token: Reward")
    val enablingTask = p2.addTasks("Token").single()
    p2.canSelectTask(enablingTask) shouldBe true

    // P1 should wait for P2's explicit decision instead of attempting the unmet requirement.
    shouldThrow<RequirementException> { p1.autoExecPolicy = EAGER }.detail shouldContain
        "requirement `Token` is not met"
    p1.count("Reward") shouldBe 0
    p2.count("Token") shouldBe 0
    p2.canSelectTask(enablingTask) shouldBe true

    p2.doTask("Token", enablingTask)
    p1.count("Reward") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }
}
