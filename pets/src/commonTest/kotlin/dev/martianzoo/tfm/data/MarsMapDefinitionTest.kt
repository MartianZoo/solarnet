package dev.martianzoo.tfm.data

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MarsMapDefinitionTest {

  private val demoMapJson =
      """
        {
          "legend": {
            "L": "LandArea", "W": "WaterArea", "V": "VolcanicArea",
            "P": "Plant", "S": "Steel", "C": "ProjectCard",
          },
          "maps": [{
            "name": "DemoMap",
            "areaPrefix": "Demo",
            "defaultMilestones": "DemoMilestone",
            "defaultAwards": "DemoAward",
            "rows": [
              [ " V S ", "L" ],
              [ "V2P", "WPP", "WPC" ],
              [ "   ", "LSS", "LC" ],
            ]
          }]
        }"""

  @Test
  internal fun testDemoMapFromJson() {
    val map: MarsMapDefinition = JsonReader.readMaps(demoMapJson).single()
    map.className shouldBe cn("DemoMap")
    map.defaultMilestones shouldBe cn("DemoMilestone")
    map.defaultAwards shouldBe cn("DemoAward")
    map.asClassDeclaration.effects shouldBe emptyList()
    map.asClassDeclaration.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("MarsMap"))
    map.areas.shouldHaveSize(7)
    map.areas[1, 1]!!.code shouldBe "VS"
    map.areas[1, 1]!!.bonusText shouldBe "Steel"
    map.areas[1, 1]!!.className shouldBe cn("Demo_1_1")
    map.areas[1, 1]!!.asClassDeclaration.properties shouldBe
        mapOf(PropertyName("row") to NumberValue(1), PropertyName("column") to NumberValue(1))
  }
}
