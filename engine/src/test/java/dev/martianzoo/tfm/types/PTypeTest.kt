package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import org.junit.jupiter.api.Test

private class PTypeTest {

  @Test
  fun testCycle() {
    val table: PClassLoader =
        loadTypes(
            "ABSTRACT CLASS Anyone",
            "CLASS Player1 : Anyone",
            "CLASS Player2 : Anyone",
            "ABSTRACT CLASS Owned<Anyone>",
            "ABSTRACT CLASS CardFront : Owned",
            "ABSTRACT CLASS Cardbound<CardFront> : Owned",
            "ABSTRACT CLASS ResourcefulCard<Class<CardResource>> : CardFront",
            "ABSTRACT CLASS CardResource : " +
                "Owned<Anyone>, Cardbound<Anyone, ResourcefulCard>", // TODO <Class<This>> ?
            "CLASS Animal : CardResource<ResourcefulCard<Class<Animal>>>",
            "CLASS Microbe : CardResource<ResourcefulCard<Class<Microbe>>>",
            "CLASS Fish : ResourcefulCard<Class<Animal>>",
            "CLASS Ants : ResourcefulCard<Class<Microbe>>",
        )
    assertThat(table.resolveType(te("Animal<Fish>")).abstract).isTrue()

    val fish = table.resolveType(te("Animal<Player1, Fish<Player1>>"))
    assertThat(fish.abstract).isFalse()

    assertThat(table.resolveType(te("Fish")).typeExprFull.toString())
        .isEqualTo("Fish<Anyone, Class<Animal>>")

    // TODO get these working
    // assertFails { table.resolve("Animal<Ants>") }
    // assertThat(table["Animal"].baseType.toString())
    //    .isEqualTo("Animal<Anyone, ResourcefulCard<Anyone, Class<Animal>>>")
    // assertFails {
    //  table.resolve("Microbe<Player1, Ants<Player2>>")
    // }
  }

  val table =
      loadTypes(
          "CLASS Foo1",
          "CLASS Foo2 : Foo1",
          "CLASS Foo3 : Foo2",
          "CLASS Bar1",
          "CLASS Bar2 : Bar1",
          "CLASS Bar3 : Bar2",
          "CLASS Qux1",
          "CLASS Qux2 : Qux1",
          "CLASS Qux3 : Qux2",
          "CLASS Complex1<Foo1, Bar1, Qux1>",
          "CLASS Complex2: Complex1<Foo2, Bar2, Qux2>",
          "CLASS Complex3: Complex2<Foo3, Bar3, Qux3>",
          "CLASS TwoSame<Foo2, Foo2>")
  init {
    table.frozen = true
  }

  fun type(s: String) = table.resolveType(te(s))

  @Test
  fun partial() {
    val base = type("Complex1")
    assertThat(base.typeExprFull.toString()).isEqualTo("Complex1<Foo1, Bar1, Qux1>")
    assertThat(base.typeExpr.toString()).isEqualTo("Complex1")

    assertThat(type("Complex1")).isEqualTo(base)
    assertThat(type("Complex1<Foo1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1, Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Bar1, Qux1>")).isEqualTo(base)

    val ofFoo2 = type("Complex1<Foo2>")
    assertThat(ofFoo2.typeExprFull.toString()).isEqualTo("Complex1<Foo2, Bar1, Qux1>")
    assertThat(ofFoo2.typeExpr.toString()).isEqualTo("Complex1<Foo2>")

    assertThat(type("Complex1<Foo2>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Foo2, Bar1>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Foo2, Qux1>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Foo2, Bar1, Qux1>")).isEqualTo(ofFoo2)
  }

  @Test
  fun outOfOrder() {
    val base = type("Complex1")
    assertThat(type("Complex1<Bar1, Foo1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1, Foo1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1, Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Qux1, Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1, Qux1, Foo1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1, Foo1, Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1, Foo1, Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1, Bar1, Foo1>")).isEqualTo(base)

    val ofFoo2 = type("Complex1<Foo2>")
    assertThat(type("Complex1<Bar1, Foo2>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Qux1, Foo2>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Foo2, Qux1, Bar1>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Bar1, Qux1, Foo2>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Bar1, Foo2, Qux1>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Qux1, Foo2, Bar1>")).isEqualTo(ofFoo2)
    assertThat(type("Complex1<Qux1, Bar1, Foo2>")).isEqualTo(ofFoo2)
  }

  @Test
  fun twoSame() {
    assertThat(type("TwoSame")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo1>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo2>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo3>")).isEqualTo(type("TwoSame<Foo3, Foo2>"))
    assertThat(type("TwoSame<Foo1, Foo1>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo1, Foo2>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo1, Foo3>")).isEqualTo(type("TwoSame<Foo2, Foo3>"))
    assertThat(type("TwoSame<Foo2, Foo1>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo2, Foo2>")).isEqualTo(type("TwoSame<Foo2, Foo2>"))
    assertThat(type("TwoSame<Foo2, Foo3>")).isEqualTo(type("TwoSame<Foo2, Foo3>"))
    assertThat(type("TwoSame<Foo3, Foo1>")).isEqualTo(type("TwoSame<Foo3, Foo2>"))
    assertThat(type("TwoSame<Foo3, Foo2>")).isEqualTo(type("TwoSame<Foo3, Foo2>"))
    assertThat(type("TwoSame<Foo3, Foo3>")).isEqualTo(type("TwoSame<Foo3, Foo3>"))
  }

  @Test
  fun roundTrip() {
    fun checkMinimal(typeIn: String, typeOut: String = typeIn) {
      assertThat(type(typeIn).typeExpr).isEqualTo(te(typeOut))
    }

    checkMinimal("TwoSame")
    checkMinimal("TwoSame<Foo1>", "TwoSame")
    checkMinimal("TwoSame<Foo2>", "TwoSame")
    checkMinimal("TwoSame<Foo3>")
    checkMinimal("TwoSame<Foo1, Foo1>", "TwoSame")
    checkMinimal("TwoSame<Foo1, Foo2>", "TwoSame")
    checkMinimal("TwoSame<Foo2, Foo1>", "TwoSame")
    checkMinimal("TwoSame<Foo2, Foo2>", "TwoSame")
    checkMinimal("TwoSame<Foo3, Foo1>", "TwoSame<Foo3>")
    checkMinimal("TwoSame<Foo3, Foo2>", "TwoSame<Foo3>")
    checkMinimal("TwoSame<Foo3, Foo3>")

    // TODO get these working too!
    // checkMinimal("TwoSame<Foo1, Foo3>", "TwoSame<Foo2, Foo3>")
    // checkMinimal("TwoSame<Foo2, Foo3>")
  }

  private fun te(s: String) = typeExpr(s)
}
