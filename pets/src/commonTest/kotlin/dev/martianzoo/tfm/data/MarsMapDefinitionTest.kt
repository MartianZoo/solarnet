package dev.martianzoo.tfm.data

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.Parsing.parse
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.ast.Effect
import dev.martianzoo.pets.ast.PropertyName
import dev.martianzoo.pets.ast.PropertyValue.NumberValue
import io.kotest.matchers.collections.shouldContainExactly
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
            "defaultMilestones": "DemoDefaultMilestones",
            "defaultAwards": "DemoDefaultAwards",
            "rows": [
              [ " V S ", "L" ],
              [ "V2P", "WPP", "WPC" ],
              [ "   ", "LSS", "LC" ],
            ]
          }]
        }"""

  @Test
  fun testDemoMapFromJson() {
    val map: MarsMapDefinition = JsonReader.readMaps(demoMapJson).single()
    map.className shouldBe cn("DemoMap")
    map.defaultMilestones shouldBe cn("DemoDefaultMilestones")
    map.defaultAwards shouldBe cn("DemoDefaultAwards")
    map.asClassDeclaration.effects.shouldContainExactly(
        parse<Effect>(
            "This IF MAX 0 Milestone, MAX 0 DemoDefaultMilestones:: DemoDefaultMilestones."
        ),
        parse<Effect>(
            "This IF MultiplayerMode, MAX 0 Award, MAX 0 DemoDefaultAwards:: DemoDefaultAwards."
        ),
    )
    map.asClassDeclaration.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("MarsMap"))
    map.areas.shouldHaveSize(7)
    map.areas[1, 1]!!.code shouldBe "VS"
    map.areas[1, 1]!!.bonusText shouldBe "Steel"
    map.areas[1, 1]!!.className shouldBe cn("Demo_1_1")
    map.areas[1, 1]!!.asClassDeclaration.properties shouldBe
        mapOf(PropertyName("row") to NumberValue(1), PropertyName("column") to NumberValue(1))
  }
}
