package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.data.Player.Companion.PLAYER2
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PhilaresTest : CardTest() {
  @Test
  fun ownerOfDeferredEffectControlsItsRefinement() {
    val game = newGame("BMX", 2)
    val p1 = game.tfm(PLAYER1)
    val p2 = game.tfm(PLAYER2)

    p1.phase("Corporation")
    p1.playCorp("TharsisRepublic", 0)
    p2.playCorp("Philares", 0)

    p1.assertCounts(40 to "Megacredit", 1 to "Mandate", 0 to "CityTile")
    p2.assertCounts(47 to "Megacredit", 1 to "Mandate", 0 to "GreeneryTile")

    p1.phase("Action")
    p1.stdAction("HandleMandates") { doTask("CityTile<M42>") }

    p2.stdAction("HandleMandates") {
      doTask("GreeneryTile<M32>")
      doTask("Steel")
    }

    p2.assertCounts(47 to "Megacredit", 1 to "Steel", 0 to "Mandate", 1 to "GreeneryTile")

    shouldThrow<IllegalArgumentException> {
      p1.stdProject("GreenerySP") { doTask("GreeneryTile<M43>") }
    }

    p1.stdProject("GreenerySP") {
      doTask("GreeneryTile<M43>")
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
      p2.doTask("Titanium")
    }
    p2.assertCounts(1 to "Steel", 1 to "Titanium")
  }

  @Test
  fun otherPlayerCanCreateAdjacencyWhilePhilaresOwnerIsAssignedAndPerformsReward() {
    val game = newGame("BMX", 2)
    val other = game.tfm(PLAYER1).also { it.autoExecMode = NONE }
    val owner = game.tfm(PLAYER2).also { it.autoExecMode = NONE }
    owner.sneak("Philares")
    owner.manual("GreeneryTile<M23>")
    val checkpoint = game.timeline.checkpoint()
    val steelBefore = owner.count("Steel")

    other.godMode().beginManual("GreeneryTile<M33>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    game.events.changesSince(checkpoint).first().actor shouldBe PLAYER1
    owner.doTask("Steel")
    owner.count("Steel") shouldBe steelBefore + 1
    game.events.changesSince(checkpoint).last().actor shouldBe PLAYER2
  }

  @Test
  fun philaresOwnerCanCreateAdjacencyAndReceivesTask() {
    val game = newGame("BMX", 2)
    val other = game.tfm(PLAYER1).also { it.autoExecMode = NONE }
    val owner = game.tfm(PLAYER2).also { it.autoExecMode = NONE }
    owner.sneak("Philares")
    other.manual("GreeneryTile<M23>")

    owner.godMode().beginManual("GreeneryTile<M33>") {
      game.tasks.extract { it.assignee }.shouldContainExactly(PLAYER2)
    }

    owner.doTask("Titanium")
    owner.count("Titanium") shouldBe 1
  }

  @Test
  fun doesNotTriggerBetweenTwoTilesOwnedByOtherPlayer() {
    val game = newGame("BMX", 2)
    val other = game.tfm(PLAYER1).also { it.autoExecMode = NONE }
    val owner = game.tfm(PLAYER2).also { it.autoExecMode = NONE }
    owner.sneak("Philares")
    other.manual("GreeneryTile<M23>")

    other.manual("GreeneryTile<M33>")

    game.tasks.isEmpty() shouldBe true
  }

  @Test
  fun doesNotTriggerBetweenOwnTiles() {
    val game = newGame("BMX", 2)
    val p1 = game.tfm(PLAYER1)

    p1.phase("Corporation")
    p1.playCorp("Philares", 0)

    p1.phase("Action")
    p1.stdAction("HandleMandates") { doTask("GreeneryTile<M42>") }

    p1.stdProject("GreenerySP") { doTask("GreeneryTile<M32>") }
    shouldThrow<TaskException> { p1.doTask("Megacredit") }
  }
}
