package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseScript
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import org.junit.jupiter.api.Test

private class ScriptTest {

  @Test
  fun testScript() {
    val script = parseScript("""

      BECOME Player1

      EXEC Foo, 5 Bar
      COUNT Qux<Wow> -> num

      BECOME Player2
      REQUIRE 4 Bar
      EXEC Abc: Xyz BY Who<Even, Cares>

    """)
    assertThat(script.lines).containsExactly(
        ScriptPragmaPlayer(gte("Player1")),
        ScriptCommand(
            Instruction.Multi(
                Gain(QuantifiedExpression(gte("Foo"))),
                Gain(QuantifiedExpression(gte("Bar"), 5)),
            ),
        ),
        ScriptCounter("num", gte("Qux", gte("Wow"))),
        ScriptPragmaPlayer(gte("Player2")),
        ScriptRequirement(
            Min(QuantifiedExpression(gte("Bar"), 4)),
        ),
        ScriptCommand(
            Instruction.Gated(
                Min(QuantifiedExpression(gte("Abc"))),
                Gain(QuantifiedExpression(gte("Xyz")))),
            gte("Who", "Even", "Cares"),
        ),
    )
  }
}
