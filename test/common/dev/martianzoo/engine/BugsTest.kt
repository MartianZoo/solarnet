package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * Gaps against proposed engine behavior, pending human review. These tests do not establish that
 * the new proposal was the engine's previous contract.
 */
internal class BugsTest {
  @Test
  internal fun `BUG - player selection overtakes a pending Admin placement bonus`() {
    val world = pendingPlacementBonusWorld()
    val player = world.actorEngine(PLAYER1)
    val next = player.tasks.ids().single()
    val pendingAdmin = world.actorEngine(ADMIN).tasks.extract { it }
    pendingAdmin.single().deferred shouldBe false

    player.selectTask(next)

    world.reader.count(parse<Metric>("Plant<Player1>")) shouldBe 1
    (next in world.tasks) shouldBe false
    world.actorEngine(ADMIN).tasks.extract { it } shouldBe pendingAdmin
    world.reader.count(parse<Metric>("Steel<Player1>")) shouldBe 0
  }

  // The proposed P1 priority requires Admin work to finish before another Player selection.
  // Current selection checks only an existing selected task and deferral.
  @Ignore
  @Test
  internal fun playerSelectionWaitsForThePendingAdminPlacementBonus() {
    val world = pendingPlacementBonusWorld()
    val player = world.actorEngine(PLAYER1)
    val next = player.tasks.ids().single()
    world.actorEngine(ADMIN).tasks.extract { it }.single().deferred shouldBe false
    val pending = world.tasks.extract { it }
    val before = world.timeline.checkpoint()

    shouldThrow<TaskException> { player.selectTask(next) }

    world.reader.count(parse<Metric>("Plant<Player1>")) shouldBe 0
    world.tasks.extract { it } shouldBe pending
    world.events.entriesSince(before).shouldBeEmpty()
    world.timeline.checkpoint() shouldBe before
  }

  private fun pendingPlacementBonusWorld(): World {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER1).sneak("Community<Tharsis_1_1>")
    val player = world.actorEngine(PLAYER1)
    val placement =
        player
            .addTasks(
                InstructionGroup.of(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
            )
            .single()
    player.addTasks(InstructionGroup.of(parse<Instruction>("Plant<Player1>!")))

    // The greenery replaces Land Claim's cube and creates the queued Admin placement signal.
    player.doTask(placement)
    return world
  }
}
