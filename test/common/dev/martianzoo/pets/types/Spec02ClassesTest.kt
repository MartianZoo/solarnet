package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.CustomClass
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Section 2 of `docs/type-system-spec.md`: classes and the subclass relation. */
internal class Spec02ClassesTest {

  /** A fragment of the real Terraforming Mars area and tile hierarchy. */
  private val mars =
      loadTypes(
          """
          ABSTRACT CLASS Area {
            ABSTRACT CLASS MarsArea {
              ABSTRACT CLASS LandArea {
                CLASS Tharsis_2_2
                ABSTRACT CLASS VolcanicArea { CLASS Tharsis_5_5 }
              }
              ABSTRACT CLASS WaterArea { CLASS Tharsis_1_1 }
            }
            ABSTRACT CLASS RemoteArea
          }
          """
              .trimIndent()
      )

  private fun klass(name: String) = mars.getClass(cn(name))

  // 2-1 Declaration

  @Test
  internal fun `2-1 a declaration introduces a class and its base type`() {
    val table = loadTypes("ABSTRACT CLASS Tile", "CLASS GreeneryTile : Tile")

    table.getClass(cn("GreeneryTile")).abstract shouldBe false
    table.getClass(cn("Tile")).abstract shouldBe true
    table.getClass(cn("GreeneryTile")).baseType.expressionFull shouldBe te("GreeneryTile")
  }

  @Test
  internal fun `2-1 a class knows the declaration it was compiled from`() {
    val table = loadTypes("\"A greenery tile\"\nCLASS GreeneryTile")
    val greenery = table.getClass(cn("GreeneryTile"))

    greenery.declaration.className shouldBe cn("GreeneryTile")
    greenery.docstring shouldBe "A greenery tile"
  }

  // 2-2 Direct supertypes

  @Test
  internal fun `2-2 a class with no declared supertype extends Component`() {
    val table = loadTypes("CLASS GreeneryTile")

    table.getClass(cn("GreeneryTile")).directSuperclasses shouldBe listOf(table.componentClass)
  }

  @Test
  internal fun `2-2 naming Component as a supertype is an error`() {
    shouldThrow<PetException> { loadTypes("CLASS GreeneryTile : Component") }
  }

  @Test
  internal fun `2-2 nesting is shorthand for naming the enclosing class as a supertype`() {
    klass("LandArea").directSuperclasses shouldBe listOf(klass("MarsArea"))
    klass("Tharsis_2_2").directSuperclasses shouldBe listOf(klass("LandArea"))
  }

  @Test
  internal fun `2-2 a class may have several abstract direct supertypes`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Occupant",
            "ABSTRACT CLASS Tile : Occupant",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
        )

    table.getClass(cn("OwnedTile")).directSuperclasses.map { "$it" } shouldContainExactly
        listOf("Tile", "Owned")
  }

  // 2-3 Concrete classes are final

  @Test
  internal fun `2-3 no class may extend a concrete class`() {
    shouldThrow<PetException> {
      loadTypes("CLASS GreeneryTile", "CLASS SpecialTile : GreeneryTile")
    }
    shouldThrow<PetException> {
      loadTypes("CLASS GreeneryTile", "ABSTRACT CLASS SpecialTile : GreeneryTile")
    }
  }

  @Test
  internal fun `2-3 a concrete class has itself as its only subclass`() {
    klass("Tharsis_2_2").allSubclasses() shouldBe setOf(klass("Tharsis_2_2"))
    klass("Tharsis_2_2").directSubclasses().shouldBeEmpty()
  }

  // 2-4 The subclass relation

  @Test
  internal fun `2-4 the subclass relation is reflexive`() {
    mars.allClasses().forEach { it.isSubtypeOf(it) shouldBe true }
  }

  @Test
  internal fun `2-4 the subclass relation is transitive`() {
    klass("Tharsis_5_5").isSubtypeOf(klass("VolcanicArea")) shouldBe true
    klass("VolcanicArea").isSubtypeOf(klass("LandArea")) shouldBe true
    klass("Tharsis_5_5").isSubtypeOf(klass("LandArea")) shouldBe true
    klass("Tharsis_5_5").isSubtypeOf(mars.componentClass) shouldBe true
  }

  @Test
  internal fun `2-4 unrelated branches are not subclasses of each other`() {
    klass("LandArea").isSubtypeOf(klass("WaterArea")) shouldBe false
    klass("WaterArea").isSubtypeOf(klass("LandArea")) shouldBe false
    klass("Area").isSubtypeOf(klass("LandArea")) shouldBe false
  }

  @Test
  internal fun `2-4 isSupertypeOf is the converse of isSubtypeOf`() {
    klass("LandArea").isSupertypeOf(klass("Tharsis_2_2")) shouldBe true
    klass("Tharsis_2_2").isSupertypeOf(klass("LandArea")) shouldBe false
  }

  @Test
  internal fun `2-4 ensureNarrows reports a failed subclass check`() {
    shouldThrow<Exception> { klass("LandArea").ensureNarrows(klass("WaterArea"), fullWorld) }
    klass("Tharsis_2_2").ensureNarrows(klass("LandArea"), fullWorld)
  }

  @Test
  internal fun `2-4 the relation survives long chains and wide tables`() {
    // Nominal subtyping is compiled into bit masks; this crosses a machine-word boundary.
    val levels =
        (0 until 70).map { index ->
          if (index == 0) "ABSTRACT CLASS Level0"
          else "ABSTRACT CLASS Level$index : Level${index-1}"
        }
    val table =
        loadTypes(
            *(levels +
                    listOf(
                        "CLASS Leaf : Level69",
                        "CLASS Unrelated",
                        "ABSTRACT CLASS ChildlessAbstract",
                    ))
                .toTypedArray()
        )
    val leaf = table.getClass(cn("Leaf"))
    val childless = table.getClass(cn("ChildlessAbstract"))

    leaf.isSubtypeOf(leaf) shouldBe true
    listOf(0, 63, 64, 69).forEach {
      leaf.isSubtypeOf(table.getClass(cn("Level$it"))) shouldBe true
    }
    leaf.isSubtypeOf(table.getClass(cn("Unrelated"))) shouldBe false
    childless.isSubtypeOf(childless) shouldBe true
    leaf.isSubtypeOf(childless) shouldBe false
  }

  // 2-5 Cycles

  @Test
  internal fun `2-5 a supertype cycle is rejected`() {
    shouldThrow<PetException> {
      loadTypes("CLASS GreeneryTile : CityTile", "CLASS CityTile : GreeneryTile")
    }
    shouldThrow<PetException> { loadTypes("CLASS GreeneryTile : GreeneryTile") }
  }

  // 2-6 Declaration order

  @Test
  internal fun `2-6 a supertype may be declared after its subclass`() {
    val table = loadTypes("CLASS GreeneryTile : Tile", "ABSTRACT CLASS Tile")

    table.getClass(cn("GreeneryTile")).isSubtypeOf(table.getClass(cn("Tile"))) shouldBe true
  }

  // 2-7 Enumerating the hierarchy

  @Test
  internal fun `2-7 a class knows its supertypes and subtypes`() {
    klass("LandArea").allSuperclasses().map { "$it" } shouldContainExactly
        listOf("Component", "Area", "MarsArea", "LandArea")
    klass("LandArea").allSubclasses().map { "$it" } shouldContainExactly
        listOf("Tharsis_2_2", "VolcanicArea", "Tharsis_5_5", "LandArea")
    klass("LandArea").directSubclasses().map { "$it" } shouldContainExactly
        listOf("Tharsis_2_2", "VolcanicArea")
  }

  // 2-8 Greatest lower bound of two classes

  @Test
  internal fun `2-8 glb of comparable classes is the lower one`() {
    (klass("LandArea") glb klass("Area")) shouldBe klass("LandArea")
    (klass("Area") glb klass("LandArea")) shouldBe klass("LandArea")
    (klass("LandArea") glb klass("LandArea")) shouldBe klass("LandArea")
  }

  @Test
  internal fun `2-8 glb of incomparable classes is their unique greatest common subclass`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "CLASS GreeneryTile : OwnedTile",
        )

    (table.getClass(cn("Owned")) glb table.getClass(cn("Tile"))) shouldBe
        table.getClass(cn("OwnedTile"))
  }

  @Test
  internal fun `2-8 glb finds an operand inherited only indirectly`() {
    val table =
        loadTypes(
            """
            ABSTRACT CLASS Occupant
            ABSTRACT CLASS OwnedOccupant : Occupant, Owned
            ABSTRACT CLASS Tile : Occupant
            ABSTRACT CLASS OwnedTile : Tile, OwnedOccupant
            ABSTRACT CLASS Placeable
            ABSTRACT CLASS PlaceableOwnedTile : OwnedTile, Placeable
            """
                .trimIndent()
        )

    (table.getClass(cn("Owned")) glb table.getClass(cn("Tile"))) shouldBe
        table.getClass(cn("OwnedTile"))
  }

  @Test
  internal fun `2-8 glb is absent when no unique greatest common subclass exists`() {
    val disjoint = klass("LandArea") glb klass("WaterArea")
    disjoint shouldBe null

    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "ABSTRACT CLASS AlsoOwnedTile : Tile, Owned",
        )
    (table.getClass(cn("Owned")) glb table.getClass(cn("Tile"))) shouldBe null
  }

  // 2-9 Least upper bound of two classes

  @Test
  internal fun `2-9 lub of comparable classes is the upper one`() {
    (klass("LandArea") lub klass("Area")) shouldBe klass("Area")
    (klass("Area") lub klass("LandArea")) shouldBe klass("Area")
    (klass("LandArea") lub klass("LandArea")) shouldBe klass("LandArea")
  }

  @Test
  internal fun `2-9 lub of siblings is their nearest common superclass`() {
    (klass("LandArea") lub klass("WaterArea")) shouldBe klass("MarsArea")
    (klass("Tharsis_5_5") lub klass("Tharsis_1_1")) shouldBe klass("MarsArea")
  }

  @Test
  internal fun `2-9 lub falls back to Component`() {
    val table = loadTypes("CLASS GreeneryTile", "CLASS Plant")

    (table.getClass(cn("GreeneryTile")) lub table.getClass(cn("Plant"))) shouldBe
        table.componentClass
  }

  @Test
  internal fun `2-9 lub prefers a minimal candidate carrying more dependencies`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Area",
            "ABSTRACT CLASS Tile<Area>",
            "ABSTRACT CLASS GlobalParameter",
            "ABSTRACT CLASS OceanTile : Tile, GlobalParameter",
            "ABSTRACT CLASS PolarOceanTile : Tile, GlobalParameter",
        )

    (table.getClass(cn("OceanTile")) lub table.getClass(cn("PolarOceanTile"))) shouldBe
        table.getClass(cn("Tile"))
  }

  // 2-10 Intersection classes

  @Test
  internal fun `2-10 a class is an intersection class when nothing else combines its supertypes`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "CLASS GreeneryTile : OwnedTile",
        )

    table.getClass(cn("OwnedTile")).isIntersectionType() shouldBe true
    table.getClass(cn("Tile")).isIntersectionType() shouldBe false
    table.getClass(cn("GreeneryTile")).isIntersectionType() shouldBe false
  }

  @Test
  internal fun `2-10 a rival combination of the same supertypes breaks the intersection`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "CLASS CommercialDistrictTile : Tile, Owned",
        )

    table.getClass(cn("OwnedTile")).isIntersectionType() shouldBe false
  }

  // 2-11 Custom classes

  @Test
  internal fun `2-11 a Custom class must have a Kotlin implementation, and only a Custom class may`() {
    val declaration = "CLASS Neighbor : Custom"

    ClassLoader(testCatalog(declaration, setOf(object : CustomClass(cn("Neighbor")) {})))
        .loadEverything()
        .getClass(cn("Neighbor"))
        .declaration
        .custom shouldBe true

    // A declared-but-unimplemented Custom class is rejected by the Catalog lookup itself.
    shouldThrowIae { loadTypes(declaration) }
    shouldThrow<PetException> {
      ClassLoader(testCatalog("CLASS Neighbor", setOf(object : CustomClass(cn("Neighbor")) {})))
          .loadEverything()
    }
  }

  @Test
  internal fun `2-11 a root class rejects an unexpected implementation`() {
    shouldThrow<PetException> {
      ClassLoader(testCatalog("", setOf(object : CustomClass(COMPONENT) {})))
    }
  }

  @Test
  internal fun `2-11 a Custom class may not inherit Pets behavior`() {
    listOf(
            "ABSTRACT CLASS Behaving { Trigger: Result }\nCLASS Trigger, Result",
            "ABSTRACT CLASS Behaving { HAS MAX 1 This }",
            "ABSTRACT CLASS Behaving { DEFAULT +Behaving. }",
        )
        .forEach { parent ->
          val catalog =
              testCatalog(
                  "$parent\nCLASS Neighbor : Behaving, Custom",
                  setOf(object : CustomClass(cn("Neighbor")) {}),
              )
          val loader = ClassLoader(catalog)

          // Also proves a failed load is not cached as a success.
          repeat(2) {
            shouldThrow<PetException> { loader.load(cn("Neighbor")) }
            loader.findClass(cn("Neighbor")) shouldBe null
          }
        }
  }

  // 2-12 Class identity

  @Test
  internal fun `2-12 a class is identified by its name within its universe`() {
    val table = loadTypes("CLASS GreeneryTile")

    table.getClass(cn("GreeneryTile")) shouldBe table.getClass(cn("GreeneryTile"))
    "${table.getClass(cn("GreeneryTile"))}" shouldBe "GreeneryTile"
    table.componentClass.className shouldBe COMPONENT
  }
}
