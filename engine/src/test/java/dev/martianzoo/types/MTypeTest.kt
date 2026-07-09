package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.engine.CanonClassesTest
import kotlin.test.Test
import io.kotest.matchers.shouldBe

internal class MTypeTest {
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

    table.getClass(cn("Animal")).baseType.expressionFull.toString() shouldBe
        "Animal<Owner, ResourceCard<Owner, Class<Animal>>>"

    table.resolve(te("Animal<Fish>")).abstract shouldBe true

    val fish = table.resolve(te("Animal<Player1, Fish<Player1>>"))
    fish.abstract shouldBe false

    table.resolve(te("Fish")).expressionFull.toString() shouldBe "Fish<Owner, Class<Animal>>"

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
      type(subb).isSubtypeOf(type(supper)) shouldBe true
      type(supper).isSubtypeOf(type(subb)) shouldBe false
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
    // checkProperSubtypes("Complex1(HAS Foo1)", "Complex1")
    // checkUnrelated("Complex1(HAS Foo2)", "Complex1(HAS Foo1)")
    // checkUnrelated("Complex1(HAS Foo1)", "Complex1(HAS Foo2)")
    // checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar2>")
    // checkProperSubtypes("Complex2<Bar2>(HAS Foo1)", "Complex1<Bar2>")
    // checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>")
    // checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
    // checkProperSubtypes("Complex2<Bar1>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
    // checkProperSubtypes("Complex2<Bar1(HAS Qux2)>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
    // check...("Complex2<Bar1(HAS Qux2)>(HAS Foo1)", "Complex1<Bar1(HAS Qux2)>(HAS Foo1)")
  }

  fun type(s: String) = table.resolve(te(s))

  @Test
  fun partial() {
    val base = type("Complex1")
    base.expressionFull.toString() shouldBe "Complex1<Foo1, Bar1, Qux1>"
    base.expression.toString() shouldBe "Complex1"

    type("Complex1") shouldBe base
    type("Complex1<Foo1>") shouldBe base
    type("Complex1<Bar1>") shouldBe base
    type("Complex1<Qux1>") shouldBe base
    type("Complex1<Foo1, Bar1>") shouldBe base
    type("Complex1<Foo1, Qux1>") shouldBe base
    type("Complex1<Bar1, Qux1>") shouldBe base
    type("Complex1<Foo1, Bar1, Qux1>") shouldBe base

    val ofFoo2 = type("Complex1<Foo2>")
    ofFoo2.expressionFull.toString() shouldBe "Complex1<Foo2, Bar1, Qux1>"
    ofFoo2.expression.toString() shouldBe "Complex1<Foo2>"

    type("Complex1<Foo2>") shouldBe ofFoo2
    type("Complex1<Foo2, Bar1>") shouldBe ofFoo2
    type("Complex1<Foo2, Qux1>") shouldBe ofFoo2
    type("Complex1<Foo2, Bar1, Qux1>") shouldBe ofFoo2
  }

  @Test
  fun outOfOrder() {
    val base = type("Complex1")
    type("Complex1<Bar1, Foo1>") shouldBe base
    type("Complex1<Qux1, Foo1>") shouldBe base
    type("Complex1<Qux1, Bar1>") shouldBe base
    type("Complex1<Foo1, Qux1, Bar1>") shouldBe base
    type("Complex1<Bar1, Qux1, Foo1>") shouldBe base
    type("Complex1<Bar1, Foo1, Qux1>") shouldBe base
    type("Complex1<Qux1, Foo1, Bar1>") shouldBe base
    type("Complex1<Qux1, Bar1, Foo1>") shouldBe base

    val ofFoo2 = type("Complex1<Foo2>")
    type("Complex1<Bar1, Foo2>") shouldBe ofFoo2
    type("Complex1<Qux1, Foo2>") shouldBe ofFoo2
    type("Complex1<Foo2, Qux1, Bar1>") shouldBe ofFoo2
    type("Complex1<Bar1, Qux1, Foo2>") shouldBe ofFoo2
    type("Complex1<Bar1, Foo2, Qux1>") shouldBe ofFoo2
    type("Complex1<Qux1, Foo2, Bar1>") shouldBe ofFoo2
    type("Complex1<Qux1, Bar1, Foo2>") shouldBe ofFoo2
  }

  @Test
  fun twoSame() {
    type("TwoSame") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo1>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo2>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo3>") shouldBe type("TwoSame<Foo3, Foo2>")
    type("TwoSame<Foo1, Foo1>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo1, Foo2>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo1, Foo3>") shouldBe type("TwoSame<Foo2, Foo3>")
    type("TwoSame<Foo2, Foo1>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo2, Foo2>") shouldBe type("TwoSame<Foo2, Foo2>")
    type("TwoSame<Foo2, Foo3>") shouldBe type("TwoSame<Foo2, Foo3>")
    type("TwoSame<Foo3, Foo1>") shouldBe type("TwoSame<Foo3, Foo2>")
    type("TwoSame<Foo3, Foo2>") shouldBe type("TwoSame<Foo3, Foo2>")
    type("TwoSame<Foo3, Foo3>") shouldBe type("TwoSame<Foo3, Foo3>")
  }

  @Test
  fun complementDependencies() {
    val table =
        loadTypes(
            """
              ABSTRACT CLASS Anyone {
                ABSTRACT CLASS Owner { CLASS Player1, Player2 }
              }
              ABSTRACT CLASS Owned<Owner>
            """
                .trimIndent())

    table.resolve(te("Owned<Player2>")).narrows(table.resolve(te("Owned<!Player1>"))) shouldBe true
    table.resolve(te("Owned<Player1>")).narrows(table.resolve(te("Owned<!Player1>"))) shouldBe false
  }

  @Test
  fun roundTrip() {
    fun checkMinimal(typeIn: String, typeOut: String = typeIn) {
      type(typeIn).expression shouldBe te(typeOut)
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
    findSubstitutions(pprod) shouldBe
        mapOf(cn("StandardResource") to cn("Plant").expression, OWNER to PLAYER1.expression)
  }

  @Test
  fun subs2() {
    val pprod = CanonClassesTest.table.resolve(te("PlayCard<Player1, Class<MediaGroup>>"))
    findSubstitutions(pprod) shouldBe
        mapOf(cn("CardFront") to cn("MediaGroup").expression, OWNER to PLAYER1.expression)
  }

  private fun te(s: String): Expression = parse(s)
}
