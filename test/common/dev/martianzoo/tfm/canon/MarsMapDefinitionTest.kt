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
      //      VS    L
      //  V2P   WPP   WPC
      //     LSS    LC
      //

      CLASS Demo_1_1 : VolcanicArea { row = 1; column = 1; Tile<This>: Steel }
      """
          .trimIndent()

  @Test
  internal fun readsMapDefinitionFromPetsComment() {
    val map: MarsMapDefinition = MarsMapReader.readMaps(demoMapPets).single()
    map.className shouldBe cn("DemoMap")
    map.areas.shouldHaveSize(7)
    map.areas[1, 1]!!.code shouldBe "VS"
    map.areas[1, 1]!!.bonusText shouldBe "Steel"
    map.areas[1, 1]!!.className shouldBe cn("Demo_1_1")
    map.areas[1, 1]!!.asClassDeclaration.properties shouldBe
        mapOf(PropertyName("row") to NumberValue(1), PropertyName("column") to NumberValue(1))
  }
}
