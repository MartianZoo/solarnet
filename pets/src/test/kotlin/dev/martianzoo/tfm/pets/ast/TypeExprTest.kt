package dev.martianzoo.tfm.pets.ast

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.SpecialClassNames.CLASS
import dev.martianzoo.tfm.pets.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.TypeExpr.ClassLiteral
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.pets.testRoundTrip
import dev.martianzoo.tfm.testlib.assertFails
import org.junit.jupiter.api.Test

// Most testing is done by AutomatedTest
private class TypeExprTest {
  private fun testRoundTrip(petsText: String) = testRoundTrip<TypeExpr>(petsText)

  @Test
  fun simpleSourceToApi() {
    val foo = typeExpr("Foo")
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
    val parsed = typeExpr(" Red< Blue  < This,Teal> , Gold > ")
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
    typeExpr("Foo")
    typeExpr("Foo<Bar>")

    assertThat(typeExpr("Class<Foo>")).isEqualTo(cn("Foo").literal)
    assertThat(typeExpr("Class<Component>")).isEqualTo(COMPONENT.literal)
    assertThat(typeExpr("Class")).isEqualTo(COMPONENT.literal)
    assertThat(typeExpr("Class<Class>")).isEqualTo(CLASS.literal)

    val two = typeExpr("Two<Class<Bar>, Class<Qux>>").asGeneric()
    assertThat(two.root).isEqualTo(cn("Two"))
    assertThat(two.args).containsExactly(cn("Bar").literal, cn("Qux").literal)

    assertFails { typeExpr("Class<Foo<Bar>>") }
    assertFails { typeExpr("Class<Foo, Bar>") }
    assertFails { typeExpr("Qux<Class<Foo<Bar>>>") }
    assertFails { typeExpr("Qux<Class<Foo, Bar>>") }
    assertFails { typeExpr("Class<Class<Foo>>") }
  }

  @Test
  fun classLiteralStuffWithLinks() {
    typeExpr("Foo^5")
    typeExpr("Foo<Bar^5>")
    typeExpr("Foo<Bar>^5")

    assertThat(typeExpr("Class<Foo^5>")).isEqualTo(ClassLiteral(cn("Foo"), 5))
    assertThat(typeExpr("Class<Component^5>")).isEqualTo(ClassLiteral(COMPONENT, 5))
    assertFails { typeExpr("Class<Class^5>") }
    assertFails { typeExpr("Class<Foo>^5") }
    assertFails { typeExpr("Class^5") }
  }
}
