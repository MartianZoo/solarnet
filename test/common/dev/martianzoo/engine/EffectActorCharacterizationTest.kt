package dev.martianzoo.engine

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.count
import dev.martianzoo.testsupport.has
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class EffectActorCharacterizationTest {
  @Test
  internal fun playerSubmittedSystemGainIsPerformedByAdmin() {
    val game = TfmEngine.newGame(canonicalPremise())
    val agents = Agents(game) { EAGER }
    val player = agents[PLAYER1]
    val checkpoint = game.events.nextOrdinal

    player.runOperation("Generation")

    player.count("Generation") shouldBe 1
    game.events.changesSince(checkpoint).all { it.actor == ADMIN } shouldBe true
  }

  @Test
  internal fun noActorCanRemoveModules() {
    val game = TfmEngine.newGame(canonicalPremise())
    val agents = Agents(game) { EAGER }
    val player = agents[PLAYER1]

    assertFailsWith<LimitsException> { player.runOperation("-TharsisMap") }
    player.count("TharsisMap") shouldBe 1

    assertFailsWith<LimitsException> { agents.runSystem("-TharsisMap") }
    player.count("TharsisMap") shouldBe 1
  }

  @Test
  internal fun adminPerformedPlacementDoesNotGiveTheChangedComponentHolderTheAreaBonus() {
    val game = TfmEngine.newGame(canonicalPremise(cn("ElysiumMap"), players = 2))
    val agents = Agents(game) { EAGER }
    val admin = agents
    agents[PLAYER1].autoExecPolicy = NONE
    admin.runSystem("Photosynthesis")
    val checkpoint = game.events.nextOrdinal

    admin.runSystem("GreeneryTile<Player1, Elysium_9_8>") {
      game.tasks
          .extract { it.assignee to it.instruction.toString() }
          .shouldContainExactly(PLAYER1 to "OxygenStep*")

      admin.world.reader.has("Neighbor") shouldBe true
      admin.world.reader.count("ProjectCard<Player1>") shouldBe 0
    }

    game.events.changesSince(checkpoint).all { it.actor == ADMIN } shouldBe true
  }

  @Test
  internal fun triggeringPlayerIsFallbackActorForDeferredEffect() {
    val game = TfmEngine.newGame(canonicalPremise())
    val agents = Agents(game) { EAGER }
    val p1 = agents[PLAYER1].also { it.autoExecPolicy = NONE }
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
  internal fun ownedEffectDoesNotTreatAdminAsAnOwner() {
    val game = TfmEngine.newGame(canonicalPremise())
    val agents = Agents(game) { EAGER }
    val admin = agents
    val terraformRatingBefore = admin.world.reader.count("TerraformRating")

    admin.runSystem("OxygenStep!")

    admin.world.reader.count("OxygenStep") shouldBe 1
    admin.world.reader.count("TerraformRating") shouldBe terraformRatingBefore
    game.tasks.isEmpty() shouldBe true
  }
}
