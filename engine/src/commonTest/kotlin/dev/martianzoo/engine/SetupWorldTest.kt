package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.api.Exceptions.TaskException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.canon.Canon.Option.*
import dev.martianzoo.tfm.engine.TEST_CLASS_SYNONYMS
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class SetupWorldTest {
  @Test
  fun setupWorldIsIndependentAndInitialized() {
    val setupWorld = newSetupWorld()
    val reader = setupWorld.gameplay(ENGINE)

    reader.count("CorporateEraExpansion") shouldBe 1
    reader.count("TerraformingMars") shouldBe 1
    reader.count("TharsisMapOption") shouldBe 1
    reader.count("MultiplayerMode") shouldBe 1
    reader.count("Player") shouldBe 2
    setupWorld.classTable.allClassNamesAndIds.shouldNotContain(cn("TerraformRating"))
    setupWorld.classTable.allClassNamesAndIds.shouldNotContain(cn("Owner"))
    setupWorld.isIdle() shouldBe true
  }

  @Test
  fun setupWorldCollectsPlayerAndExpansionOptions() {
    val options =
        Canon.Option.DEFAULTS +
            setOf(PreludeExpansion, VenusNextExpansion, ColoniesExpansion, PromoCardPack)
    val setupWorld =
        newSetupWorld(
                players = 5,
                options = options,
                selectedColonies = setOf(cn("Titan")),
            )
            .gameplay(ENGINE)
            .godMode()

    setupWorld.count("Player") shouldBe 5
    setupWorld.count("GameOption") shouldBe 9
    setupWorld.count("TitanSelected") shouldBe 1
  }

  @Test
  fun setupOptionsAreEditable() {
    val setupWorld = newSetupWorld().gameplay(ENGINE).godMode()

    setupWorld.manual("-CorporateEraExpansion")
    setupWorld.count("CorporateEraExpansion") shouldBe 0
  }

  @Test
  fun explicitExclusionsMaskDefaultsAndDoNotReachThePlayableWorld() {
    val setupWorld =
        Engine.newSetupWorld(
            Canon.setupWorldDefinition(
                players = 2,
                options = Canon.GameOptions(excluded = setOf(CorporateEraExpansion)),
            )
        )
    val setup = setupWorld.gameplay(ENGINE)

    setup.count("CorporateEraExpansion") shouldBe 0
    setup.count("Exclude<Class<CorporateEraExpansion>>") shouldBe 1

    val game = Engine.newGame(setupWorld, Canon::assemble)
    game.gameplay(ENGINE).count("CorporateEraExpansion") shouldBe 0
    game.classTable.allClassNamesAndIds.shouldNotContain(cn("Exclude"))
  }

  @Test
  fun explicitStandardSoloExclusionSuppressesTheDefaultVariant() {
    val setupWorld =
        Engine.newSetupWorld(
            Canon.setupWorldDefinition(
                players = 1,
                options =
                    Canon.GameOptions(
                        included = Canon.Option.DEFAULTS,
                        excluded = setOf(StandardSoloVariant),
                    ),
            )
        )
    val setup = setupWorld.gameplay(ENGINE)

    setup.count("StandardSoloVariant") shouldBe 0
    shouldThrow<DeadEndException> { setup.godMode().manual("ValidateSetup") }
  }

  @Test
  fun colonySelectionsRequireTheColoniesExpansion() {
    shouldThrow<DeadEndException> {
      newSetupWorld(selectedColonies = setOf(cn("Luna")))
    }
  }

  @Test
  fun worldGovernmentOptionRequiresVenusNext() {
    val setupWorld = newSetupWorld(options = Canon.Option.DEFAULTS + WorldGovernmentOption)

    shouldThrow<DeadEndException> { Engine.newGame(setupWorld, Canon::assemble) }
  }

  @Test
  fun mapCardinalityIsValidatedByPets() {
    val noMap = newSetupWorld(options = emptySet())
    shouldThrow<DeadEndException> { Engine.newGame(noMap, Canon::assemble) }

    shouldThrow<LimitsException> {
      newSetupWorld(options = setOf(TharsisMapOption, ElysiumMapOption))
    }
  }

  @Test
  fun completedSetupWorldCanCreateARealGame() {
    val setupWorld =
        newSetupWorld(
            options =
                setOf(
                    TerraformingMars,
                    CorporateEraExpansion,
                    ElysiumMapOption,
                    PreludeExpansion,
                    PromoCardPack,
                )
        )

    val game = Engine.newGame(setupWorld, Canon::assemble)

    game.reader.tfmRuleset.marsMapDefinitions.single().className shouldBe cn("Elysium")
    game.gameplay(ENGINE).count("TerraformingMars") shouldBe 1
    game.classTable.allClassNamesAndIds.shouldNotContain(cn("GameOption"))
    game.gameplay(ENGINE).count("GameModule") shouldBe 6
    game.gameplay(ENGINE).count("ElysiumMapOption") shouldBe 1
    game.gameplay(ENGINE).count("Elysium") shouldBe 1
    game.reader.tfmRuleset.milestoneDefinitions.any { it.shortName == cn("EM2") } shouldBe true
    game.reader.tfmRuleset.milestoneDefinitions.any { it.shortName == cn("HM1") } shouldBe false
    game.reader.tfmRuleset.cardDefinitions.any { it.className == cn("DoubleDown") } shouldBe true
    game.gameplay(ENGINE).count("PromoCardPack") shouldBe 1
    game.gameplay(ENGINE).count("Player") shouldBe 2
  }

  @Test
  fun setupWorldWithPendingWorkCannotCreateARealGame() {
    val setupWorld = newSetupWorld()
    setupWorld.gameplay(ENGINE).godMode().addTasks("TitanSelected")

    shouldThrow<TaskException> { Engine.newGame(setupWorld, Canon::assemble) }
  }

  @Test
  fun setupComponentsAreCopiedIntoTheGameWorld() {
    val selectedColonies = setOf(cn("Ceres"), cn("Pluto"), cn("Titan"))
    val setupWorld =
        newSetupWorld(
            players = 1,
            options = setOf(TerraformingMars, HellasMapOption, ColoniesExpansion),
            selectedColonies = selectedColonies,
        )

    val game = Engine.newGame(setupWorld, Canon::assemble)

    game.gameplay(ENGINE).count("Player") shouldBe 1
    game.gameplay(ENGINE).count("SelectedColonyTile") shouldBe 3
    setupWorld.gameplay(ENGINE).count("SelectedColonyTile") shouldBe 3
    setupWorld.gameplay(ENGINE).count("PlutoSelected") shouldBe 1
  }

  @Test
  fun setupValidationIsExpressedByPetsRules() {
    shouldThrow<DeadEndException> {
      Engine.newGame(newSetupWorld(options = emptySet()), Canon::assemble)
    }

    val incompleteColonies =
        newSetupWorld(
            options = Canon.Option.DEFAULTS + ColoniesExpansion,
            selectedColonies = setOf(cn("Luna")),
        )
    shouldThrow<RequirementException> { Engine.newGame(incompleteColonies, Canon::assemble) }
  }

  @Test
  fun colonySelectionMustBeCompletedInTheSetupWorld() {
    val setupWorld = newSetupWorld(options = Canon.Option.DEFAULTS + ColoniesExpansion)

    shouldThrow<RequirementException> { Engine.newGame(setupWorld, Canon::assemble) }
  }

  private fun newSetupWorld(
      players: Int = 2,
      options: Set<Canon.Option> = Canon.Option.DEFAULTS,
      selectedColonies: Set<ClassName> = emptySet(),
  ): World =
      Engine.newSetupWorld(
          Canon.setupWorldDefinition(players, options, selectedColonies),
          TEST_CLASS_SYNONYMS,
      )
}
