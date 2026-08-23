package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameEvent.ChangeEvent
import dev.martianzoo.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.engine.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class BootstrapLifecycleTest {
  @Test
  fun newGameReturnsCommittedCausallyCleanPreSetupBaseline() {
    val game = Engine.newGame(canonicalPremise())
    val engine = game.gameplay(ENGINE)

    engine.count("Phase") shouldBe 0
    engine.count("Generation") shouldBe 0
    engine.count("TerraformRating") shouldBe 0
    game.tasks.isEmpty() shouldBe true
    game.events.entriesSinceSetup().shouldBeEmpty()

    val bootstrapEntries = game.events.entriesSince(Checkpoint(0))
    bootstrapEntries.all { it is ChangeEvent } shouldBe true
    val changes = bootstrapEntries.filterIsInstance<ChangeEvent>()
    val engineCreation = changes.first()
    engineCreation.actor shouldBe ENGINE
    engineCreation.change.gaining shouldBe ENGINE.expression
    engineCreation.cause shouldBe null
    engineCreation.toString().shouldEndWith("(manual)")
    changes.drop(1).all { it.cause != null } shouldBe true

    shouldThrow<IllegalArgumentException> { game.timeline.rollBack(Checkpoint(0)) }
        .message
        .orEmpty()
        .shouldInclude("committed through")
  }

  @Test
  fun soloModeCreatesItsOpponent() {
    val game = Engine.newGame(canonicalPremise(players = 1))
    val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
    val soloMode = changes.single { it.change.gaining?.className == cn("SoloMode") }
    val soloOpponent = changes.single { it.change.gaining?.className == cn("SoloOpponent") }

    soloOpponent.cause shouldBe Cause(cn("SoloMode").expression, soloMode.ordinal)
    val standardVariant = changes.single {
      it.change.gaining?.className == cn("StandardSoloVariant")
    }
    standardVariant.cause shouldBe Cause(cn("SoloMode").expression, soloMode.ordinal)
  }

  @Test
  fun selectedSourcesCreateTheirRuntimeBootstrapComponents() {
    val game = Engine.newGame(canonicalPremise())
    val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
    val terraform = changes.single { it.change.gaining?.className == cn("TerraformingMars") }
    val map = changes.single { it.change.gaining?.className == cn("TharsisMap") }
    val milestones = changes.single {
      it.change.gaining?.className == cn("TharsisDefaultMilestones")
    }
    val area = changes.single { it.change.gaining?.className == cn("Tharsis_1_1") }

    map.cause shouldBe Cause(cn("TerraformingMars").expression, terraform.ordinal)
    milestones.cause shouldBe Cause(cn("TharsisMap").expression, map.ordinal)
    area.cause shouldBe Cause(cn("TharsisMap").expression, map.ordinal)
  }

  @Test
  fun selectedNondefaultMapsExistBeforeTheDefaultMapIsEvaluated() {
    listOf(Hellas, Elysium, Utopia, Cimmeria).forEach { selectedMap ->
      val game = Engine.newGame(canonicalPremise(selectedMap))
      val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
      val map = changes.single { it.change.gaining?.className == selectedMap.className }
      val terraform = changes.single { it.change.gaining?.className == cn("TerraformingMars") }

      (map.ordinal < terraform.ordinal) shouldBe true
      changes.none { it.change.gaining?.className == cn("TharsisMap") } shouldBe true
    }
  }

  @Test
  fun manualWorkflowStartsFullyEffectfulGenerationOneSetup() {
    val game = Engine.newGame(canonicalPremise())
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).setupPhase()

    val engine = game.gameplay(ENGINE)
    engine.count("SetupPhase") shouldBe 1
    engine.count("Generation") shouldBe 1
    engine.count("StartToken<Player1>") shouldBe 1
    engine.count("TerraformRating<Player1>") shouldBe 20
    engine.count("TerraformRating<Player2>") shouldBe 20

    val setupChanges = game.events.changesSince(checkpoint)
    val setupEvent = setupChanges.single { it.change.gaining.toString() == "SetupPhase" }
    setupEvent.cause shouldBe null
    setupEvent.toString().shouldEndWith("(manual)")
    setupChanges
        .filter { it.change.gaining.toString().startsWith("TerraformRating") }
        .also { it.size shouldBe 2 }
        .all { it.cause?.triggerEvent == setupEvent.ordinal } shouldBe true
  }

  @Test
  fun automaticWorkflowWaitsForSoloSetupChoices() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val workflow = TfmWorkflow.Auto(game).launch()

    val engine = game.gameplay(ENGINE)
    engine.count("SetupPhase") shouldBe 1
    engine.count("CorporationPhase") shouldBe 0
    engine.count("Generation") shouldBe 1
    game.tasks.isEmpty() shouldBe false
    workflow.isRunning shouldBe true

    workflow.shutdown()
    workflow.isRunning shouldBe false
  }
}
