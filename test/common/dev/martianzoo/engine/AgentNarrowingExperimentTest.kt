package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.TaskForm
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeInstanceOf
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

  private fun repairWorld() =
      Engine.newGame(
          testGamePremise(
              """
      CLASS Repair { HAS MAX 0 This; This:: Notice, -This }
      CLASS Notice
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
  fun mandatoryTransmutationUsesTheSoleSuccessfulSourceAndDestinationPair() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
          ABSTRACT CLASS Source
          CLASS Present : Source
          CLASS Absent : Source
          ABSTRACT CLASS Destination
          CLASS Allowed : Destination
          CLASS Forbidden : Destination { HAS MAX 0 This }
          """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("Present")
    val task = agent.addTasks("Destination FROM Source").single()

    world.actorEngine(PLAYER1).doTask(task)

    agent.count("Present") shouldBe 0
    agent.count("Allowed") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun mandatoryTransmutationRetainsAmbiguityAndRestoresItsTrialChanges() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
          ABSTRACT CLASS Source
          CLASS Present : Source
          ABSTRACT CLASS Destination
          CLASS First : Destination
          CLASS Second : Destination
          """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("Present")
    val task = agent.addTasks("Destination FROM Source").single()
    val before = world.events.nextOrdinal

    shouldThrow<NotFullySpecifiedException> { world.actorEngine(PLAYER1).doTask(task) }

    world.events.nextOrdinal shouldBe before
    world.tasks.getTaskData(task).selected shouldBe false
    agent.count("Present") shouldBe 1
    agent.count("Destination") shouldBe 0
  }

  @Test
  fun mandatoryTransmutationKeepsTheSourceAndDestinationTypeChoiceTogether() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
          ABSTRACT CLASS Place
          CLASS Red : Place
          CLASS Blue : Place
          CLASS Source<Place>
          CLASS Target<Place>
          """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("Red, Blue, Source<Red>, Source<Blue>")
    val task = agent.addTasks("Target<@Place> FROM Source<@Place>").single()

    shouldThrow<NotFullySpecifiedException> { world.actorEngine(PLAYER1).doTask(task) }

    agent.count("Source<Red>") shouldBe 1
    agent.count("Source<Blue>") shouldBe 1
    agent.count("Target<Red>") shouldBe 0
    agent.count("Target<Blue>") shouldBe 0
  }

  @Test
  fun incompatibleTypeVariablePairsDoNotMakeAdminWait() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
          ABSTRACT CLASS Place
          CLASS Red : Place
          CLASS Blue : Place
          CLASS Source<Place>
          CLASS Target<Place> { This: Die }
          CLASS Pending
          """
            )
        )
    val agents = Agents(world) { NONE }
    val agent = agents[PLAYER1]
    agent.sneak("Red, Blue, Source<Red>, Source<Blue>")
    agent.addTasks("Pending")
    val task =
        world
            .actorEngine(ADMIN)
            .addTasks(
                InstructionGroup.of(parse<InstructionTree>("Target<@Place> FROM Source<@Place>!"))
            )
            .single()

    shouldThrow<DeadEndException> { world.actorEngine(ADMIN).doTask(task) }
    shouldThrow<DeadEndException> { agents[ADMIN].autoExecNow() }

    agent.count("Source<Red>") shouldBe 1
    agent.count("Source<Blue>") shouldBe 1
    world.tasks.getTaskData(task).selected shouldBe false
  }

  @Test
  fun invalidAtomizedDestinationDoesNotHideAnotherSuccessfulPair() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
          CLASS Source
          ABSTRACT CLASS Destination
          CLASS Whole : Destination, Atomized
          CLASS Plain : Destination
          """
            )
        )
    val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
    agent.sneak("2 Source")
    val task = agent.addTasks("2 Destination FROM Source").single()

    world.actorEngine(PLAYER1).doTask(task)

    agent.count("Source") shouldBe 0
    agent.count("Plain") shouldBe 2
    agent.count("Whole") shouldBe 0
  }

  @Test
  fun `BUG - optional amount menu hides a gain repaired by its immediate effect`() {
    val world = repairWorld()
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

  @Test
  fun rawOptionalTaskRequiresAChoiceWhenZeroAndPositiveBothSucceed() {
    val unconstrainedWorld = repairWorld()
    val unconstrainedEngine = unconstrainedWorld.actorEngine(PLAYER1)
    val notice =
        unconstrainedEngine
            .addTasks(InstructionGroup.of(parse<InstructionTree>("Notice?")))
            .single()
    shouldThrow<NotFullySpecifiedException> { unconstrainedEngine.doTask(notice) }
    unconstrainedWorld.tasks.getTaskData(notice).selected shouldBe false

    val world = repairWorld()
    val engine = world.actorEngine(PLAYER1)
    val task = engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Repair?"))).single()
    val before = world.events.nextOrdinal
    shouldThrow<NotFullySpecifiedException> { engine.doTask(task) }
        .shouldNotBeInstanceOf<IncompleteAnalysisException>()
    world.events.nextOrdinal shouldBe before
    world.tasks.getTaskData(task).selected shouldBe false
    Agents(world)[PLAYER1].count("Notice") shouldBe 0

    val explicitWorld = repairWorld()
    val explicitEngine = explicitWorld.actorEngine(PLAYER1)
    val explicitTask =
        explicitEngine.addTasks(InstructionGroup.of(parse<InstructionTree>("Repair?"))).single()
    explicitEngine.doTask(parse<InstructionTree>("Repair!"), taskId = explicitTask)
    Agents(explicitWorld)[PLAYER1].count("Notice") shouldBe 1
    explicitWorld.tasks.isEmpty() shouldBe true
  }

  @Test
  fun rawAmapTaskUsesCurrentInvariantHeadroom() {
    val world = repairWorld()
    val engine = world.actorEngine(PLAYER1)
    val task = engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Repair*"))).single()

    engine.doTask(task)

    Agents(world)[PLAYER1].count("Notice") shouldBe 0
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun playerAutoexecWaitsWhileExplicitOptionalExecutionCanProveZero() {
    val world = Engine.newGame(testGamePremise("CLASS Capped { HAS MAX 0 This }"))
    val engine = world.actorEngine(PLAYER1)
    val task = engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Capped?"))).single()

    Agents(world)[PLAYER1].autoExecNow(EAGER)
    (task in world.tasks.ids()) shouldBe true
    engine.doTask(task)

    Agents(world)[PLAYER1].count("Capped") shouldBe 0
    world.tasks.isEmpty() shouldBe true
  }
}
