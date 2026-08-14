package dev.martianzoo.engine

import dev.martianzoo.data.GameConfig
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.canon.Canon
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  fun rawConfigResolvesToAffirmativeClassNames() {
    val config = GameConfig("Player1, Player2, -CorporateEraExpansion")

    val premise = Canon.gamePremise(config)

    premise.classSelections.map { it.className }.toSet() shouldBe
        setOf(cn("Player1"), cn("Player2"))
    premise.modules.containsAll(setOf(cn("MultiplayerMode"), cn("TerraformingMars"))) shouldBe true
    premise.modules.shouldNotContain(cn("CorporateEraExpansion"))
    Engine.newGame(premise).classTable.isActive(cn("CorporateEraExpansion")) shouldBe false
  }

  @Test
  fun configCookingAppliesDefaultsAndImplications() {
    val premise = Canon.gamePremise(GameConfig("Player1, Player2, VenusNextExpansion"))

    premise.modules shouldContain cn("TerraformingMars")
    premise.modules shouldContain cn("TharsisMapOption")
    premise.modules shouldContain cn("MultiplayerMode")
    premise.modules shouldContain cn("CorporateEraExpansion")
    premise.modules shouldContain cn("WorldGovernmentOption")
  }

  @Test
  fun explicitSelectionsAndExclusionsOverrideConfigCooking() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "Player1, Player2, HellasMapOption, VenusNextExpansion, -WorldGovernmentOption"
            )
        )

    premise.modules shouldContain cn("HellasMapOption")
    premise.modules.shouldNotContain(cn("TharsisMapOption"))
    premise.modules.shouldNotContain(cn("WorldGovernmentOption"))
  }

  @Test
  fun invalidConfigurationFailsWhileBootstrappingAWorld() {
    shouldThrow<IllegalArgumentException> {
      Engine.newGame(Canon.gamePremise(GameConfig("Player1, Player2, WorldGovernmentOption")))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("Player1, TypoOption"))
    }
  }

  @Test
  fun individualClassExclusionOverridesAModule() {
    val premise = Canon.gamePremise(GameConfig("Player1, Player2, -Card001"))

    Engine.newGame(premise).classTable.isActive(cn("Card001")) shouldBe false
  }

  @Test
  fun namedGoalConfigurationSelectsExactMilestoneAndAwardPools() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "Player1, Player2, HellasMapOption, MilestonesAwardsExpansion, " +
                    "Coastguard, Landshaper, Botanist, Founder"
            )
        )
    val table = Engine.newGame(premise).classTable

    table.isActive(cn("MilestoneUM7")) shouldBe true
    table.isActive(cn("MilestoneMM15")) shouldBe true
    table.isActive(cn("MilestoneHM0")) shouldBe false
    table.isActive(cn("AwardUA2")) shouldBe true
    table.isActive(cn("AwardUA6")) shouldBe true
    table.isActive(cn("AwardHA0")) shouldBe false
  }

  @Test
  fun initialComponentTypesMustBeConcreteAndNonSingleton() {
    val premise =
        Canon.gamePremise(GameConfig("Player1, Player2"))
            .copy(initialComponentTypes = setOf(cn("Card").expression))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
