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
    assertContainsAll(values("tfm_play Manutech, T"), "Titanium", "T")
  }

  @Test
  fun completesPetsClassNamesInsideExpressions() {
    assertContainsAll(values("exec PROD[Pla"), "PROD[Plant", "PROD[PlantTag")
    assertTrue("Class<ProjectCard" in values("count Class<Pro"))
    assertContainsAll(values("tfm_pay M"), "M", "Megacredit")
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
    assertContainsAll(values("count Tag OR "), "1", "PlantTag")
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
  fun keepsLabelsThroughEditsAndRestartsWhenNoLabeledTaskRemainsPending() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("2 Plant?")
    assertTrue(repl.command("tasks").single().startsWith("A "))
    assertTrue(repl.command("task A prepare").single().startsWith("A* "))

    taskLayer.addTasks("3 Heat?")
    repl.command("mode yellow")
    repl.command("task A drop")

    val remaining = repl.command("tasks").single()
    assertTrue(remaining.startsWith("A "), remaining)
    assertTrue("3 Heat<Owner>?" in remaining, remaining)
  }

  @Test
  fun treatsAnUnassignedUppercaseTokenAsAnInstruction() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("StandardAction?")
    assertEquals(listOf(null), repl.game.tasks.extract { it.whyPending })

    val output = repl.command("task PlayCardSA")

    assertEquals(listOf("um, nothing happened"), output)
    assertEquals(listOf("abstract"), repl.game.tasks.extract { it.whyPending })
  }

  @Test
  fun restoresLabelsOfTasksRestoredByRollback() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("2 Plant?")
    assertTrue(repl.command("tasks").single().startsWith("A "))

    taskLayer.addTasks("3 Heat?")
    repl.command("mode yellow")
    val checkpoint = repl.game.timeline.checkpoint()
    repl.command("task A drop")
    assertTrue(repl.command("tasks").single().startsWith("A "))
    repl.command("rollback $checkpoint")

    val restored = repl.command("tasks")
    assertTrue(restored[0].startsWith("A "), "$restored")
    assertTrue(restored[1].startsWith("B "), "$restored")
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
