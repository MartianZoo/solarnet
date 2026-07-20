package dev.martianzoo.tfm.script

import dev.martianzoo.engine.Gameplay.TaskLayer
import dev.martianzoo.script.ScriptCompletion
import dev.martianzoo.script.ScriptCompletionEngine
import dev.martianzoo.script.ScriptSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ScriptCompletionEngineTest {
  private val repl = ScriptSession()
  private val completer = ScriptCompletionEngine(repl)

  @Test
  fun completesCommandNames() {
    assertTrue("count" in values("co"))
    assertTrue("count" in values("tasks;co"))
    assertEquals("count <Metric>", candidates("co").single { it.value == "count" }.description)
  }

  @Test
  fun completesFixedCommandArguments() {
    assertEquals(listOf("blue"), values("mode b"))
    assertEquals(listOf("safe"), values("auto s"))
    assertEquals(listOf("full"), values("log f"))
  }

  @Test
  fun completesPlayersByFullAndShortName() {
    assertContainsAll(values("become P"), "Player1", "P1", "Player2", "P2")
  }

  @Test
  fun completesCardsInTheCurrentSetup() {
    repl.command("newgame BRMVPX 2")

    assertContainsAll(values("tfm_play Man"), "Mangrove", "Manutech")
  }

  @Test
  fun completesPetsClassNamesInsideExpressions() {
    assertContainsAll(values("exec PROD[Pla"), "PROD[Plant", "PROD[PlantTag")
    assertTrue("Class<ProjectCard" in values("count Class<Pro"))
  }

  @Test
  fun narrowsPetsCompletionsWithPetsParser() {
    assertContainsAll(values("exec Plant "), "FROM", "OR", "THEN")
    assertFalse(values("exec Plant ").any { it == "Player1" || it == "P1" })

    assertContainsAll(values("exec Plant OR "), "Plant", "PlantTag")
    assertContainsAll(values("exec Plant, "), "Plant", "PlantTag")
    assertContainsAll(values("exec Plant FROM P"), "Plant", "PlantTag")
    assertContainsAll(values("exec PROD["), "PROD[Plant", "PROD[PlantTag")
    assertContainsAll(values("desc Plant(HAS MAX "), "1", "Plant")
    assertContainsAll(values("count Tag + "), "1", "PlantTag")
  }

  @Test
  fun completesTaskIdsAndTaskRevisions() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("2 Plant?")
    (repl.gameplay.godMode() as TaskLayer).addTasks("3 Heat?")

    assertContainsAll(values("task "), "A", "B")
    assertEquals("2 Plant<Owner>?", candidates("task ").single { it.value == "A" }.description)
    assertTrue("prepare" in values("task A pr"))
    assertContainsAll(values("task A Play"), "PlayCard", "Player1")
  }

  @Test
  fun delegatesAsCommandCompletion() {
    assertContainsAll(values("as P"), "Player1", "P1")
    assertTrue("mode" in values("as P1 mo"))
    assertEquals(listOf("blue"), values("as P1 mode b"))
  }

  private fun values(line: String): List<String> = candidates(line).map { it.value }

  private fun candidates(line: String): List<ScriptCompletion> = completer.completeLine(line)

  private fun assertContainsAll(actual: List<String>, vararg expected: String) {
    assertTrue(
        actual.containsAll(expected.toList()),
        "Expected $actual to contain ${expected.toList()}",
    )
  }
}
