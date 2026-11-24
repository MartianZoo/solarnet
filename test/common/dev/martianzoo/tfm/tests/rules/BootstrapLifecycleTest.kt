package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agenttestsupport.runSystem
import dev.martianzoo.agenttestsupport.withTestSoloSetup
import dev.martianzoo.engine.*
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestHelpers.assertProds
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlin.test.Test

internal class BootstrapLifecycleTest {
  @Test
  internal fun startingTheGameRunsFullyEffectfulGenerationOneSetup() {
    val game = TfmEngine.newGame(canonicalPremise())
    val agents = Agents(game) { NONE }
    val p1 = agents[PLAYER1]
    val p2 = agents[PLAYER2]
    agents.runSystem("SetupPhase FROM BootstrapPhase")

    val admin = agents.world.reader
    admin.count("BootstrapPhase") shouldBe 0
    admin.count("SetupPhase") shouldBe 1
    admin.count("Generation") shouldBe 1
    admin.count("StartToken<Player1>") shouldBe 1
    admin.count("TerraformRating<Player1>") shouldBe 0
    admin.count("TerraformRating<Player2>") shouldBe 0
    p1.doTask("20 TerraformRating")
    p2.doTask("20 TerraformRating")
    admin.count("TerraformRating<Player1>") shouldBe 20
    admin.count("TerraformRating<Player2>") shouldBe 20
  }

  @Test
  internal fun soloPlayerTakesStartingTerraformRatingDuringSetup() {
    val game = TfmEngine.newGame(canonicalPremise(players = 1))
    val agents = Agents(game) { NONE }
    val player = agents[PLAYER1]
    agents.runSystem("SetupPhase FROM BootstrapPhase")

    agents.world.reader.count("TerraformRating<Player1>") shouldBe 0
    player.doTask("14 TerraformRating")
    agents.world.reader.count("TerraformRating<Player1>") shouldBe 14
  }

  @Test
  internal fun soloSetupRejectsAdjacentCitiesAndDistantGreenery() {
    for (locations in
        listOf(
            listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_4_2", "Tharsis_4_3"),
            listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_2_2", "Tharsis_4_2"),
        )) {
      shouldThrow<IllegalArgumentException> {
            withTestSoloSetup(canonicalPremise(players = 1), locations.map(::cn))
          }
          .message
          .shouldContain(
              if (locations[2] == "Tharsis_4_2") "cities cannot be adjacent"
              else "greenery must be adjacent"
          )
    }
  }

  @Test
  internal fun soloColoniesSetupRemovesOneSelectedColony() {
    val colonies = testColonyTiles(players = 1)
    val game =
        TfmEngine.newGame(
            canonicalPremise(
                ColoniesExpansion,
                players = 1,
                colonyTiles = colonies,
            )
        )
    val agents = Agents(game) { NONE }
    val admin = agents.world.reader
    val player = agents[PLAYER1]

    admin.count("SelectedColonyTile") shouldBe 4
    admin.count("SoloColoniesSetup") shouldBe 0

    agents.runSystem("SetupPhase FROM BootstrapPhase")

    admin.count("SoloColoniesSetup") shouldBe 1
    player.doTask("-SelectedColonyTile<Class<${colonies.first()}>>")
    admin.count("SelectedColonyTile") shouldBe 3
    admin.count("SoloColoniesSetup") shouldBe 0
    player.assertProds(-2 to "MC")
  }

  @Test
  internal fun selectedColoniesAreRealizedBeforeCorporationTurns() {
    val premise =
        canonicalPremise(
            ColoniesExpansion,
            colonyTiles = setOf(cn("Callisto"), cn("Enceladus")),
        )
    val game = TfmEngine.newGame(premise)
    val agents = Agents(game) { NONE }
    val admin = agents.world.reader

    admin.count("SelectedColonyTile") shouldBe 2
    admin.count("Callisto") shouldBe 0
    admin.count("DelayedEnceladus") shouldBe 0

    agents.runSystem("SetupPhase FROM BootstrapPhase")
    admin.count("SelectedColonyTile") shouldBe 2

    for (player in listOf(PLAYER1, PLAYER2)) {
      agents[player].doTasks(
          "20 TerraformRating",
          "CorporationCard",
          "10 ProjectCard<Selecting>",
          "-10 ProjectCard<Selecting>",
      )
    }
    admin.count("CorporationPhase") shouldBe 1

    admin.count("SelectedColonyTile") shouldBe 0
    admin.count("Callisto") shouldBe 1
    admin.count("DelayedEnceladus") shouldBe 1
    admin.count("Enceladus") shouldBe 0
  }

  @Test
  internal fun setupWaitsForStartingProjectSelection() {
    val game = TfmEngine.newGame(canonicalPremise(PreludeExpansion))
    val agents = Agents(game) { NONE }
    agents.runSystem("SetupPhase FROM BootstrapPhase")
    val admin = agents.world.reader
    val p1 = agents[PLAYER1]
    for (player in listOf(PLAYER1, PLAYER2)) {
      agents[player].doTasks(
          "20 TerraformRating",
          "CorporationCard",
          "2 PreludeCard",
          "10 ProjectCard<Selecting>",
      )
    }

    p1.count("CorporationCard") shouldBe 1
    p1.count("ProjectCard<Selecting>") shouldBe 10
    p1.count("PreludeCard") shouldBe 2
    admin.count("SetupPhase") shouldBe 1
    admin.count("CorporationPhase") shouldBe 0
    p1.doTask("-3 ProjectCard<Selecting>")
    agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    admin.count("CorporationPhase") shouldBe 1
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
        "30 MC",
        "20 Steel",
        "21 Pay<> FROM MC",
        "7 ProjectCard<Hand FROM Selecting>",
    )

    p1.count("ProjectCard") shouldBe 7
  }

  @Test
  internal fun soloSetupWaitsForItsChoices() {
    val setup = canonicalPremise(players = 1)
    val game = TfmEngine.newGame(setup)
    val agents = Agents(game) { NONE }
    agents.runSystem("SetupPhase FROM BootstrapPhase")

    val admin = agents.world.reader
    admin.count("SetupPhase") shouldBe 1
    admin.count("CorporationPhase") shouldBe 0
    admin.count("Generation") shouldBe 1
    game.tasks.isEmpty() shouldBe false
  }
}
