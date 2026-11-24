package dev.martianzoo.tfm.script

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class RoutineCommandTest {
  @Test
  internal fun sharedPlayAndPaymentWorkWithBothCommandSpellings() {
    val repl = actionGame("2 Steel, 10 MC, ProjectCard")
    val productionBefore = repl.agent.count("PROD[Steel]")

    repl.command("playCard(Mine)")
    assertEquals(0, repl.agent.count("Mine"))
    repl.command("pay 1 Steel, 2 MC")

    assertEquals(1, repl.agent.count("Mine"))
    assertEquals(1, repl.agent.count("Steel"))
    assertEquals(8, repl.agent.count("MC"))
    assertEquals(productionBefore, repl.agent.count("PROD[Steel]"))
    repl.command("task PROD[Steel]")
    assertEquals(productionBefore + 1, repl.agent.count("PROD[Steel]"))
  }

  @Test
  internal fun sharedActionsLeaveTheirChoicesForExplicitTasks() {
    val repl = actionGame("PROD[Energy], AiCentral")
    val cardsBefore = repl.agent.count("ProjectCard")

    repl.command("useAction1(AiCentral)")

    assertEquals(1, repl.agent.count("ActionUsedMarker<AiCentral>"))
    assertEquals(cardsBefore, repl.agent.count("ProjectCard"))
    repl.command("task 2 ProjectCard")
    assertEquals(cardsBefore + 2, repl.agent.count("ProjectCard"))
  }

  @Test
  internal fun correctionsUseTheSharedImplementation() {
    val repl = actionGame("")
    repl.command("exMachina(3 MC, 2 Steel)")
    assertEquals(3, repl.agent.count("MC"))
    assertEquals(2, repl.agent.count("Steel"))
  }

  @Test
  internal fun aStandardActionUsesTheSharedRoutine() {
    val repl = actionGame("8 Heat")

    repl.command("action(ConvertHeatAction)")
    assertEquals(8, repl.agent.count("Owed<Class<Heat>>"))
    repl.command("pay(8 Heat)")
    assertEquals(0, repl.agent.count("Heat"))
    assertEquals(0, repl.agent.count("Owed"))
    val before = repl.agent.count("TemperatureStep")
    repl.command("task TemperatureStep")
    assertEquals(before + 1, repl.agent.count("TemperatureStep"))
  }

  @Test
  internal fun assertionsWorkThroughBothCommandSpellings() {
    val repl = actionGame("2 Steel")
    assertEquals(listOf("Assertion passed."), repl.command("assert(=2 Steel, MAX 0 OxygenStep)"))
    assertEquals(listOf("Assertion passed."), repl.command("assert =2 Steel"))
    val failure = repl.command("assert(=3 Steel)").single()
    assertTrue("Assertion failed for Player1: =3 Steel" in failure)
    assertTrue("Steel<Player1> = 2" in failure)
  }

  private fun actionGame(contents: String): ScriptSession {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.agents[PLAYER1].doTask("-10 ProjectCard<Selecting>")
    repl.agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    repl.command("become Player1")
    if (contents.isNotEmpty()) repl.agent.runOperation(contents)
    repl.command("phase Action")
    repl.agent.autoExecPolicy = NONE
    repl.agent.beginOperation("OfferAction")
    return repl
  }
}
