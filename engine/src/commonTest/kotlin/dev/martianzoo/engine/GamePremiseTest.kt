package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmWorkflow
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  fun rawConfigResolvesToAffirmativeClassNames() {
    val config = GameConfig("-CorporateEraExpansion", "Player1", "Player2")

    val premise = Canon.gamePremise(config)

    premise.classSelections.map { it.className }.toSet() shouldBe
        setOf(cn("Player1"), cn("Player2"))
    premise.modules.containsAll(setOf(cn("MultiplayerMode"), cn("TerraformingMars"))) shouldBe true
    premise.modules.shouldNotContain(cn("CorporateEraExpansion"))
    Engine.newGame(premise).classTable.isActive(cn("CorporateEraExpansion")) shouldBe false
  }

  @Test
  fun configCookingAppliesDefaultsAndImplications() {
    val premise = Canon.gamePremise(GameConfig("VenusNextExpansion", "Player1", "Player2"))

    premise.modules shouldContain cn("TerraformingMars")
    premise.modules shouldContain cn("TharsisMapOption")
    premise.modules shouldContain cn("MultiplayerMode")
    premise.modules shouldContain cn("CorporateEraExpansion")
    premise.modules shouldContain cn("WorldGovernmentOption")
  }

  @Test
  fun configuredPlayerNamesBecomeRealRuntimeClassesAndActors() {
    val mom = cn("Mom")
    val ellie = cn("Ellie")
    val config = GameConfig("-CorporateEraExpansion", "Mom", "Ellie")
    GameConfig(config.toString(), "Mom", "Ellie") shouldBe config
    val premise = Canon.gamePremise(config)

    premise.playerClassNames.shouldContainExactly(mom, ellie)
    premise.classSelections
        .map { it.className }
        .filter { it == mom || it == ellie }
        .shouldContainExactly(mom, ellie)

    val game = Engine.newGame(premise)
    game.actors.shouldContainExactly(Player(mom), Player(ellie), ENGINE)
    game.reader.getComponents("Player").map { it.className }.toSet() shouldBe setOf(mom, ellie)
    TfmWorkflow.Manual(game).setupPhase()
    game.gameplay(Player(mom)).count("TerraformRating") shouldBe 20
    game.gameplay(Player(ellie)).count("TerraformRating") shouldBe 20
    getPlayerOwner(game.reader, game.reader.getComponents("StartToken").single()) shouldBe
        Player(mom)
  }

  @Test
  fun explicitSelectionsAndExclusionsOverrideConfigCooking() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMapOption, VenusNextExpansion, -WorldGovernmentOption",
                "Player1",
                "Player2",
            )
        )

    premise.modules shouldContain cn("HellasMapOption")
    premise.modules.shouldNotContain(cn("TharsisMapOption"))
    premise.modules.shouldNotContain(cn("WorldGovernmentOption"))
  }

  @Test
  fun invalidConfigurationFailsWhileBootstrappingAWorld() {
    shouldThrow<IllegalArgumentException> {
      Engine.newGame(Canon.gamePremise(GameConfig("WorldGovernmentOption", "Player1", "Player2")))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("TypoOption, VenusNextExpansion", "Player1"))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("VenusNextExpansion"))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("Mom, Ellie, VenusNextExpansion", "Player1"))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("", "One", "Two", "Three", "Four", "Five", "Six"))
    }
  }

  @Test
  fun individualClassExclusionOverridesAModule() {
    val premise = Canon.gamePremise(GameConfig("-Card001", "Player1", "Player2"))

    Engine.newGame(premise).classTable.isActive(cn("Card001")) shouldBe false
  }

  @Test
  fun namedGoalConfigurationSelectsExactMilestoneAndAwardPools() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMapOption, MilestonesAwardsExpansion, " +
                    "Coastguard, Landshaper, Botanist, Founder",
                "Player1",
                "Player2",
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
        Canon.gamePremise(GameConfig("", "Player1", "Player2"))
            .copy(initialComponentTypes = setOf(cn("Card").expression))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
