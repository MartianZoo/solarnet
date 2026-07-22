package dev.martianzoo.tfm.data

import dev.martianzoo.pets.HasClassName.Companion.classNames
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MarsMapDefinitionTest {

  val demoMapJson =
      """
        {
          "legend": {
            "L": "LandArea", "W": "WaterArea", "V": "VolcanicArea",
            "P": "Plant", "S": "Steel", "C": "ProjectCard",
          },
          "maps": [{
            "name": "Demo",
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
    map.className shouldBe cn("Demo")
    map.asClassDeclaration.supertypes.classNames().shouldContainExactlyInAnyOrder(cn("MarsMap"))
    map.areas.shouldHaveSize(7)
    map.areas[1, 1]!!.code shouldBe "VS"
    map.areas[1, 1]!!.bonusText shouldBe "Steel"
  }
}
