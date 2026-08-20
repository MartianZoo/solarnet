package dev.martianzoo.tfm.engine.games

import dev.martianzoo.data.GameConfig
import dev.martianzoo.data.Player
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

/** Live game begun Tue 2026-08-18. Quotes are transcript-derived and sometimes normalized. */
class Game20260818Test : AbstractFullGameTest() {
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

          Ecologist, Merchant, Metallurgist, MilestoneMM30, Hoverlord
          Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile
          Enceladus, Miranda, Europa, Io, Pluto
          """,
          "Dad",
          "Ellie",
      )

  @Test
  fun game20260818() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val ellie = game.tfm(Player.PLAYER2)

    engine.assertCounts(1 to "Generation")

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
      assertCounts(5 to "CardFront")
    }
    with(ellie) {
      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 0)
      assertResources(m = 22, s = 6, t = 0, p = 0, e = 4, h = 0)
      assertCounts(7 to "CardFront")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 0, venus = 2)

    // "I will buy four cards."
    ellie.buyCards(4)
    // Dad first said two, then physically corrected the purchase to three before the action phase.
    dad.buyCards(3)

    ellie.turn {
      // "I'm going to play Mining Rights... row three, column six. I get two cards and a
      // titanium... and increase titanium production."
      playProject(MiningRights, 1, steel = 4) { doTask("Card067_SpecialTile<Utopia_3_6>") }
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
    }
    with(ellie) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 5, h = 0)
      assertResources(m = 25, s = 4, t = 2, p = 0, e = 5, h = 4)
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
            doTask("-Microbe<$Psychrophiles> THEN -2 Owed<Class<Megacredit>>")
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
    }
    with(ellie) {
      assertProduction(m = 2, s = 3, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 27, s = 3, t = 2, p = 1, e = 8, h = 20)
    }
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
      stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, TR")
      stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, PROD[H], TR")
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
      stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, TR")
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
      stdAction("ClaimMilestoneSA") { doTask("Tactician") }
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
      stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, TR")
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
          1 to "$MarsUniversity",
          5 to "Animal<$Pets>",
          1 to "Asteroid<$AsteroidRights>",
      )
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 55, s = 3, t = 2, p = 0, e = 7, h = 14)
      assertCounts(
          33 to "TR",
          1 to "$HermeticOrderOfMars",
          5 to "Animal<$StratosphericBirds>",
          3 to "Microbe<$NitriteReducingBacteria>",
          5 to "Microbe<$Psychrophiles>",
          1 to "Floater<$ForcedPrecipitation>",
          0 to "Floater<$ExtractorBalloons>",
      )
    }
    assertSidebar(gen = 7, temp = -16, oxygen = 2, oceans = 4, venus = 16)

    // The resumed Generation 7 Research phase requires Dad's correction of yesterday's extra TR
    // and resulting M€, plus Industrial Microbes' omitted production and Generation 7 resources.
    dad.exMachina("-TR, -1, PROD[S, E], S, E")

    // The Generation 7 action ledger requires Dad to retain the six M€ he did not pay for these
    // cards until the following Research phase.
    dad.buyCards(2)
    dad.exMachina("6")
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
    }

    // Ellie's Generation 8 ledger requires the narrated 17-M€ payment, which omitted Valley
    // Trust's science-tag discount.
    ellie.exMachina("-2")

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

    // Dad's later three-M€ Martian Zoo action requires the omitted animal from Imported
    // Nitrogen's Earth tag to be removed.
    dad.exMachina("-Animal<$MartianZoo>")

    ellie.turn {
      // Ellie: "Nitrite Reducing Bacteria. I remove three and get a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    // Ellie's Generation 8 ledger remains one TR below the narrated Nitrite Reducing Bacteria
    // result, so the physical game did not retain that TR.
    ellie.exMachina("-TR")

    dad.turn {
      // Dad: "Now I'm going to use Asteroid Rights to remove an asteroid from Asteroid Rights. And
      // honestly, I think I'll take the money production."
      cardAction2(AsteroidRights) { doTask("PROD[Megacredit]") }.expect("-Asteroid, PROD[M]")
    }

    ellie.turn { stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, TR") }

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
      assertCounts(26 to "TR", 3 to "Animal<$MartianZoo>", 1 to "Microbe<$VenusianInsects>")
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 52, s = 6, t = 3, p = 0, e = 7, h = 14)
      assertCounts(37 to "TR")
    }
    assertSidebar(gen = 8, temp = -14, oxygen = 2, oceans = 4, venus = 24)

    dad.buyCards(2)
    // The following action phase and Generation 9 photo require Dad's Generation 8 repayment and
    // removal of the Venusian Insects microbe from the action he could not have afforded in time.
    dad.exMachina("-6, -Microbe<$VenusianInsects>")
    ellie.buyCards(1)

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
          .expect("PROD[2 M], 0 ProjectCard, -GreeneryTile, CityTile, -8 M")
    }

    ellie.turn {
      // Ellie: "Right. Venusian Animals." Dad: "Oh my god." Ellie: "Yep. It immediately adds an
      // animal to itself."
      playProject(VenusianAnimals, 13).expect("Animal")
    }

    // Ellie's Generation 9 ledger requires her printed-cost payment without Valley Trust's two-M€
    // science discount.
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

    ellie.turn { stdAction("ConvertHeatSA").expect("-8 H, TemperatureStep, TR") }

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

    // board-21-14-23.jpg and both player ledgers: after Generation 8 transition.
    with(dad) {
      assertProduction(m = 19, s = 1, t = 6, p = 8, e = 2, h = 4)
      assertResources(m = 51, s = 1, t = 7, p = 15, e = 2, h = 19)
      assertCounts(
          30 to "TR",
          5 to "Animal<$MartianZoo>",
          1 to "Microbe<$VenusianInsects>",
          1 to "Asteroid<$AsteroidRights>",
      )
    }
    with(ellie) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 8, h = 2)
      assertResources(m = 51, s = 9, t = 1, p = 6, e = 8, h = 12)
      assertCounts(
          42 to "TR",
          10 to "Animal<$StratosphericBirds>",
          1 to "Animal<$VenusianAnimals>",
          1 to "Microbe<$NitriteReducingBacteria>",
          7 to "Microbe<$Psychrophiles>",
          0 to "Floater<$ForcedPrecipitation>",
          2 to "Floater<$ExtractorBalloons>",
      )
    }
    assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 5, venus = 26)
  }
}
