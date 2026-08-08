package dev.martianzoo.repl

import com.google.common.truth.Truth.assertThat
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class SaveGameCommandTest {
  @TempDir lateinit var temporaryDirectory: Path

  @Test
  fun savesCurrentGameAsReplayableScript() {
    val source = newScriptSession()
    source.command("newgame BM 2 purple")
    val savedGame = temporaryDirectory.resolve("saved-game.rego")

    assertThat(source.command("save $savedGame"))
        .containsExactly("Saved current game to $savedGame")
    assertThat(savedGame.toFile().readText()).startsWith("newgame ")

    val replay = newScriptSession()
    assertThat(replay.command("script $savedGame").filter { it.startsWith("Error:") }).isEmpty()
    assertThat(replay.world.events.entriesSinceSetup().map(replay.world.vocabulary::renderPets))
        .containsExactlyElementsIn(
            source.world.events.entriesSinceSetup().map(source.world.vocabulary::renderPets)
        )
        .inOrder()
  }
}
