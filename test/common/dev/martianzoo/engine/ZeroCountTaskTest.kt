package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ZeroCountTaskTest {
  private val game =
      Engine.newGame(
          testGamePremise(
              """
    CLASS Foo { This:: Notice }
    CLASS Bar
    CLASS Meter
    CLASS Notice
    CLASS Following
    CLASS Silent { This: 0 Foo }
    CLASS Holder { HAS MAX 1 This }
    CLASS Held<Holder>
  """
          )
      )
  private val agent = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }

  @Test
  fun `zero identifies the matching metric task without firing gain effects`() {
    val foo = agent.addTasks("Foo / Meter").single()
    val bar = agent.addTasks("Bar / Meter").single()

    agent.doTask("0 Foo").changes.isEmpty() shouldBe true

    game.tasks.ids() shouldBe setOf(bar)
    (foo in game.tasks) shouldBe false
    agent.count("Foo") shouldBe 0
    agent.count("Notice") shouldBe 0
  }

  @Test
  fun `zero cannot complete a positive mandatory metric task`() {
    agent.runOperation("Meter")
    agent.addTasks("Foo / Meter")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> { agent.doTask("0 Foo") }

    game.events.nextOrdinal shouldBe before
    agent.doTask("Foo")
    agent.count("Foo") shouldBe 1
    agent.count("Notice") shouldBe 1
  }

  @Test
  fun `zero declines only the named optional gain`() {
    agent.addTasks("3? Foo")
    val bar = agent.addTasks("3? Bar").single()

    agent.doTask("0 Foo")

    game.tasks.ids() shouldBe setOf(bar)
    agent.count("Foo") shouldBe 0
  }

  @Test
  fun `zero identifies its metric task beside an unrelated Ok alternative`() {
    agent.addTasks("Foo / Meter")
    val bar = agent.addTasks("Bar OR Ok").single()

    agent.doTask("0 Foo").changes.isEmpty() shouldBe true

    game.tasks.ids() shouldBe setOf(bar)
  }

  @Test
  fun `zero cannot dismiss an unrelated Ok alternative`() {
    agent.addTasks("Bar OR Ok")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> { agent.doTask("0 Foo") }

    game.events.nextOrdinal shouldBe before
    agent.doTask("Ok")
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `zero cannot dismiss an unrelated selected Ok alternative`() {
    agent.addTasks("Bar OR Ok")
    agent.selectTask("Bar OR Ok")
    val before = game.events.nextOrdinal

    shouldThrow<NarrowingException> { agent.doTask("0 Foo") }

    game.events.nextOrdinal shouldBe before
    agent.doTask("Ok")
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `zero identifies its metric task beside an unavailable optional gain`() {
    agent.addTasks("Foo / Meter")
    val held = agent.addTasks("Held?").single()

    agent.doTask("0 Foo").changes.isEmpty() shouldBe true

    game.tasks.ids() shouldBe setOf(held)
  }

  @Test
  fun `zero cannot select an unrelated Ok alternative at the start of a sequence`() {
    agent.addTasks("(Bar OR Ok) THEN Following")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> { agent.doTask("0 Foo") }

    game.events.nextOrdinal shouldBe before
    agent.doTask("Ok")
    agent.doTask("Following")
    agent.count("Following") shouldBe 1
  }

  @Test
  fun `typed zero completes its metric task and admits its continuation`() {
    agent.addTasks("Foo / Meter THEN Following")

    agent.doTask("0 Foo").changes.isEmpty() shouldBe true
    agent.doTask("Following")

    agent.count("Following") shouldBe 1
    agent.count("Notice") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `a zero batch total still executes offsetting changes and their effects`() {
    agent.addTasks("Foo, -Foo")

    agent.doTasks("0 Foo")

    agent.count("Foo") shouldBe 0
    agent.count("Notice") shouldBe 1
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `Ok still completes a zero metric task and admits its continuation`() {
    agent.addTasks("Foo / Meter THEN Following")

    agent.doTask("Ok")
    agent.doTask("Following")

    agent.count("Following") shouldBe 1
    agent.count("Notice") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `zero does not need an existing holder`() {
    agent.addTasks("Held / Meter")

    agent.doTask("0 Held").changes.isEmpty() shouldBe true

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `an automatic choice between zero changes needs no decision`() {
    agent.runOperation("Foo / Meter OR Bar / Meter").changes.isEmpty() shouldBe true

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `zero can select its named arm without taking a positive alternative`() {
    agent.addTasks("Foo / Meter OR Bar")

    agent.doTask("0 Foo").changes.isEmpty() shouldBe true

    agent.count("Bar") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `distinct zero tasks for the same type remain ambiguous`() {
    agent.addTasks("Foo / Meter, Foo / Bar")
    val before = game.events.nextOrdinal

    shouldThrow<TaskException> { agent.doTask("0 Foo") }

    game.events.nextOrdinal shouldBe before
  }

  @Test
  fun `an authored zero effect creates no gain or follow-up work`() {
    agent.runOperation("Silent")

    agent.count("Foo") shouldBe 0
    agent.count("Notice") shouldBe 0
    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun `literal zero changes do not create pending work or require sources and holders`() {
    agent.runOperation("0 Foo, -0 Bar, 0 Held FROM Foo").changes.isEmpty() shouldBe true

    game.tasks.isEmpty() shouldBe true
    agent.count("Notice") shouldBe 0
  }

  @Test
  fun `zero does not bind a positive X`() {
    agent.addTasks("X Foo")

    shouldThrow<TaskException> { agent.doTask("0 Foo") }

    agent.doTask("2 Foo")
    agent.count("Foo") shouldBe 2
  }
}
