package dev.martianzoo.repl

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.World
import dev.martianzoo.script.generateReplayScript
import dev.martianzoo.tfm.engine.games.Game20230521Test
import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class Game20230521ReplayScriptTest {
  @TempDir lateinit var temporaryDirectory: Path

  @Test
  fun generatedScriptRecreatesLongGame() {
    val fixture = Game20230521Test()
    val source = fixture.completedGame()
    val generated = generateReplayScript(source)
    assertThat(generated).contains("as Player1 tfm_play InventorsGuild, 9 Megacredit")
    assertThat(generated).contains("as Player1 tfm_play BuildingIndustries, 1 Steel, 4 Megacredit")
    assertThat(generated).contains("as Player2 tfm_play RotatorImpacts")
    val scriptFile = temporaryDirectory.resolve("game-2023-replay.rego")
    Files.writeString(scriptFile, generated)

    val replay = newScriptSession()
    val output = replay.command("script $scriptFile")

    assertThat(output.filter { it.startsWith("Error:") }).isEmpty()
    assertThat(eventLog(replay.world)).containsExactlyElementsIn(eventLog(source)).inOrder()
    assertThat(componentGraph(replay.world)).isEqualTo(componentGraph(source))
  }

  private fun eventLog(world: World): List<String> =
      world.events.entriesSinceSetup().map(world.vocabulary::renderPets)

  private fun componentGraph(world: World): Map<String, Int> =
      world.reader.getComponents("Component").entries.associate { (type, count) ->
        world.vocabulary.renderPets(type.expressionFull) to count
      }
}
