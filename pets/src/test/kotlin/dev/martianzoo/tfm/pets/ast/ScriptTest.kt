package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser.parseScript
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import org.junit.jupiter.api.Test

class ScriptTest {

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
        ScriptPragmaPlayer(te("Player1")),
        ScriptCommand(
            Instruction.Multi(
                Gain(QuantifiedExpression(te("Foo"))),
                Gain(QuantifiedExpression(te("Bar"), 5)),
            ),
        ),
        ScriptCounter("num", te("Qux", te("Wow"))),
        ScriptPragmaPlayer(te("Player2")),
        ScriptRequirement(
            Min(QuantifiedExpression(te("Bar"), 4)),
        ),
        ScriptCommand(
            Instruction.Gated(
                Min(QuantifiedExpression(te("Abc"))),
                Gain(QuantifiedExpression(te("Xyz")))),
            te("Who", "Even", "Cares"),
        ),
    )
  }
}
