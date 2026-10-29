package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.engine.Exceptions.IncompleteAnalysisException
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.InstructionTree
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class AdminTimingExperimentTest {
  @Test
  fun adminUsesFiniteTransactionUniquenessEvenWithManualPlayerPolicy() {
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
    val agents = Agents(world) { NONE }
    world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!")))
    agents[PLAYER1].autoExecNow()
    agents[PLAYER1].count("Allowed") shouldBe 1
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun playerSelectionWaitsForUniquelyInferableAdminWork() {
    val world =
        Engine.newGame(
            testGamePremise(
                """
        ABSTRACT CLASS Choice
        CLASS Allowed : Choice
        CLASS Forbidden : Choice { HAS MAX 0 This }
        CLASS Foo
        """
            )
        )
    val admin = world.actorEngine(ADMIN)
    val player = world.actorEngine(PLAYER1)
    val choice = admin.addTasks(InstructionGroup.of(parse<InstructionTree>("Choice!"))).single()
    val foo = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Foo!"))).single()
    val before = world.events.nextOrdinal

    shouldThrow<TaskException> { player.selectTask(foo) }
    world.events.nextOrdinal shouldBe before
    world.tasks.getTaskData(foo).selected shouldBe false

    admin.doTask(choice)
    player.selectTask(foo)
    world.tasks.getTaskData(foo).selected shouldBe true
  }

  @Test
  fun incompleteAdminAnalysisRestoresEarlierAutomaticWorkInTheCall() {
    val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward"))
    val agents = Agents(world) { NONE }
    world
        .actorEngine(ADMIN)
        .addTasks(InstructionGroup.of(parse<InstructionTree>("Reward!, X! Token")))
    val pending = world.tasks.extract { it }
    val checkpoint = world.timeline.checkpoint()
    val events = world.events.nextOrdinal

    shouldThrow<IncompleteAnalysisException> { agents[PLAYER1].autoExecNow() }

    agents[PLAYER1].count("Reward") shouldBe 0
    agents[PLAYER1].count("Token") shouldBe 0
    world.tasks.extract { it } shouldBe pending
    world.timeline.checkpoint() shouldBe checkpoint
    world.events.nextOrdinal shouldBe events
  }

  @Test
  fun unfinishedAdminProofIsNotReplacedByARejectionFromAnotherTask() {
    for (instructions in listOf("X! Token, Failure!", "Failure!, X! Token")) {
      val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Failure { This:: Die! }"))
      val agents = Agents(world) { NONE }
      world.actorEngine(ADMIN).addTasks(InstructionGroup.of(parse<InstructionTree>(instructions)))
      val pending = world.tasks.extract { it }
      val events = world.events.nextOrdinal

      shouldThrow<IncompleteAnalysisException> { agents[PLAYER1].autoExecNow() }

      world.tasks.extract { it } shouldBe pending
      world.events.nextOrdinal shouldBe events
    }
  }

  @Test
  fun incompleteAdminAnalysisIsRetriedAfterOtherAdminWorkProgresses() {
    val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Unit").withTestSetup("Unit"))
    val agents = Agents(world) { NONE }
    world
        .actorEngine(ADMIN)
        .addTasks(InstructionGroup.of(parse<InstructionTree>("X! Token / Unit, -Unit!")))

    agents[PLAYER1].autoExecNow()

    agents[PLAYER1].count("Unit") shouldBe 0
    agents[PLAYER1].count("Token") shouldBe 0
    world.tasks.isEmpty() shouldBe true
  }

  @Test
  fun provenAdminPriorityWinsOverIncompleteProofRegardlessOfQueueOrder() {
    for (instructions in listOf("X! Token, Reward!", "Reward!, X! Token")) {
      val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward\nCLASS Foo"))
      val admin = world.actorEngine(ADMIN)
      val player = world.actorEngine(PLAYER1)
      admin.addTasks(InstructionGroup.of(parse<InstructionTree>(instructions)))
      val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Foo!"))).single()
      val pending = world.tasks.extract { it }
      val events = world.events.nextOrdinal

      player.canSelectTask(task) shouldBe false
      shouldThrow<TaskException> { player.selectTask(task) }
          .message
          .orEmpty()
          .shouldInclude("must finish before Player selection")
      shouldThrow<TaskException> { player.doTask(task) }
          .message
          .orEmpty()
          .shouldInclude("must finish before Player selection")

      world.tasks.extract { it } shouldBe pending
      world.events.nextOrdinal shouldBe events
    }
  }

  @Test
  fun playerSelectionReportsUnfinishedAdminProofWithoutChangingTheWorld() {
    val world = Engine.newGame(testGamePremise("CLASS Token\nCLASS Reward"))
    val admin = world.actorEngine(ADMIN)
    val player = world.actorEngine(PLAYER1)
    admin.addTasks(InstructionGroup.of(parse<InstructionTree>("X! Token")))
    val task = player.addTasks(InstructionGroup.of(parse<InstructionTree>("Reward!"))).single()
    val pending = world.tasks.extract { it }
    val events = world.events.nextOrdinal
    val attempts: List<() -> Unit> =
        listOf(
            { player.canSelectTask(task) },
            { player.selectTask(task) },
            { player.doTask(task) },
        )

    for (attempt in attempts) {
      shouldThrow<IncompleteAnalysisException> { attempt() }
      world.tasks.extract { it } shouldBe pending
      world.events.nextOrdinal shouldBe events
    }
  }

  @Test
  fun nextPlayerLinksExistBeforeTheNextSeatIsCreated() {
    val world = TfmEngine.newGame(canonicalPremise(players = 3))
    fun gainedAt(expression: String): Int {
      val type = world.reader.resolve(parse<Expression>(expression))
      return world.events.changesSince(0).single { it.change.gaining?.type == type }.ordinal
    }
    (gainedAt("Player2") < gainedAt("NextPlayer<Player1, Player2>")) shouldBe true
    (gainedAt("NextPlayer<Player1, Player2>") < gainedAt("Player3")) shouldBe true
    (gainedAt("Player3") < gainedAt("NextPlayer<Player2, Player3>")) shouldBe true
    (gainedAt("NextPlayer<Player2, Player3>") < gainedAt("NextPlayer<Player3, Player1>")) shouldBe
        true
  }

  @Test
  fun currentAdmissionRejectsDirectGatedAndLaterDieButDropsOptionalDie() {
    val world = Engine.newGame(testGamePremise("CLASS Foo"))
    val engine = world.actorEngine(PLAYER1)
    for (instruction in listOf("Die!", "Foo: Die!", "Foo! THEN Die!")) {
      val before = world.events.nextOrdinal
      shouldThrow<DeadEndException> {
        engine.addTasks(InstructionGroup.of(parse<InstructionTree>(instruction)))
      }
      world.events.nextOrdinal shouldBe before
      world.tasks.isEmpty() shouldBe true
    }
    engine.addTasks(InstructionGroup.of(parse<InstructionTree>("Die?")))
    world.tasks.isEmpty() shouldBe true
  }
}
