package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NotFullySpecifiedException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeInstanceOf
import kotlin.test.Test

internal class TransactionUniquenessExperimentTest {
  private fun trial(source: String, work: (World) -> Unit) {
    work(Engine.newGame(testGamePremise(source)))
  }

  @Test
  fun uniqueSuccessfulTypeUsesFinalInvariantChecks() =
      trial(
          """
      ABSTRACT CLASS Choice
      CLASS Allowed : Choice
      CLASS Forbidden : Choice { HAS MAX 0 This }
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
        agent.count("Allowed") shouldBe 1
        agent.count("Forbidden") shouldBe 0
      }

  @Test
  fun twoSuccessesRemainAmbiguousEvenWithEqualFinalCounts() =
      trial(
          """
      ABSTRACT CLASS Choice
      CLASS First : Choice { This:: -This }
      CLASS Second : Choice { This:: -This }
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        val tasks = world.tasks.extract { it }
        val before = world.events.nextOrdinal
        val ambiguous =
            shouldThrow<NotFullySpecifiedException> {
              world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
            }
        ambiguous.shouldNotBeInstanceOf<IncompleteAnalysisException>()
        world.events.nextOrdinal shouldBe before
        world.tasks.extract { it } shouldBe tasks
        agent.count("Choice") shouldBe 0
      }

  private fun ambiguousDespiteIncomplete(source: String) =
      trial(source) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        val tasks = world.tasks.extract { it }
        val before = world.events.nextOrdinal
        val ambiguous =
            shouldThrow<NotFullySpecifiedException> {
              world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
            }
        ambiguous.shouldNotBeInstanceOf<IncompleteAnalysisException>()
        world.events.nextOrdinal shouldBe before
        world.tasks.extract { it } shouldBe tasks
        agent.count("Choice") shouldBe 0
      }

  @Test
  fun twoSuccessesProveAmbiguityWithUnfinishedCandidateFirst() =
      ambiguousDespiteIncomplete(
          """
      ABSTRACT CLASS Choice
      CLASS Unfinished : Choice { This:: Extra }
      CLASS First : Choice
      CLASS Second : Choice
      ABSTRACT CLASS Extra
      CLASS Left : Extra
      CLASS Right : Extra
      """
      )

  @Test
  fun twoSuccessesProveAmbiguityWithUnfinishedCandidateLast() =
      ambiguousDespiteIncomplete(
          """
      ABSTRACT CLASS Choice
      CLASS First : Choice
      CLASS Second : Choice
      CLASS Unfinished : Choice { This:: Extra }
      ABSTRACT CLASS Extra
      CLASS Left : Extra
      CLASS Right : Extra
      """
      )

  @Test
  fun noSuccessfulCandidateRestoresState() =
      trial(
          """
      ABSTRACT CLASS Choice
      CLASS First : Choice { HAS MAX 0 This }
      CLASS Second : Choice { HAS MAX 0 This }
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        val before = world.events.nextOrdinal
        shouldThrow<NotNowException> {
          world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
        }
        world.events.nextOrdinal shouldBe before
        agent.count("Choice") shouldBe 0
      }

  @Test
  fun unresolvedImmediateEffectIsNotEliminatedAsIllegal() =
      trial(
          """
      ABSTRACT CLASS Choice
      CLASS First : Choice
      CLASS Second : Choice { This:: Extra }
      ABSTRACT CLASS Extra
      CLASS Left : Extra
      CLASS Right : Extra
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        val tasks = world.tasks.extract { it }
        val before = world.events.nextOrdinal
        shouldThrow<IncompleteAnalysisException> {
          world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
        }
        world.events.nextOrdinal shouldBe before
        world.tasks.extract { it } shouldBe tasks
        agent.count("Choice") shouldBe 0
      }

  @Test
  fun laterAdminFailureDoesNotEliminateCurrentCandidate() =
      trial(
          """
      ABSTRACT CLASS Choice
      CLASS Allowed : Choice { This: QueuedFailure BY Admin }
      CLASS Forbidden : Choice { HAS MAX 0 This }
      CLASS QueuedFailure { HAS MAX 0 This }
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        val id = agent.addTasks("Choice!").single()
        world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Choice!"), taskId = id)
        agent.count("Allowed") shouldBe 1
        val admin = world.actorEngine(ADMIN)
        shouldThrow<LimitsException> { admin.doTask(admin.tasks.ids().single()) }
        agent.count("Allowed") shouldBe 1
        agent.count("QueuedFailure") shouldBe 0
      }

  @Test
  fun taskMatchingDoesNotTryDifferentContinuations() =
      trial(
          """
      CLASS Foo
      CLASS Bar { HAS MAX 0 This }
      CLASS Baz
      """
      ) { world ->
        val agent = Agents(world)[PLAYER1].also { it.autoExecPolicy = NONE }
        agent.addTasks("Foo! THEN Bar!")
        agent.addTasks("Foo! THEN Baz!")
        val before = world.events.nextOrdinal
        shouldThrow<TaskException> {
          world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Foo!"))
        }
        world.events.nextOrdinal shouldBe before
        agent.count("Foo") shouldBe 0
      }

  @Test
  fun protectedHabitatsEliminatesOnlyProtectedVictim() {
    val world = TfmEngine.newGame(canonicalPremise(players = 3))
    val actor = world.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    actor.sneak("Plant<Player3>")
    world.testAgent(PLAYER2).sneak("ProtectedHabitats, Plant")
    val id = actor.addTasks("-Plant<Player>!").single()
    world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("-Plant<Player>!"), taskId = id)
    actor.count("Plant<Player3>") shouldBe 0
    world.reader.count(parse<Metric>("Plant<Player2>")) shouldBe 1
  }

  @Test
  fun oceanPermissionAndAreaRequestCombineWithoutSearch() {
    val world = TfmEngine.newGame(canonicalPremise())
    val actor = world.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val id = actor.addTasks("OceanTile<WaterArea>!").single()
    world.actorEngine(PLAYER1).doTask(parse<InstructionTree>("Tile<Tharsis_5_5>!"), taskId = id)
    actor.count("OceanTile<Tharsis_5_5>") shouldBe 1
  }
}
