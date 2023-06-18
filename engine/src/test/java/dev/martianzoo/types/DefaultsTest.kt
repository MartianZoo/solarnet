package dev.martianzoo.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.pets.ast.Instruction.Intensity.OPTIONAL
import org.junit.jupiter.api.Test

class DefaultsTest {
  @Test
  fun testIntensities() {
    val loader =
        loader(
            """
              CLASS Class<Component>
              ABSTRACT CLASS Component {
                DEFAULT +Component!
                DEFAULT -Component!
              }
              CLASS Ok
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
            """)
            as MClassLoader

    val d = loader.getClass(cn("Foo1")).defaults
    assertThat(d.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d.removeOnly.intensity).isEqualTo(MANDATORY)

    val d2 = loader.getClass(cn("FooBar1")).defaults
    assertThat(d2.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d2.removeOnly.intensity).isEqualTo(OPTIONAL)

    val d3 = loader.getClass(cn("Fixed")).defaults
    assertThat(d3.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d3.removeOnly.intensity).isEqualTo(MANDATORY)
  }
}
