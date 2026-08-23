package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.canonicalPremise
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class EffectActorCharacterizationTest {
  @Test
  internal fun playersCannotCreateSystemComponents() {
    val game = Engine.newGame(canonicalPremise())
    val player = game.gameplay(PLAYER1).godMode()

    assertFailsWith<DeadEndException> { player.manual("Generation") }
    game.gameplay(ENGINE).godMode().manual("Generation")

    player.count("Generation") shouldBe 1
  }

  @Test
  internal fun noActorCanRemoveModules() {
    val game = Engine.newGame(canonicalPremise())
    val player = game.gameplay(PLAYER1).godMode()

    assertFailsWith<LimitsException> { player.manual("-TharsisMap") }
    player.count("TharsisMap") shouldBe 1

    assertFailsWith<LimitsException> {
      game.gameplay(ENGINE).godMode().manual("-TharsisMap")
    }
    player.count("TharsisMap") shouldBe 1
  }

  @Test
  internal fun enginePerformedPlacementDoesNotGiveTheChangedComponentOwnerTheAreaBonus() {
    val game = Engine.newGame(canonicalPremise(Elysium, players = 2))
    val engine = game.gameplay(ENGINE).godMode().also { it.autoExecMode = NONE }
    val checkpoint = game.timeline.checkpoint()

    engine.beginManual("GreeneryTile<Player1, Elysium_9_8>") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "OxygenStep.")

      engine.has("Neighbor") shouldBe true
      engine.count("ProjectCard<Player1>") shouldBe 0
    }

    game.events.changesSince(checkpoint).all { it.actor == ENGINE } shouldBe true
  }

  @Test
  internal fun triggeringPlayerIsFallbackActorForDeferredByOwnerEffect() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.gameplay(PLAYER1).godMode().also { it.autoExecMode = NONE }
    val terraformRatingBefore = p1.count("TerraformRating")

    p1.beginManual("OxygenStep!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "TerraformRating<Player1>!")
      p1.count("TerraformRating") shouldBe terraformRatingBefore
    }

    p1.doTask("TerraformRating!")
    p1.count("TerraformRating") shouldBe terraformRatingBefore + 1
  }

  @Test
  internal fun byOwnerEffectDoesNotTreatEngineAsAnOwner() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.gameplay(ENGINE).godMode().also { it.autoExecMode = NONE }
    val terraformRatingBefore = engine.count("TerraformRating")

    engine.manual("OxygenStep!")

    engine.count("OxygenStep") shouldBe 1
    engine.count("TerraformRating") shouldBe terraformRatingBefore
    game.tasks.isEmpty() shouldBe true
  }
}
