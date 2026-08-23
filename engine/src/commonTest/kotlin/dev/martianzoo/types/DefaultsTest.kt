package dev.martianzoo.types

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class DefaultsTest {
  @Test
  internal fun testIntensities() {
    val classTable =
        loader(
            """
              ABSTRACT CLASS Foo1 {
                DEFAULT +Foo1.
              }
              ABSTRACT CLASS Bar1 {
                DEFAULT -Bar1?
              }
              CLASS FooBar1 : Foo1, Bar1
              ABSTRACT CLASS Qux1 {
                DEFAULT +Qux1!
              }
              CLASS Fixed: Qux1 {
                DEFAULT +Fixed.
              }
            """
        )

    val d = classTable.getClass(cn("Foo1")).defaults
    d.gainOnly.intensity shouldBe AMAP
    d.removeOnly.intensity shouldBe MANDATORY

    val d2 = classTable.getClass(cn("FooBar1")).defaults
    d2.gainOnly.intensity shouldBe AMAP
    d2.removeOnly.intensity shouldBe OPTIONAL

    val d3 = classTable.getClass(cn("Fixed")).defaults
    d3.gainOnly.intensity shouldBe AMAP
    d3.removeOnly.intensity shouldBe MANDATORY
  }
}
