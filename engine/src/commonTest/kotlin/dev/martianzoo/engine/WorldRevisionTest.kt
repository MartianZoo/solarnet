package dev.martianzoo.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

internal class WorldRevisionTest {
  @Test
  fun rollbackRestoresTheCheckpointWithoutReusingItsRevision() {
    val world = Engine.newGame(canonicalPremise()) as WholeWorld
    val p1 = world.gameplay(PLAYER1).godMode()
    val checkpoint = world.timeline.checkpoint()
    val originalRevision = world.revision

    p1.manual("Plant")
    val changedRevision = world.revision
    changedRevision shouldNotBe originalRevision

    world.timeline.rollBack(checkpoint)

    world.timeline.checkpoint() shouldBe checkpoint
    p1.count("Plant") shouldBe 0
    world.revision shouldNotBe originalRevision
    world.revision shouldNotBe changedRevision
  }
}
