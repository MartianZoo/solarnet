package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.testlib.assertFails
import dev.martianzoo.tfm.testlib.te
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class TypeExprTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<TypeExpr>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = te("Foo")
    assertThat(foo).isEqualTo(cn("Foo").type)
  }

  @Test
  fun simpleApiToSource() {
    assertThat(cn("Foo").type.toString()).isEqualTo("Foo")
  }

  @Test
  fun simpleRoundTrips() {
    testRoundTrip("Foo")
    testRoundTrip("Foo<Bar>")
    testRoundTrip("Foo<Bar, Baz>")
    testRoundTrip("Foo<Bar<Qux>, Baz>")
    testRoundTrip("Foo(HAS Bar)")
    testRoundTrip("Foo(HAS MAX 0 Bar)")
    testRoundTrip("Foo<Bar>(HAS Baz, 2 Qux)")
  }

  @Test
  fun complexRoundTrip() {
    testRoundTrip("Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>")
  }

  @Test
  fun complexSourceToApi() {
    val parsed = te(" Red< Blue  < This,Teal> , Gold > ")
    assertThat(parsed)
        .isEqualTo(
            cn("Red")
                .addArgs(cn("Blue").addArgs(cn("This").type, cn("Teal").type), cn("Gold").type))
  }

  @Test
  fun complexApiToSource() {
    val expr =
        cn("Aa")
            .addArgs(
                cn("Bb").type,
                cn("Cc").addArgs(cn("Dd").type),
                cn("Ee").addArgs(cn("Ff").addArgs(cn("Gg").type, cn("Hh").type), cn("Me").type),
                cn("Jj").type)
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }

  @Test
  fun classLiteralStuff() {
    te("Foo")
    te("Foo<Bar>")

    assertThat(te("Class<Foo>")).isEqualTo(CLASS.addArgs(cn("Foo")))
    assertThat(te("Class<Component>")).isEqualTo(CLASS.addArgs(COMPONENT))
    assertThat(te("Class<Class>")).isEqualTo(CLASS.addArgs(CLASS))

    val two = te("Two<Class<Bar>, Class<Qux>>")
    assertThat(two.className).isEqualTo(cn("Two"))
    assertThat(two.arguments).containsExactly(CLASS.addArgs(cn("Bar")), CLASS.addArgs(cn("Qux")))

    assertFails { te("Class<Class<Class>>") }
    assertFails { te("Class<Class<Foo>>") }
    assertFails { te("Class<Foo<Bar>>") }
    assertFails { te("Class<Foo, Bar>") }
    assertFails { te("Qux<Class<Foo<Bar>>>") }
    assertFails { te("Qux<Class<Foo, Bar>>") }
    assertFails { te("Class<Class<Component>>") }
  }

  @Test
  fun classLiteralStuffWithLinks() {
    te("Foo^5")
    te("Foo<Bar^5>")
    te("Foo<Bar>^5")

    assertFails { te("Class<Class^5>") }
    assertFails { te("Class<Foo>^5") }
    assertFails { te("Class^5") }
  }
}
