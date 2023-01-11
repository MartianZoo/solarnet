package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetParser.parsePets
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.gte
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class TypeExpressionTest {
  private fun testRoundTrip(petsText: String) =
      dev.martianzoo.tfm.pets.testRoundTrip<TypeExpression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo: TypeExpression = parsePets("Foo")
    assertThat(foo).isEqualTo(gte("Foo"))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(gte("Foo").toString()).isEqualTo("Foo")
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
    val parsed: TypeExpression = parsePets(" Red< Blue  < This,Teal> , Gold > ")
    assertThat(parsed).isEqualTo(
        gte(
            "Red",
            gte("Blue", gte("This"), gte("Teal")),
            gte("Gold")
        )
    )
  }

  @Test
  fun complexApiToSource() {
    val expr = gte(
        "Aa",
        gte("Bb"),
        gte("Cc", gte("Dd")),
        gte(
            "Ee",
            gte("Ff", gte("Gg"), gte("Hh")),
            gte("Me")
        ),
        gte("Jj")
    )
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }
}
