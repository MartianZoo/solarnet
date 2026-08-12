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
  fun completesPlayersWithoutEmittingInputOnlySynonyms() {
    assertContainsAll(values("become P"), "Player1", "Player2")
    assertFalse(values("become P").any { it == "P1" || it == "P2" })
  }

  @Test
  fun completesCardsInTheCurrentSetup() {
    repl.command("newgame BRMVPX 2")

    assertContainsAll(values("tfm_play Man"), "Mangrove", "Manutech")
    assertEquals(listOf("Titanium"), values("tfm_play Manutech, T"))
    assertEquals(listOf("AiCentral"), values("tfm_action Ai"))
    assertEquals(listOf("1", "2", "3"), values("tfm_action AiCentral "))
    assertEquals(listOf("Titanium"), values("tfm_action RotatorImpacts 1, T"))
  }

  @Test
  fun completesPetsClassNamesInsideExpressions() {
    assertContainsAll(values("exec PROD[Pla"), "PROD[Plant", "PROD[PlantTag")
    assertTrue("Class<ProjectCard" in values("count Class<Pro"))
    assertEquals(listOf("Megacredit"), values("tfm_pay M"))
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
  fun completesTaskInstructionsAndSingletonTaskActions() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("2 Plant?")
    (repl.gameplay.godMode() as TaskLayer).addTasks("3 Heat?")

    assertFalse(values("task ").any { it == "A" || it == "B" })
    assertContainsAll(values("task Pl"), "Plant", "PlantTag")
    assertTrue("prepare" in values("task pr"))
    assertContainsAll(values("task 1 Play"), "PlayCard", "Player1")
  }

  @Test
  fun taskListingsHaveNoIdsAndSingletonActionsNeedNoPosition() {
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
  fun prepareAndDropRejectMultipleTasks() {
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
  fun treatsAnUnassignedUppercaseTokenAsAnInstruction() {
    (repl.gameplay.godMode() as TaskLayer).addTasks("StandardAction?")
    assertEquals(listOf(null), repl.game.tasks.extract { it.whyPending })

    val output = repl.command("task PlayCardSA")

    assertEquals(listOf("um, nothing happened"), output)
    assertEquals(listOf("abstract"), repl.game.tasks.extract { it.whyPending })
  }

  @Test
  fun disambiguatesAnInstructionWithItsCurrentTaskPosition() {
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
  fun taskPositionsAreDerivedAgainAfterRollback() {
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
  fun delegatesAsCommandCompletion() {
    assertContainsAll(values("as P"), "Player1")
    assertFalse("P1" in values("as P"))
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
