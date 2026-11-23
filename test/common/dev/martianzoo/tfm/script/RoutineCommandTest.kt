package dev.martianzoo.tfm.script

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RoutineCommandTest {
  @Test
  internal fun sharedPlayAndPaymentWorkWithBothCommandSpellings() {
    val repl = actionGame("2 Steel, 10 MC, ProjectCard")
    val productionBefore = repl.agent.count("PROD[Steel]")

    repl.command("PlayCard(Mine)")
    assertEquals(0, repl.agent.count("Mine"))
    repl.command("Pay 1 Steel, 2 MC")

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

    repl.command("UseAction1(AiCentral)")

    assertEquals(1, repl.agent.count("ActionUsedMarker<AiCentral>"))
    assertEquals(cardsBefore, repl.agent.count("ProjectCard"))
    repl.command("task 2 ProjectCard")
    assertEquals(cardsBefore + 2, repl.agent.count("ProjectCard"))
  }

  @Test
  internal fun correctionsUseTheSharedImplementation() {
    val repl = actionGame("")
    repl.command("ExMachina(3 MC, 2 Steel)")
    assertEquals(3, repl.agent.count("MC"))
    assertEquals(2, repl.agent.count("Steel"))
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
