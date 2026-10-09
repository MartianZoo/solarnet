package dev.martianzoo.catalog

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.Exceptions.PetException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GameConfigTest {
  @Test
  internal fun flexiblyParsesCommasSpacesNewlinesAndBlankLines() {
    val config =
        GameConfig(
            """
            TerraformingMars, TharsisMap

            VenusNextExpansion, 2 SelectableCorporationCount, -1 SelectablePreludeCount,
            -WorldGovernmentRule
            """
                .trimIndent(),
            "Player1",
            "Player2",
        )

    config.includedClassNames.shouldContainExactly(
        cn("TerraformingMars"),
        cn("TharsisMap"),
        cn("VenusNextExpansion"),
    )
    config.excludedClassNames.shouldContainExactly(cn("WorldGovernmentRule"))
    config.componentAdjustments shouldBe
        mapOf(cn("SelectableCorporationCount") to 2, cn("SelectablePreludeCount") to -1)
    config.playerNames.shouldContainExactly(cn("Player1"), cn("Player2"))
    config.toString() shouldBe
        "TerraformingMars, TharsisMap, VenusNextExpansion, 2 SelectableCorporationCount, " +
            "-1 SelectablePreludeCount, -WorldGovernmentRule"
    GameConfig(config.toString(), "Player1", "Player2") shouldBe config
  }

  @Test
  internal fun entryOrderIsNotSemantic() {
    GameConfig("TerraformingMars, PreludeExpansion") shouldBe
        GameConfig("PreludeExpansion, TerraformingMars")
  }

  @Test
  internal fun acceptsArbitraryPlayerClassNamesSeparately() {
    val config = GameConfig("TerraformingMars", "Blue", "Yellow")

    config.includedClassNames.shouldContainExactly(cn("TerraformingMars"))
    config.playerNames.shouldContainExactly(cn("Blue"), cn("Yellow"))
    config.toString() shouldBe "TerraformingMars"
  }

  @Test
  internal fun rejectsDuplicateAndNonClassEntries() {
    shouldThrow<InvalidGameConfigException> { GameConfig("TerraformingMars, TerraformingMars") }
    shouldThrow<InvalidGameConfigException> { GameConfig("TerraformingMars, -TerraformingMars") }
    shouldThrow<InvalidGameConfigException> { GameConfig("TerraformingMars", "Blue", "Blue") }
    shouldThrow<InvalidGameConfigException> { GameConfig("TerraformingMars", "TerraformingMars") }
    shouldThrow<InvalidGameConfigException> { GameConfig("-TerraformingMars", "TerraformingMars") }
    shouldThrow<InvalidGameConfigException> {
      GameConfig("SelectablePreludeCount, 2 SelectablePreludeCount")
    }
    shouldThrow<InvalidGameConfigException> {
      GameConfig("2 SelectablePreludeCount, 3 SelectablePreludeCount")
    }
    shouldThrow<InvalidGameConfigException> { GameConfig("0 SelectablePreludeCount") }
    shouldThrow<InvalidGameConfigException> { GameConfig("-") }
    shouldThrow<InvalidGameConfigException> { GameConfig("Select<Class<ColonizerTrainingCamp>>") }
    shouldThrow<InvalidGameConfigException> { GameConfig("", "not a player") }
  }

  @Test
  internal fun `configuration errors are distinct from Pets errors`() {
    val configuration: Exception =
        shouldThrow<InvalidGameConfigException> { GameConfig("Plant, Plant") }
    (configuration is PetException) shouldBe false
  }
}
