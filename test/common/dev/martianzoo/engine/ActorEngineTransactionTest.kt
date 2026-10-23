package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ActorEngineTransactionTest {
  @Test
  internal fun directPlacementRepairsLandClaimBeforeReturning() {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER1).sneak("Community<Tharsis_1_1>")
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
    // Placement bonuses are still queued for Admin; the cube removal was an immediate effect.
    world.actorEngine(ADMIN).tasks.ids().size shouldBe 1
    world.reader.count(parse<Metric>("Steel<Player1>")) shouldBe 0
  }

  @Test
  internal fun greeneryBonusCanBeViewedAndResumedBetweenTransactions() {
    val world = TfmEngine.newGame(canonicalPremise())
    val agent =
        world.testAgent(PLAYER1).also {
          it.autoExecPolicy = dev.martianzoo.agent.AutoExecPolicy.NONE
        }
    agent.sneak("8 Plant")
    // During the action phase, greenery also offers the Player an oxygen increase.
    world.testAgent(ADMIN).sneak("Photosynthesis")
    agent.beginOperation("ConvertPlants")
    agent.doTask("8 Pay<Class<Plant>> FROM Plant")
    val player = world.actorEngine(PLAYER1)
    player.doTask(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
    val beforeBonus = world.timeline.checkpoint()
    shouldThrow<dev.martianzoo.pets.api.Exceptions.TaskException> {
      player.doTask(parse<Instruction>("OxygenStep!"))
    }
    world.timeline.checkpoint() shouldBe beforeBonus
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
    resumed.changes.first().transaction?.motivation shouldBe
        dev.martianzoo.state.Motivation.ADMIN_EAGER_POLICY
    world.timeline.checkpoint() shouldBe end
  }

  @Test
  internal fun directTaskRestoresRejectedProtectedHabitatsRemoval() {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER2).sneak("ProtectedHabitats, Plant")
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
  internal fun failedBlockRestoresImmediateEffectsAndPendingWork() {
    val world = TfmEngine.newGame(canonicalPremise())
    world
        .testAgent(PLAYER1)
        .sneak("Community<Tharsis_1_1>, ProtectedHabitats<Player2>, Plant<Player2>")
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
        // The first task removed the Land Claim cube and queued its placement bonus.
        world.reader.count(parse<Metric>("Community<Player1, Tharsis_1_1>")) shouldBe 0
        world.actorEngine(ADMIN).tasks.isEmpty() shouldBe false
        world.actorEngine(ADMIN).doTask(world.actorEngine(ADMIN).tasks.ids().single())
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
