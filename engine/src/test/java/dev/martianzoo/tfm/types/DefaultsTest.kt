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

    val d = loader.allDefaults[cn("Foo1")]!!
    assertThat(d.gainIntensity).isEqualTo(AMAP)
    assertThat(d.removeIntensity).isEqualTo(MANDATORY)

    val d2 = loader.allDefaults[cn("FooBar1")]!!
    assertThat(d2.gainIntensity).isEqualTo(AMAP)
    assertThat(d2.removeIntensity).isEqualTo(OPTIONAL)

    val d3 = loader.allDefaults[cn("Fixed")]!!
    assertThat(d3.gainIntensity).isEqualTo(AMAP)
    assertThat(d3.removeIntensity).isEqualTo(MANDATORY)
  }
}
