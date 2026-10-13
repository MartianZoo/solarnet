package dev.martianzoo.tfm.script

import kotlin.test.Test
import kotlin.test.assertEquals

internal class TfmSampleCommandTest {
  @Test
  internal fun `the sample game plays every generation in turn order`() {
    val repl = ScriptSession()

    assertEquals(listOf("Okay, did that."), repl.command("tfm_sample A 8"))

    assertEquals(8, repl.agent.count("Generation"))
  }
}
