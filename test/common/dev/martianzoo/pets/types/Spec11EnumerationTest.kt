package dev.martianzoo.pets.types

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 11 of `docs/type-system-spec.md`: enumeration and automatic narrowing. */
internal class Spec11EnumerationTest {

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
          ABSTRACT CLASS Tile : Occupant {
            CLASS GreeneryTile : Tile<LandArea>
            CLASS OceanTile : Tile<WaterArea>
          }
          CLASS Neighbor<Occupant, Area>
          """
              .trimIndent()
      )

  private fun type(s: String) = mars.resolve(te(s))

  private fun subtypes(s: String) = type(s).allConcreteSubtypes().map { "$it" }.toList()

  // 11-1 Enumerating concrete narrowings

  @Test
  internal fun `11-1 enumeration pairs every concrete subclass with every concrete binding`() {
    subtypes("Tile") shouldContainExactly
        listOf(
            "GreeneryTile<Tharsis_2_2>",
            "GreeneryTile<Tharsis_2_3>",
            "OceanTile<Tharsis_1_1>",
        )
    subtypes("Tile<Tharsis_2_2>") shouldContainExactly listOf("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `11-1 a concrete type enumerates only itself`() {
    subtypes("GreeneryTile<Tharsis_2_2>") shouldContainExactly listOf("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `11-1 a type with no concrete narrowing enumerates nothing`() {
    val table = loadTypes("ABSTRACT CLASS Award", "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    table.resolve(te("Award")).allConcreteSubtypes().toList() shouldBe listOf()
    // No area is a Landlord award, so an impossible intersection enumerates nothing.
    table.resolve(te("Area(NOT Area)")).allConcreteSubtypes().toList() shouldBe listOf()
  }

  // 11-2 Refinements during enumeration

  @Test
  internal fun `11-2 a difference filters the enumerated candidates`() {
    subtypes("Area(NOT WaterArea)") shouldContainExactly listOf("Tharsis_2_2", "Tharsis_2_3")
    subtypes("Tile<Area(NOT WaterArea)>") shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_2>", "GreeneryTile<Tharsis_2_3>")
  }

  @Test
  internal fun `11-2 a world-dependent refinement is not applied while enumerating`() {
    // Enumeration is world-free, so `HAS` is left for the caller to test.
    subtypes("LandArea(HAS Neighbor)") shouldContainExactly listOf("Tharsis_2_2", "Tharsis_2_3")
  }

  // 11-3 Enumeration within one class

  @Test
  internal fun `11-3 same-class enumeration keeps the root class fixed`() {
    type("GreeneryTile")
        .groundType
        .concreteSubtypesSameClass()
        .map { "$it" }
        .toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_2>", "GreeneryTile<Tharsis_2_3>")
  }

  @Test
  internal fun `11-3 same-class enumeration of an abstract class yields nothing`() {
    type("Tile").groundType.concreteSubtypesSameClass().toList() shouldBe listOf()
  }

  // 11-4 Automatic narrowing

  @Test
  internal fun `11-4 a type with exactly one concrete narrowing narrows automatically`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Tile<Area> { CLASS GreeneryTile }",
        )

    table.resolve(te("Tile")).singleConcreteSubtype(fullWorld) shouldBe
        table.resolve(te("GreeneryTile<Tharsis_2_2>"))
  }

  @Test
  internal fun `11-4 a choice anywhere blocks automatic narrowing`() {
    // Two possible classes.
    type("Tile<Tharsis_2_2>").singleConcreteSubtype(fullWorld) shouldBe
        type("GreeneryTile<Tharsis_2_2>")
    type("Tile").singleConcreteSubtype(fullWorld) shouldBe null
    // One class, but two possible areas.
    type("GreeneryTile").singleConcreteSubtype(fullWorld) shouldBe null
    // Nothing left to choose.
    type("GreeneryTile<Tharsis_2_2>").singleConcreteSubtype(fullWorld) shouldBe
        type("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `11-4 the sole candidate must also satisfy the refinement`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "CLASS Neighbor<Area>",
            "CLASS GreeneryTile<Area>",
        )

    table.resolve(te("Area(HAS Neighbor)")).singleConcreteSubtype(fullWorld) shouldBe
        table.resolve(te("Tharsis_2_2"))
    table.resolve(te("Area(HAS Neighbor)")).singleConcreteSubtype(emptyWorld) shouldBe null
    table.resolve(te("GreeneryTile<Area(HAS Neighbor)>")).singleConcreteSubtype(fullWorld) shouldBe
        table.resolve(te("GreeneryTile<Tharsis_2_2>"))
    table.resolve(te("GreeneryTile<Area(HAS Neighbor)>")).singleConcreteSubtype(emptyWorld) shouldBe
        null
  }

  @Test
  internal fun `11-4 a world-dependent refinement can single out one of several candidates`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "CLASS Neighbor<Area>",
            "CLASS ClassNeighbor<Class<Area>>",
        )
    val onlyOneArea = world("Tharsis_2_2")

    // A plain `HAS` does not enumerate, so it cannot pick between two areas...
    table.resolve(te("Area(HAS Neighbor)")).singleConcreteSubtype(onlyOneArea) shouldBe null
    // ...but a refined class literal does enumerate and test each candidate.
    table.resolve(te("Class<Area>(HAS ClassNeighbor)")).singleConcreteSubtype(onlyOneArea) shouldBe
        table.resolve(te("Class<Tharsis_2_2>"))
  }

  @Test
  internal fun `11-4 automatic narrowing sees through a difference`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "CLASS Player2 : Owner",
            "CLASS Plant : Owned<Owner>",
        )

    table.resolve(te("Plant<Owner(NOT Player1)>")).singleConcreteSubtype(fullWorld) shouldBe
        table.resolve(te("Plant<Player2>"))
  }

  @Test
  internal fun `11-4 an incompatible narrowing yields no candidate at all`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS LandArea, WaterArea }",
            "ABSTRACT CLASS Tile<Area> { CLASS GreeneryTile : Tile<LandArea> }",
        )

    table.resolve(te("Tile<WaterArea>")).singleConcreteSubtype(fullWorld) shouldBe null
  }

  @Test
  internal fun `11-4 a subclass that already fixes the dependency is found`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS LandArea, WaterArea }",
            """
            ABSTRACT CLASS Tile<Area> {
              CLASS GreeneryTile : Tile<LandArea>
              CLASS OceanTile : Tile<WaterArea>
            }
            """
                .trimIndent(),
        )

    table.resolve(te("Tile<LandArea>")).singleConcreteSubtype(fullWorld) shouldBe
        table.resolve(te("GreeneryTile"))
    // The class table's own overload has to agree; it is the one the engine calls.
    table.singleConcreteSubtype(table.resolve(te("Tile<LandArea>")), fullWorld) shouldBe
        table.resolve(te("GreeneryTile"))
  }

  @Test
  internal fun `11-4 both overloads answer alike about the same universe`() {
    listOf(
            "Tile",
            "Tile<Tharsis_2_2>",
            "GreeneryTile",
            "GreeneryTile<Tharsis_2_2>",
            "Occupant<Tharsis_1_1>",
            "LandArea",
        )
        .forEach {
          mars.singleConcreteSubtype(type(it), fullWorld) shouldBe
              type(it).singleConcreteSubtype(fullWorld)
        }
  }

  // 11-5 Enumerating over a caller-supplied set of targets

  @Test
  internal fun `11-5 a caller may supply the dependency targets to consider`() {
    val onlyOneArea: (Type) -> Sequence<Type> = { bound ->
      sequenceOf(type("Tharsis_2_3")).filter { it.isSubtypeOf(bound) }
    }

    mars.allConcreteSubtypes(type("Tile"), onlyOneArea).map { "$it" }.toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_3>")
  }
}
