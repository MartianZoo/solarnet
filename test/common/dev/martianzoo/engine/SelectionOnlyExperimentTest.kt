package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Instruction.By
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SelectionOnlyExperimentTest {
  @Test
  fun concreteSelectionRequiresExecution() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val task = agent.addTasks("Foo!").single()
    val engine = world.actorEngine(PLAYER1)

    engine.selectTask(task)
    world.reader.count(parse<Metric>("Foo")) shouldBe 0
    world.tasks.getTaskData(task).selected shouldBe true
    engine.doTask(task)
    agent.count("Foo") shouldBe 1
  }

  @Test
  fun manualAgentNarrowingDoesNotExecuteAndStillEvaluatesAmount() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Resource
        CLASS Foo : Resource
        CLASS Bar : Resource
        """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("3 Foo, 2 Bar")
    val id = agent.addTasks("-5* Resource").single()

    agent.selectTask(id)
    agent.narrowTask("-5* Foo")
    agent.count("Foo") shouldBe 3
    world.tasks.getTaskData(id).instruction shouldBe parse<InstructionTree>("-3! Foo")
    world.actorEngine(PLAYER1).doTask(id)
    agent.count("Foo") shouldBe 0
    agent.count("Bar") shouldBe 2
  }

  @Test
  fun computedZeroRemainsSelectedUntilExecution() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val id = agent.addTasks("Foo / Bar THEN Bar!").single()

    agent.selectTask(id)
    (id in world.tasks) shouldBe true
    agent.count("Foo") shouldBe 0
    agent.count("Bar") shouldBe 0
    world.actorEngine(PLAYER1).doTask(id)
    (id in world.tasks) shouldBe false
    world.tasks.ids().size shouldBe 1
  }

  @Test
  fun selectingEachLeavesItsChildrenUncreatedUntilExecution() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val id = agent.addTasks("EACH Player { Foo }").single()

    agent.selectTask(id)
    (id in world.tasks) shouldBe true
    world.tasks.getTaskData(id).selected shouldBe true
    world.tasks.ids().size shouldBe 1
    agent.count("Foo") shouldBe 0

    world.actorEngine(PLAYER1).doTask(id)
    (id in world.tasks) shouldBe false
    world.tasks.ids().size shouldBe 1
    agent.count("Foo") shouldBe 0
    val child = world.tasks.ids().single()
    world.tasks.getTaskData(child).selected shouldBe false
    world.actorEngine(PLAYER1).doTask(child)
    agent.count("Foo") shouldBe 1
  }

  @Test
  fun selectingAGatedEachChecksTheGateWithoutExpandingTheFanout() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("Foo")
    val id = agent.addTasks("Foo: EACH Player { Bar }").single()

    agent.selectTask(id)
    (id in world.tasks) shouldBe true
    world.tasks.ids().size shouldBe 1
    world.actorEngine(PLAYER1).doTask(id)
    (id in world.tasks) shouldBe false
    world.tasks.ids().size shouldBe 1
    agent.count("Bar") shouldBe 0
  }

  @Test
  fun selectingAGatedEachRejectsAFailedGateWithoutCreatingChildren() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar"))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val id = agent.addTasks("Foo: EACH Player { Bar }").single()

    shouldThrow<RequirementException> { agent.selectTask(id) }
    world.tasks.ids().toList() shouldBe listOf(id)
    world.tasks.getTaskData(id).selected shouldBe false
  }

  @Test
  fun byOnEachAppliesToItsChildrenWhenTheParentExecutes() {
    val world = Engine.newGame(testGamePremise("CLASS Foo\nCLASS Bar", players = 2))
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val id = agent.addTasks("EACH Player { Foo OR Bar } BY Player1").single()

    agent.selectTask(id)
    world.tasks.ids().toList() shouldBe listOf(id)
    world.tasks.getTaskData(id).selected shouldBe true

    world.actorEngine(PLAYER1).doTask(id)
    (id in world.tasks) shouldBe false
    val children = world.tasks.ids().map(world.tasks::getTaskData)
    children.size shouldBe 2
    children.forEach {
      it.selected shouldBe false
      (it.instruction as By).actor shouldBe parse<Expression>("Player1")
    }
  }
}
