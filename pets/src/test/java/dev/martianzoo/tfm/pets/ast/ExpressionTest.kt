package dev.martianzoo.tfm.pets.ast

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import dev.martianzoo.api.SystemClasses.CLASS
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.testlib.te
import kotlin.test.Test

// Most testing is done by AutomatedTest
internal class ExpressionTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<Expression>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = te("Foo")
    foo shouldBe cn("Foo").expression
  }

  @Test
  fun simpleApiToSource() {
    cn("Foo").expression.toString() shouldBe "Foo"
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
    testRoundTrip("!Foo")
    testRoundTrip("Foo<!Bar>")
  }

  @Test
  fun complexRoundTrip() {
    testRoundTrip("Aa<Bb<Cc<Dd, Ee, Ff<Gg<Hh<Me>>, Jj>>, Kk>>")
  }

  @Test
  fun complexSourceToApi() {
    val parsed = te(" Red< Blue  < This,Teal> , Gold > ")
    parsed shouldBe cn("Red").of(cn("Blue").of(cn("This"), cn("Teal")), cn("Gold").expression)
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
    expr.toString() shouldBe "Aa<Bb, Cc<Dd>, Ee<Ff<Gg, Hh>, Me>, Jj>"
  }

  @Test
  fun classLiteralStuff() {
    te("Foo")
    te("Foo<Bar>")

    te("Class<Foo>") shouldBe cn("Foo").classExpression()
    te("Class<Component>") shouldBe COMPONENT.classExpression()
    te("Class<Class>") shouldBe CLASS.classExpression()

    val two = te("Two<Class<Bar>, Class<Qux>>")
    two.className shouldBe cn("Two")
    two.arguments.shouldContainExactlyInAnyOrder(
        cn("Bar").classExpression(),
        cn("Qux").classExpression(),
    )
  }
}
