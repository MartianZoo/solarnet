package dev.martianzoo.tfm.petaform.parser

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.petaform.api.Expression
import dev.martianzoo.tfm.petaform.api.This
import org.junit.jupiter.api.Test

class ExpressionTest {
  private fun testRoundTrip(petaform: String) =
      assertThat(PetaformParser.parse<Expression>(petaform).petaform).isEqualTo(petaform)

  @Test
  fun simpleSourceToApi() {
    val foo: Expression = PetaformParser.parse("Foo")
    assertThat(foo).isEqualTo(Expression("Foo"))
  }

  @Test
  fun simpleApiToSource() {
    assertThat(Expression("Foo").petaform).isEqualTo("Foo")
  }

  @Test
  fun simpleRoundTrips() {
    testRoundTrip("Foo")
    testRoundTrip("Foo<Bar>")
    testRoundTrip("Foo<Bar, Baz>")
    testRoundTrip("Foo<Bar<Qux>, Baz>")
  }

  @Test
  fun complexRoundTrip() {
    testRoundTrip("Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Ii>>, Jj>>, Kk>>")
  }

  @Test
  fun complexSourceToApi() {
    val parsed: Expression = PetaformParser.parse(
        """
      Red<  // comment works
         Blue  < This,Teal>
        , Gold >
    """
    )
    assertThat(parsed).isEqualTo(
        Expression(
            "Red",
            Expression(
                "Blue",
                Expression(This),
                Expression("Teal")
            ),
            Expression("Gold")
        )
    )
  }

  @Test
  fun complexApiToSource() {
    val expr = Expression(
        "Aa",
        Expression("Bb"),
        Expression(
            "Cc",
            Expression("Dd")
        ),
        Expression(
            "Ee",
            Expression(
                This,
                Expression("Gg"),
                Expression("Hh")
            ),
            Expression("Ii")
        ),
        Expression("Jj")
    )
    assertThat(expr.petaform).isEqualTo("Aa<Bb, Cc<Dd>, Ee<This<Gg, Hh>, Ii>, Jj>")
  }
}
