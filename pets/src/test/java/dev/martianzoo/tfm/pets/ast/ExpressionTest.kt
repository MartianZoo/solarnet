package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SpecialClassNames.CLASS
import dev.martianzoo.api.SpecialClassNames.COMPONENT
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
    assertThat(foo).isEqualTo(cn("Foo").expression)
  }

  @Test
  fun simpleApiToSource() {
    assertThat(cn("Foo").expression.toString()).isEqualTo("Foo")
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
        .isEqualTo(cn("Red").of(cn("Blue").of(cn("This"), cn("Teal")), cn("Gold").expression))
  }

  @Test
  fun complexApiToSource() {
    val expr =
        cn("Aa")
            .of(
                cn("Bb").expression,
                cn("Cc").of(cn("Dd")),
                cn("Ee").of(cn("Ff").of(cn("Gg"), cn("Hh")), cn("Me").expression),
                cn("Jj").expression)
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
}
