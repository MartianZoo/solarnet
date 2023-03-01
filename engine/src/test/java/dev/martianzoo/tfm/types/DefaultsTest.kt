package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.AMAP
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.MANDATORY
import dev.martianzoo.tfm.pets.ast.Instruction.Intensity.OPTIONAL
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DefaultsTest {
  @Test
  fun testIntensities() {
    val loader = loader(
        """
          CLASS Class<Component>
          ABSTRACT CLASS Component {
            DEFAULT +This!
            DEFAULT -This!
          }
          CLASS Foo1 {
            DEFAULT +This.
          }
          CLASS Bar1 {
            DEFAULT -This?
          }
          CLASS FooBar1 : Foo1, Bar1
          CLASS Qux1 {
            DEFAULT +This!
          }
          CLASS UhOh: Foo1, Qux1
          CLASS Fixed: Qux1 {
            DEFAULT +This.
          }
        """
    )

    val d = loader.getClass(cn("Foo1")).defaults
    assertThat(d.gainIntensity).isEqualTo(AMAP)
    assertThat(d.removeIntensity).isEqualTo(MANDATORY)

    val d2 = loader.getClass(cn("FooBar1")).defaults
    assertThat(d2.gainIntensity).isEqualTo(AMAP)
    assertThat(d2.removeIntensity).isEqualTo(OPTIONAL)

    val c = loader.getClass(cn("UhOh"))
    assertThrows<RuntimeException> { c.defaults }

    val d3 = loader.getClass(cn("Fixed")).defaults
    assertThat(d3.gainIntensity).isEqualTo(AMAP)
    assertThat(d3.removeIntensity).isEqualTo(MANDATORY)
  }
}
