package dev.martianzoo.tfm.script

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class TurnCommandTest {
  @Test fun blueModeOffersAnAction() = playOfferedAction("blue")

  @Test fun greenModeOffersAnAction() = playOfferedAction("green")

  @Test fun yellowModeOffersAnAction() = playOfferedAction("yellow")

  @Test fun redModeOffersAnActionWithConsequences() = playOfferedAction("red")

  @Test
  fun purpleModeLeavesTurnOffersToTheGame() {
    val repl = manualActionGame()
    repl.command("mode purple")
    assertFailsWith<IllegalStateException> { repl.command("turn") }
    assertEquals(0, repl.agent.count("ActionScope"))
    assertEquals(0, repl.agent.count("Baton"))
    repl.command("mode green")
    repl.command("turn")
    repl.command("task UseStandardProject<SellPatentsProject>")
    assertEquals(1, repl.agent.count("ActionScope<Player2>"))
  }

  private fun playOfferedAction(mode: String) {
    val repl = manualActionGame()
    repl.command("mode $mode")
    repl.command("turn")
    assertEquals(0, repl.agent.count("ActionScope"))
    assertEquals(0, repl.agent.count("Baton"))

    repl.command("task UseStandardProject<SellPatentsProject>")
    assertEquals(1, repl.agent.count("ActionScope<Player2>"))
    assertEquals(0, repl.agent.count("ActionScope<Player1>"))
    repl.command("task MC FROM ProjectCard")
    assertEquals(1, repl.agent.count("MC"))
    assertEquals(0, repl.agent.count("ProjectCard"))
    assertEquals(0, repl.agent.count("ActionScope"))
    assertEquals(0, repl.agent.count("Baton"))
  }

  private fun manualActionGame(): ScriptSession =
      ScriptSession().also { repl ->
        repl.command("newgame B 2")
        repl.keepStartingProjects(0, 0)
        repl.command("become Player2")
        repl.agent.runOperation("ProjectCard")
        repl.command("phase Action")
        repl.command("auto none")
      }
}
