package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.CustomInstruction
import dev.martianzoo.pets.api.CustomMetric
import dev.martianzoo.pets.api.Exceptions.InvalidPetDefinitionException
import dev.martianzoo.pets.api.GameReader
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.createClassLoader
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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

  // T2-1 Declaration

  @Test
  internal fun `T2-1 a declaration introduces a class and its base type`() {
    val table = loadTypes("ABSTRACT CLASS Tile", "CLASS GreeneryTile : Tile")

    table.getClass(cn("GreeneryTile")).abstract shouldBe false
    table.getClass(cn("Tile")).abstract shouldBe true
    table.getClass(cn("GreeneryTile")).baseType.expressionFull shouldBe te("GreeneryTile")
  }

  // T2-2 Direct supertypes

  @Test
  internal fun `T2-2 a class with no declared supertype extends Component`() {
    val table = loadTypes("CLASS GreeneryTile")

    table.getClass(cn("GreeneryTile")).directSuperclasses shouldBe listOf(table.componentClass)
  }

  @Test
  internal fun `T2-2 naming Component as a supertype is an error`() {
    shouldThrow<InvalidPetDefinitionException> { loadTypes("CLASS GreeneryTile : Component") }
  }

  @Test
  internal fun `T2-2 nesting is shorthand for naming the enclosing class as a supertype`() {
    klass("LandArea").directSuperclasses shouldBe listOf(klass("MarsArea"))
    klass("Tharsis_2_2").directSuperclasses shouldBe listOf(klass("LandArea"))
  }

  @Test
  internal fun `T2-2 a class may have several abstract direct supertypes`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Occupant",
            "ABSTRACT CLASS Tile : Occupant",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
        )

    table.getClass(cn("OwnedTile")).directSuperclasses.map { "$it" } shouldContainExactly
        listOf("Tile", "Owned")
  }

  // T2-3 Concrete classes are final

  @Test
  internal fun `T2-3 no class may extend a concrete class`() {
    shouldThrow<InvalidPetDefinitionException> {
      loadTypes("CLASS GreeneryTile", "CLASS SpecialTile : GreeneryTile")
    }
    shouldThrow<InvalidPetDefinitionException> {
      loadTypes("CLASS GreeneryTile", "ABSTRACT CLASS SpecialTile : GreeneryTile")
    }
  }

  @Test
  internal fun `T2-3 a concrete class has itself as its only subclass`() {
    mars.allSubclasses(klass("Tharsis_2_2")) shouldBe setOf(klass("Tharsis_2_2"))
    mars.directSubclasses(klass("Tharsis_2_2")).shouldBeEmpty()
  }

  // T2-4 The subclass relation

  @Test
  internal fun `T2-4 the subclass relation is reflexive`() {
    mars.allClasses().forEach { it.isSubtypeOf(it) shouldBe true }
  }

  @Test
  internal fun `T2-4 the subclass relation is transitive`() {
    klass("Tharsis_5_5").isSubtypeOf(klass("VolcanicArea")) shouldBe true
    klass("VolcanicArea").isSubtypeOf(klass("LandArea")) shouldBe true
    klass("Tharsis_5_5").isSubtypeOf(klass("LandArea")) shouldBe true
    klass("Tharsis_5_5").isSubtypeOf(mars.componentClass) shouldBe true
  }

  @Test
  internal fun `T2-4 unrelated branches are not subclasses of each other`() {
    klass("LandArea").isSubtypeOf(klass("WaterArea")) shouldBe false
    klass("WaterArea").isSubtypeOf(klass("LandArea")) shouldBe false
    klass("Area").isSubtypeOf(klass("LandArea")) shouldBe false
  }

  @Test
  internal fun `T2-4 the relation survives long chains and wide tables`() {
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

  // T2-5 Cycles

  @Test
  internal fun `T2-5 a supertype cycle is rejected`() {
    shouldThrow<InvalidPetDefinitionException> {
      loadTypes("ABSTRACT CLASS Area : MarsArea", "ABSTRACT CLASS MarsArea : Area")
    }
    shouldThrow<InvalidPetDefinitionException> { loadTypes("ABSTRACT CLASS Area : Area") }
  }

  // T2-6 Declaration order

  @Test
  internal fun `T2-6 a supertype may be declared after its subclass`() {
    val table = loadTypes("CLASS GreeneryTile : Tile", "ABSTRACT CLASS Tile")

    table.getClass(cn("GreeneryTile")).isSubtypeOf(table.getClass(cn("Tile"))) shouldBe true
  }

  // T2-7 Superclasses are intrinsic; subclasses belong to the universe

  @Test
  internal fun `T2-7 superclass traversal is intrinsic and subclass traversal is table-relative`() {
    klass("LandArea").allSuperclasses().map { "$it" } shouldContainExactlyInAnyOrder
        listOf("Component", "Area", "MarsArea", "LandArea")
    mars.allSubclasses(klass("LandArea")).map { "$it" } shouldContainExactlyInAnyOrder
        listOf("Tharsis_2_2", "VolcanicArea", "Tharsis_5_5", "LandArea")
    mars.directSubclasses(klass("LandArea")).map { "$it" } shouldContainExactlyInAnyOrder
        listOf("Tharsis_2_2", "VolcanicArea")
  }

  // T2-8 Greatest lower bound of two classes

  @Test
  internal fun `T2-8 glb of comparable classes is the lower one`() {
    mars.glb(klass("LandArea"), klass("Area")) shouldBe klass("LandArea")
    mars.glb(klass("Area"), klass("LandArea")) shouldBe klass("LandArea")
    mars.glb(klass("LandArea"), klass("LandArea")) shouldBe klass("LandArea")
  }

  @Test
  internal fun `T2-8 glb of incomparable classes is their unique greatest common subclass`() {
    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "CLASS GreeneryTile : OwnedTile",
        )

    table.glb(table.getClass(cn("Owned")), table.getClass(cn("Tile"))) shouldBe
        table.getClass(cn("OwnedTile"))
  }

  @Test
  internal fun `T2-8 glb finds an operand inherited only indirectly`() {
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

    table.glb(table.getClass(cn("Owned")), table.getClass(cn("Tile"))) shouldBe
        table.getClass(cn("OwnedTile"))
  }

  @Test
  internal fun `T2-8 glb is absent when no unique greatest common subclass exists`() {
    val disjoint = mars.glb(klass("LandArea"), klass("WaterArea"))
    disjoint shouldBe null

    val table =
        loadTypes(
            "ABSTRACT CLASS Tile",
            "ABSTRACT CLASS OwnedTile : Tile, Owned",
            "ABSTRACT CLASS AlsoOwnedTile : Tile, Owned",
        )
    table.glb(table.getClass(cn("Owned")), table.getClass(cn("Tile"))) shouldBe null
  }

  // T2-9 Kotlin-backed classes

  @Test
  internal fun `T2-9 a CustomInstruction must have a Kotlin implementation, and only a CustomInstruction may`() {
    val declaration = "CLASS Neighbor : CustomInstruction"

    createClassLoader(
            testCatalog(declaration, setOf(object : CustomInstruction(cn("Neighbor")) {}))
        )
        .loadEverything()
        .getClass(cn("Neighbor"))
        .declaration
        .customMetric shouldBe false

    // A declared-but-unimplemented CustomInstruction is rejected by the Catalog lookup itself.
    shouldThrow<InvalidPetDefinitionException> { loadTypes(declaration) }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(
              testCatalog("CLASS Neighbor", setOf(object : CustomInstruction(cn("Neighbor")) {}))
          )
          .loadEverything()
    }
    val wrongKind =
        object : CustomMetric("Neighbor") {
          override fun count(game: GameReader, type: Type): Int = 0
        }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog(declaration, setOf(wrongKind))).loadEverything()
    }
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(
              testCatalog(
                  "CLASS Neighbor : CustomMetric",
                  setOf(object : CustomInstruction("Neighbor") {}),
              )
          )
          .loadEverything()
    }
  }

  @Test
  internal fun `T2-9 a root class rejects an unexpected implementation`() {
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog("", setOf(object : CustomInstruction(COMPONENT) {})))
    }
  }

  @Test
  internal fun `T2-9 a computed Signal has one implementation`() {
    val first = object : CustomInstruction("Neighbor") {}
    val second = object : CustomInstruction("Neighbor") {}
    shouldThrow<InvalidPetDefinitionException> {
      createClassLoader(testCatalog("CLASS Neighbor : CustomInstruction", setOf(first, second)))
          .loadEverything()
    }
  }

  @Test
  internal fun `T2-9 a CustomMetric may not inherit Pets behavior`() {
    listOf(
            "ABSTRACT CLASS Behaving { Trigger: Result }\nCLASS Trigger\nCLASS Result",
            "ABSTRACT CLASS Behaving { HAS MAX 1 This }",
            "ABSTRACT CLASS Behaving { DEFAULT +Behaving. }",
        )
        .forEach { parent ->
          val catalog =
              testCatalog(
                  "$parent\nCLASS Neighbor : Behaving, CustomMetric",
                  setOf(
                      object : CustomMetric("Neighbor") {
                        override fun count(game: GameReader, type: Type): Int = 0
                      }
                  ),
              )
          val loader = createClassLoader(catalog)

          // Also proves a failed load is not cached as a success.
          repeat(2) {
            shouldThrow<InvalidPetDefinitionException> { loader.load(cn("Neighbor")) }
            loader.findClass(cn("Neighbor")) shouldBe null
          }
        }
  }
}
