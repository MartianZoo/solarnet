package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ExplicitThenBindingTest {
  @Test
  fun scaledSharedXUsesExplicitBinding() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.addTasks("3X Foo THEN X Bar")
    val task = agent.tasks.extract { it }.single()
    val before = world.events.nextOrdinal

    shouldThrow<TaskException> { agent.doTask("3 Foo") }
    world.events.nextOrdinal shouldBe before
    world.tasks.getTaskData(task.id) shouldBe task

    agent.selectTask(task.bindXTo(3))
    agent.count("Foo") shouldBe 0
    agent.doTask("9 Foo")
    agent.count("Foo") shouldBe 9
    agent.count("Bar") shouldBe 0
    agent.doTask("3 Bar")
    agent.count("Bar") shouldBe 3
  }
}
