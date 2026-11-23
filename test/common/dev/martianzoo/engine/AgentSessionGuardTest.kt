package dev.martianzoo.engine

import dev.martianzoo.agent.AgentTransactionGuard
import dev.martianzoo.agent.Agents
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AgentSessionGuardTest {
  @Test
  fun applicationValidationBelongsToTheSessionThatInstalledIt() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val checked = Agents(world)
    val independent = Agents(world)
    var supplied = false
    checked.installTransactionGuard(
        object : AgentTransactionGuard {
          override fun validate() {
            if (!supplied) throw InputRequiredException("supply application information")
          }
        }
    )
    shouldThrow<InputRequiredException> { checked[PLAYER1].runOperation("Foo!") }
    checked[PLAYER1].count("Foo") shouldBe 0
    independent[PLAYER1].runOperation("Foo!")
    checked[PLAYER1].count("Foo") shouldBe 1
    shouldThrow<InputRequiredException> { checked[PLAYER1].runOperation("Foo!") }
    checked[PLAYER1].count("Foo") shouldBe 1
    supplied = true
    checked[PLAYER1].runOperation("Foo!")
    independent[PLAYER1].count("Foo") shouldBe 2
  }
}
