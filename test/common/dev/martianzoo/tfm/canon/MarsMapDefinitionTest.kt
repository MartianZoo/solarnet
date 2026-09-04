package dev.martianzoo.tfm.canon

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MarsMapDefinitionTest {

  private val demoMapPets =
      """
      CLASS DemoMap : MarsMap

      // The map areas below are code-generated based on the following comment
      //
      //                  V    LPP
      //   W    LSS    LC    L
      //                  N
      //   L    W3H
      //

      CLASS Demo_1_1 : VolcanicArea { row = 1; column = 1; Tile<This>: Steel }
      """
          .trimIndent()

  @Test
  internal fun readsMapDefinitionFromPetsComment() {
    val map: MarsMapDefinition = MarsMapReader.readMaps(demoMapPets).single()
    map.className shouldBe cn("DemoMap")
    map.areas.shouldHaveSize(9)
    map.areas.map { it.row to it.column } shouldBe
        listOf(
            1 to 3,
            1 to 4,
            2 to 1,
            2 to 2,
            2 to 3,
            2 to 4,
            3 to 4,
            4 to 2,
            4 to 3,
        )
    map.areas[2, 2]!!.code shouldBe "LSS"
    map.areas[2, 2]!!.bonusText shouldBe "2 Steel"
    map.areas[2, 2]!!.className shouldBe cn("Demo_2_2")
    map.areas[2, 2]!!.asClassDeclaration.properties shouldBe
        mapOf(PropertyName("row") to NumberValue(2), PropertyName("column") to NumberValue(2))
  }
}
