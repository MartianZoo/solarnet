package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Partial replay of the 2025 WSBG Terraforming Mars Ring Final.
 *
 * Sources:
 * - Primary recording: https://www.youtube.com/watch?v=31VxDkthyOk
 * - Corroborating commentary: https://www.youtube.com/watch?v=RbTR59QRVEc
 * - Local evidence inventory: _local/replays/Game20250919/sources.md
 */
internal class Wsbg2025Test : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          ElysiumMap
          PreludeExpansion
          """,
          "Stanley",
          "Jacopo",
          "Jon",
          "Charlie",
      )

  @Test
  internal fun wsbg2025() {
    TfmWorkflow.Auto(game).launch()

    val stanley = game.tfm(Player.PLAYER1)
    val jacopo = game.tfm(Player.PLAYER2)
    val jon = game.tfm(Player.PLAYER3)
    val charlie =
        game.tfm(game.actors.filterIsInstance<Player>()[3]).requireExplicitPaymentChoices()

    stanley.playCorp(CrediCor, 6)
    jacopo.playCorp(ValleyTrust, 7)
    jon.playCorp(RobinsonIndustries, 8)
    charlie.playCorp(CheungShingMars, 7)

    stanley.turn {
      playPrelude(UnmiContractor)
      playPrelude(SelfSufficientSettlement) { placeTile(3, 7) }
    }
    jacopo.turn {
      playPrelude(Biofuels)
      playPrelude(ResearchNetwork)
    }
    jon.turn {
      playPrelude(EarlySettlement) { placeTile(8, 8) }
      playPrelude(EcologyExperts) { playProject(Predators, 14) }
    }
    charlie.turn {
      playPrelude(AlliedBank)
      playPrelude(AquiferTurbines) { placeTile(5, 4) }
    }

    stanley.turn {
      playProject(EarthCatapult, 23)
      playProject(ArcticAlgae, 10)
    }
    jacopo.turn {
      stdAction("HandleMandates") { playPrelude(Mohole) }
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Elysium_5_6>") }
    }
    jon.turn {
      cardAction1(RobinsonIndustries) { doTask("PROD[Titanium]") }
      declineSecondAction()
    }
    charlie.turn {
      playProject(GhgFactories, 9)
      declineSecondAction()
    }
    stanley.turn {
      playProject(EnergyTapping, 1) { doTask("PROD[-Energy<Jacopo>]") }
      declineSecondAction()
    }
    jacopo.turn {
      playProject(ResearchCoordination, 4)
      assignWildTag(ResearchCoordination, "PlantTag")
      assignWildTag(ResearchNetwork, "PlantTag")
      declineSecondAction()
    }
    jon.pass()
    charlie.pass()
    stanley.pass()
    jacopo.pass()

    stanley.assertCounts(2 to "Generation", 23 to "TR", 1 to "OwnedTile")
    stanley.assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 0)
    stanley.assertResources(m = 34, s = 0, t = 0, p = 1, e = 1, h = 0)

    jacopo.assertCounts(20 to "TR", 1 to "LandClaimMarker")
    jacopo.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 0, h = 3)
    jacopo.assertResources(m = 32, s = 0, t = 0, p = 3, e = 0, h = 6)

    jon.assertCounts(20 to "TR", 1 to "OwnedTile")
    jon.assertProduction(m = 0, s = 0, t = 1, p = 2, e = 0, h = 0)
    jon.assertResources(m = 25, s = 2, t = 1, p = 2, e = 0, h = 0)

    charlie.assertCounts(21 to "TR", 0 to "OwnedTile")
    charlie.assertProduction(m = 7, s = 0, t = 0, p = 0, e = 1, h = 4)
    charlie.assertResources(m = 42, s = 0, t = 0, p = 2, e = 1, h = 4)

    jacopo.buyCards(2)
    jon.buyCards(3)
    charlie.buyCards(0)
    stanley.buyCards(2)

    jacopo.turn {
      playProject(Archaebacteria, 6)
      assignWildTag(ResearchCoordination, "PlantTag")
      assignWildTag(ResearchNetwork, "PlantTag")
      claimMilestone(cn("Ecologist"))
    }
    jon.turn {
      playProject(IndustrialCenter, 4) { placeTile(4, 8) }
      playProject(IndustrialMicrobes, 6, steel = 3)
    }
    charlie.turn {
      sellPatents(1)
      playProject(TowingAComet, 23) { placeTile(4, 4) }
    }
    stanley.turn {
      playProject(ElectroCatapult, 15)
      cardAction1(ElectroCatapult)
    }

    jacopo.pass()
    jon.turn { cardAction1(RobinsonIndustries) { doTask("PROD[Heat]") } }

    // 00:38:00-00:38:33: after Towing a Comet, Charlie visibly has 20 MC and receives the 2 MC
    // ocean-adjacency rebate needed for the Cheung Shing Mars-discounted Domed Crater.
    charlie.turn { playProject(DomedCrater, 22) { placeTile(5, 3) } }

    stanley.pass()
    jon.pass()
    charlie.pass()

    // The generation-three opening view shows 6 MC carried into production and 40 MC afterward.
    // The reconstructed ordinary actions currently account for only 35 MC at this point.
    charlie.exMachina("5 MC")

    // Engine checkpoint from the currently reconstructed chronology; no readable source ledger is
    // available here yet.
    stanley.assertCounts(3 to "Generation", 23 to "TR", 1 to "OwnedTile")
    stanley.assertProduction(m = 2, s = 0, t = 0, p = 0, e = 0, h = 0)
    stanley.assertResources(m = 45, s = 0, t = 0, p = 2, e = 0, h = 1)

    jacopo.assertCounts(20 to "TR", 1 to "LandClaimMarker")
    jacopo.assertProduction(m = 1, s = 0, t = 0, p = 2, e = 0, h = 3)
    jacopo.assertResources(m = 33, s = 0, t = 0, p = 5, e = 0, h = 9)

    jon.assertCounts(20 to "TR", 2 to "OwnedTile")
    jon.assertProduction(m = 0, s = 1, t = 1, p = 2, e = 1, h = 1)
    jon.assertResources(m = 22, s = 1, t = 2, p = 5, e = 1, h = 1)

    charlie.assertCounts(23 to "TR", 1 to "OwnedTile")
    charlie.assertProduction(m = 10, s = 0, t = 0, p = 0, e = 0, h = 4)
    charlie.assertResources(m = 40, s = 0, t = 0, p = 11, e = 0, h = 9)

    jacopo.buyCards(3)
    jon.buyCards(0)
    charlie.buyCards(0)
    stanley.buyCards(3)

    // The visible generation-three sequence requires more cash than the partial ledger gives Jon.
    // Mining Rights and its titanium production are independently visible, but the ordinary source
    // of this temporary 15 MC correction remains unresolved.
    jon.exMachina("15 MC")
    // Jon's photographed board supports Generalist, while the known cards account for only five
    // positive production types. Preserve the observed milestone while its sixth source is found.
    jon.exMachina("PROD[MC]")
    // The two oceans, Imported Hydrogen, Arctic Algae, Electro Catapult, greenery conversion, and
    // Nitrophilic Moss leave a two-plant gap. Keep it explicit while the omitted source is sought.
    stanley.exMachina("2 Plant")
    // Asteroid Mining Consortium visibly removes Stanley's titanium production. His later +2 from
    // Asteroid Mining then reconciles to the observed production of 2, so one earlier source is
    // missing from the current chronology.
    stanley.exMachina("PROD[Titanium]")

    jon.turn {
      claimMilestone(cn("Generalist"))
      playProject(MartianSurvey, 9)
    }
    charlie.turn {
      claimMilestone(cn("Specialist"))
      convertHeat()
    }
    stanley.turn {
      cardAction1(ElectroCatapult)
      playProject(OptimalAerobraking, 5)
    }
    jacopo.turn {
      sellPatents(1)
      stdProject("CitySP") { placeTile(5, 6) }
    }
    jon.turn {
      playProject(MiningRights, 7, steel = 1) { placeTile(5, 9) }
      playProject(AsteroidMiningConsortium, 13) { doTask("PROD[-Titanium<Stanley>]") }
    }
    charlie.turn { playProject(LunarBeam, 13) }
    stanley.turn {
      playProject(ConvoyFromEuropa, 13) { placeTile(3, 6) }
      playProject(ImportedHydrogen, 14) {
        doTask("3 Plant")
        placeTile(2, 5)
      }
    }
    jacopo.turn { convertPlants { placeTile(5, 5) } }
    jon.turn { playProject(LagrangeObservatory, 0, titanium = 3) }
    charlie.pass()
    stanley.turn {
      convertPlants { placeTile(2, 6) }
      playProject(NitrophilicMoss, 6)
    }
    jacopo.pass()
    jon.pass()
    stanley.pass()

    assertEquals(
        """
        |                       1     2     3     4     5     6     7     8     9
        |                      /     /     /     /     /     /     /     /     /
        |
        | 1 -               W     WT    WC    WS    LC
        |
        | 2 -            VT    L     L     W    [O]   [G1]
        |
        | 3 -        VTT    L     LC    L     WP   [O]   [C1]
        |
        | 4 -      LP    LP    LP   [O]    LP    WP    WP   [S3]
        |
        | 5 -  LPP   LPP   [C4]  [O]   [G2]  [C2]  LPP   LPP   [S3]
        |
        | 6 -      LS    LP    LP    LP    LP    LP    LP    L
        |
        | 7 -         LT    LS    L     L     LS    L     L
        |
        | 8 -           LSS    L     L     L    [C3]   L
        |
        | 9 -               LS    L     LC    LC   LSS
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }
}
