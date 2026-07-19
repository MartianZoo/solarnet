package dev.martianzoo.types

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DefaultsTest {
  @Test
  fun testIntensities() {
    val loader =
        loader(
            """
              CLASS Foo1 {
                DEFAULT +Foo1.
              }
              CLASS Bar1 {
                DEFAULT -Bar1?
              }
              CLASS FooBar1 : Foo1, Bar1
              CLASS Qux1 {
                DEFAULT +Qux1!
              }
              CLASS Fixed: Qux1 {
                DEFAULT +Fixed.
              }
            """
        )
            as MClassLoader

    val d = loader.getClass(cn("Foo1")).defaults
    d.gainOnly.intensity shouldBe AMAP
    d.removeOnly.intensity shouldBe MANDATORY

    val d2 = loader.getClass(cn("FooBar1")).defaults
    d2.gainOnly.intensity shouldBe AMAP
    d2.removeOnly.intensity shouldBe OPTIONAL

    val d3 = loader.getClass(cn("Fixed")).defaults
    d3.gainOnly.intensity shouldBe AMAP
    d3.removeOnly.intensity shouldBe MANDATORY
  }
}
