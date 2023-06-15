package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.api.SystemClasses.ANYONE
import dev.martianzoo.api.SystemClasses.COMPONENT
import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.classDeclaration
import dev.martianzoo.tfm.data.GameSetup
import dev.martianzoo.tfm.types.te
import dev.martianzoo.types.MClassLoader
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
internal class CanonClassesTest {
  companion object {
    val table = MClassLoader(Canon).loadEverything()
  }

  @Test
  fun docstrings() {
    assertThat(classDeclaration(cn("VictoryPoint")).docstring)
        .isEqualTo("Well it's a victory point")
    assertThat(classDeclaration(cn("Floater")).docstring)
        .isEqualTo("A particular kind of CardResource")
  }

  @Test
  fun childlessAbstractClass() {
    val anomalies = table.allClasses.filter { it.abstract && it.getDirectSubclasses().none() }
    assertThat(anomalies).isEmpty()
  }

  @Test
  fun abstractClassWithOnlyChild() {
    // In some cases we might like the parent and child to be treated as the same class
    val anomalies = table.allClasses.filter { it.abstract && it.getDirectSubclasses().size == 1 }
    assertThat(anomalies.classNames()).containsExactly(ANYONE, cn("NoctisArea"), cn("Barrier"))
  }

  @Test
  fun concreteExtendingConcrete() {
    val map = mutableListOf<Pair<ClassName, ClassName>>()
    table.allClasses
        .filterNot { it.abstract }
        .forEach { sup ->
          (sup.getAllSubclasses() - setOf(sup)).forEach { map += sup.className to it.className }
        }
    assertThat(map).containsExactly() // cn("CityTile") to cn("CapitalTile"))
  }

  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val owned = table.getClass(cn("Owned"))
    val tile = table.getClass(cn("Tile"))
    val ownedTile = table.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(owned glb tile).isEqualTo(ownedTile)
    assertThat(ownedTile.isIntersectionType()).isTrue()
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val cardFront = table.getClass(cn("CardFront"))
    val hasActions = table.getClass(cn("HasActions"))
    val actionCard = table.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(cardFront glb hasActions).isEqualTo(actionCard)
    assertThat(actionCard.isIntersectionType()).isTrue()
  }

  @Test
  fun component() {
    val loader = MClassLoader(Canon)

    loader.componentClass.apply {
      assertThat(className).isEqualTo(COMPONENT)
      assertThat(abstract).isTrue()
      // assertThat(directDependencyKeys).isEmpty()
      // assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }

    loader.load(cn("OceanTile")).apply {
      // assertThat(directDependencyKeys).isEmpty()
      // assertThat(allDependencyKeys).containsExactly(Key(cn("Tile"), 0))
      assertThat(directSuperclasses.classNames()).containsExactly(cn("GlobalParameter"), cn("Tile"))
      assertThat(getAllSuperclasses().classNames())
          .containsExactly(
              cn("Component"), cn("Atomized"), cn("GlobalParameter"), cn("Tile"), cn("OceanTile"))

      loader.load(cn("MarsArea"))
      assertThat(baseType).isEqualTo(loader.resolve(te("OceanTile<MarsArea>")))
    }
  }

  @Test
  fun testAllConcreteSubtypes() {
    val table = MClassLoader(GameSetup(Canon, "BRM", 2))

    fun checkConcreteSubtypeCount(expr: String, size: Int) {
      val mtype = table.resolve(te(expr))
      assertThat(mtype.allConcreteSubtypes().toList()).hasSize(size)
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
    assertThat(type.allConcreteSubtypes().count()).isEqualTo(1285) // ?
  }
}
