package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.CLASS
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.testlib.te
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class ExpressionTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<Expression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = te("Foo")
    assertThat(foo).isEqualTo(cn("Foo").expr)
  }

  @Test
  fun simpleApiToSource() {
    assertThat(cn("Foo").expr.toString()).isEqualTo("Foo")
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
                .addArgs(cn("Blue").addArgs(cn("This").expr, cn("Teal").expr), cn("Gold").expr))
  }

  @Test
  fun complexApiToSource() {
    val expr =
        cn("Aa")
            .addArgs(
                cn("Bb").expr,
                cn("Cc").addArgs(cn("Dd").expr),
                cn("Ee").addArgs(cn("Ff").addArgs(cn("Gg").expr, cn("Hh").expr), cn("Me").expr),
                cn("Jj").expr)
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }

  @Test
  fun classLiteralStuff() {
    te("Foo")
    te("Foo<Bar>")

    assertThat(te("Class<Foo>")).isEqualTo(cn("Foo").classExpression())
    assertThat(te("Class<Component>")).isEqualTo(COMPONENT.classExpression())
    assertThat(te("Class<Class>")).isEqualTo(CLASS.classExpression())

    val two = te("Two<Class<Bar>, Class<Qux>>")
    assertThat(two.className).isEqualTo(cn("Two"))
    assertThat(two.arguments)
        .containsExactly(cn("Bar").classExpression(), cn("Qux").classExpression())
  }

  @Test
  fun classLiteralStuffWithLinks() {
    te("Foo^5")
    te("Foo<Bar^5>")
    te("Foo<Bar>^5")
  }
}
