package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
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

internal class AlternativeTransactionTest {
  @Test
  fun onlySuccessfulAlternativeExecutesAfterFailedImmediateEffectsAreRestored() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Guarded { This:: Notice!, -Missing! }
                CLASS Other
                CLASS Notice
                CLASS Missing
                """
            )
        )
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("Guarded! OR Other!").single()

    world.actorEngine(PLAYER1).doTask(task)

    player.count("Guarded") shouldBe 0
    player.count("Notice") shouldBe 0
    player.count("Other") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun successfulAlternativeDoesNotRunItsQueuedFailureDuringProof() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
                CLASS Guarded { HAS MAX 0 This }
                CLASS Other { This: Later }
                CLASS Later { This:: Die! }
                """
            )
        )
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("Guarded! OR Other!").single()

    world.actorEngine(PLAYER1).doTask(task)

    player.count("Other") shouldBe 1
    player.count("Later") shouldBe 0
    world.tasks.ids().size shouldBe 1
  }

  @Test
  fun optionalAlternativesShareTheSameZeroNarrowing() {
    val world =
        Engine.newGame(
            testGamePremise("CLASS First { HAS MAX 0 This }\nCLASS Second { HAS MAX 0 This }")
        )
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("First? OR Second?").single()

    world.actorEngine(PLAYER1).doTask(task)

    player.count("First") shouldBe 0
    player.count("Second") shouldBe 0
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun overlappingAlternativesDoNotCountTheSameConcreteRequestTwice() {
    val world =
        Engine.newGame(
            testGamePremise(
                "ABSTRACT CLASS Choice\nCLASS Allowed : Choice\nCLASS Forbidden : Choice { HAS MAX 0 This }"
            )
        )
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("Choice! OR Allowed!").single()

    world.actorEngine(PLAYER1).doTask(task)

    player.count("Allowed") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun unsupportedAlternativePreventsAClaimOfUniqueSuccess() {
    val world = Engine.newGame(testGamePremise("CLASS First\nCLASS Second"))
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("X! First OR Second!").single()
    val before = world.events.nextOrdinal

    shouldThrow<IncompleteAnalysisException> { world.actorEngine(PLAYER1).doTask(task) }

    world.events.nextOrdinal shouldBe before
    player.count("Second") shouldBe 0
    player.tasks.getTaskData(task).selected shouldBe false
  }

  @Test
  fun anEmptyCompatiblePairDomainRejectsInsteadOfReportingIncompleteAnalysis() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
            ABSTRACT CLASS Place
            CLASS Red : Place
            CLASS Blue : Place
            CLASS First<Place>
            CLASS Second<Place>
            """
            )
        )
    val player = Agents(world) { NONE }[PLAYER1]
    player.sneak("Red, Blue, First<Red>, First<Blue>, Second<Red>, Second<Blue>")
    val task =
        player
            .addTasks("First<@Place> FROM First<@Place>! OR Second<@Place> FROM Second<@Place>!")
            .single()
    val before = world.events.nextOrdinal

    shouldThrow<DeadEndException> { world.actorEngine(PLAYER1).doTask(task) }

    world.events.nextOrdinal shouldBe before
    player.count("First") shouldBe 2
    player.count("Second") shouldBe 2
  }

  @Test
  fun adminReportsWhenEveryAlternativeIsADeadEnd() {
    val world =
        Engine.newGame(testGamePremise("CLASS First { This:: Die! }\nCLASS Second { This:: Die! }"))
    val agents = Agents(world) { NONE }
    val task =
        world
            .actorEngine(ADMIN)
            .addTasks(InstructionGroup.of(parse<InstructionTree>("First! OR Second!")))
            .single()
    val before = world.events.nextOrdinal

    shouldThrow<DeadEndException> { agents[PLAYER1].autoExecNow() }

    world.events.nextOrdinal shouldBe before
    agents[PLAYER1].count("First") shouldBe 0
    agents[PLAYER1].count("Second") shouldBe 0
    world.tasks.getTaskData(task).selected shouldBe false
  }

  @Test
  fun twoSuccessesProveAmbiguityEvenWithAnUnsupportedAlternativeFirst() {
    val world = Engine.newGame(testGamePremise("CLASS First\nCLASS Second\nCLASS Third"))
    val player = Agents(world) { NONE }[PLAYER1]
    val task = player.addTasks("X! First OR Second! OR Third!").single()
    val before = world.events.nextOrdinal

    val failure =
        shouldThrow<NotFullySpecifiedException> { world.actorEngine(PLAYER1).doTask(task) }

    failure.shouldNotBeInstanceOf<IncompleteAnalysisException>()
    world.events.nextOrdinal shouldBe before
    player.count("Second") shouldBe 0
    player.count("Third") shouldBe 0
  }
}
