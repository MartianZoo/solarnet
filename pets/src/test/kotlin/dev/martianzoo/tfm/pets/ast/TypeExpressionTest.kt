package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.Parsing.parsePets
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class TypeExpressionTest {
  private fun testRoundTrip(petsText: String) =
      dev.martianzoo.tfm.pets.testRoundTrip<TypeExpression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = TypeExpression.from("Foo")
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
    val parsed = TypeExpression.from(" Red< Blue  < This,Teal> , Gold > ")
    assertThat(parsed).isEqualTo(cn("Red").addArgs(cn("Blue").addArgs(cn("This").type,
        cn("Teal").type), cn("Gold").type))
  }

  @Test
  fun complexApiToSource() {
    val expr = cn("Aa").addArgs(cn("Bb").type,
        cn("Cc").addArgs(cn("Dd").type),
        cn("Ee").addArgs(cn("Ff").addArgs(cn("Gg").type, cn("Hh").type), cn("Me").type),
        cn("Jj").type)
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }
}
