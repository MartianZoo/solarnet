package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.canon.Canon
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import kotlin.test.Test

/** Which rules and content exist in games with different options. */
internal class ClassDefinitionBoundaryTest {
  @Test
  internal fun `a map defines its areas and an area can be selected on its own`() {
    val standardGame = classesInGame("")
    val hellasGame = classesInGame("HellasMap")
    val gameWithOneHellasArea = classesInGame("Hellas_1_1")

    standardGame.shouldNotContain(cn("Hellas_1_1"))
    hellasGame.shouldContain(cn("Hellas_1_1"))
    gameWithOneHellasArea.shouldContain(cn("Hellas_1_1"))
  }

  @Test
  internal fun `Amazonis changes the rules for the Mars and Venus tracks`() {
    val standardGame = classesInGame("")
    val venusGame = classesInGame("VenusNextExpansion")
    val venusOnAmazonis = classesInGame("VenusNextExpansion, AmazonisMap")

    standardGame.shouldContain(cn("StandardGpTrackRules"))
    standardGame.shouldNotContain(cn("ExtendedGlobalParametersRule"))
    venusGame.shouldContain(cn("StandardVenusTrackRules"))
    venusGame.shouldNotContain(cn("ExtendedVenusTrackRules"))
    venusOnAmazonis.shouldNotContain(cn("StandardGpTrackRules"))
    venusOnAmazonis.shouldContain(cn("ExtendedGlobalParametersRule"))
    venusOnAmazonis.shouldNotContain(cn("StandardVenusTrackRules"))
    venusOnAmazonis.shouldContain(cn("ExtendedVenusTrackRules"))
  }

  @Test
  internal fun `solo rules and player seats follow the number of players`() {
    val soloGame = classesInGame("", players = 1)
    val twoPlayerGame = classesInGame("", players = 2)

    soloGame.shouldContain(cn("SoloOpponent"))
    soloGame.shouldNotContain(cn("ClaimMilestoneAction"))
    soloGame.shouldNotContain(cn("Player2"))
    twoPlayerGame.shouldNotContain(cn("SoloOpponent"))
    twoPlayerGame.shouldContain(cn("ClaimMilestoneAction"))
    twoPlayerGame.shouldContain(cn("Player2"))
  }

  @Test
  internal fun `the TR 63 solo objective replaces the standard objective`() {
    val standardSolo = classesInGame("", players = 1)
    val tr63Solo = classesInGame("Tr63SoloObjective", players = 1)

    standardSolo.shouldContain(cn("StandardSoloObjective"))
    standardSolo.shouldNotContain(cn("Tr63SoloObjective"))
    tr63Solo.shouldNotContain(cn("StandardSoloObjective"))
    tr63Solo.shouldContain(cn("Tr63SoloObjective"))
  }

  @Test
  internal fun `Quick Start replaces Corporate Era rules when Corporate Era is excluded`() {
    val corporateEraGame = classesInGame("")
    val quickStartGame = classesInGame("-CorporateEraExpansion")

    corporateEraGame.shouldContain(cn("CopyProductionBox"))
    corporateEraGame.shouldNotContain(cn("QuickStartVariant"))
    quickStartGame.shouldNotContain(cn("CopyProductionBox"))
    quickStartGame.shouldContain(cn("QuickStartVariant"))
  }

  @Test
  internal fun `Prelude 2 cards needing Venus wait for Venus Next`() {
    val standardGame = classesInGame("")
    val prelude2Game = classesInGame("Prelude2CardPack")
    val prelude2WithVenus = classesInGame("Prelude2CardPack, VenusNextExpansion")

    standardGame.shouldNotContain(cn("Ecotec"))
    prelude2Game.shouldContain(cn("Ecotec"))
    prelude2Game.shouldNotContain(cn("AtmosphericEnhancers"))
    prelude2WithVenus.shouldContain(cn("AtmosphericEnhancers"))
  }

  @Test
  internal fun `a Prelude card brings its card back without the Prelude expansion`() {
    val gameWithPrelude = classesInGame("AcquiredSpaceAgency")
    val gameWithCorporation = classesInGame("CheungShingMars")

    gameWithPrelude.shouldContain(cn("PreludeCard"))
    gameWithCorporation.shouldNotContain(cn("PreludeCard"))
  }

  @Test
  internal fun `the beginner variant includes beginner corporations and their card back`() {
    val standardGame = classesInGame("")
    val beginnerGame = classesInGame("BeginnerVariant")

    standardGame.shouldNotContain(cn("BeginnerCorporation1"))
    standardGame.shouldNotContain(cn("BeginnerCorporationCard"))
    beginnerGame.shouldContain(cn("BeginnerCorporation1"))
    beginnerGame.shouldContain(cn("BeginnerCorporationCard"))
  }

  @Test
  internal fun `the promo pack selects the new Deimos Down instead of the original`() {
    val standardGame = classesInGame("")
    val promoGame = classesInGame("PromoCardPack")

    standardGame.shouldContain(cn("DeimosDown"))
    standardGame.shouldNotContain(cn("DeimosDownPromo"))
    promoGame.shouldNotContain(cn("DeimosDown"))
    promoGame.shouldContain(cn("DeimosDownPromo"))
  }

  @Test
  internal fun `Refugee Camps brings its Camp resource unless the card is excluded`() {
    val coloniesGame = classesInGame("ColoniesExpansion")
    val withoutRefugeeCamps = classesInGame("ColoniesExpansion, -RefugeeCamps")
    val gameSelectingCampDirectly = classesInGame("Camp")

    coloniesGame.shouldContain(cn("Camp"))
    withoutRefugeeCamps.shouldNotContain(cn("Camp"))
    gameSelectingCampDirectly.shouldContain(cn("Camp"))
  }

  @Test
  internal fun `a map milestone needing Turmoil waits for both options`() {
    val amazonisGame = classesInGame("AmazonisMap")
    val turmoilGame = classesInGame("TurmoilExpansion")
    val amazonisWithTurmoil = classesInGame("AmazonisMap, TurmoilExpansion")

    amazonisGame.shouldNotContain(cn("Lobbyist"))
    turmoilGame.shouldNotContain(cn("Lobbyist"))
    amazonisWithTurmoil.shouldContain(cn("Lobbyist"))
  }

  @Test
  internal fun `choosing a colony tile enables its solo setup`() {
    val soloWithoutTileChoice = classesInGame("ColoniesExpansion", players = 1)
    val soloWithTileChoice = classesInGame("ColoniesExpansion, Callisto", players = 1)
    val multiplayerWithTileChoice = classesInGame("ColoniesExpansion, Callisto", players = 2)

    soloWithoutTileChoice.shouldNotContain(cn("SelectedColonyTile"))
    soloWithoutTileChoice.shouldNotContain(cn("SoloColoniesSetup"))
    soloWithTileChoice.shouldContain(cn("SelectedColonyTile"))
    soloWithTileChoice.shouldContain(cn("SoloColoniesSetup"))
    multiplayerWithTileChoice.shouldNotContain(cn("SoloColoniesSetup"))
  }

  @Test
  internal fun `an extra milestone is absent until chosen`() {
    val standardGame = classesInGame("")
    val gameWithBriber = classesInGame("Briber")

    standardGame.shouldNotContain(cn("Briber"))
    gameWithBriber.shouldContain(cn("Briber"))
  }

  private fun classesInGame(config: String, players: Int = 2): Set<ClassName> =
      Canon.gamePremise(GameConfig(config, *(1..players).map { "Player$it" }.toTypedArray()))
          .classTable
          .allClassNames
}
