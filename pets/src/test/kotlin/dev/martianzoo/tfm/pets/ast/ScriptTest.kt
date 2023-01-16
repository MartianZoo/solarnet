package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parseScript
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Gain
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.ScalarAndType.Companion.sat
import dev.martianzoo.tfm.pets.ast.Script.ScriptCommand
import dev.martianzoo.tfm.pets.ast.Script.ScriptCounter
import dev.martianzoo.tfm.pets.ast.Script.ScriptPragmaPlayer
import dev.martianzoo.tfm.pets.ast.Script.ScriptRequirement
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
        ScriptPragmaPlayer(cn("Player1").type),
        ScriptCommand(
            Instruction.Multi(
                Gain(sat(type = cn("Foo").type)),
                Gain(sat(5, cn("Bar").type)),
            ),
        ),
        ScriptCounter("num", cn("Qux").addArgs(cn("Wow").type)),
        ScriptPragmaPlayer(cn("Player2").type),
        ScriptRequirement(
            Min(sat(4, cn("Bar").type)),
        ),
        ScriptCommand(
            Instruction.Gated(
                Min(sat(type = cn("Abc").type)),
                Gain(sat(type = cn("Xyz").type))),
            cn("Who").addArgs(cn("Even").type, cn("Cares").type),
        ),
    )
  }
}
