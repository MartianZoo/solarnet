package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.testRoundTrip
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class TypeExprTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<TypeExpr>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = typeExpr("Foo")
    assertThat(foo).isEqualTo(cn("Foo").ptype)
  }

  @Test
  fun simpleApiToSource() {
    assertThat(cn("Foo").ptype.toString()).isEqualTo("Foo")
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
    val parsed = typeExpr(" Red< Blue  < This,Teal> , Gold > ")
    assertThat(parsed)
        .isEqualTo(
            cn("Red")
                .addArgs(cn("Blue").addArgs(cn("This").ptype, cn("Teal").ptype), cn("Gold").ptype))
  }

  @Test
  fun complexApiToSource() {
    val expr =
        cn("Aa")
            .addArgs(
                cn("Bb").ptype,
                cn("Cc").addArgs(cn("Dd").ptype),
                cn("Ee").addArgs(cn("Ff").addArgs(cn("Gg").ptype, cn("Hh").ptype), cn("Me").ptype),
                cn("Jj").ptype)
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }
}
