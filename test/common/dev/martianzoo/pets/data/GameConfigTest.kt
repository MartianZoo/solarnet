package dev.martianzoo.pets.data

import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
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

            VenusNextExpansion, 4 StartingOption, -WorldGovernmentRule
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
    config.componentCounts shouldBe mapOf(cn("StartingOption") to 4)
    config.playerNames.shouldContainExactly(cn("Player1"), cn("Player2"))
    config.toString() shouldBe
        "TerraformingMars, TharsisMap, VenusNextExpansion, 4 StartingOption, " +
            "-WorldGovernmentRule"
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
    shouldThrow<InvalidGameConfigException> { GameConfig("StartingOption, 2 StartingOption") }
    shouldThrow<InvalidGameConfigException> {
      GameConfig("2 StartingOption, 3 StartingOption")
    }
    shouldThrow<InvalidGameConfigException> { GameConfig("0 StartingOption") }
    shouldThrow<InvalidGameConfigException> { GameConfig("-2 StartingOption") }
    shouldThrow<InvalidGameConfigException> { GameConfig("-") }
    shouldThrow<InvalidGameConfigException> { GameConfig("Select<Class<ColonizerTrainingCamp>>") }
    shouldThrow<InvalidGameConfigException> { GameConfig("", "not a player") }
  }
}
