package dev.martianzoo.engine

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.Instruction
import dev.martianzoo.pets.ast.InstructionGroup
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Characterizations promoted to their intended behavior as engine contract gaps are fixed. */
internal class BugsTest {
  @Test
  internal fun playerCanSelectOxygenAfterAutomaticPlacementProcessing() {
    val world = pendingPlacementBonusWorld()
    val player = world.actorEngine(PLAYER1)
    world.actorEngine(ADMIN).tasks.isEmpty() shouldBe true
    player.selectTask(parse<Instruction>("OxygenStep!"))
    world.reader.count(parse<Metric>("OxygenStep")) shouldBe 0
    (world.tasks.selectedTask() != null) shouldBe true
  }

  private fun pendingPlacementBonusWorld(): World {
    val world = TfmEngine.newGame(canonicalPremise())
    world.testAgent(PLAYER1).exMachina("Community<Tharsis_1_1>")
    val player = world.actorEngine(PLAYER1)
    val placement =
        player
            .addTasks(
                InstructionGroup.of(parse<Instruction>("GreeneryTile<Player1, Tharsis_1_1>!"))
            )
            .single()
    world.testAgents().correct("Photosynthesis")

    // The greenery replaces Land Claim's cube and creates the queued Admin placement signal.
    player.doTask(placement)
    return world
  }
}
