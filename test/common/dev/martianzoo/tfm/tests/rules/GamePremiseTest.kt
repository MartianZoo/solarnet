package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testAgent
import dev.martianzoo.agenttestsupport.testAgents
import dev.martianzoo.catalog.ClassSelection
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.catalog.GamePremise
import dev.martianzoo.engine.*
import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.api.Exceptions.InvalidGameConfigException
import dev.martianzoo.pets.api.SystemClasses.PLAYER
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.util.toSetStrict
import dev.martianzoo.state.Actor.Companion.ADMIN
import dev.martianzoo.state.Player
import dev.martianzoo.state.toComponent
import dev.martianzoo.tfm.canon.Bundle
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.TfmCatalog
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.cards.cardnames.ColonizerTrainingCamp
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertSame

internal class GamePremiseTest {
  @Test
  internal fun directPremisesRequireConcretePlayerClasses() {
    val catalog = Canon.withPlayers(1)

    listOf(cn("MC"), PLAYER).forEach { invalidPlayerName ->
      shouldThrow<InvalidGameConfigException> {
        GamePremise(
            catalog,
            classSelections = emptySet(),
            playerNames = listOf(invalidPlayerName),
        )
      }
    }
  }

  @Test
  internal fun conventionalPlayerCatalogsAreReusableAndAbsentFromCanon() {
    val catalog = Canon.withPlayers(6)

    assertSame(catalog, Canon.withPlayers(6))
    Canon.classTable.findClass(cn("Player1")) shouldBe null
    catalog.classTable
        .getClass(cn("Player1"))
        .isSubtypeOf(catalog.classTable.getClass(cn("Player"))) shouldBe true
    catalog.classTable.getClass(cn("Player6")).abstract shouldBe false
  }

  @Test
  internal fun ordinaryPremisesReuseCanonAndOwnTheirGeneratedClasses() {
    val premise = Canon.gamePremise(GameConfig("", "Player1", "Player2"))
    val table = premise.classTable

    assertSame(Canon, premise.catalog)
    assertSame(Canon.classTable.getClass(cn("Card")), table.getClass(cn("Card")))
    Canon.classTable.findClass(cn("Player1")) shouldBe null
    table.getClass(cn("Player1")).classTable shouldBe table
    table.getClass(cn("Premise")).classTable shouldBe table
  }

  @Test
  internal fun worldsFromOnePremiseShareTheClassModelButNotLiveState() {
    val premise = Canon.gamePremise(GameConfig("", "Player1", "Player2"))
    val first = TfmEngine.newGame(premise)
    val second = TfmEngine.newGame(premise)

    first.classTable shouldBe second.classTable
    first.testAgents()[ADMIN].beginOperation("SetupPhase FROM Phase")
    first.testAgent(ADMIN).count("SetupPhase") shouldBe 1
    second.testAgent(ADMIN).count("SetupPhase") shouldBe 0
  }

  @Test
  internal fun rawConfigResolvesModulesAndFreezesDefaultGoals() {
    val config = GameConfig("-CorporateEraExpansion", "Player1", "Player2")

    val premise = Canon.gamePremise(config)

    val defaultGoals =
        premise.classSelections.filter(ClassSelection::included).mapTo(linkedSetOf()) {
          it.className
        } - premise.modules
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
    val table = TfmEngine.newGame(premise).classTable
    table.isInhabited(cn("CorporateEraExpansion")) shouldBe false
    (cn("CorporateEraExpansion") in table.allClassNames) shouldBe false
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
    val catalog = TfmCatalog(Canon, observers)
    val premise = catalog.gamePremise(GameConfig("ObserverA, ObserverB", "Player1", "Player2"))

    val game = TfmEngine.newGame(premise)

    game.classTable.isInhabited(cn("ObserverA")) shouldBe true
    game.classTable.isInhabited(cn("ObserverB")) shouldBe true
  }

  @Test
  internal fun configuredPlayerNamesBecomeConcretePlayerClasses() {
    val blue = cn("Blue")
    val yellow = cn("Yellow")
    val config = GameConfig("-CorporateEraExpansion", "Blue", "Yellow")
    GameConfig(config.toString(), "Blue", "Yellow") shouldBe config
    val premise = Canon.gamePremise(config)

    premise.playerNames.shouldContainExactly(blue, yellow)
    premise.classSelections.none { it.className in setOf(blue, yellow) } shouldBe true

    val game = TfmEngine.newGame(premise)
    Canon.classTable.findClass(blue) shouldBe null
    game.classTable.isInhabited(blue) shouldBe true
    game.actors.shouldContainExactly(Player(blue), Player(yellow), ADMIN)
    game.reader.getComponents(cn("Player").expression).map { it.className }.toSet() shouldBe
        setOf(blue, yellow)
    game.testAgents()[ADMIN].beginOperation("SetupPhase FROM Phase")
    game.testAgent(Player(blue)).count("TerraformRating<Blue>") shouldBe 20
    game.testAgent(Player(yellow)).count("TerraformRating<Yellow>") shouldBe 20
    game.reader
        .getComponents(cn("StartToken").expression)
        .single()
        .toComponent()
        .owningPlayer shouldBe Player(blue)
  }

  @Test
  internal fun prelude1RulesCanUseOnlyThePrelude2CardPool() {
    val table =
        TfmEngine.newGame(
                Canon.gamePremise(
                    GameConfig(
                        "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
                        "Player1",
                        "Player2",
                    )
                )
            )
            .classTable

    table.isInhabited(cn("PreludePhase")) shouldBe true
    table.isInhabited(cn("SpaceLanes")) shouldBe true
    table.isInhabited(cn("MartianIndustries")) shouldBe false
    (cn("MartianIndustries") in table.allClassNames) shouldBe false
  }

  @Test
  internal fun malformedConfigurationFailsBeforeBootstrappingAWorld() {
    shouldThrow<InvalidGameConfigException> {
      Canon.gamePremise(GameConfig("TypoOption, VenusNextExpansion", "Player1"))
    }
    shouldThrow<InvalidGameConfigException> { Canon.gamePremise(GameConfig("VenusNextExpansion")) }
    shouldThrow<InvalidGameConfigException> {
      Canon.gamePremise(GameConfig("Blue, Yellow, VenusNextExpansion", "Player1"))
    }
    shouldThrow<InvalidGameConfigException> { Canon.gamePremise(GameConfig("", "MC")) }
    shouldThrow<InvalidGameConfigException> {
      Canon.gamePremise(GameConfig("2 Player", "Player1", "Player2"))
    }
  }

  @Test
  internal fun selectedColonyRequiresItsProvidingModule() {
    shouldThrow<InvalidGameConfigException> {
      Canon.gamePremise(GameConfig("Callisto", "Player1", "Player2"))
    }
  }

  @Test
  internal fun configuredColoniesReachPlayWithoutSeparateInitialTypes() {
    val game =
        TfmEngine.newGame(
            Canon.gamePremise(
                GameConfig("ColoniesExpansion, Callisto, Luna, Enceladus", "Player1", "Player2")
            )
        )
    val admin = game.testAgent(ADMIN)

    admin.count("SelectedColonyTile") shouldBe 3
    admin.count("CallistoSelected") shouldBe 1
    admin.count("LunaSelected") shouldBe 1
    admin.count("EnceladusSelected") shouldBe 1
    admin.count("SelectedColonyTile<Class<Ceres>>") shouldBe 0

    admin.beginOperation("SetupPhase FROM Phase")
    admin.runOperation("CorporationPhase FROM Phase")

    admin.count("SelectedColonyTile") shouldBe 0
    admin.count("Callisto") shouldBe 1
    admin.count("Luna") shouldBe 1
    admin.count("DelayedEnceladus") shouldBe 1
    admin.count("Ceres") shouldBe 0
  }

  @Test
  internal fun configurationsCanSeatMoreThanFivePlayers() {
    val names = listOf("One", "Two", "Three", "Four", "Five", "Six").map(::cn)

    val premise = Canon.gamePremise(GameConfig.create(included = emptyList(), playerNames = names))

    premise.playerNames shouldBe names
    TfmEngine.newGame(premise).testAgent(ADMIN).count("Player") shouldBe 6
  }

  @Test
  internal fun unconfiguredPlayerCannotBeActivatedAsAnOrdinaryClass() {
    val premise = Canon.withPlayers(3).gamePremise(GameConfig("", "Player1", "Player2"))

    shouldThrow<InvalidGameConfigException> {
      TfmEngine.newGame(
          premise.copy(classSelections = setOf(ClassSelection(cn("Player3"), included = true)))
      )
    }
  }

  @Test
  internal fun individualClassExclusionOverridesAModule() {
    val premise = Canon.gamePremise(GameConfig("-$ColonizerTrainingCamp", "Player1", "Player2"))
    val table = TfmEngine.newGame(premise).classTable

    table.isInhabited(ColonizerTrainingCamp) shouldBe false
    (ColonizerTrainingCamp in table.allClassNames) shouldBe false
  }

  @Test
  internal fun namedGoalConfigurationSelectsExactMilestoneAndAwardPools() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                """
                HellasMap,
                Coastguard, Landshaper, Builder, Terraformer,
                Botanist, Founder, Administrator, Banker
                """,
                "Player1",
                "Player2",
            )
        )
    val table = TfmEngine.newGame(premise).classTable

    table.isInhabited(cn("Coastguard")) shouldBe true
    table.isInhabited(cn("Landshaper")) shouldBe true
    table.isInhabited(cn("Terraformer")) shouldBe true
    table.isInhabited(cn("Diversifier")) shouldBe false
    table.isInhabited(cn("Botanist")) shouldBe true
    table.isInhabited(cn("Founder")) shouldBe true
    table.isInhabited(cn("Banker")) shouldBe true
    table.isInhabited(cn("Cultivator")) shouldBe false
    (cn("Diversifier") in table.allClassNames) shouldBe false
    (cn("Cultivator") in table.allClassNames) shouldBe false
  }

  @Test
  internal fun namedGoalsCanDefineOneExactPoolWithoutSelectingTheExpansionModule() {
    val premise =
        Canon.gamePremise(
            GameConfig(
                "HellasMap, Landshaper, Builder, Coastguard, Terraformer",
                "Player1",
                "Player2",
            )
        )
    val table = TfmEngine.newGame(premise).classTable

    table.isInhabited(cn("Landshaper")) shouldBe true
    table.isInhabited(cn("Diversifier")) shouldBe false
    table.isInhabited(cn("Cultivator")) shouldBe true
    (cn("Diversifier") in table.allClassNames) shouldBe false
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
        (Canon.classTable.allSubclasses(milestone) + Canon.classTable.allSubclasses(award))
            .filterNot { it.abstract }
            .mapTo(linkedSetOf()) { it.className }
  }

  @Test
  internal fun multiplayerGamesAllowSmallExactGoalPools() {
    val oneMilestone =
        TfmEngine.newGame(
                Canon.gamePremise(GameConfig("HellasMap, Coastguard", "Player1", "Player2"))
            )
            .classTable
    val oneAward =
        TfmEngine.newGame(
                Canon.gamePremise(GameConfig("HellasMap, Botanist", "Player1", "Player2"))
            )
            .classTable

    oneMilestone.isInhabited(cn("Coastguard")) shouldBe true
    oneMilestone.isInhabited(cn("Landshaper")) shouldBe false
    oneAward.isInhabited(cn("Botanist")) shouldBe true
    oneAward.isInhabited(cn("Founder")) shouldBe false
    (cn("Landshaper") in oneMilestone.allClassNames) shouldBe false
    (cn("Founder") in oneAward.allClassNames) shouldBe false
  }

  @Test
  internal fun soloModeDoesNotActivateDefaultGoalsOrMultiplayerGoalActions() {
    val table = TfmEngine.newGame(Canon.gamePremise(GameConfig("", "Player1"))).classTable

    table.allSubclasses(Canon.classTable.getClass(cn("Milestone"))).shouldBeEmpty()
    table.allSubclasses(Canon.classTable.getClass(cn("Award"))).shouldBeEmpty()
    table.isInhabited(cn("ClaimMilestoneAction")) shouldBe false
    table.isInhabited(cn("FundAwardAction")) shouldBe false
    (cn("ClaimMilestoneAction") in table.allClassNames) shouldBe false
    (cn("FundAwardAction") in table.allClassNames) shouldBe false

    shouldThrow<InvalidGameConfigException> {
      TfmEngine.newGame(Canon.gamePremise(GameConfig("Landlord", "Player1")))
    }
    val explicitMilestone =
        TfmEngine.newGame(Canon.gamePremise(GameConfig("Terraformer35", "Player1"))).classTable
    explicitMilestone.isInhabited(cn("Terraformer35")) shouldBe true
    explicitMilestone.isInhabited(cn("ClaimMilestoneAction")) shouldBe false
  }
}
