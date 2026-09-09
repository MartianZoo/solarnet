package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 8 of `docs/type-system-spec.md`: refinements. */
internal class Spec08RefinementsTest {

  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS LandArea { CLASS Tharsis_2_2, Tharsis_2_3 }
            ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant
          CLASS GreeneryTile : Tile, Owned<Owner>
          CLASS CityTile : Tile, Owned<Owner>
          CLASS Neighbor<Occupant, Area>
          """
              .trimIndent()
      )

  private fun type(s: String) = mars.resolve(te(s))

  // 8-1 What a refinement is

  @Test
  internal fun `8-1 a refined type is abstract and lies below its unrefined domain`() {
    val marked = type("LandArea(HAS Neighbor)")

    marked.abstract shouldBe true
    marked.refinement shouldBe te("LandArea(HAS Neighbor)").refinement
    marked.rootClass shouldBe mars.getClass(cn("LandArea"))
    marked.isSubtypeOf(type("LandArea")) shouldBe true
    marked.isSubtypeOf(type("Area")) shouldBe true
  }

  @Test
  internal fun `8-1 the domain may be narrowed while keeping the same predicate`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe true
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("Tharsis_2_2(HAS Neighbor)")) shouldBe false
  }

  // 8-2, 8-3 Strict `HAS`, and candidate substitution

  @Test
  internal fun `8-2 a candidate satisfies HAS when the world agrees, once it is substituted in`() {
    val world = RecordingWorld(answer = true)

    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor<CityTile>)"), world) shouldBe true
    world.questions shouldContainExactly listOf("Neighbor<CityTile<Area, Owner>, Tharsis_2_2>")
  }

  @Test
  internal fun `8-2 a world that denies the requirement rejects the candidate`() {
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), emptyWorld) shouldBe false
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe true
  }

  @Test
  internal fun `8-3 the candidate fills the first dependency of each expression that accepts it`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
            "CLASS StartToken<Player>",
            "CLASS Rock",
        )
    val world = RecordingWorld(answer = true)

    // `Component(HAS StartToken)` can only ever match a Player, because that is what a
    // `StartToken` depends on.
    table
        .resolve(te("Player1"))
        .narrows(
            table.resolve(te("Component(HAS StartToken)")),
            world,
        ) shouldBe true
    world.questions shouldContainExactly listOf("StartToken<Player1>")
  }

  @Test
  internal fun `8-3 a candidate no expression can accept fails the refinement, without a world`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner { CLASS Player1 }",
            "CLASS StartToken<Player>",
            "CLASS Rock",
        )
    val world = RecordingWorld(answer = true)

    table
        .resolve(te("Rock"))
        .narrows(
            table.resolve(te("Component(HAS StartToken)")),
            world,
        ) shouldBe false
    world.questions shouldContainExactly listOf()
  }

  @Test
  internal fun `8-3 a written argument constrains the candidate in the slot it occupies`() {
    val world = RecordingWorld(answer = true)

    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor<CityTile<Player1>>)"), world) shouldBe
        true
    world.questions shouldContainExactly listOf("Neighbor<CityTile<Area, Player1>, Tharsis_2_2>")
  }

  @Test
  internal fun `8-3 a class property may be tested against the candidate`() {
    val cards =
        loadTypes(
            """
            ABSTRACT CLASS CardFront { cost = Number }
            CLASS Ants : CardFront { cost = 9 }
            """
                .trimIndent()
        )
    val world = RecordingWorld(answer = true)

    cards
        .resolve(te("Ants"))
        .narrows(cards.resolve(te("CardFront(HAS MAX 9 cost)")), world) shouldBe true
    world.questions shouldContainExactly listOf("MAX 9 Ants.cost")
  }

  // 8-4 Forgiving `HAS?`

  @Test
  internal fun `8-4 a forgiving refinement adds an escape clause to its requirement`() {
    val world = RecordingWorld(answer = true)

    // Greenery placement: next to your own tile if possible, otherwise anywhere.
    type("Tharsis_2_2").narrows(type("LandArea(HAS? Neighbor)"), world) shouldBe true
    world.questions shouldContainExactly
        listOf("Neighbor<Occupant<Area>, Tharsis_2_2> OR MAX 0 LandArea(HAS Neighbor)")
  }

  @Test
  internal fun `8-4 forgiving and strict refinements are different predicates`() {
    type("LandArea(HAS Neighbor)") shouldBe type("LandArea(HAS Neighbor)")
    (type("LandArea(HAS Neighbor)") == type("LandArea(HAS? Neighbor)")) shouldBe false
  }

  // 8-5, 8-6, 8-7, 8-8 Difference

  private val actors =
      loadTypes(
          """
          ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }
          CLASS Marker<Player>
          """
              .trimIndent()
      )

  @Test
  internal fun `8-5 a candidate satisfies NOT only when its whole domain avoids the exclusion`() {
    val notPlayer1 = actors.resolve(te("Owner(NOT Player1)"))

    actors.resolve(te("Player2")).isSubtypeOf(notPlayer1) shouldBe true
    actors.resolve(te("Player1")).isSubtypeOf(notPlayer1) shouldBe false
    // `Player` still admits Player1, so it does not satisfy the exclusion.
    actors.resolve(te("Player")).isSubtypeOf(notPlayer1) shouldBe false
  }

  @Test
  internal fun `8-5 the exclusion is subtracted through the structural intersection`() {
    // Players inherit both Actor and Owner, so excluding Owner excludes them; Admin survives.
    val nonOwnerActor = actors.resolve(te("Actor(NOT Owner)"))

    actors.resolve(te("Admin")).isSubtypeOf(nonOwnerActor) shouldBe true
    actors.resolve(te("Player1")).isSubtypeOf(nonOwnerActor) shouldBe false
  }

  @Test
  internal fun `8-5 the difference test never consults a world`() {
    actors
        .resolve(te("Player2"))
        .narrows(actors.resolve(te("Owner(NOT Player1)")), NoGameState) shouldBe true
  }

  @Test
  internal fun `8-5 a difference works in a dependency position too`() {
    actors
        .resolve(te("Marker<Player2>"))
        .isSubtypeOf(actors.resolve(te("Marker<Player(NOT Player1)>"))) shouldBe true
    actors
        .resolve(te("Marker<Player1>"))
        .isSubtypeOf(actors.resolve(te("Marker<Player(NOT Player1)>"))) shouldBe false
  }

  @Test
  internal fun `8-5 overlap is detected even with no unique intersection class`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Occupant
            ABSTRACT CLASS Owned2
            CLASS GreeneryTile : Occupant, Owned2
            CLASS CityTile : Occupant, Owned2
            CLASS OceanTile : Occupant
            """
                .trimIndent()
        )
    val unowned = table.resolve(te("Occupant(NOT Owned2)"))

    table.resolve(te("GreeneryTile")).isSubtypeOf(unowned) shouldBe false
    table.resolve(te("CityTile")).isSubtypeOf(unowned) shouldBe false
    table.resolve(te("OceanTile")).isSubtypeOf(unowned) shouldBe true
    unowned.allConcreteSubtypes().map { "$it" }.toList() shouldContainExactly listOf("OceanTile")
  }

  @Test
  internal fun `8-6 the excluded operand must be free of refinements, recursively`() {
    shouldThrow<ExpressionException> { actors.resolve(te("Owner(NOT Player(HAS Marker))")) }
    shouldThrow<ExpressionException> { actors.resolve(te("Owner(NOT Player(NOT Player1))")) }
    shouldThrow<ExpressionException> {
      actors.resolve(te("Marker<Player(NOT Player(HAS Marker))>"))
    }
  }

  @Test
  internal fun `8-7 a difference that cannot bite is dropped`() {
    actors.resolve(te("Player1(NOT Player2)")) shouldBe actors.resolve(te("Player1"))
    actors.resolve(te("Player1(NOT Player2)")).refinement shouldBe null
  }

  @Test
  internal fun `8-8 a difference that excludes everything is still a type`() {
    val empty = actors.resolve(te("Player1(NOT Player1)"))

    empty.refinement shouldBe te("Player1(NOT Player1)").refinement
    empty.abstract shouldBe true
    empty.allConcreteSubtypes().toList() shouldContainExactly listOf()
  }

  // 8-9 Refinements and narrowing

  @Test
  internal fun `8-9 a refined type always narrows its unrefined domain`() {
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea")) shouldBe true
    type("LandArea(NOT Tharsis_2_2)").isSubtypeOf(type("LandArea")) shouldBe true
  }

  @Test
  internal fun `8-9 asking whether an unrefined type meets a HAS refinement needs a world`() {
    shouldThrow<IllegalStateException> {
      type("Tharsis_2_2").isSubtypeOf(type("LandArea(HAS Neighbor)"))
    }
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe true
  }

  @Test
  internal fun `8-9 an identical refinement is accepted without consulting a world`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe true
    type("Tharsis_2_2(NOT Tharsis_2_3)").isSubtypeOf(type("LandArea(NOT Tharsis_2_3)")) shouldBe
        true
  }

  @Test
  internal fun `8-9 a refinement that conjoins more already guarantees the weaker one`() {
    type("LandArea(HAS Neighbor, Occupant)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe
        true
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor, Occupant)")) shouldBe
        false
  }

  @Test
  internal fun `8-9 a strict refinement guarantees the forgiving version of itself`() {
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS? Neighbor)")) shouldBe true
    type("LandArea(HAS Neighbor, Occupant)").isSubtypeOf(type("LandArea(HAS? Neighbor)")) shouldBe
        true
    type("LandArea(HAS? Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe false
  }

  @Test
  internal fun `8-9 a forgiving refinement guarantees nothing but itself`() {
    // Each escape clause is relative to its own whole requirement, so conjoining more does not
    // narrow: where some area has a neighbor but none is occupied, every area satisfies
    // `HAS? Neighbor, Occupant` through the escape while only some satisfy `HAS? Neighbor`.
    type("LandArea(HAS? Neighbor)").isSubtypeOf(type("LandArea(HAS? Neighbor)")) shouldBe true
    type("LandArea(HAS? Neighbor, Occupant)").isSubtypeOf(type("LandArea(HAS? Neighbor)")) shouldBe
        false
    type("LandArea(HAS? Neighbor)").isSubtypeOf(type("LandArea(HAS? Neighbor, Occupant)")) shouldBe
        false
  }

  @Test
  internal fun `8-9 unrelated predicates never imply one another`() {
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Occupant)")) shouldBe false
    type("LandArea(HAS Neighbor)").narrows(type("LandArea(HAS Occupant)"), fullWorld) shouldBe false
    type("LandArea(NOT Tharsis_2_2)").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe
        false
  }

  @Test
  internal fun `8-9 a HAS-refined type may still satisfy a NOT, structurally`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(NOT Tharsis_2_3)")) shouldBe true
  }

  // 8-10 Greatest lower bound

  @Test
  internal fun `8-10 glb keeps a refinement the other operand lacks`() {
    (type("LandArea(HAS Neighbor)") glb type("Tharsis_2_2")) shouldBe
        type("Tharsis_2_2(HAS Neighbor)")
    (type("Tharsis_2_2") glb type("LandArea(HAS Neighbor)")) shouldBe
        type("Tharsis_2_2(HAS Neighbor)")
  }

  @Test
  internal fun `8-10 two identical refinements collapse to one`() {
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS Neighbor)")) shouldBe
        type("LandArea(HAS Neighbor)")
    (type("LandArea(HAS? Neighbor)") glb type("LandArea(HAS? Neighbor)")) shouldBe
        type("LandArea(HAS? Neighbor)")
    (type("LandArea(NOT Tharsis_2_2)") glb type("LandArea(NOT Tharsis_2_2)")) shouldBe
        type("LandArea(NOT Tharsis_2_2)")
  }

  @Test
  internal fun `8-10 when either operand is strict, glb is the strict conjunction`() {
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS Occupant)")) shouldBe
        type("LandArea(HAS Neighbor, Occupant)")
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS? Occupant)")) shouldBe
        type("LandArea(HAS Neighbor, Occupant)")
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS? Neighbor)")) shouldBe
        type("LandArea(HAS Neighbor)")
  }

  @Test
  internal fun `8-10 the conjunction really is below both operands`() {
    listOf(
            "LandArea(HAS Neighbor)" to "LandArea(HAS Occupant)",
            "LandArea(HAS Neighbor)" to "LandArea(HAS? Occupant)",
            "LandArea(HAS Neighbor)" to "LandArea(HAS? Neighbor)",
            "LandArea(HAS Neighbor)" to "Tharsis_2_2",
        )
        .forEach { (left, right) ->
          val bound = (type(left) glb type(right))!!
          bound.isSubtypeOf(type(left)) shouldBe true
          bound.isSubtypeOf(type(right)) shouldBe true
        }
  }

  @Test
  internal fun `8-10 glb is absent when the two predicates cannot be written as one`() {
    // Two forgiving predicates each carry their own escape clause (8-9).
    (type("LandArea(HAS? Neighbor)") glb type("LandArea(HAS? Occupant)")) shouldBe null
    (type("LandArea(HAS Neighbor)") glb type("LandArea(NOT Tharsis_2_2)")) shouldBe null
    (type("LandArea(NOT Tharsis_2_2)") glb type("LandArea(NOT Tharsis_2_3)")) shouldBe null
  }

  // 8-11 Least upper bound

  @Test
  internal fun `8-11 lub keeps a refinement only when both operands carry the same one`() {
    (type("Tharsis_2_2(HAS Neighbor)") lub type("Tharsis_2_3(HAS Neighbor)")) shouldBe
        type("LandArea(HAS Neighbor)")
    (type("Tharsis_2_2(HAS Neighbor)") lub type("Tharsis_2_3(HAS Occupant)")) shouldBe
        type("LandArea")
    (type("Tharsis_2_2(HAS Neighbor)") lub type("Tharsis_2_3")) shouldBe type("LandArea")
  }

  // 8-12 Refined class literals

  @Test
  internal fun `8-12 a refined class literal tests the class the candidate names`() {
    val tags =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Tag : Owned<Owner> { CLASS BuildingTag, SpaceTag }",
            "CLASS TagCount<Class<Tag>>",
        )
    val world = RecordingWorld(answer = true)

    tags
        .resolve(te("Class<BuildingTag>"))
        .narrows(
            tags.resolve(te("Class<Tag>(HAS Tag<Player1>)")),
            world,
        ) shouldBe true
    world.questions shouldContainExactly listOf("BuildingTag<Player1>")
  }

  @Test
  internal fun `8-12 two class literals do not compare their predicates as written`() {
    val tags =
        loadTypes(
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Tag : Owned<Owner> { CLASS BuildingTag, SpaceTag }",
        )

    // Same words, different meanings: for the target the predicate asks about the candidate's own
    // class, so "some tag exists" does not establish "some building tag exists".
    tags
        .resolve(te("Class<BuildingTag>(HAS Tag)"))
        .isSubtypeOf(tags.resolve(te("Class<Tag>(HAS Tag)"))) shouldBe false

    // For one and the same represented class the shortcut is still sound.
    tags
        .resolve(te("Class<BuildingTag>(HAS Tag)"))
        .isSubtypeOf(tags.resolve(te("Class<BuildingTag>(HAS Tag)"))) shouldBe true
  }

  // 8-13 Refinements inside dependencies

  @Test
  internal fun `8-13 a refinement on a dependency bound behaves like any other`() {
    type("GreeneryTile<LandArea(HAS Neighbor)>").abstract shouldBe true
    type("GreeneryTile<Tharsis_2_2, Player1>")
        .narrows(
            type("GreeneryTile<LandArea(HAS Neighbor)>"),
            fullWorld,
        ) shouldBe true
    type("GreeneryTile<Tharsis_2_2, Player1>")
        .narrows(
            type("GreeneryTile<LandArea(HAS Neighbor)>"),
            emptyWorld,
        ) shouldBe false
  }

  @Test
  internal fun `8-13 refinements survive rendering`() {
    type("GreeneryTile<LandArea(HAS Neighbor)>").expression shouldBe
        te("GreeneryTile<LandArea(HAS Neighbor)>")
    type("LandArea(NOT Tharsis_2_2)").expression shouldBe te("LandArea(NOT Tharsis_2_2)")
    mars.resolve(type("LandArea(NOT Tharsis_2_2)").expression) shouldBe
        type("LandArea(NOT Tharsis_2_2)")
  }
}
