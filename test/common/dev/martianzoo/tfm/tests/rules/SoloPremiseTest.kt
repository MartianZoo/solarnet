package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.Agents
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.GameFileImporter
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.engine.recording
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.state.RecordedInput
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.soloTileSetup
import dev.martianzoo.tfm.engine.TfmEngine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

internal class SoloPremiseTest {
  @Test
  fun setupPlacesThePremiseLocationsWithoutAdminChoicesAndReplays() = runTest {
    val locations = listOf("Tharsis_4_1", "Tharsis_5_1", "Tharsis_2_2", "Tharsis_2_3").map(::cn)
    val premise = soloTileSetup(Canon.gamePremise(GameConfig("TerraformingMars", "Me")), locations)
    val agents = Agents(TfmEngine.newGame(premise)) { NONE }

    agents.taskLog.start()
    agents.tfm(ADMIN).startGame()

    val admin = agents[ADMIN]
    admin.count("CityTile<Tharsis_4_1, SoloOpponent>") shouldBe 1
    admin.count("GreeneryTile<Tharsis_5_1, SoloOpponent>") shouldBe 1
    admin.count("CityTile<Tharsis_2_2, SoloOpponent>") shouldBe 1
    admin.count("GreeneryTile<Tharsis_2_3, SoloOpponent>") shouldBe 1
    val inputs = agents.taskLog.inputs()
    inputs.filterIsInstance<RecordedInput.Command>().any { it.actor == ADMIN } shouldBe false
    for (short in listOf(false, true)) {
      val document =
          GameFileJson.parse(GameFileJson.encode(agents.world.recording(), inputs, short))
      document.premise(Canon).premiseClassDeclarations shouldBe premise.premiseClassDeclarations
      val replay =
          GameFileImporter.replay(
              document,
              Canon,
              newAgents = { Agents(TfmEngine.newGame(it)) },
              startGame = { it.tfm(ADMIN).startGame() },
          )
      replay[ADMIN].list("Component").entries shouldBe admin.list("Component").entries
    }
  }

  @Test
  fun missingSoloLocationsFailBeforeStartingTheWorld() {
    val premise = Canon.gamePremise(GameConfig("TerraformingMars", "Me"))
    shouldThrow<InvalidGameConfigException> { TfmEngine.newGame(premise) }
  }
}
