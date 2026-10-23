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
import kotlin.test.Test

/** Characterizations promoted to their intended behavior as engine contract gaps are fixed. */
internal class BugsTest {
  @Test
  internal fun playerSelectionWaitsForThePendingAdminPlacementBonus() {
    val world = pendingPlacementBonusWorld()
    val player = world.actorEngine(PLAYER1)
    val next = player.tasks.ids().single()
    world.actorEngine(ADMIN).tasks.extract { it }.single().deferred shouldBe false
    val pending = world.tasks.extract { it }
    val before = world.events.nextOrdinal

    shouldThrow<TaskException> { player.selectTask(next) }

    world.reader.count(parse<Metric>("OxygenStep")) shouldBe 0
    world.tasks.extract { it } shouldBe pending
    world.events.entriesSince(before).shouldBeEmpty()
    world.events.nextOrdinal shouldBe before
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
    world.testAgent(ADMIN).sneak("Photosynthesis")

    // The greenery replaces Land Claim's cube and creates the queued Admin placement signal.
    player.doTask(placement)
    return world
  }
}
