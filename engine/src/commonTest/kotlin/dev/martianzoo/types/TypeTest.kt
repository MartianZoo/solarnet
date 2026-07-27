package dev.martianzoo.types

import dev.martianzoo.api.SystemClasses.OWNER
import dev.martianzoo.api.TypeInfo.StubTypeInfo
import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Transformers
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.tfm.engine.CanonClassesTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TypeTest {
  @Test
  fun testCardboundWeirdness() {
    val table: ClassTable =
        loadTypes(
            """
            ABSTRACT CLASS Anyone {
              ABSTRACT CLASS Owner { CLASS Player1, Player2 }
            }

            ABSTRACT CLASS Owned<Owner> {
              ABSTRACT CLASS CardFront
              ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>
            }

            // Treated as an extension of Cardbound<ResourceCard<Class<CardResource>>>, plus a rule
            ABSTRACT CLASS CardResource : Cardbound<ResourceCard<Class<This>>> {
              CLASS Animal, Microbe
            }
            ABSTRACT CLASS ResourceCard<Class<CardResource>> : CardFront

            CLASS Fish : ResourceCard<Class<Animal>>
            CLASS Ants : ResourceCard<Class<Microbe>>
            CLASS Pendant<CardFront> : Owned
            """
                .trimIndent()
        )

    table.getClass(cn("Animal")).baseType.expressionFull.toString() shouldBe
        "Animal<Owner, ResourceCard<Owner, Class<Animal>>>"

    val fish = table.resolve(te("Animal<Player1, Fish<Player1>>"))
    fish.abstract shouldBe false
    fish.expression.toString() shouldBe "Animal<Player1, Fish<Player1>>"
    table.resolve(te("Animal<Player1, Fish>")).also {
      it.abstract shouldBe false
      it shouldBe fish
    }
    table.resolve(te("Animal<Fish<Player1>>")).also {
      it.abstract shouldBe false
      it shouldBe fish
    }
    table.resolve(te("Animal<Fish>")).abstract shouldBe true

    table.resolve(te("Fish")).expressionFull.toString() shouldBe "Fish<Owner, Class<Animal>>"

    assertFails { table.resolve(te("Animal<Ants>")) }

    assertFails { table.resolve(te("Microbe<Player1, Ants<Player2>>")) }
    table.resolve(te("Animal<Player1, !Fish>")).abstract shouldBe true
    table.resolve(te("Animal<!Fish<Player1>>")).abstract shouldBe true
    table.resolve(te("Animal<Player1, !Fish<Player2>>")) shouldBe
        table.resolve(te("Animal<Player1>"))

    table.resolve(te("Pendant<Player1, Fish<Player2>>")).abstract shouldBe false
  }

  @Test
  fun inheritedThisRetainsItsIdentity() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Link<Class<Component>>

            ABSTRACT CLASS SelfBound : Link<Class<This>>
            ABSTRACT CLASS SelfMiddle : SelfBound
            CLASS SelfLeaf : SelfMiddle

            ABSTRACT CLASS LiteralBound : Link<Class<LiteralBound>>
            ABSTRACT CLASS LiteralMiddle : LiteralBound
            CLASS LiteralLeaf : LiteralMiddle

            ABSTRACT CLASS LeftComponent
            ABSTRACT CLASS RightComponent
            ABSTRACT CLASS Pair<Class<LeftComponent>, Class<RightComponent>>
            ABSTRACT CLASS Wrapper<Pair<Class<LeftComponent>, Class<RightComponent>>>
            ABSTRACT CLASS Mixed : LeftComponent, RightComponent, Wrapper<Pair<Class<This>, Class<Mixed>>>
            CLASS MixedLeaf : Mixed

            CLASS LeftLiteral : LeftComponent
            ABSTRACT CLASS Reordered : RightComponent, Wrapper<Pair<Class<This>, Class<LeftLiteral>>>
            CLASS ReorderedLeaf : Reordered
            """
                .trimIndent()
        )

    table.getClass(cn("SelfLeaf")).baseType.expressionFull.toString() shouldBe
        "SelfLeaf<Class<SelfLeaf>>"
    table.getClass(cn("LiteralLeaf")).baseType.expressionFull.toString() shouldBe
        "LiteralLeaf<Class<LiteralBound>>"
    table.getClass(cn("MixedLeaf")).baseType.expressionFull.toString() shouldBe
        "MixedLeaf<Pair<Class<MixedLeaf>, Class<Mixed>>>"
    table.getClass(cn("ReorderedLeaf")).baseType.expressionFull.toString() shouldBe
        "ReorderedLeaf<Pair<Class<LeftLiteral>, Class<ReorderedLeaf>>>"
  }

  @Test
  fun anyoneMeansUnrestrictedWithinTheDeclaredDependencyBound() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Anyone {
              ABSTRACT CLASS Owner {
                CLASS Opponent
                ABSTRACT CLASS Player { CLASS Player1 }
              }
            }
            ABSTRACT CLASS Owned<Anyone>
            ABSTRACT CLASS Card : Owned<Player> { CLASS Badge }
            ABSTRACT CLASS Resource : Owned<Owner> { CLASS Coin }
            """
                .trimIndent()
        )

    table.resolve(te("Badge<Anyone>")).expressionFull.toString() shouldBe "Badge<Player>"
    table.resolve(te("Coin<Anyone>")).expressionFull.toString() shouldBe "Coin<Owner>"
    table.resolve(te("Badge<Player1>")).abstract shouldBe false
    table.resolve(te("Coin<Opponent>")).abstract shouldBe false
    assertFails { table.resolve(te("Badge<Opponent>")) }
  }

  val table =
      loadTypes(
          "ABSTRACT CLASS Foo1",
          "ABSTRACT CLASS Foo2 : Foo1",
          "CLASS Foo3 : Foo2",
          "ABSTRACT CLASS Bar1",
          "ABSTRACT CLASS Bar2 : Bar1",
          "CLASS Bar3 : Bar2",
          "ABSTRACT CLASS Qux1",
          "ABSTRACT CLASS Qux2 : Qux1",
          "CLASS Qux3 : Qux2",
          "ABSTRACT CLASS Complex1<Foo1, Bar1, Qux1>",
          "ABSTRACT CLASS Complex2: Complex1<Foo2, Bar2, Qux2>",
          "CLASS Complex3: Complex2<Foo3, Bar3, Qux3>",
          "CLASS TwoSame<Foo2, Foo2>",
      )

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
                .trimIndent()
        )

    table.resolve(te("Owned<Player2>")).narrows(table.resolve(te("Owned<!Player1>"))) shouldBe true
    table.resolve(te("Owned<Player1>")).narrows(table.resolve(te("Owned<!Player1>"))) shouldBe false
  }

  @Test
  fun complementConstraintWithExplicitDomain() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS TestPlayer : Actor { CLASS TestPlayer1, TestPlayer2 }
            """
                .trimIndent()
        )
    val actor = table.resolve(te("Actor"))
    val notPlayer1 = te("!TestPlayer1")

    table.matchesConstraint(
        table.resolve(te("TestPlayer2")),
        notPlayer1,
        actor,
        StubTypeInfo,
    ) shouldBe true
    table.matchesConstraint(
        table.resolve(te("TestPlayer1")),
        notPlayer1,
        actor,
        StubTypeInfo,
    ) shouldBe false
    table.matchesConstraint(table.resolve(te("Engine")), notPlayer1, actor, StubTypeInfo) shouldBe
        true
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

  fun findSubstitutions(type: Type): Map<ClassName, Expression> =
      Transformers(type.classTable)
          .findSubstitutions(
              type.rootClass.defaultType.dependencies,
              type.dependencies,
          )

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
