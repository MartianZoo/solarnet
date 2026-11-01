package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TransactionTrialIsolationTest {
  @Test
  fun rejectedTrialsAndReadOnlyProbesNeverReachSourceListeners() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Choice
        CLASS Allowed : Choice { This:: Notice }
        CLASS Forbidden : Choice { HAS MAX 0 This; This:: Notice }
        CLASS Notice
        """
            )
        )
    val engine = world.actorEngine(PLAYER1)
    val task = engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!"))).single()
    val counts = mutableListOf<Int>()
    val subscription =
        world.components.listenToCount(
            world.reader.resolve(parse("Notice")),
            world.reader,
            counts::add,
        )
    val before = world.events.nextOrdinal
    val overlay = Engine.overlay(world)

    engine.canSelectTask(task) shouldBe true
    engine.canExecuteTask(task) shouldBe true
    counts shouldBe listOf(0)
    world.events.nextOrdinal shouldBe before
    overlay.reader.count(parse<Metric>("Notice")) shouldBe 0

    engine.doTask(task)

    counts shouldBe listOf(0, 1)
    world.reader.count(parse<Metric>("Allowed")) shouldBe 1
    subscription.cancel()
  }

  @Test
  fun failedAbstractAdminWorkWithoutPlayerWorkIsReported() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Choice
        CLASS First : Choice { This: Die }
        CLASS Second : Choice { HAS MAX 0 This }
        """
            )
        )
    val engine = world.actorEngine(ADMIN)
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!")))
    val agents = Agents(world) { NONE }
    shouldThrow<NotNowException> { agents[PLAYER1].autoExecNow() }
    agents[PLAYER1].count("Choice") shouldBe 0
  }

  @Test
  fun candidateProofIncludesDeferredAdmissionInsideAnEnclosingTransaction() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Choice
        CLASS Allowed : Choice
        CLASS Forbidden : Choice { HAS MAX 0 This }
        CLASS Token
        """,
                players = 2,
            )
        )
    val engine = world.actorEngine(PLAYER1)
    val id =
        engine
            .addTasks(
                InstructionGroup.of(
                    parse<InstructionTree>("Choice!, FINALLY Admin { LATER Player { Token! } }")
                )
            )
            .first()
    val before = world.events.nextOrdinal
    val pending = world.tasks.extract { it }

    shouldThrow<ExpressionException> {
      engine.transaction(dev.martianzoo.state.Motivation.CLIENT_REQUEST) { engine.doTask(id) }
    }

    world.events.nextOrdinal shouldBe before
    world.tasks.extract { it } shouldBe pending
    world.reader.count(parse<Metric>("Choice")) shouldBe 0
  }

  @Test
  fun cityInferenceRejectsASecondCapitalAndRepairsLandClaim() {
    val world = TfmEngine.newGame(canonicalPremise())
    val agent = world.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    agent.exMachina("Capital, CapitalTile<Tharsis_6_8>, Community<Tharsis_1_1>")
    val engine = world.actorEngine(PLAYER1)
    val task = agent.addTasks("CityTile<Tharsis_1_1>!").single()

    engine.selectTask(task)
    world.tasks.getTaskData(task).instruction.isAbstract(world.reader) shouldBe true
    engine.doTask(parse<InstructionTree>("CityTile<Player1, Tharsis_1_1>!"), taskId = task)

    agent.count("NormalCityTile<Tharsis_1_1>") shouldBe 1
    agent.count("CapitalTile") shouldBe 1
    agent.count("CapitalTile<Tharsis_1_1>") shouldBe 0
    agent.count("Community") shouldBe 0
    agent.count("Occupant<Tharsis_1_1>") shouldBe 1
    agent.count("Steel") shouldBe 0
    world.actorEngine(ADMIN).tasks.isEmpty() shouldBe false
  }
}
