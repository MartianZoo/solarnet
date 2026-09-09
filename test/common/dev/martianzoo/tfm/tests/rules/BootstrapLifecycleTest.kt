package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.engine.Timeline.Checkpoint
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ADMIN
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldInclude
import kotlin.test.Test

internal class BootstrapLifecycleTest {
  @Test
  internal fun newGameReturnsCommittedBootstrapState() {
    val resolvedPremise = canonicalPremise()
    val game = Engine.newGame(resolvedPremise)
    val admin = game.agent(ADMIN)

    admin.count("Premise") shouldBe 1
    admin.count("ModulesReady") shouldBe 0
    admin.count("StandardGpTrackRules") shouldBe 1
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

    shouldThrow<IllegalArgumentException> { game.timeline.rollBack(Checkpoint(0)) }
        .message
        .orEmpty()
        .shouldInclude("committed through")
  }

  @Test
  internal fun generatedPremiseAloneCreatesRepresentativeConfiguredWorlds() {
    data class Scenario(
        val options: List<TestOption>,
        val players: Int,
        val map: TestOption,
        val colonyTiles: Set<ClassName> = emptySet(),
    )

    val scenarios =
        listOf(
            Scenario(emptyList(), players = 1, map = Tharsis),
            Scenario(listOf(Hellas, PreludeExpansion), players = 2, map = Hellas),
            Scenario(
                listOf(Amazonis, VenusNextExpansion, Prelude2Expansion),
                players = 3,
                map = Amazonis,
            ),
            Scenario(
                listOf(Cimmeria, ColoniesExpansion),
                players = 4,
                map = Cimmeria,
                colonyTiles = testColonyTiles(players = 4),
            ),
        )

    scenarios.forEach { scenario ->
      val premise =
          canonicalPremise(
              *scenario.options.toTypedArray(),
              players = scenario.players,
              colonyTiles = scenario.colonyTiles,
          )
      val game = Engine.newGame(premise)
      val admin = game.agent(ADMIN)
      val changes = game.events.entriesSince(Checkpoint(0)).filterIsInstance<ChangeEvent>()
      val premiseEvent = changes.single { it.change.gaining?.className == cn("Premise") }
      val premiseCause = Cause(cn("Premise").expression, premiseEvent.ordinal)
      val modulesReady = changes.single { it.change.gaining?.className == cn("ModulesReady") }

      admin.count("Player") shouldBe scenario.players
      admin.count("${scenario.map.className}") shouldBe 1
      game.tasks.isEmpty() shouldBe true

      val configuredExpressions =
          premise.modules.map { it.expression } +
              premise.playerNames.map { it.expression } +
              premise.initialComponentTypes
      configuredExpressions.forEach { expression ->
        val creation = changes.single { it.change.gaining == expression }
        creation.cause shouldBe premiseCause
        (creation.ordinal < modulesReady.ordinal) shouldBe true
      }
    }
  }

  @Test
  internal fun modulesChooseTheirTrackRulesAfterTheCompleteModuleSetExists() {
    val game = Engine.newGame(canonicalPremise(Amazonis, VenusNextExpansion))
    val admin = game.agent(ADMIN)

    admin.count("ExtendedGlobalParametersRule") shouldBe 1
    admin.count("StandardGpTrackRules") shouldBe 0
    admin.count("StandardVenusTrackRules") shouldBe 0
    admin.count("ExtendedVenusTrackRules") shouldBe 1
  }

  @Test
  internal fun manualWorkflowStartsFullyEffectfulGenerationOneSetup() {
    val game = Engine.newGame(canonicalPremise())
    TfmWorkflow.Manual(game).setupPhase()

    val admin = game.agent(ADMIN)
    admin.count("BootstrapPhase") shouldBe 0
    admin.count("SetupPhase") shouldBe 1
    admin.count("Generation") shouldBe 1
    admin.count("StartToken<Player1>") shouldBe 1
    admin.count("TerraformRating<Player1>") shouldBe 20
    admin.count("TerraformRating<Player2>") shouldBe 20
  }

  @Test
  internal fun soloModeProvidesItsStartingTerraformRatingDirectly() {
    val game = Engine.newGame(canonicalPremise(players = 1))
    TfmWorkflow.Manual(game).setupPhase()

    game.agent(ADMIN).count("TerraformRating<Player1>") shouldBe 14
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
