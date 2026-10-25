package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.TaskForm
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class AgentNarrowingExperimentTest {
  // Test-only composition of existing local choices. Never scan a numeric domain to completion.
  private fun reduce(form: TaskForm): Int {
    var reductions = 0
    while (true) {
      val decision = form.decisions().singleOrNull() ?: return reductions
      val choices = form.options(decision).take(2).toList()
      if (choices.size != 1) return reductions
      form.choose(decision, choices.single())
      reductions++
    }
  }

  private fun world() =
      Engine.newGame(
          testGamePremise(
              """
      ABSTRACT CLASS Resource
      CLASS Foo : Resource
      CLASS Bar : Resource
      CLASS Reward
      """
          )
      )

  @Test
  fun automaticDraftReductionCanRemainPartialWithoutChangingWorld() {
    val world = world()
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("3 Bar")
    val id = agent.addTasks("-X! Resource THEN X! Reward").single()
    val before = world.events.nextOrdinal
    val form = agent.fillInTask(id)
    reduce(form) shouldBe 1
    form.options(form.decisions().single()).toList() shouldBe listOf("1", "2", "3")
    world.events.nextOrdinal shouldBe before
    agent.count("Bar") shouldBe 3
  }

  @Test
  fun typeAndAmountReductionsCanComposeWithoutExecuting() {
    val world = world()
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("Bar")
    val id = agent.addTasks("-X! Resource THEN X! Reward").single()
    val before = world.events.nextOrdinal
    val form = agent.fillInTask(id)
    reduce(form) shouldBe 2
    form.decisions().isEmpty() shouldBe true
    world.events.nextOrdinal shouldBe before
    agent.count("Bar") shouldBe 1
    form.commit()
    agent.count("Bar") shouldBe 1
    agent.doTask("-Bar")
    agent.count("Bar") shouldBe 0
    agent.count("Reward") shouldBe 0
    agent.doTask("Reward!")
    agent.count("Reward") shouldBe 1
  }

  @Test
  fun unboundedAmountIsNotMaterializedOrMistakenForUnique() {
    val world = world()
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val id = agent.addTasks("X! Reward").single()
    val form = agent.fillInTask(id)
    reduce(form) shouldBe 0
    form.choose(form.decisions().single(), "1000000000")
    agent.count("Reward") shouldBe 0
  }

  @Test
  fun existingPlayerPoliciesWaitForAnExplicitRequestBeforeTransactionInference() {
    for (policy in listOf(NONE, EAGER)) {
      val world =
          Engine.newGame(
              testGamePremise(
                  """
          ABSTRACT CLASS Choice
          CLASS Allowed : Choice
          CLASS Forbidden : Choice { HAS MAX 0 This }
          """
              )
          )
      val engine = world.actorEngine(PLAYER1)
      engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!")))
      val agents = Agents(world) { policy }
      agents[PLAYER1].count("Allowed") shouldBe 0
      agents[PLAYER1].autoExecNow()
      agents[PLAYER1].count("Allowed") shouldBe 0
      agents[PLAYER1].doTask("Choice!")
      agents[PLAYER1].count("Allowed") shouldBe 1
    }
  }

  @Test
  fun `BUG - optional amount menu hides a gain repaired by its immediate effect`() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        CLASS Repair { HAS MAX 0 This; This:: Notice, -This }
        CLASS Notice
        """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    val task = agent.addTasks("Repair?").single()
    val form = agent.fillInTask(task)
    form.options(form.decisions().single()).toList() shouldBe listOf("Ok")

    // The menu is incomplete: a fully specified request succeeds and has a different result.
    // Therefore the reducer above is evidence only, never an automatic-execution policy.
    agent.doTask("Repair!")
    agent.count("Repair") shouldBe 0
    agent.count("Notice") shouldBe 1
  }
}
