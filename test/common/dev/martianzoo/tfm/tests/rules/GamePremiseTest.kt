package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.engine.*
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.data.ClassSelection
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.tfm.canon.ApiUtils.getPlayerOwner
import dev.martianzoo.tfm.canon.Bundle
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.ColonizerTrainingCamp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class GamePremiseTest {
  @Test
  internal fun worldsFromOnePremiseShareTheClassModelButNotLiveState() {
    val premise = Canon.gamePremise(GameConfig("", "Player1", "Player2"))
    val first = Engine.newGame(premise)
    val second = Engine.newGame(premise)

    first.classTable shouldBe second.classTable
    TfmWorkflow.Manual(first).setupPhase()
    first.agent(ENGINE).count("SetupPhase") shouldBe 1
    second.agent(ENGINE).count("SetupPhase") shouldBe 0
  }

  @Test
  internal fun rawConfigResolvesModulesAndFreezesDefaultGoals() {
    val config = GameConfig("-CorporateEraExpansion", "Player1", "Player2")

    val premise = Canon.gamePremise(config)

    val defaultGoals =
        premise.classSelections.filter(ClassSelection::included).mapTo(linkedSetOf()) {
          it.className
        }
    val milestone = Canon.classTable.getClass(cn("Milestone"))
    val award = Canon.classTable.getClass(cn("Award"))
    defaultGoals.size shouldBe 10
    defaultGoals.containsAll(setOf(cn("Terraformer35"), cn("Landlord"))) shouldBe true
    defaultGoals.all { className ->
      val selectedClass = Canon.classTable.getClass(className)
      selectedClass.isSubtypeOf(milestone) || selectedClass.isSubtypeOf(award)
    } shouldBe true
    premise.modules.containsAll(setOf(cn("MultiplayerMode"), cn("TerraformingMars"))) shouldBe true
    premise.modules.shouldNotContain(cn("CorporateEraExpansion"))
    Engine.newGame(premise).classTable.isActive(cn("CorporateEraExpansion")) shouldBe false
  }

  @Test
  internal fun observationalModuleReferencesDoNotCreateBootstrapDependencies() {
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
    val catalog = TfmCatalog.compose(Canon, observers)
    val premise = catalog.gamePremise(GameConfig("ObserverA, ObserverB", "Player1", "Player2"))

    val game = Engine.newGame(premise)

    game.classTable.isActive(cn("ObserverA")) shouldBe true
    game.classTable.isActive(cn("ObserverB")) shouldBe true
  }

  @Test
  internal fun configuredPlayerNamesBecomeVocabularyAliasesForCanonicalPlayers() {
    val blue = cn("Blue")
    val yellow = cn("Yellow")
    val config = GameConfig("-CorporateEraExpansion", "Blue", "Yellow")
    GameConfig(config.toString(), "Blue", "Yellow") shouldBe config
    val premise = Canon.gamePremise(config)

    premise.playerNames.shouldContainExactly(blue, yellow)
    premise.playerClassNames.shouldContainExactly(cn("Player1"), cn("Player2"))
    premise.classSelections.none {
      it.className in setOf(blue, yellow, cn("Player1"), cn("Player2"))
    } shouldBe true

    val game = Engine.newGame(premise)
    Canon.classTable.findClass(blue) shouldBe null
    game.classTable.findClass(blue) shouldBe null
    game.actors.shouldContainExactly(Player.PLAYER1, Player.PLAYER2, ENGINE)
    game.vocabulary.canonicalName(blue) shouldBe cn("Player1")
    game.vocabulary.petsName(cn("Player1")) shouldBe blue
    game.reader.getComponents("Player").map { it.className }.toSet() shouldBe
        setOf(cn("Player1"), cn("Player2"))
    TfmWorkflow.Manual(game).setupPhase()
    game.agent(Player.PLAYER1).count("TerraformRating<Blue>") shouldBe 20
    game.agent(Player.PLAYER2).count("TerraformRating<Yellow>") shouldBe 20
    getPlayerOwner(game.reader, game.reader.getComponents("StartToken").single()) shouldBe
        Player.PLAYER1
  }

  @Test
  internal fun prelude1RulesCanUseOnlyThePrelude2CardPool() {
    val table =
        Engine.newGame(
                Canon.gamePremise(
                    GameConfig(
                        "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
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
  internal fun malformedConfigurationFailsBeforeBootstrappingAWorld() {
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("TypoOption, VenusNextExpansion", "Player1"))
    }
    shouldThrow<IllegalArgumentException> { Canon.gamePremise(GameConfig("VenusNextExpansion")) }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("Blue, Yellow, VenusNextExpansion", "Player1"))
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(GameConfig("", "One", "Two", "Three", "Four", "Five", "Six"))
    }
  }

  @Test
  internal fun unseatedCanonicalPlayerCannotBeActivatedAsAnOrdinaryClass() {
    val premise =
        Canon.gamePremise(GameConfig("", "Player1", "Player2"))
            .copy(classSelections = setOf(ClassSelection(cn("Player3"), included = true)))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }

  @Test
  internal fun individualClassExclusionOverridesAModule() {
    val premise = Canon.gamePremise(GameConfig("-$ColonizerTrainingCamp", "Player1", "Player2"))

    Engine.newGame(premise).classTable.isActive(ColonizerTrainingCamp) shouldBe false
  }

  @Test
  internal fun namedGoalConfigurationSelectsExactMilestoneAndAwardPools() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                """
                HellasMap,
                Coastguard, Landshaper, Builder,
                Botanist, Founder, Administrator
                """,
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
  internal fun namedGoalsCanReplaceOneDefaultPoolWithoutSelectingTheExpansionModule() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMap, Landshaper, Builder, Coastguard",
                "Player1",
                "Player2",
            )
        )
    val table = Engine.newGame(premise).classTable

    table.isActive(cn("Landshaper")) shouldBe true
    table.isActive(cn("Diversifier")) shouldBe false
    table.isActive(cn("Cultivator")) shouldBe true
  }

  @Test
  internal fun supportedBundleGoalsBecomeAnExactPremisePool() {
    val premise = Canon.gamePremise(GameConfig("AmazonisMap", "Player1", "Player2"))
    val milestone = Canon.classTable.getClass(cn("Milestone"))
    val award = Canon.classTable.getClass(cn("Award"))
    val goalSelections =
        premise.classSelections.filter { selection ->
          val selectedClass = Canon.classTable.getClass(selection.className)
          selectedClass.isSubtypeOf(milestone) || selectedClass.isSubtypeOf(award)
        }

    goalSelections
        .filter { it.included && Canon.classTable.getClass(it.className).isSubtypeOf(milestone) }
        .size shouldBe 4
    goalSelections
        .filter { it.included && Canon.classTable.getClass(it.className).isSubtypeOf(award) }
        .size shouldBe 4
    goalSelections.mapTo(linkedSetOf(), ClassSelection::className) shouldBe
        (milestone.allSubclasses() + award.allSubclasses())
            .filterNot { it.abstract }
            .mapTo(linkedSetOf()) { it.className }
  }

  @Test
  internal fun multiplayerGamesRequireThreeMilestonesAndThreeAwards() {
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(
          GameConfig(
              "HellasMap, Coastguard, Landshaper",
              "Player1",
              "Player2",
          )
      )
    }
    shouldThrow<IllegalArgumentException> {
      Canon.gamePremise(
          GameConfig(
              "HellasMap, Botanist, Founder",
              "Player1",
              "Player2",
          )
      )
    }
  }

  @Test
  internal fun soloModeDoesNotActivateDefaultGoalsOrMultiplayerGoalActions() {
    val table = Engine.newGame(Canon.gamePremise(GameConfig("", "Player1"))).classTable

    Canon.classTable.getClass(cn("Milestone")).allSubclasses().none {
      table.isActive(it.className)
    } shouldBe true
    Canon.classTable.getClass(cn("Award")).allSubclasses().none {
      table.isActive(it.className)
    } shouldBe true
    table.isActive(cn("ClaimMilestone")) shouldBe false
    table.isActive(cn("FundAward")) shouldBe false

    shouldThrow<IllegalArgumentException> {
      Engine.newGame(Canon.gamePremise(GameConfig("Landlord", "Player1")))
    }
  }

  @Test
  internal fun initialComponentTypesMustBeConcreteAndNonSingleton() {
    val premise =
        Canon.gamePremise(GameConfig("", "Player1", "Player2"))
            .copy(initialComponentTypes = setOf(cn("Card").expression))

    shouldThrow<IllegalArgumentException> { Engine.newGame(premise) }
  }
}
