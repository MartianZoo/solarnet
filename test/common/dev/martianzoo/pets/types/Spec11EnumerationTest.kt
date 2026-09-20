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

  private fun subtypes(s: String) = mars.allConcreteSubtypes(type(s)).map { "$it" }.toList()

  // T11-1 Enumerating concrete narrowings

  @Test
  internal fun `T11-1 enumeration pairs every concrete subclass with every concrete binding`() {
    subtypes("Tile") shouldContainExactly
        listOf(
            "GreeneryTile<Tharsis_2_2>",
            "GreeneryTile<Tharsis_2_3>",
            "OceanTile<Tharsis_1_1>",
        )
    subtypes("Tile<Tharsis_2_2>") shouldContainExactly listOf("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `T11-1 a concrete type enumerates only itself`() {
    subtypes("GreeneryTile<Tharsis_2_2>") shouldContainExactly listOf("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `T11-1 a type with no concrete narrowing enumerates nothing`() {
    val table = loadTypes("ABSTRACT CLASS Award", "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }")

    table.allConcreteSubtypes(table.resolve(te("Award"))).toList() shouldBe listOf()
    // No area is a Landlord award, so an impossible intersection enumerates nothing.
    table.allConcreteSubtypes(table.resolve(te("Area(NOT Area)"))).toList() shouldBe listOf()
  }

  // T11-2 Refinements during enumeration

  @Test
  internal fun `T11-2 a difference filters the enumerated candidates`() {
    subtypes("Area(NOT WaterArea)") shouldContainExactly listOf("Tharsis_2_2", "Tharsis_2_3")
    subtypes("Tile<Area(NOT WaterArea)>") shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_2>", "GreeneryTile<Tharsis_2_3>")
  }

  @Test
  internal fun `T11-2 a world-dependent refinement is not applied while enumerating`() {
    // Enumeration is world-free, so `HAS` is left for the caller to test.
    subtypes("LandArea(HAS Neighbor)") shouldContainExactly listOf("Tharsis_2_2", "Tharsis_2_3")
  }

  @Test
  internal fun `T11-2 enumeration APIs return the same unrefined concrete candidates`() {
    val table = loadTypes("CLASS Flag", "CLASS One")
    val impossible = table.resolve(te("One(NOT One)"))
    val querying = table.resolve(te("One(HAS Flag)"))
    val one = table.resolve(te("One"))

    table.allConcreteSubtypes(impossible).toList() shouldBe emptyList()
    table.concreteSubtypesSameClass(impossible).toList() shouldBe emptyList()

    table.allConcreteSubtypes(querying).toList() shouldContainExactly listOf(one)
    table.concreteSubtypesSameClass(querying).toList() shouldContainExactly listOf(one)
  }

  // T11-3 Enumeration within one class

  @Test
  internal fun `T11-3 same-class enumeration keeps the root class fixed`() {
    mars.concreteSubtypesSameClass(type("GreeneryTile")).map { "$it" }.toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_2>", "GreeneryTile<Tharsis_2_3>")
  }

  @Test
  internal fun `T11-3 same-class enumeration of an abstract class yields nothing`() {
    mars.concreteSubtypesSameClass(type("Tile")).toList() shouldBe listOf()
  }

  // T11-4 Automatic narrowing

  @Test
  internal fun `T11-4 a type with exactly one concrete narrowing narrows automatically`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "ABSTRACT CLASS Tile<Area> { CLASS GreeneryTile }",
        )

    table.singleConcreteSubtype(table.resolve(te("Tile")), fullWorld) shouldBe
        table.resolve(te("GreeneryTile<Tharsis_2_2>"))
  }

  @Test
  internal fun `T11-4 a choice anywhere blocks automatic narrowing`() {
    // Two possible classes.
    mars.singleConcreteSubtype(type("Tile<Tharsis_2_2>"), fullWorld) shouldBe
        type("GreeneryTile<Tharsis_2_2>")
    mars.singleConcreteSubtype(type("Tile"), fullWorld) shouldBe null
    // One class, but two possible areas.
    mars.singleConcreteSubtype(type("GreeneryTile"), fullWorld) shouldBe null
    // Nothing left to choose.
    mars.singleConcreteSubtype(type("GreeneryTile<Tharsis_2_2>"), fullWorld) shouldBe
        type("GreeneryTile<Tharsis_2_2>")
  }

  @Test
  internal fun `T11-4 the sole candidate must also satisfy the refinement`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2 }",
            "CLASS Neighbor<Area>",
            "CLASS GreeneryTile<Area>",
        )

    table.singleConcreteSubtype(table.resolve(te("Area(HAS Neighbor)")), fullWorld) shouldBe
        table.resolve(te("Tharsis_2_2"))
    table.singleConcreteSubtype(table.resolve(te("Area(HAS Neighbor)")), emptyWorld) shouldBe null
    table.singleConcreteSubtype(
        table.resolve(te("GreeneryTile<Area(HAS Neighbor)>")),
        fullWorld,
    ) shouldBe table.resolve(te("GreeneryTile<Tharsis_2_2>"))
    table.singleConcreteSubtype(
        table.resolve(te("GreeneryTile<Area(HAS Neighbor)>")),
        emptyWorld,
    ) shouldBe null
  }

  @Test
  internal fun `T11-4 a world-dependent refinement can single out one of several candidates`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Tharsis_2_2, Tharsis_2_3 }",
            "CLASS Neighbor<Area>",
            "CLASS ClassNeighbor<Class<Area>>",
        )
    val onlyOneArea = world("Tharsis_2_2")

    // A plain `HAS` does not enumerate, so it cannot pick between two areas...
    table.singleConcreteSubtype(table.resolve(te("Area(HAS Neighbor)")), onlyOneArea) shouldBe null
    // ...but a refined class literal does enumerate and test each candidate.
    table.singleConcreteSubtype(
        table.resolve(te("Class<Area>(HAS ClassNeighbor)")),
        onlyOneArea,
    ) shouldBe table.resolve(te("Class<Tharsis_2_2>"))
  }

  @Test
  internal fun `T11-4 automatic narrowing sees through a difference`() {
    val table =
        loadTypes(
            "CLASS Player1 : Owner",
            "CLASS Player2 : Owner",
            "CLASS Plant : Owned<Owner>",
        )

    table.singleConcreteSubtype(
        table.resolve(te("Plant<Owner(NOT Player1)>")),
        fullWorld,
    ) shouldBe table.resolve(te("Plant<Player2>"))
  }

  @Test
  internal fun `T11-4 a difference is applied after dependencies are specialized`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS Land, Water }",
            "CLASS Holder<Area>",
        )

    val requested = table.resolve(te("Holder(NOT Holder<Land>)"))
    table.allConcreteSubtypes(requested).toList() shouldBe
        listOf(table.resolve(te("Holder<Water>")))
    table.singleConcreteSubtype(requested, fullWorld) shouldBe table.resolve(te("Holder<Water>"))
  }

  @Test
  internal fun `T11-4 HAS cannot choose between candidates exposed by NOT`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Choice { CLASS One, Two, Three }",
            "CLASS Flag<Choice>",
        )
    val onlyOneHasFlag = world("Flag<One>")

    table.singleConcreteSubtype(
        table.resolve(te("Choice(HAS Flag, NOT Three)")),
        onlyOneHasFlag,
    ) shouldBe null
    table.singleConcreteSubtype(
        table.resolve(te("Choice(HAS Flag, NOT Two, NOT Three)")),
        onlyOneHasFlag,
    ) shouldBe table.resolve(te("One"))
  }

  @Test
  internal fun `T11-4 an incompatible narrowing yields no candidate at all`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area { CLASS LandArea, WaterArea }",
            "ABSTRACT CLASS Tile<Area> { CLASS GreeneryTile : Tile<LandArea> }",
        )

    table.singleConcreteSubtype(table.resolve(te("Tile<WaterArea>")), fullWorld) shouldBe null
  }

  @Test
  internal fun `T11-4 a subclass that already fixes the dependency is found`() {
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

    table.singleConcreteSubtype(table.resolve(te("Tile<LandArea>")), fullWorld) shouldBe
        table.resolve(te("GreeneryTile"))
  }

  // T11-5 Enumerating over a caller-supplied set of targets

  @Test
  internal fun `T11-5 a caller may supply the dependency targets to consider`() {
    val onlyOneArea: (Type) -> Sequence<Type> = { bound ->
      sequenceOf(type("Tharsis_2_3")).filter { it.isSubtypeOf(bound) }
    }

    mars.allConcreteSubtypes(type("Tile"), onlyOneArea).map { "$it" }.toList() shouldContainExactly
        listOf("GreeneryTile<Tharsis_2_3>")
  }
}
