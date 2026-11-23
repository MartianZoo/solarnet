package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.AutoExecPolicy.SOLE_TASK
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

/**
 * Engine contract for `LATER` and `FINALLY` (rules L2-18 and L2-19): when deferred work may run.
 */
internal class DeferredWorkTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
              ABSTRACT CLASS Choice : Owned<Player> {
                CLASS ChoiceA
                CLASS ChoiceB
              }
              CLASS Start : Owned<Player> {
                This: Choice
                This:: 2 Flag
              }
              CLASS Flag : Owned<Player>
              CLASS Cleanup : Owned<Player>
              CLASS First : Owned<Player> { This: Reaction, LATER { Second } }
              CLASS Reaction : Owned<Player>
              CLASS Second : Owned<Player>
              CLASS Done : Owned<Player>
              CLASS Flow : System {
                -WorkScope<Player1>: FINALLY Player2 { Done }
              }
              CLASS WorkScope<Player> : Scope, System {
                HAS MAX 1 This, =1 Allocation<This>
                This: SOON Player2 { Choice }, FINALLY { -This }
                -This: SOON Player1 { Cleanup, LATER { Second } }
              }
              CLASS Allocation<WorkScope> : System
              """,
              players = 2,
          )
      )
  private val agents = Agents(game) { EAGER }
  private val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
  private val p2 = agents[PLAYER2].also { it.autoExecPolicy = NONE }

  @Test
  internal fun `deferred work waits until no ordinary work remains`() {
    p1.addTasks("Start, LATER { Cleanup }")
    p1.doTask("Start")

    val cleanup = p1.tasks.matching { it.deferred }.single()
    shouldThrow<TaskException> { p1.selectTask(cleanup) }.message shouldContain "deferred"
    p1.doTask("ChoiceA")
    p1.doTask("Cleanup")

    p1.count("Cleanup") shouldBe 1
  }

  @Test
  internal fun `an operation admits the work its dispatch defers`() {
    p1.beginOperation("Start, LATER { Cleanup }") {
      p1.doTask("ChoiceA")
      p1.doTask("Cleanup")
    }

    p1.count("ChoiceA") shouldBe 1
    p1.count("Cleanup") shouldBe 1
  }

  @Test
  internal fun `automatic execution waits to perform deferred work`() {
    p1.autoExecPolicy = SOLE_TASK
    p1.addTasks("Choice, LATER { Cleanup }")

    p1.autoExecNow()
    p1.count("Cleanup") shouldBe 0

    p1.doTask("ChoiceB")
    p1.count("Cleanup") shouldBe 1
  }

  @Test
  internal fun `deferred work waits for every actor's work`() {
    p2.addTasks("Choice")
    p1.addTasks("LATER { Cleanup }")

    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p2.doTask("ChoiceA")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `pending deferred siblings wait for newly created ordinary work`() {
    p1.addTasks("Choice, LATER { First }, LATER { Cleanup }")
    p1.doTask("ChoiceA")
    p1.doTask("First")

    // First creates ordinary Reaction work, which now precedes the still-deferred Cleanup.
    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("Reaction")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `newly deferred work shares priority with existing deferred work`() {
    p1.addTasks("Choice, LATER { First }, LATER { Cleanup }")
    p1.doTask("ChoiceA")
    p1.doTask("First")

    shouldThrow<TaskException> { p1.doTask("Second") }
    p1.doTask("Reaction")
    // Second was queued later than Cleanup, but both are LATER work.
    p1.doTask("Second")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `a deferred instruction is evaluated when selected`() {
    p1.addTasks("Start, LATER { Done / Flag }")
    p1.doTask("Start")
    p1.doTask("ChoiceA")
    p1.autoExecNow(SOLE_TASK)

    p1.count("Done") shouldBe 2
  }

  @Test
  internal fun `a deferred fanout enumerates when selected`() {
    p1.addTasks("Start, LATER { EACH Flag<Anyone> { Done } }")
    p1.doTask("Start")
    p1.doTask("ChoiceA")
    p1.doTask("EACH Flag<Anyone> { Done }")
    p1.doTask("Done")
    p1.doTask("Done")

    p1.count("Done") shouldBe 2
  }

  @Test
  internal fun `a deferred group admits independent tasks`() {
    p1.addTasks("Choice, LATER { Cleanup, Done }")
    p1.doTask("ChoiceA")

    p1.doTask("Done")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `a nested LATER shares the existing deferred priority`() {
    p1.addTasks("Choice, LATER { LATER { Cleanup } }, LATER { First }")
    p1.doTask("ChoiceA")

    // Nesting does not create another priority or preserve an earlier batch.
    p1.doTask("Cleanup")
    p1.doTask("First")
    p1.doTask("Reaction")
    p1.doTask("Second")
  }

  @Test
  internal fun `final work waits for deferred work and everything it defers`() {
    p1.addTasks("Choice, LATER { First }, FINALLY { Cleanup }")
    p1.doTask("ChoiceA")

    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("First")
    p1.doTask("Reaction")
    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("Second")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `fixed scope cleanup finishes before its final continuation`() {
    val admin = agents[ADMIN]
    admin.runOperation("Flow")
    admin.beginOperation("WorkScope<Player1>")
    admin.count("Allocation") shouldBe 1
    shouldThrow<TaskException> { admin.doTask("-WorkScope<Player1>") }

    p2.doTask("ChoiceA")

    // Nothing earlier remains, so Admin performs the eligible FINALLY cleanup itself.
    admin.count("WorkScope") shouldBe 0
    admin.count("Allocation") shouldBe 0
    shouldThrow<TaskException> { p2.doTask("Done") }

    p1.doTask("Cleanup")
    shouldThrow<TaskException> { p2.doTask("Done") }
    p1.doTask("Second")
    p2.doTask("Done")
    p1.count("Cleanup") shouldBe 1
    p1.count("Second") shouldBe 1
    p2.count("Done") shouldBe 1
  }

  @Test
  internal fun `pending final siblings wait for new LATER work`() {
    p1.addTasks("FINALLY { First }, FINALLY { Cleanup }")
    p1.doTask("First")

    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("Reaction")
    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("Second")
    p1.doTask("Cleanup")
  }

  @Test
  internal fun `rollback restores deferral`() {
    p1.addTasks("Choice, LATER { Cleanup }")
    val beforeChoice = game.timeline.checkpoint()
    p1.doTask("ChoiceA")

    game.timeline.seek(beforeChoice)
    game.timeline.truncate()

    shouldThrow<TaskException> { p1.doTask("Cleanup") }
    p1.doTask("ChoiceB")
    val beforeSelection = game.timeline.checkpoint()
    val deferredTasks = game.tasks.extract { it }
    p1.selectOnlyTask()
    game.timeline.seek(beforeSelection)
    game.timeline.truncate()
    game.tasks.extract { it } shouldBe deferredTasks
    p1.doTask("Cleanup")
  }
}
