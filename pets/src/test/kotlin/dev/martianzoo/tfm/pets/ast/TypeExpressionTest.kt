package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.PetsParser
import dev.martianzoo.tfm.pets.ast.Requirement.Min
import dev.martianzoo.tfm.pets.ast.TypeExpression.Companion.te
import dev.martianzoo.tfm.pets.ast.TypeExpression.GenericTypeExpression
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

// Most testing is done by AutomatedTest
class TypeExpressionTest {
  private fun testRoundTrip(petsText: String) =
      dev.martianzoo.tfm.pets.testRoundTrip<TypeExpression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo: TypeExpression = PetsParser.parse("Foo")
    assertThat(foo).isEqualTo(te("Foo"))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(te("Foo").toString()).isEqualTo("Foo")
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
        te(
            "Red",
            te("Blue", te("This"), te("Teal")),
            te("Gold")
        )
    )
  }

  @Test
  fun complexApiToSource() {
    val expr = te(
        "Aa",
        te("Bb"),
        te("Cc", te("Dd")),
        te(
            "Ee",
            te("Ff", te("Gg"), te("Hh")),
            te("Me")
        ),
        te("Jj")
    )
    assertThat(expr.toString()).isEqualTo("Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>")
  }

  @Test fun classAlone() {
    assertThrows<RuntimeException> { te("Class", te("Foo", te("Bar"))) }
    assertThrows<RuntimeException> { te("Class", GenericTypeExpression("Foo", refinement = Min(QuantifiedExpression(te("Heat"))))) }
  }
}
