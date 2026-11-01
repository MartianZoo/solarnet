package dev.martianzoo.tfm.script

import dev.martianzoo.agent.Agent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ScriptCompletionEngineTest {
  private val repl = ScriptSession()
  private val completer = ScriptCompletionEngine(repl)

  init {
    repl.keepStartingProjects(0, 0)
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
    assertEquals(listOf("concrete"), values("auto c"))
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
  internal fun taskListingsHaveNoIdsAndSingletonActionsNeedNoPosition() {
    repl.command("become Player1")
    repl.command("auto none")
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("2? Plant")
    assertTrue(repl.command("tasks").single().startsWith("[Player1] "))
    assertTrue(repl.command("task select").single().startsWith("* [Player1] "))

    repl.command("mode yellow")
    repl.command("task drop")
    taskLayer.addTasks("3? Heat")

    val remaining = repl.command("tasks").single()
    assertTrue(remaining.startsWith("[Player1] "), remaining)
    assertTrue("3? Heat" in remaining, remaining)
  }

  @Test
  internal fun aLeadingNumberRemainsPartOfTheInstruction() {
    repl.command("newgame BRP 2")
    repl.command("become Player1")
    val taskLayer = repl.agent as Agent
    taskLayer.addTasks("3? Plant")

    repl.command("task 2! Plant")

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
