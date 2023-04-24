package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
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
            """) as MClassLoader

    val d = loader.allDefaults[cn("Foo1")]!!
    assertThat(d.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d.removeOnly.intensity).isEqualTo(MANDATORY)

    val d2 = loader.allDefaults[cn("FooBar1")]!!
    assertThat(d2.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d2.removeOnly.intensity).isEqualTo(OPTIONAL)

    val d3 = loader.allDefaults[cn("Fixed")]!!
    assertThat(d3.gainOnly.intensity).isEqualTo(AMAP)
    assertThat(d3.removeOnly.intensity).isEqualTo(MANDATORY)
  }
}
