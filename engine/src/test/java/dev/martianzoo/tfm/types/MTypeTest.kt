package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.OWNER
import dev.martianzoo.tfm.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.CanonClassesTest
import dev.martianzoo.tfm.pets.Parsing.parse
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression
import org.junit.jupiter.api.Test

private class MTypeTest {
  @Test
  fun testCardboundWeirdness() {
    val table: MClassTable =
        loadTypes(
            """
            ABSTRACT CLASS Anyone {
              ABSTRACT CLASS Owner { CLASS Player1, Player2 }
            }

            ABSTRACT CLASS Owned<Owner> {
              ABSTRACT CLASS CardFront
              ABSTRACT CLASS Cardbound<CardFront>
            }

            // Treated as an extension of Cardbound<ResourceCard<Class<CardResource>>>, plus a rule
            ABSTRACT CLASS CardResource : Cardbound<ResourceCard<Class<This>>> {
              CLASS Animal, Microbe
            }
            ABSTRACT CLASS ResourceCard<Class<CardResource>> : CardFront

            CLASS Fish : ResourceCard<Class<Animal>>
            CLASS Ants : ResourceCard<Class<Microbe>>
          """
                .trimIndent())

    assertThat(table.getClass(cn("Animal")).baseType.expressionFull.toString())
        .isEqualTo("Animal<Owner, ResourceCard<Owner, Class<Animal>>>")

    assertThat(table.resolve(te("Animal<Fish>")).abstract).isTrue()

    val fish = table.resolve(te("Animal<Player1, Fish<Player1>>"))
    assertThat(fish.abstract).isFalse()

    assertThat(table.resolve(te("Fish")).expressionFull.toString())
        .isEqualTo("Fish<Owner, Class<Animal>>")

    assertFails { table.resolve(te("Animal<Ants>")) }

    // This should FAIL
    // table.resolve(te("Microbe<Player1, Ants<Player2>>"))
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

  @Test
  fun subtypes() {
    fun checkProperSubtypes(subb: String, supper: String) {
      assertThat(type(subb).isSubtypeOf(type(supper))).isTrue()
      assertThat(type(supper).isSubtypeOf(type(subb))).isFalse()
    }

    fun checkUnrelated(subb: String, supper: String) {
      assertThat(type(subb).isSubtypeOf(type(supper))).isFalse()
      assertThat(type(supper).isSubtypeOf(type(subb))).isFalse()
    }

    checkProperSubtypes("Complex1<Foo2, Bar1, Qux1>", "Complex1<Foo1, Bar1, Qux1>")
    checkProperSubtypes("Complex1<Foo1, Bar2, Qux1>", "Complex1<Foo1, Bar1, Qux1>")
    checkProperSubtypes("Complex1<Foo1, Bar1, Qux2>", "Complex1<Foo1, Bar1, Qux1>")
    checkProperSubtypes("Complex2<Foo2, Bar2, Qux2>", "Complex1<Foo2, Bar2, Qux2>")
    checkProperSubtypes("Complex2<Foo2, Bar2, Qux2>", "Complex1<Foo1, Bar2, Qux2>")
    checkProperSubtypes("Complex2<Foo2, Bar2, Qux2>", "Complex1<Foo2, Bar1, Qux2>")
    checkProperSubtypes("Complex2<Foo2, Bar2, Qux2>", "Complex1<Foo2, Bar2, Qux1>")
    checkProperSubtypes("Complex2<Foo2, Bar2, Qux2>", "Complex1<Foo2, Bar2, Qux1>")
    checkProperSubtypes("Complex2<Foo3, Bar2, Qux2>", "Complex1<Foo3, Bar2, Qux2>")

    // Maybe these methods don't need to work
//    checkProperSubtypes("Complex1(HAS Foo1)", "Complex1")
//
//    checkUnrelated("Complex1(HAS Foo2)", "Complex1(HAS Foo1)")
//    checkUnrelated("Complex1(HAS Foo1)", "Complex1(HAS Foo2)")

//    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar2>")
//    checkProperSubtypes("Complex2<Bar2>(HAS Foo1)", "Complex1<Bar2>")
//    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>")
//    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
//    checkProperSubtypes("Complex2<Bar1>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
//    checkProperSubtypes("Complex2<Bar1(HAS Qux2)>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
//    checkProperSubtypes("Complex2<Bar1(HAS Qux2)>(HAS Foo1)", "Complex1<Bar1(HAS Qux2)>(HAS Foo1)")
  }

  fun type(s: String) = table.resolve(te(s))

  @Test
  fun partial() {
    val base = type("Complex1")
    assertThat(base.expressionFull.toString()).isEqualTo("Complex1<Foo1, Bar1, Qux1>")
    assertThat(base.expression.toString()).isEqualTo("Complex1")

    assertThat(type("Complex1")).isEqualTo(base)
    assertThat(type("Complex1<Foo1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Bar1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Bar1, Qux1>")).isEqualTo(base)
    assertThat(type("Complex1<Foo1, Bar1, Qux1>")).isEqualTo(base)

    val ofFoo2 = type("Complex1<Foo2>")
    assertThat(ofFoo2.expressionFull.toString()).isEqualTo("Complex1<Foo2, Bar1, Qux1>")
    assertThat(ofFoo2.expression.toString()).isEqualTo("Complex1<Foo2>")

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
      assertThat(type(typeIn).expression).isEqualTo(te(typeOut))
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

    // Get these working too
    // checkMinimal("TwoSame<Foo1, Foo3>", "TwoSame<Foo2, Foo3>")
    // checkMinimal("TwoSame<Foo2, Foo3>")
  }

  fun findSubstitutions(mType: MType): Map<ClassName, Expression> =
      mType.loader.transformers.findSubstitutions(
          mType.root.defaultType.dependencies, mType.dependencies)

  @Test
  fun subs() {
    val pprod = CanonClassesTest.table.resolve(te("Production<Player1, Class<Plant>>"))
    assertThat(findSubstitutions(pprod))
        .containsExactly(cn("StandardResource"), cn("Plant").expression, OWNER, PLAYER1.expression)
  }

  @Test
  fun subs2() {
    val pprod = CanonClassesTest.table.resolve(te("PlayCard<Player1, Class<MediaGroup>>"))
    assertThat(findSubstitutions(pprod))
        .containsExactly(cn("CardFront"), cn("MediaGroup").expression, OWNER, PLAYER1.expression)
  }

  private fun te(s: String): Expression = parse(s)
}
