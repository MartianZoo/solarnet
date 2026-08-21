package dev.martianzoo.tfm.engine.games

import dev.martianzoo.analysis.Summarizer
import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player
import dev.martianzoo.pets.ast.ClassName
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Live game begun Tue 2026-08-18. Quoted evidence is verbatim from the supplied transcripts. */
class Game20260818Test : AbstractFullGameTest() {
  private val colonyTiles = listOf("Enceladus", "Miranda", "Europa", "Io", "Pluto")

  // "We are playing on the Utopia Planitia board."
  // "We have Preludes. We have Venus, Colonies, Promos, Milestones and Awards expansion."
  // The transcript includes Briber, which Solarnet does not implement; it is not claimed in this
  // partial game, so the executable pool omits it. The transcript says Enceladus twice; the
  // photographed five-tile colony setup has one Enceladus.
  override val config =
      GameConfig(
          """
          UtopiaPlanitiaMapOption
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion, MilestonesAwardsExpansion
          PromoCardPack

          Ecologist, Merchant, Metallurgist, Tactician4, Hoverlord
          Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile
          ${colonyTiles.joinToString()}
          """,
          "Dad",
          "Ellie",
      )

  @Test
  fun game20260818() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val ellie = game.tfm(Player.PLAYER2)

    // board-11-00-18.jpg: initial global state, before either corporation is played.
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
    dad.assertCounts(20 to "TR", 0 to "OwnedTile")
    ellie.assertCounts(20 to "TR", 0 to "OwnedTile")

    // "I'm Point Luna... I get a titanium production." "I'm keeping seven cards."
    // "So I pay 21. I have 17 money remaining."
    dad.playCorp(PointLuna, 7).expect("PROD[T], 17, 8 ProjectCard")

    // "I have Valley Trust. I'm keeping five cards... I have 22 money."
    ellie.playCorp(ValleyTrust, 5).expect("22")

    dad.turn {
      // "I play Biofuels... two plants, a plant production, and an energy production."
      playPrelude(Biofuels).expect("2 P, PROD[P, E]")
      // "And then I play Donation and get 21 money."
      playPrelude(Donation).expect("21")
    }

    ellie.turn {
      // "Supplier... four steel, and two energy production."
      playPrelude(Supplier).expect("4 S, PROD[2 E]")
      // "Martian Industries... six money, one steel production, and one energy production."
      playPrelude(MartianIndustries).expect("6, PROD[S, E]")
    }

    dad.turn {
      // "Dad pays 10 to play Pets... Miranda comes into play, and I get an animal on Pets."
      playProject(Pets, 10).expect("Animal")
    }

    ellie.turn {
      // "I use Valley Trust and I get Double Down, which I play... copy Martian Industries."
      stdAction("HandleMandates") {
            playPrelude(DoubleDown) { doTask("CopyPrelude<$MartianIndustries>") }
          }
          .expect("PROD[S, E], 6")
      // "I spend two money to play Psychrophiles."
      playProject(Psychrophiles, 2)
    }

    dad.turn {
      // "I pay eleven for Aerial Mappers."
      playProject(AerialMappers, 11)
    }

    ellie.turn {
      // "I pay eight for Forced Precipitation."
      playProject(ForcedPrecipitation, 8)
    }

    dad.turn {
      // "I'm going to add a floater to Aerial Mappers."
      cardAction1(AerialMappers) { doTask("Floater<$AerialMappers>") }.expect("Floater")
    }

    ellie.turn {
      // "I spend 21 to play Extractor Balloons. It gets three floaters."
      playProject(ExtractorBalloons, 21).expect("3 Floater")
    }

    dad.pass()
    ellie.turn {
      // "I'm going to add a microbe to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
      // "Remove two floaters from Extractor Balloons and raise Venus."
      cardAction2(ExtractorBalloons).expect("-2 Floater, TR")
      // "Then pay two money to add a floater to Forced Precipitation."
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
      ellie.pass()
    }

    // "Dad uses World Government Terraforming to increase oxygen."
    dad.doTask("OxygenStep! BY Engine").expect("0 TR")

    // board-11-09-02.jpg and both player ledgers: after Generation 1 transition, before Research.
    with(dad) {
      assertProduction(m = 0, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 37, s = 0, t = 1, p = 3, e = 1, h = 0)
      assertCounts(20 to "TR", 5 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(ellie) {
      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 0)
      assertResources(m = 22, s = 6, t = 0, p = 0, e = 4, h = 0)
      assertCounts(21 to "TR", 7 to "CardFront")
      assertCardResources(1 to Psychrophiles, 1 to ForcedPrecipitation, 1 to ExtractorBalloons)
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 0, venus = 2)

    // "I will buy four cards."
    ellie.buyCards(4)
    // Dad first said two, then physically corrected the purchase to three before the action phase.
    dad.buyCards(3)

    ellie.turn {
      // "I'm going to play Mining Rights... row three, column six. I get two cards and a
      // titanium... and increase titanium production."
      playProject(MiningRights, 1, steel = 4) { doTask("MiningRights_SpecialTile<Utopia_3_6>") }
          .expect("ProjectCard, T, PROD[T]")
      // "I play Energy Tapping... Dad loses an energy production."
      playProject(EnergyTapping, 3) { doTask("PROD[-E<Dad>]") }.expect("PROD[E]")
    }

    dad.turn {
      // "I play CEO's Favorite Project... put a floater on Aerial Mappers."
      playProject(CeosFavoriteProject, 1) { doTask("Floater<$AerialMappers>") }.expect("Floater")
    }

    ellie.turn {
      // "I'm going to add a floater to Extractor Balloons."
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    dad.turn {
      // "I remove a floater from Aerial Mappers and draw a card."
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    ellie.turn {
      // "I'm adding a microbe to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    dad.turn { sellPatents(1) }

    ellie.turn {
      // "I pay two and add a floater to Forced Precipitation."
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
    }

    dad.turn {
      // "I pay all 28 money and one titanium to play 16 Psyche."
      playProject(SixteenPsyche, 28, titanium = 1).expect("PROD[2 T]")
    }

    ellie.pass()
    dad.pass()

    // "I'm going to use World Government Terraforming to increase Venus."
    ellie.doTask("VenusStep! BY Engine").expect("0 TR")

    // board-11-17-20.jpg and both player ledgers: after Generation 2 transition, before Research.
    with(dad) {
      assertProduction(m = 0, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 20, s = 0, t = 6, p = 4, e = 0, h = 1)
      assertCounts(20 to "TR", 6 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(ellie) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 5, h = 0)
      assertResources(m = 25, s = 4, t = 2, p = 0, e = 5, h = 4)
      assertCounts(
          21 to "TR",
          9 to "CardFront",
          1 to "OwnedTile",
          1 to "SpecialTile<Utopia_3_6>",
      )
      assertCardResources(2 to Psychrophiles, 2 to ForcedPrecipitation, 2 to ExtractorBalloons)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 0, venus = 4)

    // "I'm going to buy one."
    dad.buyCards(1)
    // Ellie corrected an initial purchase entry: "I'm buying zero cards."
    ellie.buyCards(0)

    dad.turn {
      // "I play Imported Hydrogen... five titanium and one money. Put two animals on Pets."
      // "The ocean goes row four, column one... I get a plant and a card."
      playProject(ImportedHydrogen, 1, titanium = 5) {
            doTask("2 Animal<$Pets>")
            doTask("OceanTile<Utopia_4_1>")
          }
          .expect("2 Animal, P, ProjectCard, TR")
    }

    ellie.turn {
      // "I remove two floaters from Forced Precipitation and increase Venus."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, TR")
      // "I remove two floaters from Extractor Balloons and increase Venus."
      cardAction2(ExtractorBalloons).expect("-2 Floater, TR")
    }

    dad.turn {
      // "I pay eight for Cartel... three money production."
      playProject(Cartel, 8).expect("PROD[3 M]")
    }

    ellie.turn {
      // "I play Colonizer Training Camp, paying four steel."
      playProject(ColonizerTrainingCamp, steel = 4)
    }

    dad.turn { sellPatents(1) }

    ellie.turn {
      sellPatents(1)
      // "I play Beam from a Thorium Asteroid... two titanium and 26 money."
      playProject(BeamFromAThoriumAsteroid, 26, titanium = 2).expect("PROD[3 E, 3 H]")
    }

    dad.turn {
      // "I pay four for Research Coordination."
      playProject(ResearchCoordination, 4)
    }

    ellie.turn {
      // "I'm going to add to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    dad.turn {
      // "I pay four for Venus Governor... two money production."
      doTask("VenusTag<WildTagUse<$ResearchCoordination>>")
      playProject(VenusGovernor, 4).expect("PROD[2 M]")
    }

    ellie.pass()
    dad.turn {
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
      dad.pass()
    }

    // "Dad increases temperature with World Government Terraforming."
    dad.doTask("TemperatureStep! BY Engine").expect("0 TR")

    // Both player ledgers: after Generation 3 transition, before Research.
    with(dad) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 4, p = 6, e = 0, h = 1)
    }
    with(ellie) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 23, s = 2, t = 1, p = 0, e = 8, h = 12)
    }
    assertSidebar(gen = 4, temp = -28, oxygen = 1, oceans = 1, venus = 8)

    ellie.buyCards(2)
    dad.buyCards(2)

    ellie.turn {
      // "I'm trading with Pluto... paying three energy, and I get three cards."
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 3 ProjectCard")
    }

    dad.turn {
      // "Then I play Mars University for eight... discard one and draw one."
      playProject(MarsUniversity, 8) { doTask("-ProjectCard") }
    }

    ellie.turn {
      // "I pay seven for Flooding... row three, column one."
      playProject(Flooding, 7) { doTask("OceanTile<Utopia_3_1>") }.expect("3 P, TR")
      // "I use one Psychrophiles microbe to play Potatoes... lose two plants and get two money
      // production."
      playProject(Potatoes, 0) {
            doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("-Microbe, -2 P, PROD[2 M]")
    }

    dad.turn {
      // "I pay three for Mercurian Alloys."
      doTask("ScienceTag<WildTagUse<$ResearchCoordination>>")
      playProject(MercurianAlloys, 3)
    }

    ellie.turn {
      // "I'm just going to take my turn, and that is add to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    dad.turn {
      // "I pay two money and two titanium for Asteroid Rights... it gets two asteroids."
      playProject(AsteroidRights, 2, titanium = 2).expect("2 Asteroid")
    }

    ellie.turn {
      // "I play Mine, paying two steel."
      playProject(Mine, steel = 2).expect("PROD[S]")
    }

    dad.turn {
      // "I remove an asteroid and get two titanium."
      cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T")
    }

    ellie.turn {
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    dad.turn {
      // "I pay five for Floating Habs."
      doTask("ScienceTag<WildTagUse<$ResearchCoordination>>")
      playProject(FloatingHabs, 5)
    }

    ellie.turn {
      sellPatents(1)
      // "I spend 11 on Nitrate [sic] Reducing Bacteria."
      playProject(NitriteReducingBacteria, 11).expect("3 Microbe")
    }

    dad.turn {
      // "I pay two with Floating Habs and put a floater on Aerial Mappers."
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
    }

    ellie.turn {
      // "I take three microbes off Nitrate Reducing Bacteria and gain a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    dad.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    ellie.pass()
    dad.pass()

    // "I'm going to increase Venus" with World Government Terraforming.
    ellie.doTask("VenusStep! BY Engine").expect("0 TR")

    // board-13-20-01.jpg and both player ledgers: after Generation 4 transition, before Research.
    with(dad) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 7, p = 7, e = 0, h = 1)
      assertCounts(21 to "TR", 13 to "CardFront")
      assertCardResources(3 to Pets, 0 to AerialMappers, 1 to AsteroidRights)
    }
    with(ellie) {
      assertProduction(m = 2, s = 3, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 27, s = 3, t = 2, p = 1, e = 8, h = 20)
      assertCounts(25 to "TR", 14 to "CardFront")
      assertCardResources(
          3 to Psychrophiles,
          0 to NitriteReducingBacteria,
          1 to ForcedPrecipitation,
          1 to ExtractorBalloons,
      )
    }
    engine.assertCounts(1 to "OceanTile<Utopia_4_1>", 1 to "OceanTile<Utopia_3_1>")
    assertSidebar(gen = 5, temp = -28, oxygen = 1, oceans = 2, venus = 10)

    dad.buyCards(1)
    ellie.buyCards(1)

    dad.turn {
      // Point Luna tableau in board-13-46-12.jpg: "Energy Market. Cost me three."
      playProject(EnergyMarket, 3)
    }

    ellie.turn {
      // Valley Trust tableau in board-13-46-12.jpg: "Hydrogen to Venus. I spend two titanium and
      // five real... add two to Forced Precipitation."
      playProject(HydrogenToVenus, 5, titanium = 2) {
            doTask("2 Floater<$ForcedPrecipitation>")
          }
          .expect("2 Floater, TR")
      // User clarification: Ellie played Hermetic Order of Mars. Her ledger combines its six-M€
      // gain with the following twelve-M€ Stratospheric Birds payment.
      playProject(HermeticOrderOfMars, 10).expect("PROD[2 M]")
    }

    dad.turn {
      // "Use my Energy Market to pay six, which gives me three energy, and then use that three
      // energy to send my little boat to Io and take ten heat."
      cardAction1(EnergyMarket) { doTask("-6 THEN 3 Energy") }
      stdAction("TradeSA", 2) { doTask("Trade<Io>") }.expect("-3 E, 10 H")
    }

    ellie.turn {
      // Valley Trust tableau in board-13-46-12.jpg: Stratospheric Birds. The played card consumes
      // one Forced Precipitation floater.
      playProject(StratosphericBirds, 12) {
        doTask("-Floater<$ForcedPrecipitation>")
      }
      // "I spend three energy to trade with Miranda. Three animals on Stratospheric Birds."
      stdAction("TradeSA", 2) {
        doTask("Trade<Miranda>")
        doTask("3 Animal<$StratosphericBirds>")
      }
    }

    dad.turn {
      // "Big Asteroid... all titanium... overspending one... four titanium back, two temperature
      // boops... remove one plant."
      playProject(BigAsteroid, titanium = 7) { doTask("-Plant<Ellie>") }
          .expect("-3 T, 2 TemperatureStep, 2 TR, PROD[H]")
    }

    ellie.turn {
      // Ellie's ledger records two eight-heat conversions after Big Asteroid.
      convertHeat().expect("-8 H, TemperatureStep, TR")
      convertHeat().expect("-8 H, TemperatureStep, PROD[H], TR")
    }

    dad.turn {
      // "Lunar Mining. It costs me 11... six Earth tags... six titanium production."
      doTask("EarthTag<WildTagUse<$ResearchCoordination>>")
      playProject(LunarMining, 11).expect("PROD[3 T]")
    }

    ellie.turn { cardAction1(StratosphericBirds).expect("Animal") }

    dad.turn {
      cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T")
    }

    ellie.turn {
      cardAction1(ForcedPrecipitation).expect("-2, Floater")
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    dad.turn {
      // "Luna Metropolis... five titanium and one real money... seven money production."
      doTask("EarthTag<WildTagUse<$ResearchCoordination>>")
      playProject(LunaMetropolis, 1, titanium = 5).expect("PROD[7 M], Animal")
    }

    ellie.turn {
      cardAction1(Psychrophiles).expect("Microbe")
      cardAction1(NitriteReducingBacteria).expect("Microbe")
    }

    dad.turn {
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    ellie.pass()
    dad.turn {
      convertHeat().expect("-8 H, TemperatureStep, TR")
      pass()
    }

    // "World Government us an ocean... nine-eight."
    dad.doTask("OceanTile<Utopia_9_8>! BY Engine").expect("0 TR")

    // Both player ledgers: after Generation 5 transition, before Research.
    with(dad) {
      assertProduction(m = 12, s = 0, t = 6, p = 1, e = 0, h = 1)
      assertResources(m = 37, s = 0, t = 7, p = 8, e = 0, h = 4)
    }
    with(ellie) {
      assertProduction(m = 4, s = 3, t = 1, p = 0, e = 8, h = 4)
      assertResources(m = 33, s = 6, t = 1, p = 0, e = 8, h = 13)
    }
    assertSidebar(gen = 6, temp = -18, oxygen = 1, oceans = 3, venus = 12)

    dad.buyCards(2)
    ellie.buyCards(1)

    ellie.turn {
      // "I will spend three energy to trade with Enceladus. That is five microbes going to
      // Nitrate Reducing Bacteria."
      stdAction("TradeSA", 2) {
        doTask("Trade<Enceladus>")
        doTask("5 Microbe<$NitriteReducingBacteria>")
      }
    }

    dad.turn {
      // "I'm going to play Industrial Microbes for full price. And now I'm going to pay eight to
      // become the Ecologist."
      playProject(IndustrialMicrobes, 12).expect("PROD[S, E]")
      doTask("MicrobeTag<WildTagUse<$ResearchCoordination>>")
      stdAction("ClaimMilestoneSA") { doTask("Ecologist") }
    }

    // Dad never narrated or logged Industrial Microbes' steel and energy production; both remain
    // absent from the generation-seven photograph and ledger.
    dad.exMachina("PROD[-S, -E]")

    ellie.turn {
      // "Forced Precipitation and Extractor Balloons. Remove two off both of them to raise Venus
      // by two. Oh, it is at 16, which means I get an extra TR."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, TR")
      cardAction2(ExtractorBalloons).expect("-2 Floater, 2 TR")
    }
    dad.turn {
      // "Import some GHG for two titanium, one real money, draw a card, get two heat production."
      playProject(ImportOfAdvancedGhg, 1, titanium = 2).expect("PROD[2 H]")
      // "For my second, let's just get this other milestone taken care of. Eight to be the
      // Metallurgist."
      stdAction("ClaimMilestoneSA") { doTask("Metallurgist") }
    }
    ellie.turn {
      stdAction("ClaimMilestoneSA") { doTask("Tactician4") }
    }
    dad.turn {
      // "Use Floating Habs to spend two money to put a floater on Aerial Mappers, and use that to
      // draw a card."
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }
    ellie.turn {
      cardAction1(Psychrophiles).expect("Microbe")
    }
    dad.turn {
      sellPatents(1)
      // "Hired Raiders, pay one... I'm going to take three money."
      playProject(HiredRaiders, 1) { doTask("3 M<Dad> FROM M<Ellie>") }
    }
    ellie.turn {
      // "Nitrate Reducing Bacteria. I will reduce the nitrates. Spend three of them to gain a TR."
      // The transcript places this immediately after Hired Raiders; move it to the preceding legal
      // Ellie turn rather than assigning any of Dad's photographed cards to her.
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }
    dad.turn {
      // "Use Asteroid Rights to spend one of my three money to put an asteroid on Asteroid Rights."
      cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }.expect("-1, Asteroid")
    }
    ellie.turn {
      // "Before I forget, I will heat boop."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }
    dad.turn {
      // "Use Energy Market to spend my last two money to get one energy resource."
      cardAction1(EnergyMarket) { doTask("-2 THEN 1 Energy") }.expect("-2 M, Energy")
      // "I'm going to convert plants and get in this spot where I get a plant and four money."
      convertPlants {
            doTask("GreeneryTile<Utopia_4_2>")
          }
          .expect("-7 P, 4 M, OxygenStep, TR")
    }

    // Dad accidentally took another TR, not realizing the app gave it to him already
    dad.exMachina("TR")

    ellie.turn {
      // "Noctis City... six steel and six real... place a city tile... on three-two."
      playProject(NoctisCity, 6, steel = 6) {
            doTask("CityTile<Utopia_3_2>")
          }
          .expect("PROD[3 M, -E], -4 M, Animal<Dad>")
    }
    dad.pass()
    ellie.turn {
      // "I stratobird. Five. I get silver stratobirds."
      cardAction1(StratosphericBirds).expect("Animal")
    }
    ellie.pass()

    // "I will World Government an ocean on six-four."
    ellie.doTask("OceanTile<Utopia_6_4>! BY Engine").expect("0 TR")

    // board-13-46-12.jpg and both player ledgers: after Generation 6 transition.
    with(dad) {
      assertProduction(m = 12, s = 0, t = 6, p = 1, e = 0, h = 3)
      assertResources(m = 42, s = 0, t = 11, p = 2, e = 0, h = 8)
      assertCounts(
          26 to "TR",
          17 to "CardFront",
          1 to "Ecologist",
          1 to "Metallurgist",
          1 to "$MarsUniversity",
          2 to "OwnedTile",
          1 to "GreeneryTile<Utopia_4_2>",
          1 to "CityTile<RemoteArea>",
      )
      assertCardResources(5 to Pets, 0 to AerialMappers, 1 to AsteroidRights)
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 55, s = 3, t = 2, p = 0, e = 7, h = 14)
      assertCounts(
          33 to "TR",
          17 to "CardFront",
          1 to "Tactician4",
          1 to "$HermeticOrderOfMars",
          2 to "OwnedTile",
          1 to "CityTile<Utopia_3_2>",
      )
      assertCardResources(
          5 to StratosphericBirds,
          3 to NitriteReducingBacteria,
          5 to Psychrophiles,
          1 to ForcedPrecipitation,
          0 to ExtractorBalloons,
      )
    }
    engine.assertCounts(
        1 to "OceanTile<Utopia_9_8>",
        1 to "OceanTile<Utopia_6_4>",
    )
    assertSidebar(gen = 7, temp = -16, oxygen = 2, oceans = 4, venus = 16)

    // When we resumed the physical game, we knew about the errors found above, so we made this
    // manual correction to put things right again.
    dad.exMachina("-TR, -1, PROD[S, E], S, E")

    dad.buyCards(2)
    dad.exMachina("6") // And then I immediately screwed up and forgot to pay for my cards!
    ellie.buyCards(3)

    dad.turn {
      // Dad: "Sure. Let's pay eight to fund Traveller. I have funded the Traveller award."
      stdAction("FundAwardSA") { doTask("Traveller") }
    }

    ellie.turn {
      // Ellie: "I pay one for Market Manipulation. Increase the colony track one step."
      // Dad: "So she's increasing Pluto." Ellie: "Yes. Decrease Io."
      playProject(MarketManipulation, 1) {
        doTask("ColonyProduction<Pluto> FROM ColonyProduction<Io>")
      }
      // Ellie: "Then I will spend three energy to trade with Pluto, which now gives me three
      // cards." Dad: "Nice. Three cards free and clear."
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 3 ProjectCard")
    }

    dad.turn {
      // Dad: "I guess I'll play a Martian Zoo. I believe that cost me full price. I want to pay
      // 12."
      playProject(MartianZoo, 12)
    }

    ellie.turn {
      // Ellie: "Io Sulphur Research for 17. I have three Venus tags from the two floater cards and
      // Strata Birds, so three cards."
      playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") }

      // She forgot her Valley Trust discount
      ellie.exMachina("-2")
    }

    dad.turn {
      // Dad: "I'm going to play Nuclear Power. That cost me ten. I lose two money production. I
      // gain three energy production."
      playProject(NuclearPower, 10).expect("PROD[-2 M, 3 E]")
    }

    ellie.turn {
      // Ellie: "Air-Scrapping Expedition for 13. Raise Venus one step, and I get a TR. Add three
      // floaters to a Venus card. That'll be Forced Precipitation."
      playProject(AirScrappingExpedition, 13) {
            doTask("3 Floater<$ForcedPrecipitation>")
          }
          .expect("VenusStep, TR, 3 Floater")
    }

    dad.turn {
      // Dad: "I am going to play Miranda—Miranda Resort. I guess it costs me three titanium. And I
      // get one, two, three, four, five, six, seven money production."
      doTask("EarthTag<WildTagUse<$ResearchCoordination>>")
      playProject(MirandaResort, titanium = 3).expect("PROD[7 M]")
    }

    ellie.turn {
      // Ellie: "I used Forced Precipitation. Remove two floaters to increase Venus."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, VenusStep, TR")
    }

    dad.turn {
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
    }

    ellie.turn { cardAction1(ExtractorBalloons).expect("Floater") }

    dad.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    ellie.turn { cardAction1(StratosphericBirds).expect("Animal") }

    dad.turn {
      // Dad: "I'm going to play Business Contactos, which cost me seven of my nine money. I look at
      // the top four cards and I pick two of them. Then, because I played an Earth tag, I draw a
      // card and I get a little aminal on Martian Zoo."
      playProject(BusinessContacts, 7).expect("2 ProjectCard, Animal")
    }

    ellie.turn { cardAction1(Psychrophiles).expect("Microbe") }

    dad.turn {
      // Dad: "I'm going to import some Nitrogen. I'm going to slightly overspend by spending six
      // titanium. I will draw the card for the Earth tag. I'll get a TR. I'll get four plants. I
      // don't have a microbe card. And I think I'm going to take two animals on Martian Zoo."
      playProject(ImportedNitrogen, titanium = 6) {
            doTask("2 Animal<$MartianZoo>")
          }
          .expect("TR, 4 P, 3 Animal, 0 ProjectCard")
    }

    // I forgot the extra animal from MZ's effect
    dad.exMachina("-Animal<$MartianZoo>")

    ellie.turn {
      // Ellie: "Nitrite Reducing Bacteria. I remove three and get a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    // It looks like she forgot to take her TR
    ellie.exMachina("-TR")

    dad.turn {
      // Dad: "Now I'm going to use Asteroid Rights to remove an asteroid from Asteroid Rights. And
      // honestly, I think I'll take the money production."
      cardAction2(AsteroidRights) { doTask("PROD[Megacredit]") }.expect("-Asteroid, PROD[M]")
    }

    ellie.turn { convertHeat().expect("-8 H, TemperatureStep, TR") }

    dad.turn {
      // Dad: "I'll take the Martian Zoo action to take three money."
      cardAction1(MartianZoo).expect("3")
    }

    ellie.turn {
      // Ellie: "Neutralizer Factory. Pay seven. We've definitely met the ten percent Venus
      // requirement. Increase Venus one step."
      playProject(NeutralizerFactory, 7).expect("VenusStep, TR")
    }

    dad.turn {
      // Dad: "Venusian Insects is the card that I played and spent five on."
      playProject(VenusianInsects, 5)
    }

    ellie.pass()
    dad.turn {
      // Dad: "Now I'm taking its action, putting a microbe on it. On it like a bonnet."
      cardAction1(VenusianInsects).expect("Microbe")
      // Dad: "I think I will use Energy Market to reduce my energy production by one and get eight
      // money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8")
      // Dad: "Then I have enough money for Nitrophilic Moss. We do have the three-ocean
      // requirement. And I will lose two plants and gain two plant production. And that costs the
      // eight money."
      playProject(NitrophilicMoss, 8).expect("-2 P, PROD[2 P]")
      pass()
    }

    // Dad uses World Government Terraforming to increase Venus.
    dad.doTask("VenusStep! BY Engine").expect("0 TR")

    // Both player ledgers: after Generation 7 transition, before Research.
    with(dad) {
      assertProduction(m = 18, s = 1, t = 6, p = 3, e = 3, h = 3)
      assertResources(m = 44, s = 2, t = 8, p = 7, e = 3, h = 12)
      assertCounts(26 to "TR")
      assertCardResources(3 to MartianZoo, 1 to VenusianInsects)
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 52, s = 6, t = 3, p = 0, e = 7, h = 14)
      assertCounts(37 to "TR")
    }
    assertSidebar(gen = 8, temp = -14, oxygen = 2, oceans = 4, venus = 24)

    dad.buyCards(2)
    ellie.buyCards(1)

    // I suddenly realized my mistake last turn when I forgot to pay for my cards!
    // So I pay for them now, and realize I wouldn't have afforded VI last round.
    // I let it stay, but reason that it should lose a microbe that I wouldn't have had the
    // chance to play.
    dad.exMachina("-6, -Microbe<$VenusianInsects>")

    ellie.turn {
      // Ellie: "Ice Moon Colony. I will pay two of my three titanium for six money, and then 17
      // real. I will place on Miranda. And it gives me an animal to my Strata Birds. I place an
      // ocean tile." Dad: "It's row eight." Ellie: "Four. Eight-four."
      playProject(IceMoonColony, 17, titanium = 2) {
            doTask("Colony<Miranda>")
            doTask("Animal<$StratosphericBirds>")
            doTask("OceanTile<Utopia_8_7>")
          }
          .expect("Animal, OceanTile, TR, -15 M, 2 P")
      // Ellie: "And for my second action, three energy to trade with Miranda. I get two aminals and
      // a card."
      stdAction("TradeSA", 2) {
            doTask("Trade<Miranda>")
            doTask("2 Animal<$StratosphericBirds>")
          }
          .expect("-3 E, 2 Animal, ProjectCard")
    }

    dad.turn {
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
    }

    ellie.turn {
      // Ellie: "I remembered why I left a titanium, and that is so I can play Diversity Support.
      // I've got all six standard resources and microbes, animals, floaters. So pay one money, get
      // one TR."
      playProject(DiversitySupport, 1).expect("TR")
    }

    dad.turn {
      // Dad: "It'll involve the playing of Plantation, which I can do because I have a science tag
      // and a wild tag. I've gotten so much use out of this wild tag. So that costs me 15 entire
      // money. And then I place a greenery tile, which I'm going to place at five-three."
      doTask("ScienceTag<WildTagUse<$ResearchCoordination>>")
      playProject(Plantation, 15) { doTask("GreeneryTile<Utopia_5_3>") }
          .expect("GreeneryTile, OxygenStep, TR, -13 M")
      // Dad: "Then I'm going to Kaguya its ass. I'm playing Kaguya Tech for ten full money. I get
      // two money production. I get a card. I swap this greenery tile. I flip it, basically."
      playProject(KaguyaTech, 10) {
            doTask("CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>")
          }
          .expect("PROD[2 M], 0 ProjectCard, -GreeneryTile, CityTile, Animal<$Pets>, -8 M")
    }

    // The Generation 9 photograph still has five animals on Pets, so Dad missed the animal caused
    // by Kaguya Tech's city placement.
    dad.exMachina("-Animal<$Pets>")

    ellie.turn {
      // Ellie: "Right. Venusian Animals." Dad: "Oh my god." Ellie: "Yep. It immediately adds an
      // animal to itself."
      playProject(VenusianAnimals, 13).expect("Animal")
    }

    // Again forgot her Valley Trust discount.
    ellie.exMachina("-2")

    dad.turn {
      // Dad: "I'm gonna go ahead and use three real and seven titanium. And I'm going to get four
      // plant production, two TR plus another TR for raising temp to minus 12."
      playProject(NitrogenRichAsteroid, 3, titanium = 7) { doTask("PROD[4 Plant]") }
          .expect("PROD[4 P], 3 TR, TemperatureStep")
    }

    ellie.turn { cardAction2(ForcedPrecipitation).expect("-2 Floater, VenusStep, TR") }

    dad.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    ellie.turn { cardAction1(ExtractorBalloons).expect("Floater") }

    dad.turn {
      // Dad: "Business Network. Cost me four. I lose a money production. Aw, I only have 19 money
      // production now. And I get an animal on Martian Zoo and a card."
      playProject(BusinessNetwork, 4).expect("PROD[-M], Animal, 0 ProjectCard")
    }

    ellie.turn { cardAction1(Psychrophiles).expect("Microbe") }

    dad.turn {
      cardAction2(EnergyMarket).expect("PROD[-E], 8")
    }

    ellie.turn { convertHeat().expect("-8 H, TemperatureStep, TR") }

    dad.turn {
      // Dad: "I lose two steel and I get four money back. All right, so in the end what happened is
      // I paid two steel and two real. I gain an energy production and you lose two heat
      // production."
      playProject(HeatTrappers, 2, steel = 2) { doTask("PROD[-2 H<Ellie>]") }
          .expect("PROD[E<Dad>, -2 H<Ellie>]")
    }

    ellie.turn {
      // Ellie: "Luckily I have this power tag so I can play Power Supply Consortium."
      // Dad: "I lose an energy production." Ellie: "I pay five and I gain an energy production."
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-E<Dad>]") }
          .expect("PROD[-E<Dad>, E<Ellie>]")
    }

    dad.turn {
      // Dad: "I guess that means if I want to trade, I better do it now. I'll trade for three
      // energy and I'll do Europa. So I get a plant production."
      stdAction("TradeSA", 2) { doTask("Trade<Europa>") }.expect("-3 E, PROD[P]")
    }

    ellie.turn { cardAction1(StratosphericBirds).expect("Animal") }

    dad.turn {
      // Dad: "All right, I am going to import some zhuzh. This time it's just Imported Zhuzh. I'm
      // going to pay seven real for it. I get one heat production, three heat. I get a silver
      // animal on Martian Zoo. I get a card."
      playProject(ImportedGhg, 7).expect("PROD[H], 3 H, Animal, 0 ProjectCard")
    }

    ellie.turn {
      // Ellie: "Imported Nutrients. I pay a titanium and 11 real, gain four plants, and add four
      // microbes to Nitrite-Reducing Bacteria." Dad: "Man, you're just churning that thing."
      playProject(ImportedNutrients, 11, titanium = 1) {
            doTask("4 Microbe<$NitriteReducingBacteria>")
          }
          .expect("4 P, 4 Microbe")
    }

    dad.turn { cardAction1(VenusianInsects).expect("Microbe") }

    ellie.turn {
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    dad.turn {
      cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }.expect("-1, Asteroid")
    }

    ellie.pass()
    dad.turn {
      // Dad: "I will use Martian Zoo to take five money."
      cardAction1(MartianZoo).expect("5")
      // Dad: "It appears I never used Business Network. So I'm going to use Business Network to
      // look at a card. I think that makes me want to take it more, so I will pay three for it."
      cardAction1(BusinessNetwork) { buyCards(1) }.expect("-3, ProjectCard")
      pass()
    }

    // Ellie uses World Government Terraforming to increase oxygen.
    ellie.doTask("OxygenStep! BY Engine").expect("0 TR")

    // board-21-13-43.jpg, board-21-14-23.jpg, and both player ledgers: after Generation 8
    // transition.
    with(dad) {
      assertProduction(m = 19, s = 1, t = 6, p = 8, e = 2, h = 4)
      assertResources(m = 51, s = 1, t = 7, p = 15, e = 2, h = 19)
      assertCounts(
          30 to "TR",
          26 to "CardFront",
          3 to "OwnedTile",
          1 to "CityTile<Utopia_5_3>",
          0 to "Colony",
      )
      // Pets scores one VP per two animals; this count does not mean five VP.
      assertCardResources(
          5 to Pets,
          5 to MartianZoo,
          1 to VenusianInsects,
          0 to AerialMappers,
          1 to AsteroidRights,
      )
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 8, h = 2)
      assertResources(m = 51, s = 9, t = 1, p = 6, e = 8, h = 12)
      assertCounts(
          42 to "TR",
          22 to "CardFront",
          2 to "OwnedTile",
          1 to "Colony<Miranda>",
      )
      assertCardResources(
          10 to StratosphericBirds,
          1 to VenusianAnimals,
          1 to NitriteReducingBacteria,
          7 to Psychrophiles,
          0 to ForcedPrecipitation,
          2 to ExtractorBalloons,
      )
    }
    engine.assertCounts(
        1 to "Traveller",
        1 to "OceanTile<Utopia_8_7>",
        2 to "ReserveTradeFleet",
        0 to "FlownTradeFleet",
    )
    assertColonyProductions(3, 2, 1, 3, 2)
    assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 5, venus = 26)

    // Before Generation 9 Research, both players reconciled the physical table against their
    // resource logs. Dad took three M€ and an animal on Martian Zoo and discarded a card; Ellie
    // took six M€ and one TR.
    dad.exMachina("3, Animal<$MartianZoo>, -ProjectCard")
    ellie.exMachina("6, TR")

    // "You kept all your cards?" "Bada-bing. Yeah. I just hated to give them up."
    dad.buyCards(4).expect("-12, 4 ProjectCard")
    ellie.buyCards(4).expect("-12, 4 ProjectCard")

    dad.turn {
      // "I'm planting a forest. I'm eternally hopeful. I'm going to place on six, four to get two
      // steel and two money."
      convertPlants { doTask("GreeneryTile<Utopia_6_3>") }.expect("-8 P, 2 S, 2 M, TR")
      // "Well, let us just go ahead and use floating hubs to put a cube on aerial mappers."
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
    }

    ellie.turn {
      // "I'm going to use my extractor balloons, spend the two floaters, increase Venus. To 28."
      cardAction2(ExtractorBalloons).expect("-2 Floater, VenusStep, TR")
      // "Then I'm going to spend three floaters, trade with Aran. This might not be the right call,
      // but enchiladas, actually." "Yeah, gain three microbes."
      stdAction("TradeSA", 2) {
        doTask("Trade<Enceladus>")
        doTask("3 Microbe<$NitriteReducingBacteria>")
      }
    }

    // "I will use aerial mappers to remove a floater from aerial mappers and get a card."
    dad.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    ellie.turn {
      // "Nine for sponsored academies." "I pitch a card." "And then you get three cards and I also
      // get a card."
      playProject(SponsoredAcademies, 7)
          .expect("ProjectCard<Dad>, ProjectCard<Ellie>, Animal<Ellie>")
      // "La Grange Observatoire." "One titanium and four money." "I believe I get a card."
      playProject(LagrangeObservatory, 4, titanium = 1)
          .expect("0 ProjectCard, Animal<$VenusianAnimals>")
    }

    // "I am going to use my business network to look at this card." "I will not buy this card."
    dad.turn { cardAction1(BusinessNetwork) { doTask("Ok") } }

    ellie.turn {
      // "May as well. Spend all nine of my steel on aquifer pumping."
      playProject(AquiferPumping, steel = 9)
    }

    dad.turn {
      // "I'm gonna play advanced alloys." "It costs me nine." "No discounts."
      playProject(AdvancedAlloys, 9) { doTask("-ProjectCard") }.expect("-ProjectCard")
      // "I am gonna go ahead and play Solar Logistics." "But I spend four titanium on that." "I get
      // two titanium from it. I get a minimal on Martian Zoo. I get a card."
      playProject(SolarLogistics, titanium = 4).expect("-2 T, Animal<$MartianZoo>")
    }

    ellie.turn {
      // "I spend eight on aquifer pumping. The action, that is." "Row seven, column six, I
      // believe."
      // "Yes, two plants, two money."
      cardAction1(AquiferPumping) {
            pay(8)
            doTask("OceanTile<Utopia_7_6>")
          }
          .expect("2 P, -6 M, TR")
    }

    dad.turn {
      // "I'm gonna play Ice Asteroid. For four titanium and three wheel. It's a space event. So I
      // draw a card from solar logistics. I place two ocean tiles." "So that gets me two TR and 10
      // money."
      playProject(IceAsteroid, 3, titanium = 4) {
            doTask("OceanTile<Utopia_7_5>")
            doTask("OceanTile<Utopia_8_6>")
          }
          .expect("2 TR, 7 M")
      // "Yeah, what the hell, let's buy a standard project, shall we?" "Aquifer." "I'm just gonna
      // take two plants by placing on four, five."
      stdProject("AquiferSP") { doTask("OceanTile<Utopia_4_5>") }.expect("2 P, TR")
    }

    // "I spend 26 money. Lose two energy productions. Gain five money productions." "Place." "It'll
    // go right here. That would be row six, column five. Yeah. For six money and two plants."
    // "Actually, any chance I can undo and play conscription first?" "Yeah, sure."
    ellie.turn {
      playProject(Conscription, 5)
      playProject(Capital, 10) { doTask("CityTile<Utopia_6_5>") }
          .expect("PROD[5 M, -2 E], -4 M, 2 P, Animal<Dad>")
    }

    dad.turn {
      // "Three steel, nine mega credit. That gets me tectonic stress power." "And so I get my
      // three energy production."
      playProject(TectonicStressPower, 9, steel = 3).expect("PROD[3 E]")
    }

    ellie.turn {
      // "I'm just gonna heat boop." "A heat boop has been done. That means converting heat to
      // temperature."
      convertHeat().expect("-8 H, TemperatureStep, TR")
      // "I will use nitrate-reducing bacteria, remove three microbes, gain a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    dad.turn {
      // "I will use asteroid rights to take one asteroid off of asteroid rights and give myself
      // two titanium."
      cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T")
      // "I will plant a greenery or plant a forest, as they like to call it on this app." "I'll
      // put it next to my city for two money."
      convertPlants { doTask("GreeneryTile<Utopia_5_2>") }.expect("-8 P, 2 M, TR")
    }

    ellie.turn {
      // "You know, it occurred to me I can probably spend 15 on a final Venus boop."
      stdProject("AirScrappingSP")
      // "I'm going to psychrophile."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    dad.turn {
      // "Well, I'm going to heat boop." "I'm going to heat boop. I didn't move it either time."
      convertHeat().expect("-8 H, TemperatureStep, TR")
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    // "Anyways, I stratoburb."
    ellie.turn { cardAction1(StratosphericBirds).expect("Animal") }

    // "Energy market. Reduce energy production to four, gain eight money."
    dad.turn { cardAction2(EnergyMarket).expect("PROD[-E], 8") }

    ellie.pass()
    dad.turn {
      // "I'm going to play Lunar Exports, which costs me three titanium and four money." "And I
      // get a card from Point Luna." "I'm actually going to take the money production."
      playProject(LunarExports, 2, titanium = 3) { doTask("PROD[5 Megacredit]") }
          .expect("PROD[5 M], Animal<$MartianZoo>")
      // "Let's play Solar Net for seven real money. I draw two cards."
      playProject(Solarnet, 7).expect("ProjectCard")
      // "Let's play Algae for ten money." "I get two plant production and one plant."
      playProject(Algae, 10).expect("PROD[2 P], P")
      // "I will go ahead and use my Martian Zoo now to take eight money."
      cardAction1(MartianZoo).expect("8")
      // "I will use my Venusian Insects to take a Venusian insect, which is apparently a kind of
      // microbe now."
      cardAction1(VenusianInsects).expect("Microbe")
      doTask("PlantTag<WildTagUse<$ResearchCoordination>>")
      // "Okay, here goes insects." "And I get one, two, three, four, five, five plant production."
      playProject(Insects, 9).expect("PROD[5 P]")
      pass()
    }

    // "The world government is me, and well, I'm not going to do oxygen. I do temperature up to
    // minus two."
    dad.doTask("TemperatureStep! BY Engine").expect("0 TR")

    // board-16-19-30.jpg and both player ledgers: after Generation 9 transition.
    with(dad) {
      assertProduction(m = 24, s = 1, t = 6, p = 15, e = 4, h = 4)
      assertResources(m = 64, s = 1, t = 6, p = 17, e = 4, h = 9)
      assertCounts(
          37 to "TR",
          33 to "CardFront",
          5 to "OwnedTile",
      )
      assertCardResources(
          6 to Pets,
          8 to MartianZoo,
          2 to VenusianInsects,
          0 to AerialMappers,
          0 to AsteroidRights,
      )
    }
    with(ellie) {
      assertProduction(m = 12, s = 3, t = 1, p = 0, e = 6, h = 2)
      assertResources(m = 64, s = 3, t = 1, p = 10, e = 6, h = 11)
      assertCounts(
          48 to "TR",
          26 to "CardFront",
          3 to "OwnedTile",
      )
      assertCardResources(
          11 to StratosphericBirds,
          3 to VenusianAnimals,
          1 to NitriteReducingBacteria,
          8 to Psychrophiles,
          0 to ForcedPrecipitation,
          0 to ExtractorBalloons,
      )
    }
    engine.assertCounts(
        1 to "OceanTile<Utopia_7_6>",
        1 to "OceanTile<Utopia_7_5>",
        1 to "OceanTile<Utopia_8_6>",
        1 to "OceanTile<Utopia_4_5>",
    )
    dad.assertCounts(
        1 to "GreeneryTile<Utopia_6_3>",
        1 to "GreeneryTile<Utopia_5_2>",
    )
    ellie.assertCounts(1 to "CityTile<Utopia_6_5>")
    assertSidebar(gen = 10, temp = -2, oxygen = 6, oceans = 9, venus = 30)

    // "Well, I'm buying three cards." "All right, Ellie buys three cards, and I'm going to stupidly
    // buy two cards. You know what? I'm going to buy three cards. Talk about stupid. Three cards."
    dad.buyCards(3).expect("-9, 3 ProjectCard")
    ellie.buyCards(3).expect("-9, 3 ProjectCard")

    ellie.turn {
      // "I will plant forest, put my greenery on... Looks like 5-5, right?" "Okay. Two money and a
      // plant."
      convertPlants { doTask("GreeneryTile<Utopia_5_5>") }.expect("-7 P, TR")
      // "And I will greenery standard project." "But you've got two TR from one move." "Yeah, and
      // an extra for being the one to get it." "Anyways, the second one goes... 2-1."
      stdProject("GreenerySP") { doTask("GreeneryTile<Utopia_2_1>") }.expect("2 TR")
    }

    dad.turn {
      // "I am feeling like I had better put a cute little city down while I can. So I paid for
      // standard project." "4-4 for two money and two plants."
      stdProject("CitySP") { doTask("CityTile<Utopia_4_4>") }
      // "Ecological zone. Cost me 12 entire." "Well, for these two, I get two animals right away."
      // "Putting it on 2-2?" "Yes. For two steel."
      playProject(EcologicalZone, 12) {
            doTask("EcologicalZone_SpecialTile<Utopia_2_2>")
          }
          .expect("2 Animal<$EcologicalZone>, -ProjectCard")
    }

    ellie.turn {
      // "Eight for cryosleep because I have science tag discount. Pay eight. And I get a new
      // animal."
      playProject(CryoSleep, 8).expect("Animal<$VenusianAnimals>, -ProjectCard")
      // "And I spend two energy to trade with Miranda for two animals and a card."
      stdAction("TradeSA", 2) {
            doTask("Trade<Miranda>")
            doTask("2 Animal<$StratosphericBirds>")
          }
          .expect("-2 E, 2 Animal<$StratosphericBirds>, ProjectCard")
    }

    dad.turn {
      // "I will use my business network to look at a card. Absolutely not."
      cardAction1(BusinessNetwork) { doTask("Ok") }
      // "Heat boob." "Now your turn."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    ellie.turn {
      // "Back to viral research, baby. Cost you eight?" "Yes. Almost forgot. Draw one card."
      // "I will choose Nitrate Reducing Bacteria." "Six microbes."
      playProject(BactoviralResearch, 8) {
            doTask("6 Microbe<$NitriteReducingBacteria>")
          }
          .expect(
              "6 Microbe<$NitriteReducingBacteria>, 0 Animal<Dad, $EcologicalZone<Dad>>, 0 ProjectCard"
          )
    }

    dad.turn {
      // "Herbivores, again with the full price." "I do add an animal to this card and an animal to
      // Ecozone. And you lose a plant production." "Oh, shit, I don't have any plant production."
      playProject(Herbivores, 12)
          .expect("Animal<$Herbivores>, Animal<$EcologicalZone>, PROD[0 P<Ellie>], -ProjectCard")
    }

    ellie.turn {
      // "Jovian lanterns for 20." "Increase your TR one step." "Add two floaters to any card. I
      // will
      // add it to itself."
      playProject(JovianLanterns, 20) { doTask("2 Floater<$JovianLanterns>") }
          .expect("TR, 2 Floater<$JovianLanterns>, -ProjectCard")
    }

    dad.turn {
      // "Plant boop, plant boop." "So two money and two plants." "And played greenery. So I add a
      // minimal to herbivores. I add two of them."
      convertPlants { doTask("GreeneryTile<Utopia_4_3>") }.expect("-7 P, TR, Animal<$Herbivores>")
      convertPlants { doTask("GreeneryTile<Utopia_5_4>") }.expect("-7 P, TR, Animal<$Herbivores>")
    }

    ellie.turn {
      // "I use Jovian Lantern, spend a titanium to add two floaters here."
      cardAction1(JovianLanterns).expect("-T, 2 Floater<$JovianLanterns>")
      // "Actually, I'm just going to go like add a thing to extractor balloons."
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    dad.turn {
      // "I think I'm going to use Martian Zoo to take eight money and then spend eighteen money on
      // lava flows." "Two cards and four money."
      cardAction1(MartianZoo).expect("8")
      playProject(LavaFlows, 18) { doTask("LavaFlows_SpecialTile<Utopia_8_5>") }
          .expect("-14, ProjectCard, 2 TemperatureStep, 2 TR")
    }
    // Dad's ledger omitted the two TR from Lava Flows' temperature steps.
    dad.exMachina("-2 TR")

    ellie.turn {
      // "Oh, I add a Strato Bird."
      cardAction1(StratosphericBirds).expect("Animal")
      // "Oh, probably be smart for me to do my own heat boob."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    dad.turn {
      // "It's weird, but I'm gonna play a card I've never played before in my life. Food Factory."
      // "And three real gives me four money production. Takes away one of my plant production."
      intentionalOverpay()
      playProject(FoodFactory, 3, steel = 3).expect("PROD[4 M, -P], -ProjectCard")
    }

    ellie.turn {
      // "I will add Psychrophile and I will remove three nitrites for a TR."
      cardAction1(Psychrophiles).expect("Microbe")
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    dad.turn {
      // "I'm going to sell a patent to get a money and then spend two money on floating habs. To
      // use
      // floating habs to put a floater onto aerial mappers."
      sellPatents(1).expect("1, -ProjectCard")
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
    }

    ellie.turn {
      // "I'll pay three psychrophiles for green houses." "Gain one plant for each city tile in
      // play. That's one, two, three, four, five."
      playProject(Greenhouses, 0) {
            doTask("3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("5 P, 0 Animal<Dad, $EcologicalZone<Dad>>, -ProjectCard")
      // "And I will greenery boop." "It's six, six, sorry." "It's the last possible spot next to my
      // capital for two money."
      convertPlants { doTask("GreeneryTile<Utopia_6_6>") }.expect("-8 P, 2 M, TR")
    }

    // "I'm going to use aerial mappers to take a floater off of aerial mappers and draw a card."
    dad.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    ellie.pass()
    dad.turn {
      // "And then I'm going to use energy market to reduce energy production and give myself eight
      // money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8")
      // "I guess I play Dawn City for three titanium." "I lose an energy production. I gain a
      // titanium production." "And so when I place that, I believe I get a pet."
      // "Oh shit. I don't." "Okay, since I already committed to it, what I will do is I will sell a
      // patent to get one money, spend nine money on robotic workforce just to make an honest card
      // out of it." "I'll copy industrial microbes." "Robotic workforce in Dawn City, we pretended
      // they were in that order."
      sellPatents(1).expect("1, -ProjectCard")
      intentionalOverpay()
      playProject(RoboticWorkforce, 9) {
            doTask("-ProjectCard")
            doTask("CopyProductionBox<$IndustrialMicrobes>")
          }
          .expect("-9, -ProjectCard, PROD[S, E]")
      doTask("ScienceTag<WildTagUse<$ResearchCoordination>>")
      playProject(DawnCity, titanium = 3).expect("PROD[-E, T], Animal<$Pets>, -ProjectCard")
      // "I'm gonna add a Venusian insect."
      cardAction1(VenusianInsects).expect("Microbe")
      // "I'm going to yet again, sell a patent for one money and spend that one money on asteroid
      // rights to put an asteroid onto that."
      sellPatents(1).expect("1, -ProjectCard")
      cardAction1(AsteroidRights) { doTask("Asteroid<$AsteroidRights>") }.expect("-1, Asteroid")
      // "But I'll take the cards using three energy for the Pluto, take two cards."
      stdAction("TradeSA", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 2 ProjectCard")
      pass()
    }

    // Ellie uses World Government Terraforming to raise oxygen to 12%.
    ellie.doTask("OxygenStep! BY Engine").expect("0 TR")

    // board-16-44-30.jpg and both player ledgers: after Generation 10 transition.
    with(dad) {
      assertProduction(m = 29, s = 2, t = 7, p = 13, e = 3, h = 4)
      assertResources(m = 69, s = 2, t = 10, p = 18, e = 3, h = 6)
      assertCounts(
          40 to "TR",
          38 to "CardFront",
          11 to "OwnedTile",
      )
      assertCardResources(
          8 to Pets,
          8 to MartianZoo,
          3 to EcologicalZone,
          3 to Herbivores,
          3 to VenusianInsects,
          0 to AerialMappers,
          1 to AsteroidRights,
      )
    }
    with(ellie) {
      assertProduction(m = 12, s = 3, t = 1, p = 0, e = 6, h = 2)
      assertResources(m = 69, s = 6, t = 1, p = 0, e = 6, h = 9)
      assertCounts(
          55 to "TR",
          30 to "CardFront",
          6 to "OwnedTile",
      )
      assertCardResources(
          14 to StratosphericBirds,
          5 to VenusianAnimals,
          4 to NitriteReducingBacteria,
          6 to Psychrophiles,
          0 to ForcedPrecipitation,
          1 to ExtractorBalloons,
          4 to JovianLanterns,
      )
    }
    dad.assertCounts(
        1 to "CityTile<Utopia_4_4>",
        1 to "SpecialTile<Utopia_2_2>",
        1 to "GreeneryTile<Utopia_4_3>",
        1 to "GreeneryTile<Utopia_5_4>",
        1 to "SpecialTile<Utopia_8_5>",
    )
    ellie.assertCounts(
        1 to "GreeneryTile<Utopia_5_5>",
        1 to "GreeneryTile<Utopia_2_1>",
        1 to "GreeneryTile<Utopia_6_6>",
    )
    assertSidebar(gen = 11, temp = 8, oxygen = 12, oceans = 9, venus = 30)

    // "Man. Yeah. I'm buying two. I'll buy one. I'm gonna actually buy it."
    dad.buyCards(1).expect("-3, ProjectCard")
    ellie.buyCards(2).expect("-6, 2 ProjectCard")

    dad.turn {
      // "Yep. Boop, boop. Indeed. And the game will officially end this round."
      // "One goes here for just two money. That is three, four." "I think I'll just take the four
      // money down here."
      convertPlants { doTask("GreeneryTile<Utopia_3_4>") }.expect("-8 P, 2 M, TR")
      // The second placement is gesture-only in the transcript; its four-M€ ocean income locates
      // it at the open land area between the row-six and row-seven oceans.
      convertPlants { doTask("GreeneryTile<Utopia_7_4>") }.expect("-8 P, 4 M, TR")
    }

    ellie.turn {
      // "I will... pay two energy to trade with Miranda... for one aminal and a card. This time
      // I'll
      // put the aminal on Venusian."
      stdAction("TradeSA", 2) {
            doTask("Trade<Miranda>")
            doTask("Animal<$VenusianAnimals>")
          }
          .expect("-2 E, Animal<$VenusianAnimals>")
      // "Productive outpost for zero... Gain all my colony bonuses, which is literally just draw a
      // card."
      playProject(ProductiveOutpost, 0).expect("0 ProjectCard")
    }

    dad.turn {
      // "I pay fourteen... Mogul." "Yeah. I think I got that one."
      stdAction("FundAwardSA") { doTask("Mogul") }
      // "Listen, all of y'all. It's sabotage. So... You lose... Seven money, and that's it."
      playProject(Sabotage, 1) { doTask("-7 M<Ellie>") }.expect("-ProjectCard")
    }

    ellie.turn {
      // "I'm going to spend thirteen money, no titanus." "Lose two money production." "Place a
      // colony on Pluto. To get two cards."
      playProject(PioneerSettlement, 13) { doTask("Colony<Pluto>") }
          .expect("-13, PROD[-2 M], ProjectCard")
    }

    dad.turn {
      // "I'm going to spend two on floating habs. To put a dingus on aerial mappers." "Use aerial
      // mappers to draw a card."
      cardAction1(FloatingHabs) { doTask("Floater<$AerialMappers>") }.expect("-2, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    ellie.turn { cardAction1(JovianLanterns) }

    dad.turn {
      // "Immigrant City, spending six worth of steel and seven rail." "I better decrease my energy
      // production, decrease my money production by two and then back up one." "Five, six, on five,
      // six."
      playProject(ImmigrantCity, 7, steel = 2) {
            doTask("CityTile<Utopia_5_6>")
          }
          .expect("PROD[-M, -E], -5 M, 2 P, Animal<$Pets>, -ProjectCard")
    }

    ellie.turn { cardAction2(NitriteReducingBacteria) }

    dad.turn {
      // "I'm gonna put it right here, two energy."
      // The City standard project and Immigrant City each increase M€ production for this
      // placement.
      stdProject("CitySP") { doTask("CityTile<Utopia_2_3>") }.expect("PROD[2 M]")
      // "And for my second trick, commercial district, from sixteen, lose an energy production,
      // gain four money production, place a shitty tile, not a shitty tile."
      playProject(CommercialDistrict, 16) {
            doTask("CommercialDistrict_SpecialTile<Utopia_3_3>")
          }
          .expect("PROD[4 M], -ProjectCard")
    }
    // Dad confirms he forgot Immigrant City's trigger. His ledger records only the standard
    // project's one M€ production step, so remove the omitted Immigrant City step here.
    dad.exMachina("PROD[-M]")

    ellie.turn { cardAction1(Psychrophiles) }

    dad.turn {
      // "I'm gonna play robot pollinators for all of my money. It gives me a plant production and
      // one plant per plant tag. One, two, three, four, five. Five plants."
      playProject(RobotPollinators, 9).expect("PROD[P], -ProjectCard")
      // "I'm just gonna do the plant boop now." "Did not give me TR good." "This plant boop will go
      // here for two energy and a card. That is two, four."
      convertPlants { doTask("GreeneryTile<Utopia_2_4>") }.expect("-8 P, 2 E, ProjectCard, 0 TR")
    }

    // "I'm gonna add a strato bird." "I'm at 15 strato birds."
    ellie.turn { cardAction1(StratosphericBirds) }

    dad.turn {
      // "Methane from Titan." "I'm gonna spend six titanium." "I mostly played it for the two
      // points."
      intentionalOverpay()
      playProject(MethaneFromTitan, titanium = 6).expect("PROD[2 P, 2 H], -ProjectCard")
    }

    // "I add an extractor balloon."
    ellie.turn { cardAction1(ExtractorBalloons) }

    // "I'll go ahead and use Martian Zoo to take eight money."
    dad.turn { cardAction1(MartianZoo) }

    ellie.turn {
      // "I'll use local heat trapping. One money. Spend five heat. And I will add two Venusian
      // animals."
      playProject(LocalHeatTrapping, 1) {
        doTask("2 Animal<$VenusianAnimals>")
      }
    }

    dad.turn {
      // "Trading colony for my four titanium." "No, no, no. I'm gonna get three microbes." "Three
      // microbes which go on to Venusian insects."
      playProject(TradingColony, titanium = 4) {
            doTask("Colony<Enceladus>")
            doTask("3 Microbe<$VenusianInsects>")
          }
          .expect("PROD[0 M], -ProjectCard")
    }

    ellie.turn {
      // "Airliners for 11 requires that you have three floaters." "Gain two money production, add
      // two floaters to another card, which will be Jovian lanterns."
      playProject(Airliners, 11) { doTask("2 Floater<$JovianLanterns>") }.expect("PROD[2 M]")
    }

    dad.turn {
      // "Now, I'm going to fly my little boat to Angelatus." "I get three and one." "They all four
      // go on to the New Zealand insects."
      stdAction("TradeSA", 2) {
        doTask("Trade<Enceladus>")
        doTask("ColonyProduction<Enceladus>")
        doTask("3 Microbe<$VenusianInsects>")
        doTask("Microbe<$VenusianInsects>")
      }
    }

    ellie.turn {
      // "My seven psychrophiles and three real." "Increase money production two steps. Increase
      // plant production three steps. Increase... No, gain two plants."
      playProject(KelpFarming, 3) {
            doTask("7 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("PROD[2 M, 3 P]")
    }

    dad.turn {
      // "Just to be funny, I'm going to play one for land claim, just so you can go there."
      // The source does not identify the claimed area; Utopia_1_1 is a neutral test inference.
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Utopia_1_1>") }.expect("-1, -ProjectCard")
    }

    // "I sell a card for a money."
    ellie.turn { sellPatents(1).expect("1, -ProjectCard") }

    // "I'll go ahead and sell five cards for five money. None of them have victory points on them."
    dad.turn { sellPatents(5).expect("5, -5 ProjectCard") }

    // "Wait. I sell a card for a money."
    ellie.turn { sellPatents(1).expect("1, -ProjectCard") }

    // "I'll spend eight on lightning harvest. One energy product, one money product, and a point."
    dad.turn { playProject(LightningHarvest, 8).expect("PROD[M, E], -ProjectCard") }

    // Ellie's ledger groups Media Archives' net thirteen-M€ gain with the twenty-five-M€ Water
    // Import from Europa payment below as one twelve-M€ debit at entry 325.
    ellie.turn {
      // "Sell three cards for three money. Wait, actually. Hold on. Just in case that can be useful
      // somehow. I'll sell two for two."
      sellPatents(2).expect("2, -2 ProjectCard")
      // "I'm going to play Media Archives." "I have eight. I have 13, so that's 21 money for you."
      playProject(MediaArchives, 8).expect("13, -ProjectCard")
    }

    // "Oh my god, I forgot to use my business network. Fine, I'll use that then."
    dad.turn { cardAction1(BusinessNetwork) { doTask("Ok") }.expect("0 ProjectCard") }

    // "Finally playing. Water Import from Europa."
    ellie.turn { playProject(WaterImportFromEuropa, 25).expect("-25, -ProjectCard") }

    // The transcript identifies Sub-Zero Salt Fish in Dad's hand, while the generic reconstructed
    // hand is one card short after the sourced plays and patent sales.
    dad.exMachina("ProjectCard")
    dad.turn {
      // "Okay, now I can use Energy Market. Get all up to 12 money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8")
      // "Play Sub-Zero Salt Fish." "Now you lose plant production. I spend five on that." "It's an
      // animal tag." "So I get an Ecomole."
      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Ellie>]") }
          .expect("-5, PROD[-P<Ellie>], Animal<$EcologicalZone>, -ProjectCard")
    }
    // Dad took Energy Market's eight M€ but did not record its energy-production decrease.
    dad.exMachina("PROD[E]")

    ellie.turn {
      // "But, well, still, for the means, I can play Predators."
      // "Well, I guess you're going to take my Ecomal, then."
      playProject(Predators, 14).expect("0 Animal<Dad, $EcologicalZone<Dad>>")
      cardAction1(Predators) { doTask("-Animal<Dad, $EcologicalZone<Dad>>") }
          .expect("Animal<$Predators>, -Animal<Dad, $EcologicalZone<Dad>>")
    }

    // "And then, you know, for all the good it'll do, I'll just use the action to add another
    // animal."
    dad.turn { cardAction1(SubZeroSaltFish).expect("Animal") }

    ellie.turn {
      // "Thanks to my steel, I can spend all six and three real for a point from Artificial Lake."
      playProject(ArtificialLake, 3, steel = 6).expect("0 OceanTile, -ProjectCard")
    }
    dad.pass()
    ellie.pass()

    // Both ledgers: after the final production phase and before final greenery placement.
    with(dad) {
      assertProduction(m = 34, s = 2, t = 7, p = 16, e = 2, h = 6)
      assertResources(m = 83, s = 3, t = 7, p = 17, e = 2, h = 16)
      assertCounts(42 to "TR")
    }
    with(ellie) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
      assertCounts(56 to "TR")
    }
    // The resource apps incremented their display to 12 during final production; the engine keeps
    // the completed action generation numbered 11.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // "So I'm going to 1-2 and 1-3."
    dad.convertPlants { doTask("GreeneryTile<Utopia_1_2>") }.expect("-8 P")
    dad.convertPlants { doTask("GreeneryTile<Utopia_1_3>") }.expect("-8 P")
    dad.doTask("Ok")
    ellie.doTask("Ok")

    val score = Summarizer(game)
    dad.assertCounts(
        33 to "AwardTally<Dad, Mogul>",
        11 to "AwardTally<Dad, Traveller>",
        42 to "TR",
    )
    ellie.assertCounts(
        14 to "AwardTally<Ellie, Mogul>",
        8 to "AwardTally<Ellie, Traveller>",
        56 to "TR",
    )
    score.net("Milestone", "VP<Dad>") shouldBe 10
    score.net("Milestone", "VP<Ellie>") shouldBe 5
    score.net("FirstPlace", "VP<Dad>") shouldBe 10
    score.net("FirstPlace", "VP<Ellie>") shouldBe 0
    score.net("SecondPlace", "VP<Dad>") shouldBe 0
    score.net("SecondPlace", "VP<Ellie>") shouldBe 0
    score.net("GreeneryTile", "VP<Dad>") shouldBe 10
    score.net("GreeneryTile", "VP<Ellie>") shouldBe 3
    score.net("CityTile", "VP<Dad>") shouldBe 15
    score.net("CityTile", "VP<Ellie>") shouldBe 6
    score.net("Card", "VP<Dad>") shouldBe 35
    score.net("Card", "VP<Ellie>") shouldBe 46
    score.net("$Pets", "VP<Dad>") shouldBe 5
    score.net("$VenusianInsects", "VP<Dad>") shouldBe 5
    score.net("$EcologicalZone", "VP<Dad>") shouldBe 1
    score.net("$Herbivores", "VP<Dad>") shouldBe 4
    // "Resource points on cards. One, four. Holy shit. Yeah. One, four, eight, and fifteen."
    // The earlier explicit count, "I'm at 15 strato birds," identifies the last value.
    score.net("$Predators", "VP<Ellie>") shouldBe 1
    score.net("$JovianLanterns", "VP<Ellie>") shouldBe 4
    score.net("$VenusianAnimals", "VP<Ellie>") shouldBe 8
    score.net("$StratosphericBirds", "VP<Ellie>") shouldBe 15

    // The spoken 118-115 tally omitted Dad's four Herbivores points. Ellie's spoken total is also
    // one point below the complete replay categories, which sum to 116.
    dad.assertCounts(122 to "VP", 1 to "Victory")
    ellie.assertCounts(116 to "VP", 0 to "Victory")

    with(dad) {
      assertProduction(m = 34, s = 2, t = 7, p = 16, e = 2, h = 6)
      assertResources(m = 83, s = 3, t = 7, p = 1, e = 4, h = 16)
    }
    with(ellie) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
    }
  }

  private fun assertColonyProductions(vararg productions: Int) {
    require(productions.size == colonyTiles.size)
    engine.assertCounts(
        *productions
            .zip(colonyTiles) { production, colony ->
              production to "ColonyProduction<$colony>"
            }
            .toTypedArray()
    )
  }

  private fun TfmGameplay.assertCardResources(vararg resources: Pair<Int, ClassName>) {
    assertCounts(*resources.map { (count, card) -> count to "CardResource<$card>" }.toTypedArray())
  }
}
