package dev.martianzoo.pets.types

import dev.martianzoo.pets.api.Exceptions.ExpressionException
import dev.martianzoo.pets.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.api.TypeInfo.NoGameState
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

/** Section 1 of `docs/type-system-spec.md`: universes and identity. */
internal class Spec01UniversesTest {

  // T1-1 One universe per Catalog

  @Test
  internal fun `T1-1 a catalog compiles to one class per declared name`() {
    val table = loadTypes("ABSTRACT CLASS Tile", "CLASS GreeneryTile : Tile")

    table.getClass(cn("GreeneryTile")) shouldBe table.getClass(cn("GreeneryTile"))
    (table.getClass(cn("GreeneryTile")) === table.getClass(cn("GreeneryTile"))) shouldBe true
  }

  @Test
  internal fun `T1-1 two compilations of identical source are different universes`() {
    fun universe() = loadTypes("ABSTRACT CLASS Tile", "CLASS GreeneryTile : Tile")

    val left = universe()
    val right = universe()

    left.getClass(cn("GreeneryTile")) shouldNotBe right.getClass(cn("GreeneryTile"))
    left.resolve(te("GreeneryTile")) shouldNotBe right.resolve(te("GreeneryTile"))
  }

  // T1-2 Values are universe-scoped

  @Test
  internal fun `T1-2 comparing values from two universes is an error, not a false answer`() {
    fun universe() = loadTypes("ABSTRACT CLASS Area", "CLASS GreeneryTile<Area>")

    val left = universe()
    val right = universe()
    val leftArea = left.getClass(cn("Area"))
    val rightArea = right.getClass(cn("Area"))
    val leftTile = left.resolve(te("GreeneryTile"))
    val rightTile = right.resolve(te("GreeneryTile"))

    shouldThrowIae { leftArea.isSubtypeOf(rightArea) }
    shouldThrowIae { leftArea lub rightArea }
    shouldThrowIae { leftTile.isSubtypeOf(rightTile) }
    shouldThrowIae { leftTile glb rightTile }
    shouldThrowIae { leftTile.narrows(rightTile, NoGameState) }
    shouldThrowIae { left.getClass(cn("GreeneryTile")).withAllDependencies(rightTile.dependencies) }
    shouldThrowIae { left.allSubclasses(rightArea) }
    shouldThrowIae { left.matchesConstraint(leftTile, te("Area"), rightTile, NoGameState) }
  }

  @Test
  internal fun `T1-2 knows reports whether a type belongs to this universe`() {
    fun universe() = loadTypes("ABSTRACT CLASS Area", "CLASS GreeneryTile<Area>")

    val left = universe()
    val right = universe()

    left.knows(left.resolve(te("GreeneryTile"))) shouldBe true
    left.knows(right.resolve(te("GreeneryTile"))) shouldBe false
  }

  // T1-3 Resolution is a function of the expression

  @Test
  internal fun `T1-3 one expression always resolves to the identical type object`() {
    val table = loadTypes("ABSTRACT CLASS Area", "CLASS GreeneryTile<Area>")

    (table.resolve(te("GreeneryTile")) === table.resolve(te("GreeneryTile"))) shouldBe true
  }

  @Test
  internal fun `T1-3 different spellings of one type are equal but need not be identical`() {
    val table = loadTypes("ABSTRACT CLASS Area", "CLASS GreeneryTile<Area>")

    table.resolve(te("GreeneryTile<Area>")) shouldBe table.resolve(te("GreeneryTile"))
    (table.resolve(te("GreeneryTile<Area>")) === table.resolve(te("GreeneryTile"))) shouldBe false
  }

  @Test
  internal fun `T1-3 a type's own renderings resolve back to it`() {
    val table = loadTypes("ABSTRACT CLASS Area { CLASS Tharsis_2_2 }", "CLASS GreeneryTile<Area>")
    val tile = table.resolve(te("GreeneryTile<Tharsis_2_2>"))

    table.resolve(tile.expression) shouldBe tile
    table.resolve(tile.expressionFull) shouldBe tile
  }

  // T1-4, T1-5 The two required classes

  @Test
  internal fun `T1-4 Component is the abstract root of every universe`() {
    val table = loadTypes()
    val component = table.componentClass

    component.className shouldBe COMPONENT
    component.abstract shouldBe true
    component.directSuperclasses.shouldBeEmpty()
    component.allSuperclasses() shouldBe setOf(component)
    component.dependencies.keys.shouldBeEmpty()
    component.baseType.expressionFull shouldBe te("Component")
  }

  @Test
  internal fun `T1-4 every class has Component as a supertype`() {
    val table = loadTypes("ABSTRACT CLASS Tile", "CLASS GreeneryTile : Tile")

    table.allClasses().forEach { it.isSubtypeOf(table.componentClass) shouldBe true }
  }

  @Test
  internal fun `T1-5 the Class class exists and is bounded by Component`() {
    val table = loadTypes()

    table.classClass.className shouldBe cn("Class")
    table.classClass.baseType.expressionFull shouldBe te("Class<Component>")
  }

  // T1-6 Freezing

  @Test
  internal fun `T1-6 enumeration requires a frozen table but lookup does not`() {
    val catalog = testCatalog("CLASS GreeneryTile")
    val loader = ClassLoader(catalog)

    loader.findClass(cn("GreeneryTile")) shouldBe null
    shouldThrowIae { loader.allClasses() }

    val tile = loader.load(cn("GreeneryTile"))
    loader.findClass(cn("GreeneryTile")) shouldBe tile

    val table = loader.freeze()
    table.getClass(cn("GreeneryTile")) shouldBe tile
    table.allClassNames shouldBe catalog.allClassNames
  }

  // T1-7 Canonical names only

  @Test
  internal fun `T1-7 only the exact declared name resolves`() {
    val table = loadTypes("CLASS GreeneryTile")

    table.getClass(cn("GreeneryTile")).className shouldBe cn("GreeneryTile")
    table.findClass(cn("Greenery")) shouldBe null
    shouldThrow<ExpressionException> { table.getClass(cn("Greenery")) }
    shouldThrow<ExpressionException> { table.resolve(te("Greenery")) }
  }
}
