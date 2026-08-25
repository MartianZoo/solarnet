package dev.martianzoo.script.tfm

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
    repl.command("newgame \"TerraformingMars\" Mom Ellie")

    assertContainsAll(values("become "), "Mom", "Ellie")
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
    assertEquals(listOf("Megacredit"), values("tfm_pay M"))
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
    (repl.gameplay.godMode() as TaskLayer).addTasks("2 Plant?")
    (repl.gameplay.godMode() as TaskLayer).addTasks("3 Heat?")

    assertFalse(values("task ").any { it == "A" || it == "B" })
    assertContainsAll(values("task Pl"), "Plant", "PlantTag")
    assertTrue("prepare" in values("task pr"))
    assertContainsAll(values("task 1 Play"), "PlayCard", "Player1")
  }

  @Test
  internal fun taskListingsHaveNoIdsAndSingletonActionsNeedNoPosition() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("2 Plant?")
    assertTrue(repl.command("tasks").single().startsWith("[Engine] "))
    assertTrue(repl.command("task prepare").single().startsWith("* [Engine] "))

    repl.command("mode yellow")
    repl.command("task drop")
    taskLayer.addTasks("3 Heat?")

    val remaining = repl.command("tasks").single()
    assertTrue(remaining.startsWith("[Engine] "), remaining)
    assertTrue("3 Heat<Owner>?" in remaining, remaining)
  }

  @Test
  internal fun prepareAndDropRejectMultipleTasks() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("2 Plant?")
    taskLayer.addTasks("3 Heat?")
    repl.command("mode yellow")

    assertEquals(
        listOf(
            "this requires exactly one pending task",
            "Usage: task [<number>] <Instruction> | task <prepare | drop>",
        ),
        repl.command("task prepare"),
    )
    assertEquals(
        listOf(
            "this requires exactly one pending task",
            "Usage: task [<number>] <Instruction> | task <prepare | drop>",
        ),
        repl.command("task drop"),
    )
  }

  @Test
  internal fun treatsAnUnassignedUppercaseTokenAsAnInstruction() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("StandardAction?")
    assertEquals(listOf(null), repl.game.tasks.extract { it.whyPending })

    val output = repl.command("task PlayCardSA")

    assertEquals(listOf("um, nothing happened"), output)
    assertEquals(listOf("abstract"), repl.game.tasks.extract { it.whyPending })
  }

  @Test
  internal fun disambiguatesAnInstructionWithItsCurrentTaskPosition() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("Plant? OR Ok")
    taskLayer.addTasks("Heat? OR Ok")

    repl.command("task 2 Ok")

    assertEquals(
        listOf("Plant<Owner>? OR Ok"),
        repl.game.tasks.extract { it.instruction.toString() },
    )
  }

  @Test
  internal fun taskPositionsAreDerivedAgainAfterRollback() {
    val taskLayer = repl.gameplay.godMode() as TaskLayer
    taskLayer.addTasks("Plant? OR Ok")
    taskLayer.addTasks("Heat? OR Ok")
    val checkpoint = repl.game.timeline.checkpoint()
    repl.command("task 2 Ok")
    assertTrue("Plant<Owner>?" in repl.command("tasks").single())
    repl.command("rollback $checkpoint")

    repl.command("task 2 Ok")
    assertTrue("Plant<Owner>?" in repl.command("tasks").single())
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
