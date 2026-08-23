package dev.martianzoo.engine

import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.data.ClassSelection
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.api.Bundle
import dev.martianzoo.tfm.api.TfmAuthority
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.ColonizerTrainingCamp
import dev.martianzoo.util.toSetStrict
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

    premise.classSelections shouldBe emptySet()
    premise.modules.containsAll(setOf(cn("MultiplayerMode"), cn("TerraformingMars"))) shouldBe true
    premise.modules.shouldNotContain(cn("CorporateEraExpansion"))
    Engine.newGame(premise).classTable.isActive(cn("CorporateEraExpansion")) shouldBe false
  }

  @Test
  fun observationalModuleReferencesDoNotCreateBootstrapDependencies() {
    val observers =
        object : Bundle(cn("Observers")) {
          override val explicitClassDeclarations =
              parseClasses(
                      """
                      CLASS ObserverA : Module { ObserverB: ObservationFromA }
                      CLASS ObserverB : Module { ObserverA: ObservationFromB }
                      CLASS ObservationFromA
                      CLASS ObservationFromB
                      """
                          .trimIndent()
                  )
                  .toSetStrict()
        }
    val authority = TfmAuthority.compose(Canon, observers)
    val premise = authority.gamePremise(GameConfig("ObserverA, ObserverB", "Player1", "Player2"))

    val game = Engine.newGame(premise)

    game.classTable.isActive(cn("ObserverA")) shouldBe true
    game.classTable.isActive(cn("ObserverB")) shouldBe true
  }

  @Test
  fun configuredPlayerNamesBecomeVocabularyAliasesForCanonicalPlayers() {
    val mom = cn("Mom")
    val ellie = cn("Ellie")
    val config = GameConfig("-CorporateEraExpansion", "Mom", "Ellie")
    GameConfig(config.toString(), "Mom", "Ellie") shouldBe config
    val premise = Canon.gamePremise(config)

    premise.playerNames.shouldContainExactly(mom, ellie)
    premise.playerClassNames.shouldContainExactly(cn("Player1"), cn("Player2"))
    premise.classSelections shouldBe emptySet()

    val game = Engine.newGame(premise)
    Canon.classTable.findClass(mom) shouldBe null
    game.classTable.findClass(mom) shouldBe null
    game.actors.shouldContainExactly(Player.PLAYER1, Player.PLAYER2, ENGINE)
    game.vocabulary.canonicalName(mom) shouldBe cn("Player1")
    game.vocabulary.petsName(cn("Player1")) shouldBe mom
    game.reader.getComponents("Player").map { it.className }.toSet() shouldBe
        setOf(cn("Player1"), cn("Player2"))
    TfmWorkflow.Manual(game).setupPhase()
    game.gameplay(Player.PLAYER1).count("TerraformRating<Mom>") shouldBe 20
    game.gameplay(Player.PLAYER2).count("TerraformRating<Ellie>") shouldBe 20
    getPlayerOwner(game.reader, game.reader.getComponents("StartToken").single()) shouldBe
        Player.PLAYER1
  }

  @Test
  fun preludeRulesCanUseOnlyThePrelude2CardPool() {
    val table =
        Engine.newGame(
                Canon.gamePremise(
                    GameConfig(
                        "Prelude2Expansion, -Prelude1Deck",
                        "Player1",
                        "Player2",
                    )
                )
            )
            .classTable

    table.isActive(cn("PreludePhase")) shouldBe true
    table.isActive(cn("AppliedScience")) shouldBe true
    table.isActive(cn("MartianIndustries")) shouldBe false
  }

  @Test
  fun malformedConfigurationFailsBeforeBootstrappingAWorld() {
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
  fun unseatedCanonicalPlayerCannotBeActivatedAsAnOrdinaryClass() {
    val premise =
        Canon.gamePremise(GameConfig("", "Player1", "Player2"))
            .copy(classSelections = setOf(ClassSelection(cn("Player3"), included = true)))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }

  @Test
  fun individualClassExclusionOverridesAModule() {
    val premise = Canon.gamePremise(GameConfig("-$ColonizerTrainingCamp", "Player1", "Player2"))

    Engine.newGame(premise).classTable.isActive(ColonizerTrainingCamp) shouldBe false
  }

  @Test
  fun namedGoalConfigurationSelectsExactMilestoneAndAwardPools() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMap, Coastguard, Landshaper, Botanist, Founder",
                "Player1",
                "Player2",
            )
        )
    val table = Engine.newGame(premise).classTable

    table.isActive(cn("Coastguard")) shouldBe true
    table.isActive(cn("Landshaper")) shouldBe true
    table.isActive(cn("Diversifier")) shouldBe false
    table.isActive(cn("Botanist")) shouldBe true
    table.isActive(cn("Founder")) shouldBe true
    table.isActive(cn("Cultivator")) shouldBe false
  }

  @Test
  fun namedGoalsCanReplaceOneDefaultPoolWithoutSelectingTheExpansionModule() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMap, Landshaper",
                "Player1",
                "Player2",
            )
        )
    val table = Engine.newGame(premise).classTable

    premise.modules.shouldNotContain(cn("HellasDefaultMilestones"))
    premise.modules shouldContain cn("HellasDefaultAwards")
    table.isActive(cn("Landshaper")) shouldBe true
    table.isActive(cn("Diversifier")) shouldBe false
    table.isActive(cn("Cultivator")) shouldBe true
  }

  @Test
  fun soloModeDoesNotActivateAwardsOrMultiplayerGoalActions() {
    val table = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1"))).classTable

    Canon.awardDefinitions.none { table.isActive(it.className) } shouldBe true
    table.isActive(cn("ClaimMilestoneSA")) shouldBe false
    table.isActive(cn("FundAwardSA")) shouldBe false

    shouldThrow<IllegalArgumentException> {
      Engine.newGame(Canon.gamePremise(GameConfig("Landlord", "Player1")))
    }
  }

  @Test
  fun initialComponentTypesMustBeConcreteAndNonSingleton() {
    val premise =
        Canon.gamePremise(GameConfig("", "Player1", "Player2"))
            .copy(initialComponentTypes = setOf(cn("Card").expression))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
