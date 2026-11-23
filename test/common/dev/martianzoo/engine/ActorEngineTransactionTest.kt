package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Motivation
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import kotlin.test.Test

internal class ActorEngineTransactionTest {
  @Test
  internal fun cancellationPropagatesThroughTypedEngineTransactionsToTheCallerScope() {
    val world = Engine.newGame(testGamePremise())
    val player = world.actorEngine(PLAYER1)
    val task = player.addTasks(InstructionGroup.of(parse<Instruction>("Token!"))).single()
    val before = world.events.nextOrdinal
    val cancel: () -> Int = { throw Exceptions.AbortTransactionException() }

    shouldThrow<Exceptions.AbortTransactionException> {
      val result: Int =
          player.transaction(Motivation.CLIENT_REQUEST) {
            player.doTask(task)
            cancel()
          }
      result shouldBe 7
    }
    world.events.nextOrdinal shouldBe before

    var resumed = false
    world.timeline.atomic {
      player.transaction<Int>(Motivation.CLIENT_REQUEST) {
        player.doTask(task)
        cancel()
      }
      resumed = true
    }
    resumed shouldBe false
    world.events.nextOrdinal shouldBe before
    world.reader.count("Token") shouldBe 0
    (task in world.tasks) shouldBe true
  }

  @Test
  internal fun directPlacementRepairsLandClaimBeforeReturning() {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER1).exMachina("Community<Tharsis_1_1>")
    val player = world.actorEngine(PLAYER1)
    val task =
        player
            .addTasks(
                InstructionGroup.of(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
            )
            .single()

    player.doTask(task)

    world.reader.count(parse<Metric>("Community<Player1, Tharsis_1_1>")) shouldBe 0
    world.reader.count(parse<Metric>("GreeneryTile<Player1, Tharsis_1_1>")) shouldBe 1
    world.reader.count(parse<Metric>("Occupant<Tharsis_1_1>")) shouldBe 1
    (task in world.tasks) shouldBe false
    // The placement signal has run; its resource choice remains with the Player.
    world.actorEngine(ADMIN).tasks.isEmpty() shouldBe true
    world.reader.count(parse<Metric>("Steel<Player1>")) shouldBe 0
  }

  @Test
  internal fun greeneryBonusCanBeViewedAndResumedBetweenTransactions() {
    val world = TfmEngine.newGame(canonicalPremise())
    val agent =
        world.testAgent(PLAYER1).also {
          it.autoExecPolicy = dev.martianzoo.agent.AutoExecPolicy.NONE
        }
    agent.exMachina("8 Plant")
    // During the action phase, greenery also offers the Player an oxygen increase.
    world.testAgents().correct("Photosynthesis")
    agent.beginOperation("UseAction<ConvertPlantsAction, Action1>")
    agent.doTask("8 Pay<Class<Plant>> FROM Plant")
    val player = world.actorEngine(PLAYER1)
    player.doTask(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
    val beforeBonus = world.timeline.checkpoint()
    world.actorEngine(ADMIN).tasks.extract { it }.all { it.deferred } shouldBe true
    val steel = parse<Metric>("Steel<Player1>")

    agent.doTasks("OxygenStep", "2 Steel")
    world.reader.count(steel) shouldBe 2
    val end = world.timeline.checkpoint()
    val playback = world.recording().open()
    playback.seek(beforeBonus.serial)
    playback.world.reader.count(steel) shouldBe 0
    playback.seek(end.serial)
    playback.world.reader.count(steel) shouldBe 2

    world.timeline.seek(beforeBonus)
    world.reader.count(steel) shouldBe 0
    world.timeline.truncate()
    val resumed = agent.doTasks("OxygenStep", "2 Steel")

    world.reader.count(steel) shouldBe 2
    resumed.changes.first().transaction?.motivation shouldBe Motivation.CLIENT_REQUEST
    world.timeline.checkpoint() shouldBe end
  }

  @Test
  internal fun directTaskRestoresRejectedProtectedHabitatsRemoval() {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER2).exMachina("ProtectedHabitats, Plant")
    val player = world.actorEngine(PLAYER1)
    val attack =
        player.addTasks(InstructionGroup.of(parse<Instruction>("-Plant<Player2>!"))).single()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    shouldThrow<DeadEndException> {
      player.doTask(attack)
    }

    world.reader.count(parse<Metric>("Plant<Player2>")) shouldBe 1
    world.tasks.extract { it } shouldBe pending
    world.events.entriesSince(before).shouldBeEmpty()
    world.events.nextOrdinal shouldBe before
  }

  @Test
  internal fun assertionFailureRestoresRawTransactionAndAllowsAnotherTransaction() {
    val world =
        Engine.newGame(testGamePremise("CLASS Token { This: Reward BY Admin }\nCLASS Reward"))
    val player = world.actorEngine(PLAYER1)
    val checkpoint = world.timeline.checkpoint()
    val events = world.events.nextOrdinal
    val failure = AssertionError("fail after changes and queued work")

    val caught =
        shouldThrow<AssertionError> {
          player.transaction(Motivation.CLIENT_REQUEST) {
            player
                .addTasks(InstructionGroup.of(parse<Instruction>("Token!")))
                .forEach(player::doTask)
            world.reader.count(parse<Metric>("Token")) shouldBe 1
            world.tasks.isEmpty() shouldBe false
            throw failure
          }
        }

    caught shouldBeSameInstanceAs failure
    world.reader.count(parse<Metric>("Token")) shouldBe 0
    world.tasks.isEmpty() shouldBe true
    world.events.nextOrdinal shouldBe events
    world.timeline.checkpoint() shouldBe checkpoint
    world.timeline.positions.last() shouldBe checkpoint

    player.transaction(Motivation.CLIENT_REQUEST) {
      player.addTasks(InstructionGroup.of(parse<Instruction>("Token!"))).forEach(player::doTask)
    }
    world.reader.count(parse<Metric>("Token")) shouldBe 1
    world.timeline.checkpoint().serial shouldBe checkpoint.serial + 2
  }

  @Test
  internal fun failedBlockRestoresImmediateEffectsAndPendingWork() {
    val world = TfmEngine.newGame(canonicalPremise())
    world
        .testAgent(PLAYER1)
        .exMachina("Community<Tharsis_1_1>, ProtectedHabitats<Player2>, Plant<Player2>")
    val player = world.actorEngine(PLAYER1)
    val placement =
        player
            .addTasks(
                InstructionGroup.of(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
            )
            .single()
    val attack =
        player.addTasks(InstructionGroup.of(parse<Instruction>("-Plant<Player2>!"))).single()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    shouldThrow<DeadEndException> {
      player.transact(afterChanges = {}) {
        player.doTask(placement)
        // The first task removed Land Claim and processed the automatic placement signal.
        world.reader.count(parse<Metric>("Community<Player1, Tharsis_1_1>")) shouldBe 0
        world.actorEngine(ADMIN).tasks.isEmpty() shouldBe true
        player.doTask(attack)
      }
    }

    world.reader.count(parse<Metric>("Community<Player1, Tharsis_1_1>")) shouldBe 1
    world.reader.count(parse<Metric>("GreeneryTile<Player1, Tharsis_1_1>")) shouldBe 0
    world.reader.count(parse<Metric>("Plant<Player2>")) shouldBe 1
    world.tasks.extract { it } shouldBe pending
    world.events.entriesSince(before).shouldBeEmpty()
    world.events.nextOrdinal shouldBe before
  }
}
