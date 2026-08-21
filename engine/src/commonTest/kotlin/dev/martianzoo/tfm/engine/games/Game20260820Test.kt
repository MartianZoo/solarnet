package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.GameConfig
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Synthetic Magnet Burst - https://terraforming-mars.herokuapp.com/the-end?id=pa9f45e80d897
class Game20260820Test : CardTrackingFullGameTest() {
  // The archived metadata specifies Hellas, Corporate Era, Venus, Prelude, Prelude 2, drafting,
  // World Government, two players, and the following full-random milestone and award pools.
  // Thawer and Briber are unsupported; Builder7 and Sponsor are same-role setup substitutes.
  // PromoCardPack supplies Merger but was not enabled in the archive.
  override val config =
      GameConfig(
          """
          HellasMapOption
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, MilestonesAwardsExpansion, PromoCardPack

          Energizer, Builder7, Generalist, Diversifier, Terraformer29, Sponsor
          Scientist, Landscaper, Founder, Contractor, Forecaster, Incorporator
          """,
          "Pink",
          "Green",
      )
  override val inputOnlySynonyms = emptyList<Pair<String, String>>()

  @Test
  fun game20260820() {
    TfmWorkflow.Auto(game).launch()

    val pink = p1
    val green = p2

    engine.assertCounts(1 to "Generation")

    // Pink rejected CrediCor and EcoTec; Venus Contract and Focused Organization; and Windmills,
    // Open City, and Energy Saving.
    // Pink played Tharsis Republic
    pink
        .playCorp(TharsisRepublic) {
          // Pink kept 7 project cards
          buyCards(
              Mine,
              RoboticWorkforce,
              GreatEscarpmentConsortium,
              DesignedMicroorganisms,
              ImmigrantCity,
              Sponsors,
              OrbitalReflectors,
          )
        }
        .expect("7 ProjectCard")

    // Green rejected Ecoline and Morning Star Inc.; Project Eden and Floating Trade Hub; and
    // Magnetic Field Dome, Heather, Insulation, Cartel, Caretaker Contract, and Protected Habitats.
    // Green played Nirgal Enterprises
    // Green gained 1 steel production
    // Green gained 1 plant production
    // Green gained 1 energy production
    // Green kept 4 project cards
    green
        .playCorp(NirgalEnterprises) {
          buyCards(MineralDeposit, AquiferPumping, TectonicStressPower, RotatorImpacts)
        }
        .expect("PROD[Steel, Plant, Energy], 4 ProjectCard")

    pink.turn {
      // Pink played Recession
      // Pink gained 10 M€
      // Green lost 1 M€ production because of Pink
      // Green lost 5 M€ because of Pink
      playPrelude(Recession).expect("10 Megacredit, PROD[-Megacredit<Green>], -5 Megacredit<Green>")
      // Pink played Merger
      playPrelude(Merger) {
            // Pink played Sagitta Frontier Services
            doTask("PlayCard<Class<CorporationCard>, Class<$SagittaFrontierServices>>")
            // Pink gained 2 M€ production
            // Pink gained 1 energy production
            // Pink drew Micro-Mills
            draw(MicroMills)
          }
          .expect("PROD[2 Megacredit, Energy], ProjectCard, -7 Megacredit")
    }

    green.turn {
      // Green played Nobel Prize
      playPrelude(NobelPrize) {
            // Green gained 5 M€
            // Green drew Insects,Stratopolis
            draw(Insects, Stratopolis)
          }
          .expect("5 Megacredit, 2 ProjectCard")
      // Green played Suitable Infrastructure
      // Green gained 5 steel
      playPrelude(SuitableInfrastructure).expect("5 Steel")
    }

    pink.turn {
      // Pink took the first action of Tharsis Republic corporation
      stdAction("HandleMandates") {
            // Pink placed city tile at 61
            doTask("CityTile<Hellas_9_7>")
            // Pink placed ocean tile at 34
            doTask("OceanTile<Hellas_5_6>")
            // Pink drew Industrial Center
            draw(IndustrialCenter)
            // Pink gained 3 M€
            // Pink gained 1 M€ production
          }
          .expect("-3 Megacredit, PROD[Megacredit]")
      // Pink played Micro-Mills
      // Pink gained 1 heat production
      // Pink gained 4 M€ for playing Micro-Mills, which has no tags.
      playProject(MicroMills, 3).expect("PROD[Heat], Megacredit")
    }

    green.turn {
      // Green played Mineral Deposit
      // Green gained 5 steel
      playProject(MineralDeposit, 5).expect("5 Steel")
    }

    pink.turn {
      // Pink played Mine
      // Pink gained 1 steel production
      // Pink gained 1 M€ for playing Mine, which has exactly 1 tag.
      playProject(Mine, 4).expect("PROD[Steel], -3 Megacredit")
      // Pink played Great Escarpment Consortium
      playProject(GreatEscarpmentConsortium, 6) {
            // Pink stole 1 steel production from Green
            doTask("PROD[-Steel<Green>]")
            // Pink gained 1 steel production
            // Pink gained 4 M€ for playing Great Escarpment Consortium, which has no tags.
          }
          .expect("PROD[Steel], -2 Megacredit")
    }

    green.turn {
      // Green played Aquifer Pumping
      // The later Aquifer Pumping action requires four of Green's ten steel to remain available.
      intentionalUnderpay()
      playProject(AquiferPumping, 6, steel = 6)
    }

    pink.turn {
      // Pink played Sponsors
      // Pink gained 2 M€ production
      // Pink gained 1 M€ for playing Sponsors, which has exactly 1 tag.
      playProject(Sponsors, 6).expect("PROD[2 Megacredit], -5 Megacredit")
      // Pink played Robotic Workforce
      playProject(RoboticWorkforce, 9) {
            // Pink copied Mine production with Robotic Workforce
            doTask("CopyProductionBox<$Mine>")
            // Pink gained 1 steel production
          }
          .expect("PROD[Steel], -8 Megacredit, 0 Mine")
    }

    green.turn {
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
            pay(steel = 4)
            // Green placed ocean tile at 46
            doTask("OceanTile<Hellas_7_3>")
            // Green gained 2 titanium
          }
          .expect("2 Titanium")
    }
    pink.pass()
    green.pass()
    // Pink acted as World Government and placed an ocean at 08.
    pink.doTask("OceanTile<Hellas_2_1>! BY Engine")

    // Generation 2
    // Green bought Giant Ice Asteroid, Investment Loan, Earth Office, Power Supply Consortium.
    green.buyCards(GiantIceAsteroid, InvestmentLoan, EarthOffice, PowerSupplyConsortium)

    // Screenshot 2026-08-20 at 9.40.13 PM.png was taken after Green's generation 2 purchase and
    // before Pink's.
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 3, venus = 0)
    pink.assertResources(m = 28, s = 3, t = 0, p = 0, e = 1, h = 1)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 0, e = 1, h = 1)
    green.assertResources(m = 15, s = 0, t = 2, p = 1, e = 1, h = 0)
    green.assertProduction(m = -1, s = 0, t = 0, p = 1, e = 1, h = 0)
    pink.cardsInHand.size shouldBe 4
    green.cardsInHand.size shouldBe 8

    // Pink bought Invention Contest, Decomposers, Colonizer Training Camp, Imported Hydrogen.
    pink.buyCards(InventionContest, Decomposers, ColonizerTrainingCamp, ImportedHydrogen)

    green.turn {
      // Green played Earth Office
      playProject(EarthOffice, 1)
    }
    pink.turn {
      // Pink played Designed Microorganisms
      // Pink gained 2 plant production
      playProject(DesignedMicroorganisms, 16).expect("PROD[2 Plant]")
    }
    green.turn {
      // Power Supply Consortium requires Nobel Prize's wild icon to count as Green's second power
      // tag.
      doTask("PowerTag<WildTagUse<$NobelPrize>>")
      // Green played Power Supply Consortium
      playProject(PowerSupplyConsortium, 5) {
            // Green stole 1 energy production from Pink
            doTask("PROD[-Energy<Pink>]")
            // Green gained 2 M€ from Suitable Infrastructure
          }
          .expect("-3 Megacredit")
    }
    pink.pass()
    green.turn {
      // Green played Investment Loan
      // Green lost 1 M€ production
      // Green gained 10 M€
      playProject(InvestmentLoan, 0).expect("PROD[-Megacredit], 10 Megacredit")
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
            pay(8)
            // Green placed ocean tile at 35
            doTask("OceanTile<Hellas_5_7>")
            // Green gained 3 heat
            // Green gained 2 M€ from 1 ocean.
          }
          .expect("3 Heat, -6 Megacredit")
    }
    green.turn {
      // Green played Rotator Impacts
      playProject(RotatorImpacts, titanium = 2)
      // Green used Rotator Impacts action and added 1 Asteroid.
      cardAction1(RotatorImpacts) {
            pay(6)
          }
          .expect("Asteroid<$RotatorImpacts>")
    }
    green.pass()
    // Green acted as World Government and increased oxygen.
    green.doTask("OxygenStep! BY Engine")

    // Generation 3
    // Green bought Mars University, Neutralizer Factory, and Great Dam.
    green.buyCards(MarsUniversity, NeutralizerFactory, GreatDamPromo)
    // Pink bought Hackers and ArchaeBacteria.
    pink.buyCards(Hackers, Archaebacteria)

    // Screenshot 2026-08-20 at 9.44.22 PM.png was taken after generation 3 purchases and before
    // the first action.
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 4, venus = 0)
    pink.assertResources(m = 20, s = 6, t = 0, p = 2, e = 0, h = 3)
    pink.assertProduction(m = 5, s = 3, t = 0, p = 2, e = 0, h = 1)
    green.assertResources(m = 20, s = 0, t = 0, p = 2, e = 2, h = 4)
    green.assertProduction(m = -2, s = 0, t = 0, p = 1, e = 2, h = 0)
    pink.cardsInHand.size shouldBe 9
    green.cardsInHand.size shouldBe 7

    pink.turn {
      // Pink played Invention Contest and drew Ishtar Mining.
      playProject(InventionContest, 2) { draw(IshtarMining) }.expect("0 ProjectCard")
    }
    green.turn {
      // Green played Mars University, discarded Stratopolis, and drew House Printing.
      playProject(MarsUniversity, 8, steel = 0) {
        discard(Stratopolis)
        draw(HousePrinting)
        doTask("-ProjectCard")
      }
    }
    pink.turn {
      // Pink played ArchaeBacteria
      // Pink gained 1 plant production
      // Pink gained 1 M€ for playing ArchaeBacteria, which has exactly 1 tag.
      playProject(Archaebacteria, 6).expect("PROD[Plant], -5 Megacredit")
    }
    green.turn {
      // Green used Rotator Impacts action and removed 1 Asteroid.
      cardAction2(RotatorImpacts).expect("-Asteroid<$RotatorImpacts>")
    }
    pink.pass()
    green.turn {
      // Green used Aquifer Pumping action
      cardAction1(AquiferPumping) {
            pay(8)
            // Green placed ocean tile at 26
            doTask("OceanTile<Hellas_4_6>")
            // Green gained 1 plant and 4 M€ from two oceans.
          }
          .expect("Plant, -4 Megacredit")
    }
    green.pass()
    // Pink acted as World Government and increased Venus.
    pink.doTask("VenusStep! BY Engine")

    // Generation 4
    pink.buyCards(TitaniumMine, EcologicalZone)
    green.buyCards(MediaGroup, Ironworks, Research, MirandaResort)

    green.turn {
      // Green played Media Group.
      playProject(MediaGroup, 3)
    }
    pink.turn {
      // Pink played Orbital Reflectors, gained two heat production, and drew Water Splitting Plant.
      playProject(OrbitalReflectors, 26) { draw(WaterSplittingPlant) }
          .expect("PROD[2 Heat], 0 ProjectCard")
      // Pink played Ishtar Mining and gained one titanium production.
      playProject(IshtarMining, 5).expect("PROD[Titanium], -4 Megacredit")
    }
    green.turn {
      // Green played Miranda Resort and gained three M€ production.
      // Miranda Resort counted Nobel Prize's wild icon as Green's third earth tag.
      doTask("EarthTag<WildTagUse<$NobelPrize>>")
      playProject(MirandaResort, 12).expect("PROD[3 Megacredit], -10 Megacredit")
      // Nobel Prize's wild icon counts as Green's eighth distinct tag.
      doTask("MicrobeTag<WildTagUse<$NobelPrize>>")
      // Green used Nirgal Enterprises' free milestone action to claim Diversifier.
      stdAction("ClaimMilestoneSA") { doTask("Diversifier") }.expect("Milestone")
    }
    pink.turn {
      // Pink played Titanium Mine and gained one titanium production.
      playProject(TitaniumMine, 1, steel = 3).expect("PROD[Titanium], 0 Megacredit")
    }
    green.pass()
    pink.pass()
    // Green acted as World Government and increased oxygen.
    green.doTask("OxygenStep! BY Engine")

    // Screenshot 2026-08-20 at 9.53.11 PM.png was taken in generation 5 drafting, before purchases.
    assertSidebar(gen = 5, temp = -30, oxygen = 2, oceans = 5, venus = 8)
    pink.assertResources(m = 31, s = 9, t = 2, p = 8, e = 0, h = 7)
    pink.assertProduction(m = 5, s = 3, t = 2, p = 3, e = 0, h = 3)
    green.assertResources(m = 30, s = 0, t = 0, p = 5, e = 2, h = 8)
    green.assertProduction(m = 1, s = 0, t = 0, p = 1, e = 2, h = 0)
    pink.cardsInHand.size shouldBe 8
    green.cardsInHand.size shouldBe 8
  }
}
