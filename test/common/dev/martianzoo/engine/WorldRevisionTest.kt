package dev.martianzoo.engine

import dev.martianzoo.pets.data.Player.Companion.PLAYER1
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

internal class WorldRevisionTest {
  @Test
  internal fun rollbackRestoresTheCheckpointWithoutReusingItsRevision() {
    val world = Engine.newGame(testGamePremise()) as WholeWorld
    val p1 = world.agent(PLAYER1).godMode()
    val checkpoint = world.timeline.checkpoint()
    val originalRevision = world.revision

    p1.manual("Token")
    val changedRevision = world.revision
    changedRevision shouldNotBe originalRevision

    world.timeline.rollBack(checkpoint)

    world.timeline.checkpoint() shouldBe checkpoint
    p1.count("Token") shouldBe 0
    world.revision shouldNotBe originalRevision
    world.revision shouldNotBe changedRevision
  }
}
