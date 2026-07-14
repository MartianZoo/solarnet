package dev.martianzoo.tfm.engine

import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.classDeclaration
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.types.MClassLoader
import dev.martianzoo.types.te
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    val table = MClassLoader(Canon).loadEverything()
  }

  @Test
  fun docstrings() {
    classDeclaration(cn("VictoryPoint")).docstring shouldBe "Well it's a victory point"
    classDeclaration(cn("Floater")).docstring shouldBe "A particular kind of CardResource"
  }

  @Test
  fun childlessAbstractClass() {
    val anomalies = table.allClasses().filter { it.abstract && it.directSubclasses().none() }
    anomalies.shouldBeEmpty()
  }

  @Test
  fun abstractClassWithOnlyChild() {
    // In some cases we might like the parent and child to be treated as the same class
    val anomalies = table.allClasses().filter { it.abstract && it.directSubclasses().size == 1 }
    anomalies
        .classNames()
        .shouldContainExactlyInAnyOrder(ANYONE, cn("Owner"), cn("NoctisArea"), cn("Barrier"))
  }

  @Test
  fun concreteExtendingConcrete() {
    val map = mutableListOf<Pair<ClassName, ClassName>>()
    table
        .allClasses()
        .filterNot { it.abstract }
        .forEach { sup ->
          (sup.allSubclasses() - setOf(sup)).forEach { map += sup.className to it.className }
        }
    map.shouldBeEmpty()
  }

  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val owned = table.getClass(cn("Owned"))
    val tile = table.getClass(cn("Tile"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    owned glb tile shouldBe ownedTile
    ownedTile.isIntersectionType() shouldBe true
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val cardFront = table.getClass(cn("CardFront"))
    val hasActions = table.getClass(cn("HasActions"))
    val actionCard = table.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    cardFront glb hasActions shouldBe actionCard
    actionCard.isIntersectionType() shouldBe true
  }

  @Test
  fun component() {
    val loader = MClassLoader(Canon)

    with(loader.componentClass) {
      className shouldBe COMPONENT
      abstract shouldBe true
      // directDependencyKeys.shouldBeEmpty()
      // allDependencyKeys.shouldBeEmpty()
      directSuperclasses.shouldBeEmpty()
    }

    with(loader.load(cn("OceanTile"))) {
      // directDependencyKeys.shouldBeEmpty()
      // allDependencyKeys.shouldContainExactlyInAnyOrder(Key(cn("Tile"), 0))
      directSuperclasses
          .classNames()
          .shouldContainExactlyInAnyOrder(cn("GlobalParameter"), cn("Tile"))
      allSuperclasses()
          .classNames()
          .shouldContainExactlyInAnyOrder(
              cn("Component"),
              cn("Atomized"),
              cn("GlobalParameter"),
              cn("Tile"),
              cn("OceanTile"),
          )

      loader.load(cn("MarsArea"))
      baseType shouldBe loader.resolve(te("OceanTile<MarsArea>"))
    }
  }

  @Test
  fun testAllConcreteSubtypes() {
    val table = MClassLoader(GameSetup(Canon, "BRM", 2))

    fun checkConcreteSubtypeCount(expr: String, size: Int) {
      val mtype = table.resolve(te(expr))
      mtype.allConcreteSubtypes().toList().shouldHaveSize(size)
    }

    checkConcreteSubtypeCount("Plant<Player1>", 1)
    checkConcreteSubtypeCount("Plant", 2)
    checkConcreteSubtypeCount("StandardResource<Player1>", 6)
    checkConcreteSubtypeCount("StandardResource", 12)
    checkConcreteSubtypeCount("Class<StandardResource>", 6)

    checkConcreteSubtypeCount("Class<MarsArea>", 61)
    checkConcreteSubtypeCount("Class<RemoteArea>", 2)
    checkConcreteSubtypeCount("Class<Tile>", 11)
    checkConcreteSubtypeCount("Class<SpecialTile>", 8)

    checkConcreteSubtypeCount("CityTile", 63 * 2)
    checkConcreteSubtypeCount("OceanTile", 61)
    checkConcreteSubtypeCount("GreeneryTile", 61 * 2)
    checkConcreteSubtypeCount("SpecialTile", (8 * 61) * 2)

    // Do this one the long way because the error message is horrific
    val type = table.resolve(te("Tile"))
    type.allConcreteSubtypes().count() shouldBe 1285 // ?
  }
}
