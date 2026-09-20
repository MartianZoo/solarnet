package dev.martianzoo.pets.types

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 7 of `docs/type-system-spec.md`: greatest lower bounds of types. */
internal class Spec07BoundsTest {

  private val mars =
      loadTypes(
          """
          CLASS Player1 : Owner
          CLASS Player2 : Owner
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea {
                CLASS Tharsis_2_2
                CLASS Tharsis_2_3
              }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
          }
          ABSTRACT CLASS Occupant<Area>
          ABSTRACT CLASS Tile : Occupant
          ABSTRACT CLASS OwnedTile : Tile, Owned<Owner>
          CLASS GreeneryTile : OwnedTile, Tile<MarsArea>
          CLASS OceanTile : Tile<WaterArea>
          """
              .trimIndent()
      )

  private fun type(s: String) = mars.resolve(te(s))

  private val sample =
      listOf(
          "Component",
          "Area",
          "LandArea",
          "Tharsis_2_2",
          "Occupant",
          "Occupant<LandArea>",
          "Tile",
          "Tile<Tharsis_2_2>",
          "OwnedTile",
          "OwnedTile<Player1>",
          "GreeneryTile",
          "GreeneryTile<Tharsis_2_2, Player1>",
          "OceanTile",
          "Owned<Player1>",
      )

  // T7-1 Greatest lower bound

  @Test
  internal fun `T7-1 glb of comparable types is the narrower one`() {
    mars.glb(type("Tharsis_2_2"), type("LandArea")) shouldBe type("Tharsis_2_2")
    mars.glb(type("GreeneryTile"), type("Tile")) shouldBe type("GreeneryTile")
    mars.glb(type("GreeneryTile"), type("GreeneryTile")) shouldBe type("GreeneryTile")
  }

  @Test
  internal fun `T7-1 glb intersects the root classes and every dependency`() {
    mars.glb(type("Tile<Tharsis_2_2>"), type("Owned<Player1>")) shouldBe
        type("OwnedTile<Tharsis_2_2, Player1>")
    mars.glb(type("Occupant<LandArea>"), type("Tile<Tharsis_2_2>")) shouldBe
        type("Tile<Tharsis_2_2>")
    mars.glb(type("GreeneryTile<Tharsis_2_2>"), type("GreeneryTile<Player1>")) shouldBe
        type("GreeneryTile<Tharsis_2_2, Player1>")
  }

  @Test
  internal fun `T7-1 glb is absent when the classes have no unique common subclass`() {
    mars.glb(type("GreeneryTile"), type("OceanTile")) shouldBe null
    mars.glb(type("LandArea"), type("WaterArea")) shouldBe null
  }

  @Test
  internal fun `T7-1 glb is absent when one dependency pair has no common narrowing`() {
    mars.glb(type("Tile<Tharsis_2_2>"), type("Tile<Tharsis_2_3>")) shouldBe null
    mars.glb(type("GreeneryTile<Player1>"), type("GreeneryTile<Player2>")) shouldBe null
  }

  @Test
  internal fun `T7-1 absent covers both disjointness and an overlap no class names`() {
    // Disjoint: no component could be both, and their extensions really are disjoint.
    mars.glb(type("Tharsis_2_2"), type("Tharsis_2_3")) shouldBe null
    mars.allConcreteSubtypes(type("Tharsis_2_2")).toSet() shouldBe setOf(type("Tharsis_2_2"))

    // Not writable: rival classes each combine the two, so concrete types below both do exist.
    val rivals =
        loadTypes(
            """
            ABSTRACT CLASS Tile
            ABSTRACT CLASS Owned2
            CLASS GreeneryTile : Tile, Owned2
            CLASS CommercialDistrictTile : Tile, Owned2
            """
                .trimIndent()
        )
    rivals.glb(rivals.resolve(te("Tile")), rivals.resolve(te("Owned2"))) shouldBe null
    rivals.allConcreteSubtypes(rivals.resolve(te("Tile"))).toSet() shouldBe
        setOf(rivals.resolve(te("GreeneryTile")), rivals.resolve(te("CommercialDistrictTile")))
  }

  @Test
  internal fun `T7-1 when glb exists it narrows both operands`() {
    sample.forEach { a ->
      sample.forEach { b ->
        val bound = mars.glb(type(a), type(b))
        if (bound != null) {
          bound.isSubtypeOf(type(a)) shouldBe true
          bound.isSubtypeOf(type(b)) shouldBe true
        }
      }
    }
  }

  @Test
  internal fun `T7-1 when glb exists it is the greatest such type in the sample`() {
    sample.forEach { a ->
      sample.forEach { b ->
        val bound = mars.glb(type(a), type(b))
        sample.forEach { c ->
          if (type(c).isSubtypeOf(type(a)) && type(c).isSubtypeOf(type(b))) {
            (bound != null && type(c).isSubtypeOf(bound)) shouldBe true
          }
        }
      }
    }
  }

  @Test
  internal fun `T7-1 the selected class contributes all of its declared dependency bounds`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area",
            "ABSTRACT CLASS LandArea : Area { CLASS Land1 }",
            "CLASS Water1 : Area",
            "CLASS Player1 : Owner",
            "ABSTRACT CLASS Left<Area>",
            "ABSTRACT CLASS Right",
            "CLASS Both<Owner> : Left<LandArea>, Right",
        )

    val intersection = table.glb(table.resolve(te("Left")), table.resolve(te("Right")))
    intersection shouldBe table.resolve(te("Both"))
    table.resolve(intersection!!.expression) shouldBe intersection
    table.resolve(intersection.expressionFull) shouldBe intersection
    intersection.isSubtypeOf(table.resolve(te("Left"))) shouldBe true
    intersection.isSubtypeOf(table.resolve(te("Right"))) shouldBe true

    table.glb(table.resolve(te("Left<Water1>")), table.resolve(te("Right"))) shouldBe null
  }

  // T7-2 Algebraic shape

  @Test
  internal fun `T7-2 glb is idempotent`() {
    sample.forEach { a ->
      mars.glb(type(a), type(a)) shouldBe type(a)
    }
  }

  @Test
  internal fun `T7-2 glb is commutative`() {
    sample.forEach { a ->
      sample.forEach { b ->
        mars.glb(type(a), type(b)) shouldBe mars.glb(type(b), type(a))
      }
    }
  }

  @Test
  internal fun `T7-2 glb is commutative for refinement conjunctions`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "CLASS Neighbor<Area>",
            "CLASS Marker<Area>",
        )
    val left = table.resolve(te("Area(HAS Neighbor)"))
    val right = table.resolve(te("Area(HAS Marker)"))

    "${table.glb(left, right)}" shouldBe "Area(HAS Neighbor, HAS Marker)"
    "${table.glb(right, left)}" shouldBe "Area(HAS Marker, HAS Neighbor)"
    table.glb(left, right) shouldBe table.glb(right, left)
  }

  // T7-3 Universe safety

  @Test
  internal fun `T7-3 bounds across universes are rejected rather than answered`() {
    val other = loadTypes("ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    shouldThrowIae { mars.glb(type("Tharsis_2_2"), other.resolve(te("Area"))) }
  }
}
