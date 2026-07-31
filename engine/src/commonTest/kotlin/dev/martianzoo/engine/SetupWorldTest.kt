package dev.martianzoo.engine

import dev.martianzoo.api.Exceptions.DeadEndException
import dev.martianzoo.api.Exceptions.LimitsException
import dev.martianzoo.data.Actor.Companion.ENGINE
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.tfm.api.tfmRuleset
import dev.martianzoo.tfm.canon.Canon
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
    reader.count("TharsisMapOption") shouldBe 1
    setupWorld.classTable.allClassNamesAndIds.shouldNotContain(cn("TerraformRating"))
    setupWorld.classTable.allClassNamesAndIds.shouldNotContain(cn("Owner"))
    setupWorld.isIdle() shouldBe true
  }

  @Test
  fun setupWorldCollectsPlayerAndExpansionOptions() {
    val setupWorld = newSetupWorld().gameplay(ENGINE).godMode()

    setupWorld.manual("5 Player")
    setupWorld.manual("PreludeExpansion, VenusNextExpansion, ColoniesExpansion, PromoCardPack")
    setupWorld.manual("TitanSelected")

    setupWorld.count("Player") shouldBe 5
    setupWorld.count("GameOption") shouldBe 6
    setupWorld.count("TitanSelected") shouldBe 1
    shouldThrow<LimitsException> { setupWorld.manual("Player") }
    shouldThrow<LimitsException> { setupWorld.manual("PreludeExpansion") }
  }

  @Test
  fun corporateEraCanBeRemoved() {
    val setupWorld = newSetupWorld().gameplay(ENGINE).godMode()

    setupWorld.manual("-CorporateEraExpansion")
    setupWorld.count("CorporateEraExpansion") shouldBe 0
  }

  @Test
  fun mapCardinalityRequiresAnExplicitReplacement() {
    val setupWorld = newSetupWorld().gameplay(ENGINE).godMode()

    shouldThrow<LimitsException> { setupWorld.manual("ElysiumMapOption") }
    setupWorld.manual("ElysiumMapOption FROM TharsisMapOption")
    setupWorld.count("MarsMapOption") shouldBe 1
    setupWorld.count("TharsisMapOption") shouldBe 0
    setupWorld.count("ElysiumMapOption") shouldBe 1

    setupWorld.manual("TerraCimmeriaMapOption FROM ElysiumMapOption")
    setupWorld.count("MarsMapOption") shouldBe 1
    setupWorld.count("ElysiumMapOption") shouldBe 0
    setupWorld.count("TerraCimmeriaMapOption") shouldBe 1
  }

  @Test
  fun completedSetupWorldCanCreateARealGame() {
    val setupWorld = newSetupWorld()
    with(setupWorld.gameplay(ENGINE).godMode()) {
      manual("2 Player")
      manual("ElysiumMapOption FROM TharsisMapOption")
      manual("PreludeExpansion, PromoCardPack")
    }

    val game = Engine.newGame(setupWorld, Canon::assemble)

    game.reader.tfmRuleset.marsMapDefinitions.single().className shouldBe cn("Elysium")
    game.gameplay(ENGINE).count("TerraformingMars") shouldBe 1
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
    with(setupWorld.gameplay(ENGINE).godMode()) {
      manual("2 Player")
      addTasks("PreludeExpansion")
    }

    shouldThrow<IllegalArgumentException> { Engine.newGame(setupWorld, Canon::assemble) }
  }

  @Test
  fun setupComponentsAreCopiedIntoTheGameWorld() {
    val setupWorld = newSetupWorld()
    with(setupWorld.gameplay(ENGINE).godMode()) {
      manual("Player")
      manual("HellasMapOption FROM TharsisMapOption")
      manual("-CorporateEraExpansion")
      manual("SoloMode, ColoniesExpansion")
      manual("CeresSelected, PlutoSelected, TitanSelected")
    }

    val game = Engine.newGame(setupWorld, Canon::assemble)

    game.gameplay(ENGINE).count("Player") shouldBe 1
    game.gameplay(ENGINE).count("SelectedColonyTile") shouldBe 3
    setupWorld.gameplay(ENGINE).count("SelectedColonyTile") shouldBe 3
    setupWorld.gameplay(ENGINE).count("PlutoSelected") shouldBe 1
  }

  @Test
  fun setupValidationIsExpressedByPetsRules() {
    shouldThrow<DeadEndException> { Engine.newGame(newSetupWorld(), Canon::assemble) }

    val multiplayerSolo = newSetupWorld()
    multiplayerSolo.gameplay(ENGINE).godMode().manual("2 Player, SoloMode")
    shouldThrow<DeadEndException> { Engine.newGame(multiplayerSolo, Canon::assemble) }

    val incompleteColonies = newSetupWorld()
    incompleteColonies
        .gameplay(ENGINE)
        .godMode()
        .manual("2 Player, ColoniesExpansion, LunaSelected")
    shouldThrow<DeadEndException> { Engine.newGame(incompleteColonies, Canon::assemble) }
  }

  @Test
  fun colonySelectionCanBeDeferredByASetupInstruction() {
    val setupWorld = newSetupWorld()
    setupWorld
        .gameplay(ENGINE)
        .godMode()
        .manual("2 Player, ColoniesExpansion, DeferredColonySelection")

    val game = Engine.newGame(setupWorld, Canon::assemble)

    game.gameplay(ENGINE).count("DeferredColonySelection") shouldBe 1
  }

  private fun newSetupWorld(): World = Engine.newSetupWorld(Canon.setupWorldDefinition)
}
