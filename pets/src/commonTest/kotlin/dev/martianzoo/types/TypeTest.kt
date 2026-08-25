package dev.martianzoo.types

import dev.martianzoo.api.TypeInfo
import dev.martianzoo.api.TypeInfo.NoGameState
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Expression
import dev.martianzoo.pets.ast.Metric
import dev.martianzoo.pets.ast.Requirement
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TypeTest {
  @Test
  internal fun getsInheritedConcretePropertyValues() {
    val table =
        loadTypes(
            "ABSTRACT CLASS TemperatureStep",
            """
            ABSTRACT CLASS Area {
              row = Number
              score = Metric
              requirement = Requirement
            }
            """
                .trimIndent(),
            """
            CLASS ConcreteArea : Area {
              row = 8
              score = COUNT "TemperatureStep"
              requirement = HAS "TemperatureStep"
            }
            """
                .trimIndent(),
        )

    val area = table.resolve(te("ConcreteArea"))
    area.getNumberPropertyValue("row") shouldBe 8
    area.getMetricPropertyValue("score") shouldBe parse<Metric>("TemperatureStep")
    area.getRequirementPropertyValue("requirement") shouldBe parse<Requirement>("TemperatureStep")
  }

  @Test
  internal fun getsAbsentOptionalRequirementPropertyValue() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Goal { requirement = Requirement? }",
            "CLASS OptionalGoal : Goal",
        )

    table.resolve(te("OptionalGoal")).getRequirementPropertyValue("requirement") shouldBe null
  }

  @Test
  internal fun classTablesCannotBeMixed() {
    fun universe() = loadTypes("ABSTRACT CLASS Foo", "CLASS Bar<Foo>")

    val left = universe()
    val right = universe()
    val leftFoo = left.getClass(cn("Foo"))
    val rightFoo = right.getClass(cn("Foo"))
    val leftBar = left.resolve(te("Bar<Foo>"))
    val rightBar = right.resolve(te("Bar<Foo>"))

    shouldThrow<IllegalArgumentException> { leftFoo.isSubtypeOf(rightFoo) }
    shouldThrow<IllegalArgumentException> { leftFoo lub rightFoo }
    shouldThrow<IllegalArgumentException> { leftBar.isSubtypeOf(rightBar) }
    shouldThrow<IllegalArgumentException> { leftBar glb rightBar }
    shouldThrow<IllegalArgumentException> {
      left.getClass(cn("Bar")).withAllDependencies(rightBar.dependencies)
    }
    shouldThrow<IllegalArgumentException> {
      left.matchesConstraint(leftBar, te("Foo"), rightBar, NoGameState)
    }
  }

  @Test
  internal fun testCardboundWeirdness() {
    val table: ClassTable =
        loadTypes(
            """
            CLASS Player1 : Owner
            CLASS Player2 : Owner

            ABSTRACT CLASS CardFront : Owned<Owner>
            ABSTRACT CLASS Cardbound<CardFront<Owner>> : Owned<Owner>

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
  internal fun inheritedThisRetainsItsIdentity() {
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
  internal fun anyoneMeansUnrestrictedWithinTheDeclaredDependencyBound() {
    val table =
        loadTypes(
            """
            CLASS SoloOpponent : Owner
            ABSTRACT CLASS Player : Owner { CLASS Player1 }
            ABSTRACT CLASS Card : Owned<Player> { CLASS Badge }
            ABSTRACT CLASS Resource : Owned<Owner> { CLASS Coin }
            """
                .trimIndent()
        )

    table.resolve(te("Badge<Anyone>")).expressionFull.toString() shouldBe "Badge<Player>"
    table.resolve(te("Coin<Anyone>")).expressionFull.toString() shouldBe "Coin<Owner>"
    table.resolve(te("Badge<Player1>")).abstract shouldBe false
    table.resolve(te("Coin<SoloOpponent>")).abstract shouldBe false
    assertFails { table.resolve(te("Badge<SoloOpponent>")) }
  }

  private val table =
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
  internal fun subtypes() {
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

    checkProperSubtypes("Complex1(HAS Foo1)", "Complex1")
    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar2>")
    checkProperSubtypes("Complex2<Bar2>(HAS Foo1)", "Complex1<Bar2>")
    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>")
    checkProperSubtypes("Complex1<Bar2>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
    checkProperSubtypes("Complex2<Bar1>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
    checkProperSubtypes("Complex2<Bar1(HAS Qux2)>(HAS Foo1)", "Complex1<Bar1>(HAS Foo1)")
  }

  @Test
  internal fun refinementsParticipateInStaticHierarchyOperations() {
    val plain = type("Foo1")
    val narrower = type("Foo2(HAS Complex1)")
    val refined = type("Foo1(HAS Complex1)")
    val other = type("Foo1(HAS Complex2)")

    (plain lub refined) shouldBe plain
    (refined lub plain) shouldBe plain
    (narrower lub refined) shouldBe refined
    (refined lub refined) shouldBe refined
    (refined lub other) shouldBe plain

    refined.isSubtypeOf(refined) shouldBe true
    narrower.isSubtypeOf(refined) shouldBe true
    other.isSubtypeOf(refined) shouldBe false
    shouldThrow<IllegalStateException> { plain.isSubtypeOf(refined) }

    (type("Foo1(HAS Complex1)") glb type("Foo1(HAS? Complex1)")) shouldBe null
  }

  private fun type(s: String) = table.resolve(te(s))

  @Test
  internal fun partial() {
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
  internal fun outOfOrder() {
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
  internal fun twoSame() {
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
  internal fun complementDependencies() {
    val table =
        loadTypes(
            """
            CLASS Player1 : Owner
            CLASS Player2 : Owner
            """
                .trimIndent()
        )

    table.resolve(te("Owned<Player2>")).isSubtypeOf(table.resolve(te("Owned<!Player1>"))) shouldBe
        true
    table.resolve(te("Owned<Player1>")).isSubtypeOf(table.resolve(te("Owned<!Player1>"))) shouldBe
        false
  }

  @Test
  internal fun complementConstraintWithExplicitDomain() {
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
        NoGameState,
    ) shouldBe true
    table.matchesConstraint(
        table.resolve(te("TestPlayer1")),
        notPlayer1,
        actor,
        NoGameState,
    ) shouldBe false
    table.matchesConstraint(table.resolve(te("Engine")), notPlayer1, actor, NoGameState) shouldBe
        true
  }

  @Test
  internal fun roundTrip() {
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

    checkMinimal("TwoSame<Foo1, Foo3>", "TwoSame<Foo2, Foo3>")
    checkMinimal("TwoSame<Foo2, Foo3>")
  }

  @Test
  internal fun automaticNarrowingHonorsConstraints() {
    val narrowingTable =
        loadTypes(
            """
            ABSTRACT CLASS Place { CLASS OnlyPlace }
            CLASS Marker<Place>
            CLASS Holder<Place>
            """
                .trimIndent()
        )
    val hasMarker = narrowingTable.resolve(te("Holder<Place(HAS Marker)>"))
    val markedPlace = narrowingTable.resolve(te("Place(HAS Marker)"))
    val met = typeInfo(has = true)
    val unmet = typeInfo(has = false)

    markedPlace.singleConcreteSubtype(met) shouldBe narrowingTable.resolve(te("OnlyPlace"))
    markedPlace.singleConcreteSubtype(unmet) shouldBe null
    hasMarker.singleConcreteSubtype(met) shouldBe narrowingTable.resolve(te("Holder<OnlyPlace>"))
    hasMarker.singleConcreteSubtype(unmet) shouldBe null

    val multipleTable =
        loadTypes(
            """
            ABSTRACT CLASS Place { CLASS FirstPlace, SecondPlace }
            CLASS Marker<Place>
            CLASS ClassMarker<Class<Place>>
            """
                .trimIndent()
        )
    val onlyFirstIsMarked = typeInfo { "FirstPlace" in it.toString() }
    multipleTable.resolve(te("Place(HAS Marker)")).singleConcreteSubtype(onlyFirstIsMarked) shouldBe
        null
    multipleTable
        .resolve(te("Class<Place>(HAS ClassMarker)"))
        .singleConcreteSubtype(onlyFirstIsMarked) shouldBe
        multipleTable.resolve(te("Class<FirstPlace>"))

    val complementTable =
        loadTypes(
            """
            CLASS Player1 : Owner
            CLASS Player2 : Owner
            CLASS Possession<Owner>
            """
                .trimIndent()
        )
    complementTable.resolve(te("Possession<!Player1>")).singleConcreteSubtype(met) shouldBe
        complementTable.resolve(te("Possession<Player2>"))

    val incompatibleTable =
        loadTypes(
            """
            ABSTRACT CLASS Choice { CLASS Left, Right }
            ABSTRACT CLASS General<Choice> { CLASS Specific : General<Left> }
            """
                .trimIndent()
        )
    incompatibleTable.resolve(te("General<Right>")).singleConcreteSubtype(met) shouldBe null

    val specializedTable =
        loadTypes(
            """
            ABSTRACT CLASS Choice { CLASS Left, Right }
            ABSTRACT CLASS Holder<Choice> {
              CLASS LeftHolder : Holder<Left>
              CLASS RightHolder : Holder<Right>
            }
            """
                .trimIndent()
        )
    specializedTable.resolve(te("Holder<Left>")).singleConcreteSubtype(met) shouldBe
        specializedTable.resolve(te("LeftHolder"))
  }

  private fun typeInfo(has: Boolean): TypeInfo = typeInfo { has }

  private fun typeInfo(has: (Requirement) -> Boolean): TypeInfo =
      object : TypeInfo {
        override fun isAbstract(e: Expression): Boolean = error("unused")

        override fun ensureNarrows(wide: Expression, narrow: Expression): Unit = error("unused")

        override fun has(requirement: Requirement): Boolean = has(requirement)
      }

  private fun te(s: String): Expression = parse(s)
}
