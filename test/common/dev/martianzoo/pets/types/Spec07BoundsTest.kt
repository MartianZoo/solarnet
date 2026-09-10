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
              ABSTRACT CLASS LandArea { CLASS Tharsis_2_2, Tharsis_2_3 }
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
    (type("Tharsis_2_2") glb type("LandArea")) shouldBe type("Tharsis_2_2")
    (type("GreeneryTile") glb type("Tile")) shouldBe type("GreeneryTile")
    (type("GreeneryTile") glb type("GreeneryTile")) shouldBe type("GreeneryTile")
  }

  @Test
  internal fun `T7-1 glb intersects the root classes and every dependency`() {
    (type("Tile<Tharsis_2_2>") glb type("Owned<Player1>")) shouldBe
        type("OwnedTile<Tharsis_2_2, Player1>")
    (type("Occupant<LandArea>") glb type("Tile<Tharsis_2_2>")) shouldBe type("Tile<Tharsis_2_2>")
    (type("GreeneryTile<Tharsis_2_2>") glb type("GreeneryTile<Player1>")) shouldBe
        type("GreeneryTile<Tharsis_2_2, Player1>")
  }

  @Test
  internal fun `T7-1 glb is absent when the classes have no unique common subclass`() {
    (type("GreeneryTile") glb type("OceanTile")) shouldBe null
    (type("LandArea") glb type("WaterArea")) shouldBe null
  }

  @Test
  internal fun `T7-1 glb is absent when one dependency pair has no common narrowing`() {
    (type("Tile<Tharsis_2_2>") glb type("Tile<Tharsis_2_3>")) shouldBe null
    (type("GreeneryTile<Player1>") glb type("GreeneryTile<Player2>")) shouldBe null
  }

  @Test
  internal fun `T7-1 when glb exists it narrows both operands`() {
    sample.forEach { a ->
      sample.forEach { b ->
        val bound = type(a) glb type(b)
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
        val bound = type(a) glb type(b)
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

    val intersection = table.resolve(te("Left")) glb table.resolve(te("Right"))
    intersection shouldBe table.resolve(te("Both"))
    table.resolve(intersection!!.expression) shouldBe intersection
    table.resolve(intersection.expressionFull) shouldBe intersection
    intersection.isSubtypeOf(table.resolve(te("Left"))) shouldBe true
    intersection.isSubtypeOf(table.resolve(te("Right"))) shouldBe true

    (table.resolve(te("Left<Water1>")) glb table.resolve(te("Right"))) shouldBe null
  }

  // T7-2 Algebraic shape

  @Test
  internal fun `T7-2 glb is idempotent`() {
    sample.forEach { a ->
      (type(a) glb type(a)) shouldBe type(a)
    }
  }

  @Test
  internal fun `T7-2 glb is commutative`() {
    sample.forEach { a ->
      sample.forEach { b ->
        (type(a) glb type(b)) shouldBe (type(b) glb type(a))
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

    "${(left glb right)}" shouldBe "Area(HAS Neighbor, HAS Marker)"
    "${(right glb left)}" shouldBe "Area(HAS Marker, HAS Neighbor)"
    (left glb right) shouldBe (right glb left)
  }

  // T7-3 Universe safety

  @Test
  internal fun `T7-3 bounds across universes are rejected rather than answered`() {
    val other = loadTypes("ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    shouldThrowIae { type("Tharsis_2_2") glb other.resolve(te("Area")) }
  }
}
