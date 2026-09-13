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

  // T8-1 What a refinement is

  @Test
  internal fun `T8-1 a refined type is abstract and lies below its unrefined domain`() {
    val marked = type("LandArea(HAS Neighbor)")

    marked.abstract shouldBe true
    marked.refinement shouldBe te("LandArea(HAS Neighbor)").refinement
    marked.rootClass shouldBe mars.getClass(cn("LandArea"))
    marked.isSubtypeOf(type("LandArea")) shouldBe true
    marked.isSubtypeOf(type("Area")) shouldBe true
  }

  @Test
  internal fun `T8-1 the domain may be narrowed while keeping the same predicate`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe true
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("Tharsis_2_2(HAS Neighbor)")) shouldBe false
  }

  // T8-2, T8-3 Strict `HAS`, and candidate substitution

  @Test
  internal fun `T8-2 a candidate satisfies HAS when the world agrees, once it is substituted in`() {
    val world = RecordingWorld(answer = true)

    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor<CityTile>)"), world) shouldBe true
    world.questions shouldContainExactly listOf("Neighbor<CityTile<Area, Owner>, Tharsis_2_2>")
  }

  @Test
  internal fun `T8-2 a world that denies the requirement rejects the candidate`() {
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), emptyWorld) shouldBe false
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe true
  }

  @Test
  internal fun `T8-3 the candidate fills the first dependency of each expression that accepts it`() {
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
  internal fun `T8-3 a candidate no expression can accept fails the refinement, without a world`() {
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
  internal fun `T8-3 a written argument constrains the candidate in the slot it occupies`() {
    val world = RecordingWorld(answer = true)

    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor<CityTile<Player1>>)"), world) shouldBe
        true
    world.questions shouldContainExactly listOf("Neighbor<CityTile<Area, Player1>, Tharsis_2_2>")
  }

  @Test
  internal fun `T8-3 an exact argument leaves the candidate for another compatible slot`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
        )
    val world = RecordingWorld(answer = true)

    table
        .resolve(te("Tharsis_2_2"))
        .narrows(table.resolve(te("Area(HAS Adjacency<Tharsis_2_2>)")), world) shouldBe true
    world.questions shouldContainExactly listOf("Adjacency<Tharsis_2_2, Tharsis_2_2>")
  }

  @Test
  internal fun `T8-3 a broad argument remains eligible for candidate narrowing`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "ABSTRACT CLASS Adjacency<Area, Area>",
        )
    val world = RecordingWorld(answer = true)

    table
        .resolve(te("Tharsis_2_2"))
        .narrows(table.resolve(te("Area(HAS Adjacency<Area>)")), world) shouldBe true
    world.questions shouldContainExactly listOf("Adjacency<Tharsis_2_2, Area>")
  }

  @Test
  internal fun `T8-3 a class property may be tested against the candidate`() {
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

  // T8-4, T8-5, T8-6, T8-7 Difference

  private val actors =
      loadTypes(
          """
          ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }
          CLASS Marker<Player>
          """
              .trimIndent()
      )

  @Test
  internal fun `T8-4 a candidate satisfies NOT only when its whole domain avoids the exclusion`() {
    val notPlayer1 = actors.resolve(te("Owner(NOT Player1)"))

    actors.resolve(te("Player2")).isSubtypeOf(notPlayer1) shouldBe true
    actors.resolve(te("Player1")).isSubtypeOf(notPlayer1) shouldBe false
    // `Player` still admits Player1, so it does not satisfy the exclusion.
    actors.resolve(te("Player")).isSubtypeOf(notPlayer1) shouldBe false
  }

  @Test
  internal fun `T8-4 the exclusion is subtracted through the structural intersection`() {
    // Players inherit both Actor and Owner, so excluding Owner excludes them; Admin survives.
    val nonOwnerActor = actors.resolve(te("Actor(NOT Owner)"))

    actors.resolve(te("Admin")).isSubtypeOf(nonOwnerActor) shouldBe true
    actors.resolve(te("Player1")).isSubtypeOf(nonOwnerActor) shouldBe false
  }

  @Test
  internal fun `T8-4 the difference test never consults a world`() {
    actors
        .resolve(te("Player2"))
        .narrows(actors.resolve(te("Owner(NOT Player1)")), NoGameState) shouldBe true
  }

  @Test
  internal fun `T8-4 a difference works in a dependency position too`() {
    actors
        .resolve(te("Marker<Player2>"))
        .isSubtypeOf(actors.resolve(te("Marker<Player(NOT Player1)>"))) shouldBe true
    actors
        .resolve(te("Marker<Player1>"))
        .isSubtypeOf(actors.resolve(te("Marker<Player(NOT Player1)>"))) shouldBe false
  }

  @Test
  internal fun `T8-4 overlap is detected even with no greatest common subclass`() {
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
  internal fun `T8-4 an abstract common subclass is not evidence of concrete overlap`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Left
            ABSTRACT CLASS Right
            ABSTRACT CLASS AbstractOverlap : Left, Right
            CLASS LeftOnly : Left
            CLASS RightOnly : Right
            """
                .trimIndent()
        )

    table.resolve(te("Left(NOT Right)")) shouldBe table.resolve(te("Left"))
  }

  @Test
  internal fun `T8-5 the excluded operand must be free of refinements, recursively`() {
    shouldThrow<ExpressionException> { actors.resolve(te("Owner(NOT Player(HAS Marker))")) }
    shouldThrow<ExpressionException> { actors.resolve(te("Owner(NOT Player(NOT Player1))")) }
    shouldThrow<ExpressionException> {
      actors.resolve(te("Marker<Player(NOT Player(HAS Marker))>"))
    }
  }

  @Test
  internal fun `T8-6 a difference that cannot bite is dropped`() {
    actors.resolve(te("Player1(NOT Player2)")) shouldBe actors.resolve(te("Player1"))
    actors.resolve(te("Player1(NOT Player2)")).refinement shouldBe null
  }

  @Test
  internal fun `T8-7 a difference that excludes everything is still a type`() {
    val empty = actors.resolve(te("Player1(NOT Player1)"))

    empty.refinement shouldBe te("Player1(NOT Player1)").refinement
    empty.abstract shouldBe true
    empty.allConcreteSubtypes().toList() shouldContainExactly listOf()
  }

  // T8-8 Refinements and narrowing

  @Test
  internal fun `T8-8 a refined type always narrows its unrefined domain`() {
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea")) shouldBe true
    type("LandArea(NOT Tharsis_2_2)").isSubtypeOf(type("LandArea")) shouldBe true
  }

  @Test
  internal fun `T8-8 asking whether an unrefined type meets a HAS refinement needs a world`() {
    shouldThrow<IllegalStateException> {
      type("Tharsis_2_2").isSubtypeOf(type("LandArea(HAS Neighbor)"))
    }
    type("Tharsis_2_2").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe true
  }

  @Test
  internal fun `T8-8 an identical refinement is accepted without consulting a world`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe true
    type("Tharsis_2_2(NOT Tharsis_2_3)").isSubtypeOf(type("LandArea(NOT Tharsis_2_3)")) shouldBe
        true
  }

  @Test
  internal fun `T8-8 a refinement that conjoins more already guarantees the weaker one`() {
    type("LandArea(HAS Neighbor, HAS Occupant)")
        .isSubtypeOf(type("LandArea(HAS Neighbor)")) shouldBe true
    type("LandArea(HAS Neighbor)")
        .isSubtypeOf(type("LandArea(HAS Neighbor, HAS Occupant)")) shouldBe false
  }

  @Test
  internal fun `T8-8 unrelated predicates never imply one another`() {
    type("LandArea(HAS Neighbor)").isSubtypeOf(type("LandArea(HAS Occupant)")) shouldBe false
    type("LandArea(HAS Neighbor)").narrows(type("LandArea(HAS Occupant)"), fullWorld) shouldBe false
    type("LandArea(NOT Tharsis_2_2)").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe
        false
  }

  @Test
  internal fun `T8-8 a HAS-refined type may still satisfy a NOT, structurally`() {
    type("Tharsis_2_2(HAS Neighbor)").isSubtypeOf(type("LandArea(NOT Tharsis_2_3)")) shouldBe true
  }

  @Test
  internal fun `T8-8 HAS and NOT clauses jointly filter a candidate`() {
    val refined = type("LandArea(HAS Neighbor, NOT Tharsis_2_3)")

    type("Tharsis_2_2").narrows(refined, fullWorld) shouldBe true
    type("Tharsis_2_3").narrows(refined, fullWorld) shouldBe false
  }

  // T8-9 Greatest lower bound

  @Test
  internal fun `T8-9 glb keeps a refinement the other operand lacks`() {
    (type("LandArea(HAS Neighbor)") glb type("Tharsis_2_2")) shouldBe
        type("Tharsis_2_2(HAS Neighbor)")
    (type("Tharsis_2_2") glb type("LandArea(HAS Neighbor)")) shouldBe
        type("Tharsis_2_2(HAS Neighbor)")
  }

  @Test
  internal fun `T8-9 two identical refinements collapse to one`() {
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS Neighbor)")) shouldBe
        type("LandArea(HAS Neighbor)")
    (type("LandArea(NOT Tharsis_2_2)") glb type("LandArea(NOT Tharsis_2_2)")) shouldBe
        type("LandArea(NOT Tharsis_2_2)")
  }

  @Test
  internal fun `T8-9 two HAS refinements combine as a conjunction`() {
    (type("LandArea(HAS Neighbor)") glb type("LandArea(HAS Occupant)")) shouldBe
        type("LandArea(HAS Neighbor, HAS Occupant)")
  }

  @Test
  internal fun `T8-9 the conjunction really is below both operands`() {
    listOf(
            "LandArea(HAS Neighbor)" to "LandArea(HAS Occupant)",
            "LandArea(HAS Neighbor)" to "Tharsis_2_2",
        )
        .forEach { (left, right) ->
          val bound = (type(left) glb type(right))!!
          bound.isSubtypeOf(type(left)) shouldBe true
          bound.isSubtypeOf(type(right)) shouldBe true
        }
  }

  @Test
  internal fun `T8-9 unlike refinements combine as a conjunction`() {
    (type("LandArea(HAS Neighbor)") glb type("LandArea(NOT Tharsis_2_2)")) shouldBe
        type("LandArea(HAS Neighbor, NOT Tharsis_2_2)")
    (type("Area(NOT Tharsis_2_2)") glb type("Area(NOT WaterArea)")) shouldBe
        type("Area(NOT Tharsis_2_2, NOT WaterArea)")
  }

  @Test
  internal fun `T8-9 multiple NOT clauses jointly filter structural enumeration`() {
    type("Area(NOT Tharsis_2_2, NOT WaterArea)")
        .allConcreteSubtypes()
        .map { "$it" }
        .toList() shouldContainExactly listOf("Tharsis_2_3")
  }

  // T8-10 Refined class literals

  @Test
  internal fun `T8-10 a refined class literal tests the class the candidate names`() {
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
  internal fun `T8-10 two class literals do not compare their predicates as written`() {
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

  // T8-11 Refinements inside dependencies

  @Test
  internal fun `T8-11 a refinement on a dependency bound behaves like any other`() {
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
  internal fun `T8-11 refinements survive rendering`() {
    type("GreeneryTile<LandArea(HAS Neighbor)>").expression shouldBe
        te("GreeneryTile<LandArea(HAS Neighbor)>")
    type("LandArea(NOT Tharsis_2_2)").expression shouldBe te("LandArea(NOT Tharsis_2_2)")
    mars.resolve(type("LandArea(NOT Tharsis_2_2)").expression) shouldBe
        type("LandArea(NOT Tharsis_2_2)")
  }
}
