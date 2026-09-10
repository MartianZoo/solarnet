package dev.martianzoo.engine

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class EffectActorCharacterizationTest {
  @Test
  internal fun playersCannotCreateSystemComponents() {
    val game = Engine.newGame(canonicalPremise())
    val player = game.testAgent(PLAYER1)

    assertFailsWith<DeadEndException> { player.runOperation("Generation") }
    game.testAgent(ADMIN).runOperation("Generation")

    player.count("Generation") shouldBe 1
  }

  @Test
  internal fun noActorCanRemoveModules() {
    val game = Engine.newGame(canonicalPremise())
    val player = game.testAgent(PLAYER1)

    assertFailsWith<LimitsException> { player.runOperation("-TharsisMap") }
    player.count("TharsisMap") shouldBe 1

    assertFailsWith<LimitsException> { game.testAgent(ADMIN).runOperation("-TharsisMap") }
    player.count("TharsisMap") shouldBe 1
  }

  @Test
  internal fun adminPerformedPlacementDoesNotGiveTheChangedComponentOwnerTheAreaBonus() {
    val game = Engine.newGame(canonicalPremise(cn("ElysiumMap"), players = 2))
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    admin.runOperation("Photosynthesis")
    val checkpoint = game.timeline.checkpoint()

    admin.beginOperation("GreeneryTile<Player1, Elysium_9_8>") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "OxygenStep.")

      admin.has("Neighbor") shouldBe true
      admin.count("ProjectCard<Player1>") shouldBe 0
    }

    game.events.changesSince(checkpoint).all { it.actor == ADMIN } shouldBe true
  }

  @Test
  internal fun triggeringPlayerIsFallbackActorForDeferredByOwnerEffect() {
    val game = Engine.newGame(canonicalPremise())
    val p1 = game.testAgent(PLAYER1).also { it.autoExecPolicy = NONE }
    val terraformRatingBefore = p1.count("TerraformRating")

    p1.beginOperation("OxygenStep!") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "TerraformRating<Player1>!")
      p1.count("TerraformRating") shouldBe terraformRatingBefore
    }

    p1.doTask("TerraformRating!")
    p1.count("TerraformRating") shouldBe terraformRatingBefore + 1
  }

  @Test
  internal fun byOwnerEffectDoesNotTreatAdminAsAnOwner() {
    val game = Engine.newGame(canonicalPremise())
    val admin = game.testAgent(ADMIN).also { it.autoExecPolicy = NONE }
    val terraformRatingBefore = admin.count("TerraformRating")

    admin.runOperation("OxygenStep!")

    admin.count("OxygenStep") shouldBe 1
    admin.count("TerraformRating") shouldBe terraformRatingBefore
    game.tasks.isEmpty() shouldBe true
  }
}
