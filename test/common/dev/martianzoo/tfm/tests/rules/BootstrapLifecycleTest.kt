package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.pets.data.GameEvent.ChangeEvent
import dev.martianzoo.pets.data.GameEvent.ChangeEvent.Cause
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class BootstrapLifecycleTest {
  @Test
  internal fun newGameReturnsCommittedCausallyCleanBootstrapState() {
    val game = Engine.newGame(canonicalPremise())
    val admin = game.agent(ADMIN)

    admin.count("Phase") shouldBe 1
    admin.count("BootstrapPhase") shouldBe 1
    admin.count("Generation") shouldBe 0
    admin.count("TerraformRating") shouldBe 0
    admin.count("Player") shouldBe 2
    admin.count("ProdOffset<Player1, Class<MC>>") shouldBe 5
    admin.count("ProdOffset<Player2, Class<MC>>") shouldBe 5
    admin.count("StartToken<Player1>") shouldBe 1
    admin.count("GpIncomplete") shouldBe 3
    admin.count("Class") shouldBe game.classTable.allClasses().count { !it.abstract }
    game.tasks.isEmpty() shouldBe true
    game.events.entriesSinceSetup().shouldBeEmpty()

    val bootstrapEntries = game.events.entriesSince(Checkpoint(0))
    val changes = bootstrapEntries.filterIsInstance<ChangeEvent>()
    val adminCreation = changes.first()
    adminCreation.actor shouldBe ADMIN
    adminCreation.change.gaining shouldBe ADMIN.expression
    adminCreation.cause shouldBe null
    adminCreation.toString().shouldEndWith("(manual)")
    changes.none { it.change.gaining?.className == cn("Class") } shouldBe true
    changes.drop(1).all { it.cause != null } shouldBe true
    val terraform = changes.single { it.change.gaining?.className == cn("TerraformingMars") }
    val bootstrap = changes.single { it.change.gaining?.className == cn("BootstrapPhase") }
    bootstrap.cause shouldBe Cause(cn("TerraformingMars").expression, terraform.ordinal)

    shouldThrow<IllegalArgumentException> { game.timeline.rollBack(Checkpoint(0)) }
        .message
        .orEmpty()
        .shouldInclude("committed through")
  }

  @Test
  internal fun soloModeCreatesItsOpponent() {
    val game = Engine.newGame(canonicalPremise(players = 1))
    val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
    val soloMode = changes.single { it.change.gaining?.className == cn("SoloMode") }
    val soloOpponent = changes.single { it.change.gaining?.className == cn("SoloOpponent") }

    soloOpponent.cause shouldBe Cause(cn("SoloMode").expression, soloMode.ordinal)
    val standardObjective = changes.single {
      it.change.gaining?.className == cn("StandardSoloObjective")
    }
    standardObjective.cause shouldBe Cause(cn("SoloMode").expression, soloMode.ordinal)
  }

  @Test
  internal fun selectedSourcesCreateTheirRuntimeBootstrapComponents() {
    val game = Engine.newGame(canonicalPremise())
    val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
    val terraform = changes.single { it.change.gaining?.className == cn("TerraformingMars") }
    val map = changes.single { it.change.gaining?.className == cn("TharsisMap") }
    val area = changes.single { it.change.gaining?.className == cn("Tharsis_1_1") }

    changes.drop(1).first() shouldBe terraform
    map.cause shouldBe Cause(cn("TerraformingMars").expression, terraform.ordinal)
    area.cause shouldBe Cause(cn("TharsisMap").expression, map.ordinal)
  }

  @Test
  internal fun selectedNondefaultMapsExistBeforeTheDefaultMapIsEvaluated() {
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
  internal fun manualWorkflowStartsFullyEffectfulGenerationOneSetup() {
    val game = Engine.newGame(canonicalPremise())
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).setupPhase()

    val admin = game.agent(ADMIN)
    admin.count("BootstrapPhase") shouldBe 0
    admin.count("SetupPhase") shouldBe 1
    admin.count("Generation") shouldBe 1
    admin.count("StartToken<Player1>") shouldBe 1
    admin.count("TerraformRating<Player1>") shouldBe 20
    admin.count("TerraformRating<Player2>") shouldBe 20

    val setupChanges = game.events.changesSince(checkpoint)
    val setupEvent = setupChanges.single { it.change.gaining.toString() == "SetupPhase" }
    val generationEvent = setupChanges.single { it.change.gaining.toString() == "Generation" }
    setupEvent.cause shouldBe null
    setupEvent.toString().shouldEndWith("(manual)")
    generationEvent.cause shouldBe Cause(cn("SetupPhase").expression, setupEvent.ordinal)
    setupChanges.none { it.change.gaining.toString().startsWith("StartToken") } shouldBe true
    setupChanges
        .filter { it.change.gaining.toString().startsWith("TerraformRating") }
        .also { it.size shouldBe 2 }
        .all { it.cause == Cause(cn("MultiplayerMode").expression, setupEvent.ordinal) } shouldBe
        true
  }

  @Test
  internal fun soloModeProvidesItsStartingTerraformRatingDirectly() {
    val game = Engine.newGame(canonicalPremise(players = 1))
    val checkpoint = game.timeline.checkpoint()

    TfmWorkflow.Manual(game).setupPhase()

    game.agent(ADMIN).count("TerraformRating<Player1>") shouldBe 14
    val setupChanges = game.events.changesSince(checkpoint)
    val setupEvent = setupChanges.single { it.change.gaining.toString() == "SetupPhase" }
    val rating = setupChanges.single { it.change.gaining?.className == cn("TerraformRating") }
    rating.change.count shouldBe 14
    rating.cause shouldBe Cause(cn("SoloMode").expression, setupEvent.ordinal)
    setupChanges.none { it.change.removing?.className == cn("TerraformRating") } shouldBe true
  }

  @Test
  internal fun soloColoniesSetupAutoNarrowsThePlayerOwner() {
    val game =
        Engine.newGame(
            canonicalPremise(
                ColoniesExpansion,
                players = 1,
                colonyTiles = testColonyTiles(players = 1),
            )
        )

    TfmWorkflow.Manual(game).setupPhase()

    game.tfm(PLAYER1).production(cn("MC")) shouldBe -2
  }

  @Test
  internal fun setupKeepsStartingCardsInHandUntilCorporationTurns() {
    val game = Engine.newGame(canonicalPremise(PreludeExpansion))
    val workflow = TfmWorkflow.Auto(game).launch()
    val admin = game.agent(ADMIN)
    val p1 = game.tfm(PLAYER1)

    admin.count("SetupPhase") shouldBe 1
    p1.count("CorporationCard<Hand>") shouldBe 1
    p1.count("ProjectCard<Hand>") shouldBe 10
    p1.count("PreludeCard<Hand>") shouldBe 2

    retainStartingProjects(game, 7, 5)
    admin.count("CorporationPhase") shouldBe 1
    p1.playCorp(cn("InterplanetaryCinematics"), 7)

    p1.count("ProjectCard<Hand>") shouldBe 7
    p1.count("ProjectCard<Selecting>") shouldBe 0
    workflow.shutdown()
  }

  @Test
  internal fun automaticWorkflowWaitsForSoloSetupChoices() {
    val setup = canonicalPremise(players = 1)
    val game = Engine.newGame(setup)
    val workflow = TfmWorkflow.Auto(game).launch()

    val admin = game.agent(ADMIN)
    admin.count("SetupPhase") shouldBe 1
    admin.count("CorporationPhase") shouldBe 0
    admin.count("Generation") shouldBe 1
    game.tasks.isEmpty() shouldBe false
    workflow.isRunning shouldBe true

    workflow.shutdown()
    workflow.isRunning shouldBe false
  }
}
