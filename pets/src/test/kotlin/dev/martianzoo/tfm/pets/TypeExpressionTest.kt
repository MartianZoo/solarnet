package dev.martianzoo.tfm.pets

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Most testing is done by AutomatedTest
class TypeExpressionTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<TypeExpression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo: TypeExpression = PetsParser.parse("Foo")
    assertThat(foo).isEqualTo(TypeExpression("Foo"))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(TypeExpression("Foo").toString()).isEqualTo("Foo")
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
    val parsed: TypeExpression = PetsParser.parse(" Red< Blue  < This,Teal> , Gold > ")
    assertThat(parsed).isEqualTo(
        TypeExpression(
            "Red",
            TypeExpression(
                "Blue",
                TypeExpression("This"),
                TypeExpression("Teal")
            ),
            TypeExpression("Gold")
        )
    )
  }

  @Test
  fun complexApiToSource() {
    val expr = TypeExpression(
        "Aa",
        TypeExpression("Bb"),
        TypeExpression("Cc", TypeExpression("Dd")),
        TypeExpression(
            "Ee",
            TypeExpression("Ff", TypeExpression("Gg"), TypeExpression("Hh")),
            TypeExpression("Me")
        ),
        TypeExpression("Jj")
    )
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }

  @Test fun classAlone() {
    assertThrows<RuntimeException> { TypeExpression("Class", TypeExpression("Foo", te("Bar"))) }
    assertThrows<RuntimeException> { TypeExpression("Class", TypeExpression("Foo", requirement = Requirement.Min(te("Heat")))) }
  }
}
