package dev.martianzoo.pets.types

import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** Section 7 of `docs/type-system-spec.md`: greatest lower and least upper bounds of types. */
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

  // T7-2 Least upper bound

  @Test
  internal fun `T7-2 lub of comparable types is the wider one`() {
    (type("Tharsis_2_2") lub type("LandArea")) shouldBe type("LandArea")
    (type("GreeneryTile") lub type("Tile")) shouldBe type("Tile")
    (type("GreeneryTile") lub type("GreeneryTile")) shouldBe type("GreeneryTile")
  }

  @Test
  internal fun `T7-2 lub joins the root classes and the dependencies they share`() {
    (type("GreeneryTile<Tharsis_2_2, Player1>") lub type("OceanTile<Tharsis_1_1>")) shouldBe
        type("Tile<MarsArea>")
    (type("GreeneryTile<Tharsis_2_2, Player1>") lub
        type("GreeneryTile<Tharsis_2_3, Player1>")) shouldBe type("GreeneryTile<LandArea, Player1>")
  }

  @Test
  internal fun `T7-2 lub always exists, falling back to Component`() {
    (type("Tharsis_2_2") lub type("Player1")) shouldBe type("Component")
  }

  @Test
  internal fun `T7-2 lub is an upper bound of both operands`() {
    sample.forEach { a ->
      sample.forEach { b ->
        val bound = type(a) lub type(b)
        type(a).isSubtypeOf(bound) shouldBe true
        type(b).isSubtypeOf(bound) shouldBe true
      }
    }
  }

  // T7-3 Algebraic shape

  @Test
  internal fun `T7-3 both operations are idempotent`() {
    sample.forEach { a ->
      (type(a) glb type(a)) shouldBe type(a)
      (type(a) lub type(a)) shouldBe type(a)
    }
  }

  @Test
  internal fun `T7-3 both operations are commutative on structural types`() {
    sample.forEach { a ->
      sample.forEach { b ->
        (type(a) glb type(b)) shouldBe (type(b) glb type(a))
        (type(a) lub type(b)) shouldBe (type(b) lub type(a))
      }
    }
  }

  @Test
  internal fun `T7-3 lub may pick a different tied candidate when the operands swap`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS GlobalParameter",
            "ABSTRACT CLASS OceanTile : Tile, GlobalParameter",
            "ABSTRACT CLASS PolarOceanTile : GlobalParameter, Tile",
        )
    val left = table.resolve(te("OceanTile"))
    val right = table.resolve(te("PolarOceanTile"))

    // Neither `Tile` nor `GlobalParameter` is more minimal than the other, and rule T2-9 breaks the
    // tie by the order each operand happens to list its supertypes. Which one wins is deliberately
    // unspecified (appendix B), so this pins only that the two orders can disagree and that each
    // answer is one of the minimal common supertypes.
    val candidates = setOf(table.resolve(te("Tile")), table.resolve(te("GlobalParameter")))
    (left lub right) shouldBeIn candidates
    (right lub left) shouldBeIn candidates
    (left lub right) shouldNotBe (right lub left)
  }

  @Test
  internal fun `T7-3 glb writes a joined requirement in operand order`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "CLASS Neighbor<Area>",
            "CLASS Marker<Area>",
        )
    val left = table.resolve(te("Area(HAS Neighbor)"))
    val right = table.resolve(te("Area(HAS Marker)"))

    // Equivalent predicates, differently written; the two results are therefore not `==`.
    "${(left glb right)}" shouldBe "Area(HAS Neighbor, HAS Marker)"
    "${(right glb left)}" shouldBe "Area(HAS Marker, HAS Neighbor)"
    ((left glb right) == (right glb left)) shouldBe false
  }

  // T7-4 Universe safety

  @Test
  internal fun `T7-4 bounds across universes are rejected rather than answered`() {
    val other = loadTypes("ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    shouldThrowIae { type("Tharsis_2_2") glb other.resolve(te("Area")) }
    shouldThrowIae { type("Tharsis_2_2") lub other.resolve(te("Area")) }
  }
}
