package dev.martianzoo.engine

import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  fun rawConfigResolvesToAffirmativeClassNames() {
    val config =
        GameConfig(
            "Player1, Player2, MultiplayerMode, TerraformingMars, TharsisMapOption, " +
                "-CorporateEraExpansion"
        )

    val premise = Canon.gamePremise(config)

    premise.classSelections.map { it.className }.toSet() shouldBe
        setOf(cn("Player1"), cn("Player2"))
    premise.modules.containsAll(setOf(cn("MultiplayerMode"), cn("TerraformingMars"))) shouldBe true
    premise.modules.shouldNotContain(cn("CorporateEraExpansion"))
    Engine.newGame(premise).classTable.isActive(cn("CorporateEraExpansion")) shouldBe false
  }

  @Test
  fun structuredConfigAppliesDefaults() {
    val premise =
        Canon.gamePremise(GameConfig("Player1, Player2, TerraformingMars, TharsisMapOption"))

    premise.modules.containsAll(
        setOf(
            cn("MultiplayerMode"),
            cn("CorporateEraExpansion"),
        )
    ) shouldBe true
  }

  @Test
  fun invalidConfigurationFailsWhileBootstrappingAWorld() {
    shouldThrow<IllegalArgumentException> {
      Engine.newGame(Canon.gamePremise(GameConfig("Player1, SoloMode, TerraformingMars")))
    }
    shouldThrow<IllegalArgumentException> {
      Engine.newGame(
          Canon.gamePremise(
              GameConfig(
                  "Player1, Player2, MultiplayerMode, TerraformingMars, TharsisMapOption, " +
                      "WorldGovernmentOption"
              )
          )
      )
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("Player1, TerraformingMars, TharsisMapOption, TypoOption"))
    }
  }

  @Test
  fun individualCardExclusionIsNotAConfigurationFeature() {
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(
          GameConfig("Player1, Player2, TerraformingMars, TharsisMapOption, -Card001")
      )
    }
  }

  @Test
  fun initialComponentTypesMustBeConcreteAndNonSingleton() {
    val premise =
        Canon.gamePremise(GameConfig("Player1, Player2, TerraformingMars, TharsisMapOption"))
            .copy(initialComponentTypes = setOf(cn("Card").expression))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
