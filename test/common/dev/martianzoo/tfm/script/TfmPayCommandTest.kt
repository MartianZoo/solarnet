package dev.martianzoo.tfm.script

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.testsupport.PLAYER2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TfmPayCommandTest {
  @Test
  internal fun `cash payment keeps unused steel and card-held microbes`() {
    val repl = paymentGame("Psychrophiles, 2 Microbe<Psychrophiles>, 2 Steel, 23 MC")
    repl.agent.doTasks(
        "UseAction<PlayCardAction, Action1>",
        "PlayCard<Class<ProjectCard>, Class<ProtectedValley>, Hand>",
    )

    val output = repl.command("tfm_pay 23 MC")

    assertEquals(0, repl.agent.count("Owed"), output.joinToString("\n"))
    assertEquals(0, repl.agent.count("MC"))
    assertEquals(2, repl.agent.count("Steel"))
    assertEquals(2, repl.agent.count("Microbe<Psychrophiles>"))
    assertEquals(NONE, repl.agent.autoExecPolicy)
    assertTrue(output.any { "Pay" in it }, output.joinToString("\n"))
    repl.agent.doTask("ProtectedValley FROM ProjectCard")
    assertEquals(1, repl.agent.count("ProtectedValley"))
  }

  @Test
  internal fun `metal-only payment uses the stated units and preserves cash`() {
    val repl = paymentGame("3 Steel, 10 MC")
    repl.agent.doTasks(
        "UseAction<PlayCardAction, Action1>",
        "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
    )

    val output = repl.command("tfm_pay 2 Steel")

    assertEquals(0, repl.agent.count("Owed"), output.joinToString("\n"))
    assertEquals(1, repl.agent.count("Steel"))
    assertEquals(10, repl.agent.count("MC"))
    assertEquals(NONE, repl.agent.autoExecPolicy)
    repl.agent.doTask("Mine FROM ProjectCard")
    assertEquals(1, repl.agent.count("Mine"))
  }

  @Test
  internal fun `a failed standalone payment preserves resources and policy`() {
    val repl = paymentGame("2 Steel, 6 MC")
    repl.agent.doTasks(
        "UseAction<PlayCardAction, Action1>",
        "PlayCard<Class<ProjectCard>, Class<Mine>, Hand>",
    )
    repl.agent.autoExecPolicy = EAGER

    val output = repl.command("tfm_pay 1 Energy")

    assertTrue(output.single().contains("does not narrow"), output.joinToString("\n"))
    assertEquals(6, repl.agent.count("MC"))
    assertEquals(2, repl.agent.count("Steel"))
    assertEquals(4, repl.agent.count("Owed"))
    assertEquals(EAGER, repl.agent.autoExecPolicy)
  }

  private fun paymentGame(contents: String): ScriptSession {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.agents[PLAYER1].doTask("-10 ProjectCard<Selecting>")
    repl.agents[PLAYER2].doTask("-10 ProjectCard<Selecting>")
    repl.command("become Player1")
    repl.agent.runOperation("ProjectCard, $contents")
    repl.command("phase Action")
    repl.agent.beginOperation("OfferAction")
    repl.agent.autoExecPolicy = NONE
    return repl
  }
}
