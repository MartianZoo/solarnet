package dev.martianzoo.tfm.script

import dev.martianzoo.script.ScriptSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
}
