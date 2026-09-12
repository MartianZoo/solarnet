package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** Section 6 of `docs/type-system-spec.md`: the subtype relation on types. */
internal class Spec06SubtypingTest {

  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea { CLASS Tharsis_2_2, Tharsis_2_3 }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant
          CLASS GreeneryTile : Tile<MarsArea>, Owned<Owner>
          CLASS OceanTile : Tile<WaterArea>
          CLASS Neighbor<Occupant, Area>
          """
              .trimIndent()
      )

  private fun type(s: String) = mars.resolve(te(s))

  private fun narrows(narrow: String, wide: String) = type(narrow).isSubtypeOf(type(wide))

  // T6-1 The two forms of the test

  @Test
  internal fun `T6-1 narrows answers, ensureNarrows explains`() {
    type("Tharsis_2_2").narrows(type("LandArea"), NoGameState) shouldBe true
    type("Tharsis_2_2").ensureNarrows(type("LandArea"), NoGameState)

    type("LandArea").narrows(type("Tharsis_2_2"), NoGameState) shouldBe false
    shouldThrow<NarrowingException> {
      type("LandArea").ensureNarrows(type("Tharsis_2_2"), NoGameState)
    }
  }

  @Test
  internal fun `T6-1 isSubtypeOf and isSupertypeOf are the world-free spellings`() {
    type("Tharsis_2_2").isSubtypeOf(type("LandArea")) shouldBe true
    type("LandArea").isSupertypeOf(type("Tharsis_2_2")) shouldBe true
    type("LandArea").isSubtypeOf(type("Tharsis_2_2")) shouldBe false
  }

  // T6-2 The structural rule

  @Test
  internal fun `T6-2 the root class must be a subclass`() {
    narrows("GreeneryTile", "Tile") shouldBe true
    narrows("GreeneryTile", "Occupant") shouldBe true
    narrows("GreeneryTile", "Component") shouldBe true
    narrows("Tile", "GreeneryTile") shouldBe false
    narrows("OceanTile", "GreeneryTile") shouldBe false
  }

  @Test
  internal fun `T6-2 every dependency must narrow too`() {
    narrows("GreeneryTile<Tharsis_2_2, Player1>", "GreeneryTile<LandArea, Player1>") shouldBe true
    narrows("GreeneryTile<Tharsis_2_2, Player1>", "GreeneryTile<Tharsis_2_3, Player1>") shouldBe
        false
    narrows("GreeneryTile<Tharsis_2_2, Player1>", "GreeneryTile<Tharsis_2_2, Player2>") shouldBe
        false
  }

  @Test
  internal fun `T6-2 a dependency the wider type does not have is not checked`() {
    // `Tile` has no owner dependency, so a greenery tile's owner is irrelevant to the test.
    narrows("GreeneryTile<Tharsis_2_2, Player1>", "Tile<Tharsis_2_2>") shouldBe true
    narrows("GreeneryTile<Tharsis_2_2, Player2>", "Tile<Tharsis_2_2>") shouldBe true
  }

  // T6-3 Covariance

  @Test
  internal fun `T6-3 dependencies are covariant`() {
    narrows("Occupant<Tharsis_2_2>", "Occupant<LandArea>") shouldBe true
    narrows("Occupant<LandArea>", "Occupant<Area>") shouldBe true
    narrows("Occupant<Area>", "Occupant<LandArea>") shouldBe false
  }

  @Test
  internal fun `T6-3 narrowing the class and a dependency compose`() {
    narrows("GreeneryTile<Tharsis_2_2>", "Tile<MarsArea>") shouldBe true
    narrows("GreeneryTile<Tharsis_2_2>", "Occupant<Area>") shouldBe true
    narrows("Tile<MarsArea>", "GreeneryTile<Tharsis_2_2>") shouldBe false
  }

  // T6-4 The relation's shape

  private val sample =
      listOf(
          "Component",
          "Area",
          "MarsArea",
          "LandArea",
          "Tharsis_2_2",
          "Occupant",
          "Occupant<LandArea>",
          "Tile",
          "Tile<Tharsis_2_2>",
          "GreeneryTile",
          "GreeneryTile<Tharsis_2_2>",
          "GreeneryTile<Tharsis_2_2, Player1>",
          "OceanTile",
      )

  @Test
  internal fun `T6-4 the relation is reflexive`() {
    sample.forEach { narrows(it, it) shouldBe true }
  }

  @Test
  internal fun `T6-4 the relation is transitive`() {
    sample.forEach { a ->
      sample.forEach { b ->
        sample.forEach { c ->
          if (narrows(a, b) && narrows(b, c)) narrows(a, c) shouldBe true
        }
      }
    }
  }

  @Test
  internal fun `T6-4 two types that narrow each other are the same type`() {
    sample.forEach { a ->
      sample.forEach { b ->
        if (narrows(a, b) && narrows(b, a)) type(a) shouldBe type(b)
      }
    }
  }

  @Test
  internal fun `T6-4 narrowing with a world is only a preorder`() {
    // In a world where every land area has a neighbour, these two narrow each other...
    type("LandArea").narrows(type("LandArea(HAS Neighbor)"), fullWorld) shouldBe true
    type("LandArea(HAS Neighbor)").narrows(type("LandArea"), fullWorld) shouldBe true

    // ...while remaining distinct types, so antisymmetry fails.
    type("LandArea") shouldNotBe type("LandArea(HAS Neighbor)")
  }

  // T6-5 Universe safety

  @Test
  internal fun `T6-5 narrowing across universes is rejected rather than answered`() {
    val other =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Occupant<Area>",
        )

    shouldThrowIae { type("Tharsis_2_2").isSubtypeOf(other.resolve(te("Area"))) }
  }

  // T6-6 Constrained narrowing

  @Test
  internal fun `T6-6 matchesConstraint reads a constraint inside a domain`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }",
        )
    val actor = table.resolve(te("Actor"))

    fun matches(candidate: String, constraint: String) =
        table.matchesConstraint(table.resolve(te(candidate)), te(constraint), actor, NoGameState)

    matches("Player1", "Player") shouldBe true
    matches("Admin", "Player") shouldBe false
    matches("Player1", "Player2") shouldBe false
    matches("Player1", "Actor") shouldBe true
  }

  @Test
  internal fun `T6-6 a constraint may exclude part of the domain`() {
    val table = loadTypes("ABSTRACT CLASS Player : Owner, Actor { CLASS Player1, Player2 }")
    val actor = table.resolve(te("Actor"))

    fun matches(candidate: String) =
        table.matchesConstraint(
            table.resolve(te(candidate)),
            te("Actor(NOT Player1)"),
            actor,
            NoGameState,
        )

    matches("Player2") shouldBe true
    matches("Player1") shouldBe false
    matches("Admin") shouldBe true
  }

  @Test
  internal fun `T6-6 a constraint that cannot meet the domain simply fails`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Player : Owner, Actor { CLASS Player1 }",
            "CLASS Plant",
        )
    val actor = table.resolve(te("Actor"))

    table.matchesConstraint(
        table.resolve(te("Player1")),
        te("Plant"),
        actor,
        NoGameState,
    ) shouldBe false
  }
}
