package dev.martianzoo.tfm.script

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionEngine
import dev.martianzoo.script.ScriptSession
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class ScriptCompletionEngineTest {
  private val repl = ScriptSession()
  private val completer = ScriptCompletionEngine(repl)

  @Test
  fun completesCommandNames() {
    assertThat(values("co")).contains("count")
    assertThat(values("tasks;co")).contains("count")
    assertThat(candidates("co").single { it.value == "count" }.description)
        .isEqualTo("count <Metric>")
  }

  @Test
  fun completesFixedCommandArguments() {
    assertThat(values("mode b")).containsExactly("blue")
    assertThat(values("auto s")).containsExactly("safe")
    assertThat(values("log f")).containsExactly("full")
  }

  @Test
  fun completesPlayersByFullAndShortName() {
    assertThat(values("become P")).containsAtLeast("Player1", "P1", "Player2", "P2")
  }

  @Test
  fun completesCardsInTheCurrentSetup() {
    repl.command("newgame BRMVPX 2")

    assertThat(values("tfm_play Man")).containsAtLeast("Mangrove", "Manutech")
  }

  @Test
  fun completesPetsClassNamesInsideExpressions() {
    assertThat(values("exec PROD[Pla")).containsAtLeast("PROD[Plant", "PROD[PlantTag")
    assertThat(values("count Class<Pro")).contains("Class<ProjectCard")
  }

  @Test
  fun narrowsPetsCompletionsWithPetsParser() {
    assertThat(values("exec Plant ")).containsAtLeast("FROM", "OR", "THEN")
    assertThat(values("exec Plant ")).containsNoneOf("Player1", "P1")

    assertThat(values("exec Plant OR ")).containsAtLeast("Plant", "PlantTag")
    assertThat(values("exec Plant, ")).containsAtLeast("Plant", "PlantTag")
    assertThat(values("exec Plant FROM P")).containsAtLeast("Plant", "PlantTag")
    assertThat(values("exec PROD[")).containsAtLeast("PROD[Plant", "PROD[PlantTag")
    assertThat(values("desc Plant(HAS MAX ")).containsAtLeast("1", "Plant")
    assertThat(values("count Tag + ")).containsAtLeast("1", "PlantTag")
  }

  @Test
  fun completesTaskIdsAndTaskRevisions() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("2 Plant?")
    (repl.gameplay.godMode() as TaskLayer).addTasks("3 Heat?")

    assertThat(values("task ")).containsAtLeast("A", "B")
    assertThat(candidates("task ").single { it.value == "A" }.description)
        .isEqualTo("2 Plant<Owner>?")
    assertThat(values("task A pr")).contains("prepare")
    assertThat(values("task A Play")).containsAtLeast("PlayCard", "Player1")
  }

  @Test
  fun completesScriptPaths(@TempDir tempDir: Path) {
    val file = tempDir.resolve("completion-test.rego").toFile()
    val dir = tempDir.resolve("completion-dir").toFile()
    file.writeText("status\n")
    dir.mkdir()

    assertThat(values("script ${tempDir.resolve("completion-test")}")).contains(file.toString())
    val dirCandidate = candidates("script ${tempDir.resolve("completion-d")}").single()
    assertThat(dirCandidate.value).isEqualTo("${dir}${File.separator}")
    assertThat(dirCandidate.complete).isFalse()
  }

  @Test
  fun delegatesAsCommandCompletion() {
    assertThat(values("as P")).containsAtLeast("Player1", "P1")
    assertThat(values("as P1 mo")).contains("mode")
    assertThat(values("as P1 mode b")).containsExactly("blue")
  }

  private fun values(line: String): List<String> = candidates(line).map { it.value }

  private fun candidates(line: String): List<ScriptCompletion> = completer.completeLine(line)
}
