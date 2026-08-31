package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

internal class DoCommandTest {
  @Test
  internal fun doRequiresPurpleMode() {
    val repl = ScriptSession()

    val output = repl.command("do tasks(Plant)")

    assertEquals(listOf("DO requires purple mode", "Usage: do <RoutineCall>"), output)
  }

  @Test
  internal fun routineCallKeepsNestedCommasTogether() {
    val call = RoutineCall.parse("tasks(PROD[Energy, Steel], Animal<Dad, Pets>)")

    assertEquals("tasks", call.name)
    assertEquals(listOf("PROD[Energy, Steel]", "Animal<Dad, Pets>"), call.arguments)
  }

  @Test
  internal fun routineCallNameMustUseLowerCamelCase() {
    assertFailsWith<ScriptSession.UsageException> { RoutineCall.parse("Tasks(Plant)") }
  }

  @Test
  internal fun playCardRoutineRejectsMcBeyondTheRemainingDebt() {
    val repl = ScriptSession()
    repl.command("newgame BRP 2")
    repl.command("become Player1")
    repl.gameplay.godMode().manual("30 MC, ProjectCard")
    repl.command("phase Action")
    repl.gameplay.godMode().beginManual("NewTurn")
    repl.command("mode purple")

    val output = repl.command("do playCard(Mine, -5 MC)")

    assertTrue(output.single().contains("Overpaying 5 MC when only 4 is owed"), output.single())
    assertEquals(30, repl.gameplay.count("MC"))
    assertEquals(1, repl.gameplay.count("ProjectCard"))
    assertEquals(0, repl.gameplay.count("Mine"))
  }
}
