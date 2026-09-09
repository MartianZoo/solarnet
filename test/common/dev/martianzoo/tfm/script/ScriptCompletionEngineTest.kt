package dev.martianzoo.tfm.script

import dev.martianzoo.engine.Agent
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

  init {
    repl.command("as Player1 task -10 ProjectCard<Hand>")
    repl.command("as Player2 task -10 ProjectCard<Hand>")
  }

  @Test
  internal fun completesCommandNames() {
    assertTrue("count" in values("co"))
    assertTrue("count" in values("tasks;co"))
    assertEquals("count <Metric>", candidates("co").single { it.value == "count" }.description)
  }

  @Test
  internal fun completesFixedCommandArguments() {
    assertEquals(listOf("blue"), values("mode b"))
    assertEquals(listOf("safe"), values("auto s"))
    assertEquals(listOf("full"), values("log f"))
  }

  @Test
  internal fun completesParticipatingPlayers() {
    assertContainsAll(values("become P"), "Player1", "Player2")
  }

  @Test
  internal fun completesConfiguredPlayerNames() {
    repl.command("newgame \"TerraformingMars\" Blue Yellow")

    assertContainsAll(values("become "), "Blue", "Yellow")
    assertFalse(values("become ").any { it == "Player1" || it == "Player2" })
  }

  @Test
  internal fun completesCardsInTheCurrentSetup() {
    repl.command("newgame BRVPX 2")

    assertContainsAll(values("tfm_play Man"), "Mangrove", "Manutech")
    assertEquals(listOf("Titanium"), values("tfm_play Manutech, T"))
    assertEquals(listOf("AiCentral"), values("tfm_action Ai"))
    assertEquals(listOf("1", "2", "3"), values("tfm_action AiCentral "))
    assertEquals(listOf("Titanium"), values("tfm_action RotatorImpacts 1, T"))
  }

  @Test
  internal fun completesPetsClassNamesInsideExpressions() {
    assertContainsAll(values("exec PROD[Pla"), "PROD[Plant", "PROD[PlantTag")
    assertTrue("Class<ProjectCard" in values("count Class<Pro"))
    assertEquals(listOf("MC"), values("tfm_pay M"))
  }

  @Test
  internal fun narrowsPetsCompletionsWithPetsParser() {
    assertContainsAll(values("exec Plant "), "FROM", "OR", "THEN")
    assertFalse("Player1" in values("exec Plant "))

    assertContainsAll(values("exec Plant OR "), "Plant", "PlantTag")
    assertContainsAll(values("exec Plant, "), "Plant", "PlantTag")
    assertContainsAll(values("exec Plant FROM P"), "Plant", "PlantTag")
    assertContainsAll(values("exec PROD["), "PROD[Plant", "PROD[PlantTag")
    assertContainsAll(values("desc Plant(HAS MAX "), "1", "Plant")
    assertContainsAll(values("count Tag OR "), "1", "PlantTag")
  }

  @Test
  internal fun completesTaskInstructionsAndSingletonTaskActions() {
    (repl.agent as Agent).addTasks("2 Plant?")
    (repl.agent as Agent).addTasks("3 Heat?")

    assertFalse(values("task ").any { it == "A" || it == "B" })
    assertContainsAll(values("task Pl"), "Plant", "PlantTag")
    assertTrue("select" in values("task se"))
    assertContainsAll(values("task 1 Play"), "PlayCardFromHandAction", "Player1")
  }

  @Test
  internal fun taskListingsHaveNoIdsAndSingletonActionsNeedNoPosition() {
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("2 Plant?")
    assertTrue(repl.command("tasks").single().startsWith("[Admin] "))
    assertTrue(repl.command("task select").single().startsWith("* [Admin] "))

    repl.command("mode yellow")
    repl.command("task drop")
    taskLayer.addTasks("3 Heat?")

    val remaining = repl.command("tasks").single()
    assertTrue(remaining.startsWith("[Admin] "), remaining)
    assertTrue("3 Heat<Owner>?" in remaining, remaining)
  }

  @Test
  internal fun selectAndDropRejectMultipleTasks() {
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("2 Plant?")
    taskLayer.addTasks("3 Heat?")
    repl.command("mode yellow")

    assertEquals(
        listOf(
            "this requires exactly one pending task",
            "Usage: task <Instruction> | task <select | drop>",
        ),
        repl.command("task select"),
    )
    assertEquals(
        listOf(
            "this requires exactly one pending task",
            "Usage: task <Instruction> | task <select | drop>",
        ),
        repl.command("task drop"),
    )
  }

  @Test
  internal fun treatsAnUnassignedUppercaseTokenAsAnInstruction() {
    (repl.agent as Agent).addTasks("MarsMap?")
    assertEquals(1, repl.game.tasks.ids().size)

    val output = repl.command("task TharsisMap")

    assertEquals(listOf("um, nothing happened"), output)
    assertTrue(repl.game.tasks.isEmpty())
  }

  @Test
  internal fun rejectsAnInstructionMatchingDistinctTasks() {
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("Plant? OR Ok")
    taskLayer.addTasks("Heat? OR Ok")

    val output = repl.command("task Ok")

    assertTrue(output.single().contains("there wasn't exactly one matching task"))
    assertEquals(2, repl.game.tasks.ids().size)
  }

  @Test
  internal fun aLeadingNumberRemainsPartOfTheInstruction() {
    repl.command("newgame BRP 2")
    repl.command("become Player1")
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("3 Plant?")

    repl.command("task 2 Plant!")

    assertEquals(2, repl.agent.count("Plant"))
  }

  @Test
  internal fun delegatesAsCommandCompletion() {
    assertContainsAll(values("as P"), "Player1")
    assertTrue("mode" in values("as Player1 mo"))
    assertEquals(listOf("blue"), values("as Player1 mode b"))
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
