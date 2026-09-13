package dev.martianzoo.pets.types

import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 7 of `docs/type-system-spec.md`: intersection of type constraints. */
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

  // T7-1 Constraint intersection

  @Test
  internal fun `T7-1 intersection of comparable types uses the narrower root`() {
    (type("Tharsis_2_2") intersect type("LandArea")) shouldBe type("Tharsis_2_2")
    (type("GreeneryTile") intersect type("Tile")) shouldBe type("GreeneryTile")
    (type("GreeneryTile") intersect type("GreeneryTile")) shouldBe type("GreeneryTile")
  }

  @Test
  internal fun `T7-1 intersection combines dependencies after selecting a comparable root`() {
    (type("Occupant<LandArea>") intersect type("Tile<Tharsis_2_2>")) shouldBe
        type("Tile<Tharsis_2_2>")
    (type("GreeneryTile<Tharsis_2_2>") intersect type("GreeneryTile<Player1>")) shouldBe
        type("GreeneryTile<Tharsis_2_2, Player1>")
  }

  @Test
  internal fun `T7-1 intersection does not infer a third root class`() {
    (type("Tile<Tharsis_2_2>") intersect type("Owned<Player1>")) shouldBe null
    (type("GreeneryTile") intersect type("OceanTile")) shouldBe null
    (type("LandArea") intersect type("WaterArea")) shouldBe null
  }

  @Test
  internal fun `T7-1 intersection is absent when one dependency pair is incomparable`() {
    (type("Tile<Tharsis_2_2>") intersect type("Tile<Tharsis_2_3>")) shouldBe null
    (type("GreeneryTile<Player1>") intersect type("GreeneryTile<Player2>")) shouldBe null
  }

  @Test
  internal fun `T7-1 an intersection narrows both operands`() {
    sample.forEach { a ->
      sample.forEach { b ->
        val bound = type(a) intersect type(b)
        if (bound != null) {
          bound.isSubtypeOf(type(a)) shouldBe true
          bound.isSubtypeOf(type(b)) shouldBe true
        }
      }
    }
  }

  @Test
  internal fun `T7-1 intersection does not select a common descendant with extra dependencies`() {
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

    (table.resolve(te("Left")) intersect table.resolve(te("Right"))) shouldBe null
    (table.resolve(te("Left<Water1>")) intersect table.resolve(te("Right"))) shouldBe null
  }

  // T7-2 Algebraic shape

  @Test
  internal fun `T7-2 intersection is idempotent`() {
    sample.forEach { a ->
      (type(a) intersect type(a)) shouldBe type(a)
    }
  }

  @Test
  internal fun `T7-2 intersection is commutative`() {
    sample.forEach { a ->
      sample.forEach { b ->
        (type(a) intersect type(b)) shouldBe (type(b) intersect type(a))
      }
    }
  }

  @Test
  internal fun `T7-2 intersection is commutative for refinement conjunctions`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "CLASS Neighbor<Area>",
            "CLASS Marker<Area>",
        )
    val left = table.resolve(te("Area(HAS Neighbor)"))
    val right = table.resolve(te("Area(HAS Marker)"))

    "${(left intersect right)}" shouldBe "Area(HAS Neighbor, HAS Marker)"
    "${(right intersect left)}" shouldBe "Area(HAS Marker, HAS Neighbor)"
    (left intersect right) shouldBe (right intersect left)
  }

  // T7-3 Universe safety

  @Test
  internal fun `T7-3 bounds across universes are rejected rather than answered`() {
    val other = loadTypes("ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    shouldThrowIae { type("Tharsis_2_2") intersect other.resolve(te("Area")) }
  }
}
