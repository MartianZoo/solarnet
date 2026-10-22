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
  internal fun clientRestoresRejectedTaskThroughTimeline() {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER2).sneak("ProtectedHabitats, Plant")
    val player = world.actorEngine(PLAYER1)
    val attack =
        player.addTasks(InstructionGroup.of(parse<Instruction>("-Plant<Player2>!"))).single()
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    shouldThrow<DeadEndException> {
      world.timeline.atomic { player.doTask(attack) }
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
