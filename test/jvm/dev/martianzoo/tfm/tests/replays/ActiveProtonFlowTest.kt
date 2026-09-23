package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.CONCRETE
import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

// Partial database replay through generation 2: Active Proton Flow (gadeac80e6446)
// Source: _local/replays/Game20260920/Heroku-gadeac80e6446/
// http://newazure.local:8080/the-end?id=pf8b6b214192d
internal class ActiveProtonFlowTest :
    CardTrackingFullGameTest(requireEveryProjectCardChangeNamed = true) {
  override val config =
      GameConfig(
          """
          TharsisMap, BeginnerVariant, QuickStartVariant

          Sponsor, Producer, Engineer, Fundraiser, Spacefarer
          Forecaster, EstateDealer, SpaceBaron, Suburbian, Celebrity
          """,
          "KB",
          "ER",
      )

  private val green
    get() = p1

  private val pink
    get() = p2

  @Test
  internal fun activeProtonFlowThroughGeneration2() {
    TfmWorkflow.Automatic(agents).launch()

    green.autoExecPolicy = NONE
    pink.autoExecPolicy = NONE
    pink.doTask("StandardCorporationCard<Selecting> / CorporationOption")
    pink.doTask("10 ProjectCard")
    pink.doTask("NewTurn")
    pink.doTask("StandardCorporationCard<Hand FROM Selecting>")
    pink.doTask("10 ProjectCard<Selecting FROM Hand>")
    pink.doTask("-5 ProjectCard<Selecting>")
    pink.doTask("5 ProjectCard<Hand FROM Selecting>")
    pink.discardUnselectedProjectCards(
        ReleaseOfInertGases,
        RadChemFactory,
        RegolithEaters,
        Greenhouses,
        AsteroidMining,
    )
    green.doTask("BeginnerCorporationCard")
    green.doTask("10 ProjectCard")
    green.doTask("NewTurn")
    green.autoExecPolicy = CONCRETE
    pink.autoExecPolicy = CONCRETE

    generation1()
    generation2()

    // Close generation 2's card ledger before the next Research deal begins.
    assertCardTrackingComplete()
    green.pass()

    // Database save 37: generation 3 Research, immediately after generation 2 production.
    green.assertResources(m = 33, s = 2, t = 2, p = 2, e = 5, h = 9)
    green.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 5, h = 1)
    green.assertCounts(21 to "TerraformRating", 5 to "CardFront OR PlayedEvent")
    green.cardsHand shouldBe
        setOf(
            GreatDam,
            IceCapMelting,
            Predators,
            NuclearPower,
            EquatorialMagnetizer,
            PermafrostExtraction,
            OreProcessor,
            ImportedHydrogen,
            ResearchOutpost,
            MagneticFieldGenerators,
            ImmigrantCity,
        )

    pink.assertResources(m = 20, s = 8, t = 4, p = 7, e = 2, h = 7)
    pink.assertProduction(m = -1, s = 3, t = 1, p = 1, e = 2, h = 4)
    pink.assertCounts(21 to "TerraformRating", 4 to "CardFront OR PlayedEvent")
    pink.cardsHand shouldBe setOf(Flooding, LavaFlows, CupolaCity, HeatTrappers, NitrophilicMoss)

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 2)
    admin.assertCounts(2 to "Tile")
    checkHandSizes()
  }

  private fun generation1() {
    green.inTurn {
      doTask("10 ProjectCard<Selecting FROM Hand>")
      doTask("PlayCard<Class<BeginnerCorporationCard>, Class<BeginnerCorporation1>, Hand>")
      green.pay()
      doTask("42 MC")
    }
    green.draw(
        GreatDam,
        GeothermalPower,
        IceCapMelting,
        OptimalAerobraking,
        Predators,
        NuclearPower,
        EquatorialMagnetizer,
        ConvoyFromEuropa,
        SpaceMirrors,
        PermafrostExtraction,
    )
    green.autoExecPolicy = EAGER
    pink.autoExecPolicy = EAGER
    pink.playCorp(MiningGuild) {
      buyCards(PowerPlant, Flooding, ArcticAlgae, LavaFlows, CupolaCity)
    }

    green.turn { playProject(SpaceMirrors, 3) }
    pink.turn { playProject(ArcticAlgae, 12) }
    green.turn {
      playProject(OptimalAerobraking, 7)
      playProject(ConvoyFromEuropa, 15) {
            draw(OreProcessor)
            placeTile(2, 6)
            draw(ImportedHydrogen, ResearchOutpost)
          }
          .expect("-12 MC, 3 Heat")
    }
    pink.turn { playProject(PowerPlant, steel = 2) }
    green.turn { playProject(GeothermalPower, 11) }
    pink.pass()
    green.turn {
      cardAction1(SpaceMirrors)
      pass()
    }
  }

  private fun generation2() {
    pink.buyCards(HeatTrappers, NitrophilicMoss, BlackPolarDust)
    pink.discardUnselectedProjectCards(Farming)
    green.buyCards(MagneticFieldGenerators, ImmigrantCity)
    green.discardUnselectedProjectCards(Ants, LargeConvoy)

    // Database save 28: immediately after both generation 2 Research purchases.
    green.assertResources(m = 18, s = 1, t = 1, p = 1, e = 4, h = 4)
    green.assertProduction(m = 1, s = 1, t = 1, p = 1, e = 4, h = 1)
    green.assertCounts(21 to "TerraformRating", 11 to "ProjectCard")
    pink.assertResources(m = 15, s = 5, t = 1, p = 4, e = 2, h = 1)
    pink.assertProduction(m = 1, s = 2, t = 1, p = 1, e = 2, h = 1)
    pink.assertCounts(20 to "TerraformRating", 6 to "ProjectCard")
    assertSidebar(gen = 2, temp = -30, oxygen = 0, oceans = 1)
    checkHandSizes()

    pink.turn { playProject(BlackPolarDust, 15) { placeTile(9, 9) } }
    green.turn { cardAction1(SpaceMirrors) }
    pink.pass()
  }
}
