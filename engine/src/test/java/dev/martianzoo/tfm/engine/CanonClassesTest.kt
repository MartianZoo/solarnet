package dev.martianzoo.tfm.engine

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.GameSetup
import dev.martianzoo.tfm.api.SpecialClassNames.ANYONE
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.ast.ClassName
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Expression.Companion.expression
import dev.martianzoo.tfm.pets.ast.classNames
import dev.martianzoo.tfm.types.MClass
import dev.martianzoo.tfm.types.MClassLoader
import dev.martianzoo.util.toStrings
import kotlin.Int.Companion.MAX_VALUE
import org.junit.jupiter.api.Test

/** Tests for the Canon data set. */
private class CanonClassesTest {
  val loader = MClassLoader(Canon).loadEverything()

  @Test
  fun redundantSuperclasses() {
    val redundancies =
        loader.allClasses.flatMap { mclass ->
          val direct: List<MClass> = mclass.directSuperclasses
          val indirect = direct.flatMap { it.properSuperclasses }.toSet()
          val redundant = direct.intersect(indirect)
          redundant.map { mclass.className to it.className }
        }
    assertThat(redundancies)
        .containsExactly(
            cn("GreeneryTile") to cn("Tile"),
            cn("SpecialTile") to cn("Tile"),
            cn("ActionUsedMarker") to cn("Owned"),
        )
  }

  @Test
  fun childlessAbstractClass() {
    val anomalies = loader.allClasses.filter { it.abstract && it.directSubclasses.none() }
    assertThat(anomalies).isEmpty()
  }

  @Test
  fun abstractClassWithOnlyChild() {
    // In some cases we might like the parent and child to be treated as the same class
    val anomalies = loader.allClasses.filter { it.abstract && it.directSubclasses.size == 1 }
    assertThat(anomalies.classNames())
        .containsExactly(ANYONE, cn("NoctisArea"), cn("Barrier"), cn("Generational"))
  }

  @Test
  fun onlyCapitalTileExtendsConcrete() {
    val map = mutableListOf<Pair<ClassName, ClassName>>()
    loader.allClasses
        .filterNot { it.abstract }
        .forEach { sup ->
          (sup.allSubclasses - setOf(sup)).forEach { map += sup.className to it.className }
        }
    assertThat(map).containsExactly(cn("CityTile") to cn("Tile008"))
  }

  @Test
  fun testOwnedTileIsAnIntersectionType() {
    val owned = loader.getClass(cn("Owned"))
    val tile = loader.getClass(cn("Tile"))
    val ownedTile = loader.getClass(cn("OwnedTile"))

    // Nothing can be both Owned and a Tile without being an OwnedTile!
    assertThat(owned glb tile).isEqualTo(ownedTile)
    assertThat(ownedTile.intersectionType).isTrue()
  }

  @Test
  fun testActionCardIsAnIntersectionType() {
    val cardFront = loader.getClass(cn("CardFront"))
    val hasActions = loader.getClass(cn("HasActions"))
    val actionCard = loader.getClass(cn("ActionCard"))

    // Nothing can be both a CardFront and a HasActions but an ActionCard!
    assertThat(cardFront glb hasActions).isEqualTo(actionCard)
    assertThat(actionCard.intersectionType).isTrue()
  }

  @Test
  fun component() {
    val table = MClassLoader(Canon)

    table.componentClass.apply {
      assertThat(className).isEqualTo(COMPONENT)
      assertThat(abstract).isTrue()
      // assertThat(directDependencyKeys).isEmpty()
      // assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }

    table.load(cn("OceanTile")).apply {
      // assertThat(directDependencyKeys).isEmpty()
      // assertThat(allDependencyKeys).containsExactly(Key(cn("Tile"), 0))
      assertThat(directSuperclasses.classNames())
          .containsExactly(cn("GlobalParameter"), cn("Tile"))
          .inOrder()
      assertThat(allSuperclasses.classNames())
          .containsExactly(cn("Component"), cn("Atomized"), cn("GlobalParameter"), cn("Tile"), cn("OceanTile"))
          .inOrder()

      table.load(cn("MarsArea"))
      assertThat(baseType).isEqualTo(table.resolve(expression("OceanTile<MarsArea>")))
    }
  }

  @Test
  fun testAllConcreteSubtypes() {
    val loader = Engine.loadClasses(GameSetup(Canon, "BRM", 2))

    fun checkConcreteSubtypeCount(expr: String, size: Int) {
      val mtype = loader.resolve(expression(expr))
      assertThat(mtype.allConcreteSubtypes().toList()).hasSize(size)
    }

    checkConcreteSubtypeCount("Plant<Player1>", 1)
    checkConcreteSubtypeCount("Plant", 2)
    checkConcreteSubtypeCount("StandardResource<Player1>", 6)
    checkConcreteSubtypeCount("StandardResource", 12)
    checkConcreteSubtypeCount("Class<StandardResource>", 6)

    checkConcreteSubtypeCount("Class<MarsArea>", 61)
    checkConcreteSubtypeCount("Class<RemoteArea>", 2)
    checkConcreteSubtypeCount("Class<Tile>", 12)
    checkConcreteSubtypeCount("Class<SpecialTile>", 9)

    checkConcreteSubtypeCount("CityTile", (63 + 61) * 2)
    checkConcreteSubtypeCount("OceanTile", 61)
    checkConcreteSubtypeCount("GreeneryTile", 61 * 2)
    checkConcreteSubtypeCount("SpecialTile", (9 * 61) * 2)

    // Do this one the long way because the error message is horrific
    val type = loader.resolve(expression("Tile"))
    assertThat(type.allConcreteSubtypes().count()).isEqualTo(1407)
  }

  @Test
  fun classInvariants() {
    val table = MClassLoader(Canon).loadEverything()

    val temp = table.getClass(cn("TemperatureStep"))
    assertThat(temp.typeInvariants.toStrings()).containsExactly("MAX 19 This")

    val ocean = table.getClass(cn("OceanTile"))
    assertThat(ocean.typeInvariants.toStrings()).containsExactly("MAX 1 This")

    val area = table.getClass(cn("Area"))
    assertThat(area.typeInvariants.toStrings()).containsExactly("=1 This", "MAX 1 Tile<This>")
  }

  @Test
  fun typeLimits() {
    val table = MClassLoader(Canon).loadEverything()

    fun assertRange(name: String, intRange: IntRange) =
        assertThat(table.getClass(cn(name)).componentCountRange).isEqualTo(intRange)

    assertRange("TemperatureStep", 0..19)
    assertRange("VenusStep", 0..15)
    assertRange("OxygenStep", 0..14)

    assertRange("Tile", 0..1)
    assertRange("OceanTile", 0..1)
    assertRange("CardFront", 0..1)
    assertRange("Ants", 0..1)
    assertRange("ActionUsedMarker", 0..1)
    assertRange("Tag", 0..2)

    assertRange("Die", 0..0)

    assertRange("Production", 0..MAX_VALUE)
    assertRange("Resource", 0..MAX_VALUE)
    assertRange("Generation", 1..MAX_VALUE)

    assertRange("Area", 1..1)
    assertRange("Tharsis_5_5", 1..1)
  }

  @Test
  fun generalInvariants() {
    val table = MClassLoader(Canon).loadEverything()
    assertThat(table.generalInvariants.toStrings()).containsExactly(
        "MAX 1 Phase",
        "MAX 9 OceanTile",
    )
  }
}
