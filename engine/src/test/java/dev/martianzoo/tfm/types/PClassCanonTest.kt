package dev.martianzoo.tfm.types

import com.google.common.truth.Truth.assertThat
import dev.martianzoo.tfm.api.SpecialClassNames.COMPONENT
import dev.martianzoo.tfm.api.SpecialClassNames.THIS
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.pets.AstTransforms.replaceAll
import dev.martianzoo.tfm.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.pets.ast.Requirement.Companion.requirement
import dev.martianzoo.tfm.pets.ast.TypeExpr.Companion.typeExpr
import dev.martianzoo.tfm.types.Dependency.Key
import dev.martianzoo.util.toStrings
import org.junit.jupiter.api.Test

/** Tests of [PClass] that use the [Canon] dataset because it's convenient. */
private class PClassCanonTest {

  @Test
  fun component() {
    val table = PClassLoader(Canon)

    table.componentClass.apply {
      assertThat(className).isEqualTo(COMPONENT)
      assertThat(abstract).isTrue()
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).isEmpty()
      assertThat(directSuperclasses).isEmpty()
    }

    table.load(cn("OceanTile")).apply {
      assertThat(directDependencyKeys).isEmpty()
      assertThat(allDependencyKeys).containsExactly(Key(cn("Tile"), 0))
      assertThat(directSuperclasses.toStrings())
          .containsExactly("GlobalParameter", "Tile")
          .inOrder()
      assertThat(allSuperclasses.toStrings())
          .containsExactly("Component", "GlobalParameter", "Tile", "OceanTile")
          .inOrder()

      table.load(cn("MarsArea"))
      assertThat(baseType).isEqualTo(table.resolveType(typeExpr("OceanTile<MarsArea>")))
    }
  }

  @Test
  fun canGetBaseTypes() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.baseType }
  }

  @Test
  fun getClassEffects() {
    val table = PClassLoader(Canon).loadEverything()
    table.allClasses.forEach { it.classEffects }
  }

  @Test
  fun classInvariants() {
    val table = PClassLoader(Canon).loadEverything()
    val temp = table.getClass(cn("TemperatureStep"))
    assertThat(temp.invariants).containsExactly(requirement("MAX 19 This"))
    val ocean = table.getClass(cn("OceanTile"))
    assertThat(ocean.invariants).containsExactly(requirement("MAX 9 OceanTile"))
    val area = table.getClass(cn("Area"))
    assertThat(area.invariants)
        .containsExactly(
            requirement("=1 This"),
            requirement("MAX 1 Tile<This>"),
        )
  }

  @Test
  fun checkTypes() {
    val table = PClassLoader(Canon).loadEverything()

    table.allClasses.forEach { pclass ->
      pclass.classEffects.forEach {
        table.checkAllTypes(it.replaceAll(THIS.type, pclass.className.type))
      }
    }
  }
}
