package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Live game begun Tue 2026-08-18. Quoted evidence is verbatim from the supplied transcripts. */
internal class OtbGame20260818Test : AbstractFullGameTest() {
  private val colonyTiles = listOf("Enceladus", "Miranda", "Europa", "Io", "Pluto")

  // "We are playing on the Utopia Planitia board."
  // "We have Preludes. We have Venus, Colonies, Promos, Milestones and Awards expansion."
  // The transcript includes Briber, which Solarnet does not implement; it is not claimed in this
  // partial game, so the executable pool omits it. The transcript says Enceladus twice; the
  // photographed five-tile colony setup has one Enceladus.
  override val config =
      GameConfig(
          """
          UtopiaMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion
          PromoCardPack

          Ecologist, Merchant, Metallurgist, Tactician, Hoverlord
          Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Yellow",
      )

  @Test
  internal fun otbGame20260818() {
    TfmWorkflow.Auto(game).launch()
    val green = game.tfm(Player.PLAYER1)
    val yellow = game.tfm(Player.PLAYER2)

    // board-11-00-18.jpg: initial global state, before either corporation is played.
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
    green.assertCounts(20 to "TR", 0 to "OwnedTile")
    yellow.assertCounts(20 to "TR", 0 to "OwnedTile")

    // "I'm Point Luna... I get a titanium production." "I'm keeping seven cards."
    // "So I pay 21. I have 17 money remaining."
    green.playCorp(PointLuna, 7).expect("PROD[T], 17 MC, 8 ProjectCard")

    // "I have Valley Trust. I'm keeping five cards... I have 22 money."
    yellow.playCorp(ValleyTrust, 5).expect("22 MC")

    green.turn {
      // "I play Biofuels... two plants, a plant production, and an energy production."
      playPrelude(Biofuels).expect("2 P, PROD[P, E]")
      // "And then I play Donation and get 21 money."
      playPrelude(Donation).expect("21 MC")
    }

    yellow.turn {
      // "Supplier... four steel, and two energy production."
      playPrelude(Supplier).expect("4 S, PROD[2 E]")
      // "Martian Industries... six money, one steel production, and one energy production."
      playPrelude(MartianIndustries).expect("6 MC, PROD[S, E]")
    }

    green.turn {
      // "[Green] pays 10 to play Pets... Miranda comes into play, and I get an animal on Pets."
      playProject(Pets, 10).expect("Animal")
    }

    yellow.turn {
      // "I use Valley Trust and I get Double Down, which I play... copy Martian Industries."
      stdAction("DoRequiredActions") {
            playPrelude(DoubleDown) { doTask("CopyPrelude<$MartianIndustries>") }
          }
          .expect("PROD[S, E], 6 MC")
      // "I spend two money to play Psychrophiles."
      playProject(Psychrophiles, 2)
    }

    green.turn {
      // "I pay eleven for Aerial Mappers."
      playProject(AerialMappers, 11)
    }

    yellow.turn {
      // "I pay eight for Forced Precipitation."
      playProject(ForcedPrecipitation, 8)
    }

    green.turn {
      // "I'm going to add a floater to Aerial Mappers."
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }.expect("Floater")
    }

    yellow.turn {
      // "I spend 21 to play Extractor Balloons. It gets three floaters."
      playProject(ExtractorBalloons, 21).expect("3 Floater")
    }

    green.pass()
    yellow.turn {
      // "I'm going to add a microbe to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
      // "Remove two floaters from Extractor Balloons and raise Venus."
      cardAction2(ExtractorBalloons).expect("-2 Floater, TR")
      // "Then pay two money to add a floater to Forced Precipitation."
      cardAction1(ForcedPrecipitation).expect("-2 MC, Floater")
      yellow.pass()
    }

    // "[Green] uses World Government Terraforming to increase oxygen."
    green.wgt("OxygenStep").expect("0 TR")

    // board-11-09-02.jpg and both player ledgers: after Generation 1 transition, before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 37, s = 0, t = 1, p = 3, e = 1, h = 0)
      assertCounts(20 to "TR", 5 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(yellow) {
      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 0)
      assertResources(m = 22, s = 6, t = 0, p = 0, e = 4, h = 0)
      assertCounts(21 to "TR", 7 to "CardFront")
      assertCardResources(1 to Psychrophiles, 1 to ForcedPrecipitation, 1 to ExtractorBalloons)
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 0, venus = 2)

    // "I will buy four cards."
    yellow.buyCards(4)
    // Green first said two, then physically corrected the purchase to three before the action
    // phase.
    green.buyCards(3)

    yellow.turn {
      // "I'm going to play Mining Rights... row three, column six. I get two cards and a
      // titanium... and increase titanium production."
      playProject(MiningRights, 1, steel = 4) { placeTile(3, 6) }.expect("ProjectCard, T, PROD[T]")
      // "I play Energy Tapping... [Green] loses an energy production."
      playProject(EnergyTapping, 3) { doTask("PROD[-E<Green>]") }.expect("PROD[E]")
    }

    green.turn {
      // "I play CEO's Favorite Project... put a floater on Aerial Mappers."
      playProject(CeosFavoriteProject, 1) { addCardResources(AerialMappers) }.expect("Floater")
    }

    yellow.turn {
      // "I'm going to add a floater to Extractor Balloons."
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    green.turn {
      // "I remove a floater from Aerial Mappers and draw a card."
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    yellow.turn {
      // "I'm adding a microbe to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    green.turn { sellPatents(1) }

    yellow.turn {
      // "I pay two and add a floater to Forced Precipitation."
      cardAction1(ForcedPrecipitation).expect("-2 MC, Floater")
    }

    green.turn {
      // "I pay all 28 money and one titanium to play 16 Psyche."
      playProject(SixteenPsyche, 28, titanium = 1).expect("PROD[2 T]")
    }

    yellow.pass()
    green.pass()

    // "I'm going to use World Government Terraforming to increase Venus."
    yellow.wgt("VenusStep").expect("0 TR")

    // board-11-17-20.jpg and both player ledgers: after Generation 2 transition, before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 20, s = 0, t = 6, p = 4, e = 0, h = 1)
      assertCounts(20 to "TR", 6 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(yellow) {
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
    green.buyCards(1)
    // Yellow corrected an initial purchase entry: "I'm buying zero cards."
    yellow.buyCards(0)

    green.turn {
      // "I play Imported Hydrogen... five titanium and one money. Put two animals on Pets."
      // "The ocean goes row four, column one... I get a plant and a card."
      playProject(ImportedHydrogen, 1, titanium = 5) {
            addCardResources(Pets)
            placeTile(4, 1)
          }
          .expect("2 Animal, P, ProjectCard, TR")
    }

    yellow.turn {
      // "I remove two floaters from Forced Precipitation and increase Venus."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, TR")
      // "I remove two floaters from Extractor Balloons and increase Venus."
      cardAction2(ExtractorBalloons).expect("-2 Floater, TR")
    }

    green.turn {
      // "I pay eight for Cartel... three money production."
      playProject(Cartel, 8).expect("PROD[3 M]")
    }

    yellow.turn {
      // "I play Colonizer Training Camp, paying four steel."
      playProject(ColonizerTrainingCamp, steel = 4)
    }

    green.turn { sellPatents(1) }

    yellow.turn {
      sellPatents(1)
      // "I play Beam from a Thorium Asteroid... two titanium and 26 money."
      playProject(BeamFromAThoriumAsteroid, 26, titanium = 2).expect("PROD[3 E, 3 H]")
    }

    green.turn {
      // "I pay four for Research Coordination."
      playProject(ResearchCoordination, 4)
    }

    yellow.turn {
      // "I'm going to add to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    green.turn {
      // "I pay four for Venus Governor... two money production."
      assignWildTag(ResearchCoordination, "VenusTag")
      playProject(VenusGovernor, 4).expect("PROD[2 M]")
    }

    yellow.pass()
    green.turn {
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
      green.pass()
    }

    // "[Green] increases temperature with World Government Terraforming."
    green.wgt("TemperatureStep").expect("0 TR")

    // Both player ledgers: after Generation 3 transition, before Research.
    with(green) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 4, p = 6, e = 0, h = 1)
    }
    with(yellow) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 23, s = 2, t = 1, p = 0, e = 8, h = 12)
    }
    assertSidebar(gen = 4, temp = -28, oxygen = 1, oceans = 1, venus = 8)

    yellow.buyCards(2)
    green.buyCards(2)

    yellow.turn {
      // "I'm trading with Pluto... paying three energy, and I get three cards."
      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 3 ProjectCard")
    }

    green.turn {
      // "Then I play Mars University for eight... discard one and draw one."
      playProject(MarsUniversity, 8) { doTask("-ProjectCard") }
    }

    yellow.turn {
      // "I pay seven for Flooding... row three, column one."
      playProject(Flooding, 7) { placeTile(3, 1) }.expect("3 P, TR")
      // "I use one Psychrophiles microbe to play Potatoes... lose two plants and get two money
      // production."
      playProject(Potatoes, 0) {
            doTask("PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("-Microbe, -2 P, PROD[2 M]")
    }

    green.turn {
      // "I pay three for Mercurian Alloys."
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(MercurianAlloys, 3)
    }

    yellow.turn {
      // "I'm just going to take my turn, and that is add to Psychrophiles."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    green.turn {
      // "I pay two money and two titanium for Asteroid Rights... it gets two asteroids."
      playProject(AsteroidRights, 2, titanium = 2).expect("2 Asteroid")
    }

    yellow.turn {
      // "I play Mine, paying two steel."
      playProject(Mine, steel = 2).expect("PROD[S]")
    }

    green.turn {
      // "I remove an asteroid and get two titanium."
      cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T")
    }

    yellow.turn {
      cardAction1(ForcedPrecipitation).expect("-2 MC, Floater")
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    green.turn {
      // "I pay five for Floating Habs."
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(FloatingHabs, 5)
    }

    yellow.turn {
      sellPatents(1)
      // "I spend 11 on Nitrate [sic] Reducing Bacteria."
      playProject(NitriteReducingBacteria, 11).expect("3 Microbe")
    }

    green.turn {
      // "I pay two with Floating Habs and put a floater on Aerial Mappers."
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
    }

    yellow.turn {
      // "I take three microbes off Nitrate Reducing Bacteria and gain a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    green.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    yellow.pass()
    green.pass()

    // "I'm going to increase Venus" with World Government Terraforming.
    yellow.wgt("VenusStep").expect("0 TR")

    // board-13-20-01.jpg and both player ledgers: after Generation 4 transition, before Research.
    with(green) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 7, p = 7, e = 0, h = 1)
      assertCounts(21 to "TR", 13 to "CardFront")
      assertCardResources(3 to Pets, 0 to AerialMappers, 1 to AsteroidRights)
    }
    with(yellow) {
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

    green.buyCards(1)
    yellow.buyCards(1)

    green.turn {
      // Point Luna tableau in board-13-46-12.jpg: "Energy Market. Cost me three."
      playProject(EnergyMarket, 3)
    }

    yellow.turn {
      // Valley Trust tableau in board-13-46-12.jpg: "Hydrogen to Venus. I spend two titanium and
      // five real... add two to Forced Precipitation."
      playProject(HydrogenToVenus, 5, titanium = 2) { addCardResources(ForcedPrecipitation) }
          .expect("2 Floater, TR")
      // User clarification: Yellow played Hermetic Order of Mars. Her ledger combines its six-M€
      // gain with the following twelve-M€ Stratospheric Birds payment.
      playProject(HermeticOrderOfMars, 10).expect("PROD[2 M]")
    }

    green.turn {
      // "Use my Energy Market to pay six, which gives me three energy, and then use that three
      // energy to send my little boat to Io and take ten heat."
      cardAction1(EnergyMarket, x = 3)
      stdAction("TradeAction", 2) { doTask("Trade<Io>") }.expect("-3 E, 10 H")
    }

    yellow.turn {
      // Valley Trust tableau in board-13-46-12.jpg: Stratospheric Birds. The played card consumes
      // one Forced Precipitation floater.
      playProject(StratosphericBirds, 12) { doTask("-Floater<$ForcedPrecipitation>") }
      // "I spend three energy to trade with Miranda. Three animals on Stratospheric Birds."
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(StratosphericBirds)
      }
    }

    green.turn {
      // "Big Asteroid... all titanium... overspending one... four titanium back, two temperature
      // boops... remove one plant."
      playProject(BigAsteroid, titanium = 7) { doTask("-Plant<Yellow>") }
          .expect("-3 T, 2 TemperatureStep, 2 TR, PROD[H]")
    }

    yellow.turn {
      // Yellow's ledger records two eight-heat conversions after Big Asteroid.
      convertHeat().expect("-8 H, TemperatureStep, TR")
      convertHeat().expect("-8 H, TemperatureStep, PROD[H], TR")
    }

    green.turn {
      // "Lunar Mining. It costs me 11... six Earth tags... six titanium production."
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(LunarMining, 11).expect("PROD[3 T]")
    }

    yellow.turn { cardAction1(StratosphericBirds).expect("Animal") }

    green.turn { cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T") }

    yellow.turn {
      cardAction1(ForcedPrecipitation).expect("-2 MC, Floater")
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    green.turn {
      // "Luna Metropolis... five titanium and one real money... seven money production."
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(LunaMetropolis, 1, titanium = 5).expect("PROD[7 M], Animal")
    }

    yellow.turn {
      cardAction1(Psychrophiles).expect("Microbe")
      cardAction1(NitriteReducingBacteria).expect("Microbe")
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    yellow.pass()
    green.turn {
      convertHeat().expect("-8 H, TemperatureStep, TR")
      pass()
    }

    // "World Government us an ocean... nine-eight."
    green.wgt("OceanTile<Utopia_9_8>").expect("0 TR")

    // Both player ledgers: after Generation 5 transition, before Research.
    with(green) {
      assertProduction(m = 12, s = 0, t = 6, p = 1, e = 0, h = 1)
      assertResources(m = 37, s = 0, t = 7, p = 8, e = 0, h = 4)
    }
    with(yellow) {
      assertProduction(m = 4, s = 3, t = 1, p = 0, e = 8, h = 4)
      assertResources(m = 33, s = 6, t = 1, p = 0, e = 8, h = 13)
    }
    assertSidebar(gen = 6, temp = -18, oxygen = 1, oceans = 3, venus = 12)

    green.buyCards(2)
    yellow.buyCards(1)

    yellow.turn {
      // "I will spend three energy to trade with Enceladus. That is five microbes going to
      // Nitrate Reducing Bacteria."
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    green.turn {
      // "I'm going to play Industrial Microbes for full price. And now I'm going to pay eight to
      // become the Ecologist."
      playProject(IndustrialMicrobes, 12).expect("PROD[S, E]")
      assignWildTag(ResearchCoordination, "MicrobeTag")
      stdAction("ClaimMilestone") { doTask("Ecologist") }
    }

    // Green never narrated or logged Industrial Microbes' steel and energy production; both remain
    // absent from the generation-seven photograph and ledger.
    green.exMachina("PROD[-S, -E]")

    yellow.turn {
      // "Forced Precipitation and Extractor Balloons. Remove two off both of them to raise Venus
      // by two. Oh, it is at 16, which means I get an extra TR."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, TR")
      cardAction2(ExtractorBalloons).expect("-2 Floater, 2 TR")
    }
    green.turn {
      // "Import some GHG for two titanium, one real money, draw a card, get two heat production."
      playProject(ImportOfAdvancedGhg, 1, titanium = 2).expect("PROD[2 H]")
      // "For my second, let's just get this other milestone taken care of. Eight to be the
      // Metallurgist."
      stdAction("ClaimMilestone") { doTask("Metallurgist") }
    }
    yellow.turn { stdAction("ClaimMilestone") { doTask("Tactician") } }
    green.turn {
      // "Use Floating Habs to spend two money to put a floater on Aerial Mappers, and use that to
      // draw a card."
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }
    yellow.turn { cardAction1(Psychrophiles).expect("Microbe") }
    green.turn {
      sellPatents(1)
      // "Hired Raiders, pay one... I'm going to take three money."
      playProject(HiredRaiders, 1) { doTask("3 M<Green> FROM M<Yellow>") }
    }
    yellow.turn {
      // "Nitrate Reducing Bacteria. I will reduce the nitrates. Spend three of them to gain a TR."
      // The transcript places this immediately after Hired Raiders; move it to the preceding legal
      // Yellow turn rather than assigning any of Green's photographed cards to her.
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }
    green.turn {
      // "Use Asteroid Rights to spend one of my three money to put an asteroid on Asteroid Rights."
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }.expect("-1 MC, Asteroid")
    }
    yellow.turn {
      // "Before I forget, I will heat boop."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }
    green.turn {
      // "Use Energy Market to spend my last two money to get one energy resource."
      cardAction1(EnergyMarket, x = 1).expect("-2 M, Energy")
      // "I'm going to convert plants and get in this spot where I get a plant and four money."
      convertPlants { placeTile(4, 2) }.expect("-7 P, 4 M, OxygenStep, TR")
    }

    // Green accidentally took another TR, not realizing the app gave it to him already
    green.exMachina("TR")

    yellow.turn {
      // "Noctis City... six steel and six real... place a city tile... on three-two."
      playProject(NoctisCity, 6, steel = 6) { placeTile(3, 2) }
          .expect("PROD[3 M, -E], -4 M, Animal<Green>")
    }
    green.pass()
    yellow.turn {
      // "I stratobird. Five. I get silver stratobirds."
      cardAction1(StratosphericBirds).expect("Animal")
    }
    yellow.pass()

    // "I will World Government an ocean on six-four."
    yellow.wgt("OceanTile<Utopia_6_4>").expect("0 TR")

    // board-13-46-12.jpg and both player ledgers: after Generation 6 transition.
    with(green) {
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
    with(yellow) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 55, s = 3, t = 2, p = 0, e = 7, h = 14)
      assertCounts(
          33 to "TR",
          17 to "CardFront",
          1 to "Tactician",
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
    green.exMachina("-TR, -1 MC, PROD[S, E], S, E")

    green.buyCards(2)
    green.exMachina("6 MC") // And then I immediately screwed up and forgot to pay for my cards!
    yellow.buyCards(3)

    green.turn {
      // Green: "Sure. Let's pay eight to fund Traveller. I have funded the Traveller award."
      stdAction("FundAward") { doTask("Traveller") }
    }

    yellow.turn {
      // Yellow: "I pay one for Market Manipulation. Increase the colony track one step."
      // Green: "So she's increasing Pluto." Yellow: "Yes. Decrease Io."
      playProject(MarketManipulation, 1) {
        doTask("ColonyProduction<Pluto> FROM ColonyProduction<Io>")
      }
      // Yellow: "Then I will spend three energy to trade with Pluto, which now gives me three
      // cards." Green: "Nice. Three cards free and clear."
      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 3 ProjectCard")
    }

    green.turn {
      // Green: "I guess I'll play a Martian Zoo. I believe that cost me full price. I want to pay
      // 12."
      playProject(MartianZoo, 12)
    }

    yellow.turn {
      // Yellow: "Io Sulphur Research for 17. I have three Venus tags from the two floater cards and
      // Strata Birds, so three cards."
      playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") }

      // She forgot her Valley Trust discount
      yellow.exMachina("-2 MC")
    }

    green.turn {
      // Green: "I'm going to play Nuclear Power. That cost me ten. I lose two money production. I
      // gain three energy production."
      playProject(NuclearPower, 10).expect("PROD[-2 M, 3 E]")
    }

    yellow.turn {
      // Yellow: "Air-Scrapping Expedition for 13. Raise Venus one step, and I get a TR. Add three
      // floaters to a Venus card. That'll be Forced Precipitation."
      playProject(AirScrappingExpedition, 13) { addCardResources(ForcedPrecipitation) }
          .expect("VenusStep, TR, 3 Floater")
    }

    green.turn {
      // Green: "I am going to play Miranda—Miranda Resort. I guess it costs me three titanium. And
      // I
      // get one, two, three, four, five, six, seven money production."
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(MirandaResort, titanium = 3).expect("PROD[7 M]")
    }

    yellow.turn {
      // Yellow: "I used Forced Precipitation. Remove two floaters to increase Venus."
      cardAction2(ForcedPrecipitation).expect("-2 Floater, VenusStep, TR")
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
    }

    yellow.turn { cardAction1(ExtractorBalloons).expect("Floater") }

    green.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    yellow.turn { cardAction1(StratosphericBirds).expect("Animal") }

    green.turn {
      // Green: "I'm going to play Business Contactos, which cost me seven of my nine money. I look
      // at
      // the top four cards and I pick two of them. Then, because I played an Earth tag, I draw a
      // card and I get a little aminal on Martian Zoo."
      playProject(BusinessContacts, 7).expect("2 ProjectCard, Animal")
    }

    yellow.turn { cardAction1(Psychrophiles).expect("Microbe") }

    green.turn {
      // Green: "I'm going to import some Nitrogen. I'm going to slightly overspend by spending six
      // titanium. I will draw the card for the Earth tag. I'll get a TR. I'll get four plants. I
      // don't have a microbe card. And I think I'm going to take two animals on Martian Zoo."
      playProject(ImportedNitrogen, titanium = 6) { addCardResources(MartianZoo) }
          .expect("TR, 4 P, 3 Animal, 0 ProjectCard")
    }

    // I forgot the extra animal from MZ's effect
    green.exMachina("-Animal<$MartianZoo>")

    yellow.turn {
      // Yellow: "Nitrite Reducing Bacteria. I remove three and get a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    // It looks like she forgot to take her TR
    yellow.exMachina("-TR")

    green.turn {
      // Green: "Now I'm going to use Asteroid Rights to remove an asteroid from Asteroid Rights.
      // And
      // honestly, I think I'll take the money production."
      cardAction2(AsteroidRights) { doTask("PROD[1 MC]") }.expect("-Asteroid, PROD[M]")
    }

    yellow.turn { convertHeat().expect("-8 H, TemperatureStep, TR") }

    green.turn {
      // Green: "I'll take the Martian Zoo action to take three money."
      cardAction1(MartianZoo).expect("3 MC")
    }

    yellow.turn {
      // Yellow: "Neutralizer Factory. Pay seven. We've definitely met the ten percent Venus
      // requirement. Increase Venus one step."
      playProject(NeutralizerFactory, 7).expect("VenusStep, TR")
    }

    green.turn {
      // Green: "Venusian Insects is the card that I played and spent five on."
      playProject(VenusianInsects, 5)
    }

    yellow.pass()
    green.turn {
      // Green: "Now I'm taking its action, putting a microbe on it. On it like a bonnet."
      cardAction1(VenusianInsects).expect("Microbe")
      // Green: "I think I will use Energy Market to reduce my energy production by one and get
      // eight
      // money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8 MC")
      // Green: "Then I have enough money for Nitrophilic Moss. We do have the three-ocean
      // requirement. And I will lose two plants and gain two plant production. And that costs the
      // eight money."
      playProject(NitrophilicMoss, 8).expect("-2 P, PROD[2 P]")
      pass()
    }

    // Green uses World Government Terraforming to increase Venus.
    green.wgt("VenusStep").expect("0 TR")

    // Both player ledgers: after Generation 7 transition, before Research.
    with(green) {
      assertProduction(m = 18, s = 1, t = 6, p = 3, e = 3, h = 3)
      assertResources(m = 44, s = 2, t = 8, p = 7, e = 3, h = 12)
      assertCounts(26 to "TR")
      assertCardResources(3 to MartianZoo, 1 to VenusianInsects)
    }
    with(yellow) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 52, s = 6, t = 3, p = 0, e = 7, h = 14)
      assertCounts(37 to "TR")
    }
    assertSidebar(gen = 8, temp = -14, oxygen = 2, oceans = 4, venus = 24)

    green.buyCards(2)
    yellow.buyCards(1)

    // I suddenly realized my mistake last turn when I forgot to pay for my cards!
    // So I pay for them now, and realize I wouldn't have afforded VI last round.
    // I let it stay, but reason that it should lose a microbe that I wouldn't have had the
    // chance to play.
    green.exMachina("-6 MC, -Microbe<$VenusianInsects>")

    yellow.turn {
      // Yellow: "Ice Moon Colony. I will pay two of my three titanium for six money, and then 17
      // real. I will place on Miranda. And it gives me an animal to my Strata Birds. I place an
      // ocean tile." Green: "It's row eight." Yellow: "Four. Eight-four."
      playProject(IceMoonColony, 17, titanium = 2) {
            doTask("Colony<Miranda>")
            addCardResources(StratosphericBirds)
            placeTile(8, 7)
          }
          .expect("Animal, OceanTile, TR, -15 M, 2 P")
      // Yellow: "And for my second action, three energy to trade with Miranda. I get two aminals
      // and
      // a card."
      stdAction("TradeAction", 2) {
            doTask("Trade<Miranda>")
            addCardResources(StratosphericBirds)
          }
          .expect("-3 E, 2 Animal, ProjectCard")
    }

    green.turn {
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
    }

    yellow.turn {
      // Yellow: "I remembered why I left a titanium, and that is so I can play Diversity Support.
      // I've got all six standard resources and microbes, animals, floaters. So pay one money, get
      // one TR."
      playProject(DiversitySupport, 1).expect("TR")
    }

    green.turn {
      // Green: "It'll involve the playing of Plantation, which I can do because I have a science
      // tag
      // and a wild tag. I've gotten so much use out of this wild tag. So that costs me 15 entire
      // money. And then I place a greenery tile, which I'm going to place at five-three."
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(Plantation, 15) { placeTile(5, 3) }.expect("GreeneryTile, OxygenStep, TR, -13 M")
      // Green: "Then I'm going to Kaguya its ass. I'm playing Kaguya Tech for ten full money. I get
      // two money production. I get a card. I swap this greenery tile. I flip it, basically."
      playProject(KaguyaTech, 10) { doTask("CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>") }
          .expect("PROD[2 M], 0 ProjectCard, -GreeneryTile, CityTile, Animal<$Pets>, -8 M")
    }

    // The Generation 9 photograph still has five animals on Pets, so Green missed the animal caused
    // by Kaguya Tech's city placement.
    green.exMachina("-Animal<$Pets>")

    yellow.turn {
      // Yellow: "Right. Venusian Animals." Green: "Oh my god." Yellow: "Yep. It immediately adds an
      // animal to itself."
      playProject(VenusianAnimals, 13).expect("Animal")
    }

    // Again forgot her Valley Trust discount.
    yellow.exMachina("-2 MC")

    green.turn {
      // Green: "I'm gonna go ahead and use three real and seven titanium. And I'm going to get four
      // plant production, two TR plus another TR for raising temp to minus 12."
      playProject(NitrogenRichAsteroid, 3, titanium = 7) { doTask("PROD[4 Plant]") }
          .expect("PROD[4 P], 3 TR, TemperatureStep")
    }

    yellow.turn { cardAction2(ForcedPrecipitation).expect("-2 Floater, VenusStep, TR") }

    green.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    yellow.turn { cardAction1(ExtractorBalloons).expect("Floater") }

    green.turn {
      // Green: "Business Network. Cost me four. I lose a money production. Aw, I only have 19 money
      // production now. And I get an animal on Martian Zoo and a card."
      playProject(BusinessNetwork, 4).expect("PROD[-M], Animal, 0 ProjectCard")
    }

    yellow.turn { cardAction1(Psychrophiles).expect("Microbe") }

    green.turn { cardAction2(EnergyMarket).expect("PROD[-E], 8 MC") }

    yellow.turn { convertHeat().expect("-8 H, TemperatureStep, TR") }

    green.turn {
      // Green: "I lose two steel and I get four money back. All right, so in the end what happened
      // is
      // I paid two steel and two real. I gain an energy production and you lose two heat
      // production."
      playProject(HeatTrappers, 2, steel = 2) { doTask("PROD[-2 H<Yellow>]") }
          .expect("PROD[E<Green>, -2 H<Yellow>]")
    }

    yellow.turn {
      // Yellow: "Luckily I have this power tag so I can play Power Supply Consortium."
      // Green: "I lose an energy production." Yellow: "I pay five and I gain an energy production."
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-E<Green>]") }
          .expect("PROD[-E<Green>, E<Yellow>]")
    }

    green.turn {
      // Green: "I guess that means if I want to trade, I better do it now. I'll trade for three
      // energy and I'll do Europa. So I get a plant production."
      stdAction("TradeAction", 2) { doTask("Trade<Europa>") }.expect("-3 E, PROD[P]")
    }

    yellow.turn { cardAction1(StratosphericBirds).expect("Animal") }

    green.turn {
      // Green: "All right, I am going to import some zhuzh. This time it's just Imported Zhuzh. I'm
      // going to pay seven real for it. I get one heat production, three heat. I get a silver
      // animal on Martian Zoo. I get a card."
      playProject(ImportedGhg, 7).expect("PROD[H], 3 H, Animal, 0 ProjectCard")
    }

    yellow.turn {
      // Yellow: "Imported Nutrients. I pay a titanium and 11 real, gain four plants, and add four
      // microbes to Nitrite-Reducing Bacteria." Green: "Man, you're just churning that thing."
      playProject(ImportedNutrients, 11, titanium = 1) { addCardResources(NitriteReducingBacteria) }
          .expect("4 P, 4 Microbe")
    }

    green.turn { cardAction1(VenusianInsects).expect("Microbe") }

    yellow.turn { cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR") }

    green.turn {
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }.expect("-1 MC, Asteroid")
    }

    yellow.pass()
    green.turn {
      // Green: "I will use Martian Zoo to take five money."
      cardAction1(MartianZoo).expect("5 MC")
      // Green: "It appears I never used Business Network. So I'm going to use Business Network to
      // look at a card. I think that makes me want to take it more, so I will pay three for it."
      cardAction1(BusinessNetwork) { green.buyCards(1) }.expect("-3 MC, ProjectCard")
      pass()
    }

    // Yellow uses World Government Terraforming to increase oxygen.
    yellow.wgt("OxygenStep").expect("0 TR")

    // board-21-13-43.jpg, board-21-14-23.jpg, and both player ledgers: after Generation 8
    // transition.
    with(green) {
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
    with(yellow) {
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
        2 to "TradeFleet",
        0 to "Trade",
    )
    assertColonyProductions(3, 2, 1, 3, 2)
    assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 5, venus = 26)

    // Before Generation 9 Research, both players reconciled the physical table against their
    // resource logs. Green took three M€ and an animal on Martian Zoo and discarded a card; Yellow
    // took six M€ and one TR.
    green.exMachina("3 MC, Animal<$MartianZoo>, -ProjectCard")
    yellow.exMachina("6 MC, TR")

    // "You kept all your cards?" "Bada-bing. Yeah. I just hated to give them up."
    green.buyCards(4).expect("-12 MC, 4 ProjectCard")
    yellow.buyCards(4).expect("-12 MC, 4 ProjectCard")

    green.turn {
      // "I'm planting a forest. I'm eternally hopeful. I'm going to place on six, four to get two
      // steel and two money."
      convertPlants { placeTile(6, 3) }.expect("-8 P, 2 S, 2 M, TR")
      // "Well, let us just go ahead and use floating hubs to put a cube on aerial mappers."
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
    }

    yellow.turn {
      // "I'm going to use my extractor balloons, spend the two floaters, increase Venus. To 28."
      cardAction2(ExtractorBalloons).expect("-2 Floater, VenusStep, TR")
      // "Then I'm going to spend three floaters, trade with Aran. This might not be the right call,
      // but enchiladas, actually." "Yeah, gain three microbes."
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    // "I will use aerial mappers to remove a floater from aerial mappers and get a card."
    green.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    yellow.turn {
      // "Nine for sponsored academies." "I pitch a card." "And then you get three cards and I also
      // get a card."
      playProject(SponsoredAcademies, 7)
          .expect("ProjectCard<Green>, ProjectCard<Yellow>, Animal<Yellow>")
      // "La Grange Observatoire." "One titanium and four money." "I believe I get a card."
      playProject(LagrangeObservatory, 4, titanium = 1)
          .expect("0 ProjectCard, Animal<$VenusianAnimals>")
    }

    // "I am going to use my business network to look at this card." "I will not buy this card."
    green.turn {
      cardAction1(BusinessNetwork) { /* Decline buying the revealed card. */
        green.buyCards(0)
      }
    }

    yellow.turn {
      // "May as well. Spend all nine of my steel on aquifer pumping."
      playProject(AquiferPumping, steel = 9)
    }

    green.turn {
      // "I'm gonna play advanced alloys." "It costs me nine." "No discounts."
      playProject(AdvancedAlloys, 9) { doTask("-ProjectCard") }.expect("-ProjectCard")
      // "I am gonna go ahead and play Solar Logistics." "But I spend four titanium on that." "I get
      // two titanium from it. I get a minimal on Martian Zoo. I get a card."
      playProject(SolarLogistics, titanium = 4).expect("-2 T, Animal<$MartianZoo>")
    }

    yellow.turn {
      // "I spend eight on aquifer pumping. The action, that is." "Row seven, column six, I
      // believe."
      // "Yes, two plants, two money."
      cardAction1(AquiferPumping) {
            pay(8)
            placeTile(7, 6)
          }
          .expect("2 P, -6 M, TR")
    }

    green.turn {
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
      stdProject("AquiferSP") { placeTile(4, 5) }.expect("2 P, TR")
    }

    // "I spend 26 money. Lose two energy productions. Gain five money productions." "Place." "It'll
    // go right here. That would be row six, column five. Yeah. For six money and two plants."
    // "Actually, any chance I can undo and play conscription first?" "Yeah, sure."
    yellow.turn {
      playProject(Conscription, 5)
      playProject(Capital, 10) { placeTile(6, 5) }
          .expect("PROD[5 M, -2 E], -4 M, 2 P, Animal<Green>")
    }

    green.turn {
      // "Three steel, nine MC. That gets me tectonic stress power." "And so I get my
      // three energy production."
      playProject(TectonicStressPower, 9, steel = 3).expect("PROD[3 E]")
    }

    yellow.turn {
      // "I'm just gonna heat boop." "A heat boop has been done. That means converting heat to
      // temperature."
      convertHeat().expect("-8 H, TemperatureStep, TR")
      // "I will use nitrate-reducing bacteria, remove three microbes, gain a TR."
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    green.turn {
      // "I will use asteroid rights to take one asteroid off of asteroid rights and give myself
      // two titanium."
      cardAction2(AsteroidRights) { doTask("2 T") }.expect("-Asteroid, 2 T")
      // "I will plant a greenery or plant a forest, as they like to call it on this app." "I'll
      // put it next to my city for two money."
      convertPlants { placeTile(5, 2) }.expect("-8 P, 2 M, TR")
    }

    yellow.turn {
      // "You know, it occurred to me I can probably spend 15 on a final Venus boop."
      stdProject("AirScrappingSP")
      // "I'm going to psychrophile."
      cardAction1(Psychrophiles).expect("Microbe")
    }

    green.turn {
      // "Well, I'm going to heat boop." "I'm going to heat boop. I didn't move it either time."
      convertHeat().expect("-8 H, TemperatureStep, TR")
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    // "Anyways, I stratoburb."
    yellow.turn { cardAction1(StratosphericBirds).expect("Animal") }

    // "Energy market. Reduce energy production to four, gain eight money."
    green.turn { cardAction2(EnergyMarket).expect("PROD[-E], 8 MC") }

    yellow.pass()
    green.turn {
      // "I'm going to play Lunar Exports, which costs me three titanium and four money." "And I
      // get a card from Point Luna." "I'm actually going to take the money production."
      playProject(LunarExports, 2, titanium = 3) { doTask("PROD[5 MC]") }
          .expect("PROD[5 M], Animal<$MartianZoo>")
      // "Let's play Solar Net for seven real money. I draw two cards."
      playProject(Solarnet, 7).expect("ProjectCard")
      // "Let's play Algae for ten money." "I get two plant production and one plant."
      playProject(Algae, 10).expect("PROD[2 P], P")
      // "I will go ahead and use my Martian Zoo now to take eight money."
      cardAction1(MartianZoo).expect("8 MC")
      // "I will use my Venusian Insects to take a Venusian insect, which is apparently a kind of
      // microbe now."
      cardAction1(VenusianInsects).expect("Microbe")
      assignWildTag(ResearchCoordination, "PlantTag")
      // "Okay, here goes insects." "And I get one, two, three, four, five, five plant production."
      playProject(Insects, 9).expect("PROD[5 P]")
      pass()
    }

    // "The world government is me, and well, I'm not going to do oxygen. I do temperature up to
    // minus two."
    green.wgt("TemperatureStep").expect("0 TR")

    // board-16-19-30.jpg and both player ledgers: after Generation 9 transition.
    with(green) {
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
    with(yellow) {
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
    green.assertCounts(
        1 to "GreeneryTile<Utopia_6_3>",
        1 to "GreeneryTile<Utopia_5_2>",
    )
    yellow.assertCounts(1 to "CityTile<Utopia_6_5>")
    assertSidebar(gen = 10, temp = -2, oxygen = 6, oceans = 9, venus = 30)

    // "Well, I'm buying three cards." "All right, [Yellow] buys three cards, and I'm going to
    // stupidly
    // buy two cards. You know what? I'm going to buy three cards. Talk about stupid. Three cards."
    green.buyCards(3).expect("-9 MC, 3 ProjectCard")
    yellow.buyCards(3).expect("-9 MC, 3 ProjectCard")

    yellow.turn {
      // "I will plant forest, put my greenery on... Looks like 5-5, right?" "Okay. Two money and a
      // plant."
      convertPlants { placeTile(5, 5) }.expect("-7 P, TR")
      // "And I will greenery standard project." "But you've got two TR from one move." "Yeah, and
      // an extra for being the one to get it." "Anyways, the second one goes... 2-1."
      stdProject("GreenerySP") { placeTile(2, 1) }.expect("2 TR")
    }

    green.turn {
      // "I am feeling like I had better put a cute little city down while I can. So I paid for
      // standard project." "4-4 for two money and two plants."
      stdProject("CitySP") { placeTile(4, 4) }
      // "Ecological zone. Cost me 12 entire." "Well, for these two, I get two animals right away."
      // "Putting it on 2-2?" "Yes. For two steel."
      playProject(EcologicalZone, 12) { placeTile(2, 2) }
          .expect("2 Animal<$EcologicalZone>, -ProjectCard")
    }

    yellow.turn {
      // "Eight for cryosleep because I have science tag discount. Pay eight. And I get a new
      // animal."
      playProject(CryoSleep, 8).expect("Animal<$VenusianAnimals>, -ProjectCard")
      // "And I spend two energy to trade with Miranda for two animals and a card."
      stdAction("TradeAction", 2) {
            doTask("Trade<Miranda>")
            addCardResources(StratosphericBirds)
          }
          .expect("-2 E, 2 Animal<$StratosphericBirds>, ProjectCard")
    }

    green.turn {
      // "I will use my business network to look at a card. Absolutely not."
      cardAction1(BusinessNetwork) { /* Decline buying the revealed card. */
        green.buyCards(0)
      }
      // "Heat boob." "Now your turn."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    yellow.turn {
      // "Back to viral research, baby. Cost you eight?" "Yes. Almost forgot. Draw one card."
      // "I will choose Nitrate Reducing Bacteria." "Six microbes."
      playProject(BactoviralResearch, 8) { addCardResources(NitriteReducingBacteria) }
          .expect(
              "6 Microbe<$NitriteReducingBacteria>, 0 Animal<Green, $EcologicalZone<Green>>, 0 ProjectCard"
          )
    }

    green.turn {
      // "Herbivores, again with the full price." "I do add an animal to this card and an animal to
      // Ecozone. And you lose a plant production." "Oh, shit, I don't have any plant production."
      playProject(Herbivores, 12)
          .expect("Animal<$Herbivores>, Animal<$EcologicalZone>, PROD[0 P<Yellow>], -ProjectCard")
    }

    yellow.turn {
      // "Jovian lanterns for 20." "Increase your TR one step." "Add two floaters to any card. I
      // will
      // add it to itself."
      playProject(JovianLanterns, 20) { addCardResources(JovianLanterns) }
          .expect("TR, 2 Floater<$JovianLanterns>, -ProjectCard")
    }

    green.turn {
      // "Plant boop, plant boop." "So two money and two plants." "And played greenery. So I add a
      // minimal to herbivores. I add two of them."
      convertPlants { placeTile(4, 3) }.expect("-7 P, TR, Animal<$Herbivores>")
      convertPlants { placeTile(5, 4) }.expect("-7 P, TR, Animal<$Herbivores>")
    }

    yellow.turn {
      // "I use Jovian Lantern, spend a titanium to add two floaters here."
      cardAction1(JovianLanterns).expect("-T, 2 Floater<$JovianLanterns>")
      // "Actually, I'm just going to go like add a thing to extractor balloons."
      cardAction1(ExtractorBalloons).expect("Floater")
    }

    green.turn {
      // "I think I'm going to use Martian Zoo to take eight money and then spend eighteen money on
      // lava flows." "Two cards and four money."
      cardAction1(MartianZoo).expect("8 MC")
      playProject(LavaFlows, 18) { placeTile(8, 5) }
          .expect("-14 MC, ProjectCard, 2 TemperatureStep, 2 TR")
    }
    // Green's ledger omitted the two TR from Lava Flows' temperature steps.
    green.exMachina("-2 TR")

    yellow.turn {
      // "Oh, I add a Strato Bird."
      cardAction1(StratosphericBirds).expect("Animal")
      // "Oh, probably be smart for me to do my own heat boob."
      convertHeat().expect("-8 H, TemperatureStep, TR")
    }

    green.turn {
      // "It's weird, but I'm gonna play a card I've never played before in my life. Food Factory."
      // "And three real gives me four money production. Takes away one of my plant production."
      playProject(FoodFactory, 3, steel = 3).expect("PROD[4 M, -P], -ProjectCard")
    }

    yellow.turn {
      // "I will add Psychrophile and I will remove three nitrites for a TR."
      cardAction1(Psychrophiles).expect("Microbe")
      cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR")
    }

    green.turn {
      // "I'm going to sell a patent to get a money and then spend two money on floating habs. To
      // use
      // floating habs to put a floater onto aerial mappers."
      sellPatents(1).expect("1 MC, -ProjectCard")
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
    }

    yellow.turn {
      // "I'll pay three psychrophiles for green houses." "Gain one plant for each city tile in
      // play. That's one, two, three, four, five."
      playProject(Greenhouses, 0) {
            doTask("3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("5 P, 0 Animal<Green, $EcologicalZone<Green>>, -ProjectCard")
      // "And I will greenery boop." "It's six, six, sorry." "It's the last possible spot next to my
      // capital for two money."
      convertPlants { placeTile(6, 6) }.expect("-8 P, 2 M, TR")
    }

    // "I'm going to use aerial mappers to take a floater off of aerial mappers and draw a card."
    green.turn { cardAction2(AerialMappers).expect("-Floater, ProjectCard") }

    yellow.pass()
    green.turn {
      // "And then I'm going to use energy market to reduce energy production and give myself eight
      // money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8 MC")
      // "I guess I play Dawn City for three titanium." "I lose an energy production. I gain a
      // titanium production." "And so when I place that, I believe I get a pet."
      // "Oh shit. I don't." "Okay, since I already committed to it, what I will do is I will sell a
      // patent to get one money, spend nine money on robotic workforce just to make an honest card
      // out of it." "I'll copy industrial microbes." "Robotic workforce in Dawn City, we pretended
      // they were in that order."
      sellPatents(1).expect("1 MC, -ProjectCard")
      playProject(RoboticWorkforce, 9) {
            doTask("-ProjectCard")
            doTask("CopyProductionBox<$IndustrialMicrobes>")
          }
          .expect("-9 MC, -ProjectCard, PROD[S, E]")
      assignWildTag(ResearchCoordination, "ScienceTag")
      playProject(DawnCity, titanium = 3).expect("PROD[-E, T], Animal<$Pets>, -ProjectCard")
      // "I'm gonna add a Venusian insect."
      cardAction1(VenusianInsects).expect("Microbe")
      // "I'm going to yet again, sell a patent for one money and spend that one money on asteroid
      // rights to put an asteroid onto that."
      sellPatents(1).expect("1 MC, -ProjectCard")
      cardAction1(AsteroidRights) { addCardResources(AsteroidRights) }.expect("-1 MC, Asteroid")
      // "But I'll take the cards using three energy for the Pluto, take two cards."
      stdAction("TradeAction", 2) { doTask("Trade<Pluto>") }.expect("-3 E, 2 ProjectCard")
      pass()
    }

    // Yellow uses World Government Terraforming to raise oxygen to 12%.
    yellow.wgt("OxygenStep").expect("0 TR")

    // board-16-44-30.jpg and both player ledgers: after Generation 10 transition.
    with(green) {
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
    with(yellow) {
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
    green.assertCounts(
        1 to "CityTile<Utopia_4_4>",
        1 to "SpecialTile<Utopia_2_2>",
        1 to "GreeneryTile<Utopia_4_3>",
        1 to "GreeneryTile<Utopia_5_4>",
        1 to "SpecialTile<Utopia_8_5>",
    )
    yellow.assertCounts(
        1 to "GreeneryTile<Utopia_5_5>",
        1 to "GreeneryTile<Utopia_2_1>",
        1 to "GreeneryTile<Utopia_6_6>",
    )
    assertSidebar(gen = 11, temp = 8, oxygen = 12, oceans = 9, venus = 30)

    // "Man. Yeah. I'm buying two. I'll buy one. I'm gonna actually buy it."
    green.buyCards(1).expect("-3 MC, ProjectCard")
    yellow.buyCards(2).expect("-6 MC, 2 ProjectCard")

    green.turn {
      // "Yep. Boop, boop. Indeed. And the game will officially end this round."
      // "One goes here for just two money. That is three, four." "I think I'll just take the four
      // money down here."
      convertPlants { placeTile(3, 4) }.expect("-8 P, 2 M, TR")
      // The second placement is gesture-only in the transcript; its four-M€ ocean income locates
      // it at the open land area between the row-six and row-seven oceans.
      convertPlants { placeTile(7, 4) }.expect("-8 P, 4 M, TR")
    }

    yellow.turn {
      // "I will... pay two energy to trade with Miranda... for one aminal and a card. This time
      // I'll
      // put the aminal on Venusian."
      stdAction("TradeAction", 2) {
            doTask("Trade<Miranda>")
            addCardResources(VenusianAnimals)
          }
          .expect("-2 E, Animal<$VenusianAnimals>")
      // "Productive outpost for zero... Gain all my colony bonuses, which is literally just draw a
      // card."
      playProject(ProductiveOutpost, 0).expect("0 ProjectCard")
    }

    green.turn {
      // "I pay fourteen... Mogul." "Yeah. I think I got that one."
      stdAction("FundAward", which = 2) { doTask("Mogul") }
      // "Listen, all of y'all. It's sabotage. So... You lose... Seven money, and that's it."
      playProject(Sabotage, 1) { doTask("-7 M<Yellow>") }.expect("-ProjectCard")
    }

    yellow.turn {
      // "I'm going to spend thirteen money, no titanus." "Lose two money production." "Place a
      // colony on Pluto. To get two cards."
      playProject(PioneerSettlement, 13) { doTask("Colony<Pluto>") }
          .expect("-13 MC, PROD[-2 M], ProjectCard")
    }

    green.turn {
      // "I'm going to spend two on floating habs. To put a dingus on aerial mappers." "Use aerial
      // mappers to draw a card."
      cardAction1(FloatingHabs) { addCardResources(AerialMappers) }.expect("-2 MC, Floater")
      cardAction2(AerialMappers).expect("-Floater, ProjectCard")
    }

    yellow.turn { cardAction1(JovianLanterns) }

    green.turn {
      // "Immigrant City, spending six worth of steel and seven rail." "I better decrease my energy
      // production, decrease my money production by two and then back up one." "Five, six, on five,
      // six."
      playProject(ImmigrantCity, 7, steel = 2) { placeTile(5, 6) }
          .expect("PROD[-M, -E], -5 M, 2 P, Animal<$Pets>, -ProjectCard")
    }

    yellow.turn { cardAction2(NitriteReducingBacteria) }

    green.turn {
      // "I'm gonna put it right here, two energy."
      // The City standard project and Immigrant City each increase M€ production for this
      // placement.
      stdProject("CitySP") { placeTile(2, 3) }.expect("PROD[2 M]")
      // "And for my second trick, commercial district, from sixteen, lose an energy production,
      // gain four money production, place a shitty tile, not a shitty tile."
      playProject(CommercialDistrict, 16) { placeTile(3, 3) }.expect("PROD[4 M], -ProjectCard")
    }
    // Green confirms he forgot Immigrant City's trigger. His ledger records only the standard
    // project's one M€ production step, so remove the omitted Immigrant City step here.
    green.exMachina("PROD[-M]")

    yellow.turn { cardAction1(Psychrophiles) }

    green.turn {
      // "I'm gonna play robot pollinators for all of my money. It gives me a plant production and
      // one plant per plant tag. One, two, three, four, five. Five plants."
      playProject(RobotPollinators, 9).expect("PROD[P], -ProjectCard")
      // "I'm just gonna do the plant boop now." "Did not give me TR good." "This plant boop will go
      // here for two energy and a card. That is two, four."
      convertPlants { placeTile(2, 4) }.expect("-8 P, 2 E, ProjectCard, 0 TR")
    }

    // "I'm gonna add a strato bird." "I'm at 15 strato birds."
    yellow.turn { cardAction1(StratosphericBirds) }

    green.turn {
      // "Methane from Titan." "I'm gonna spend six titanium." "I mostly played it for the two
      // points."
      intentionalOverpay(2)
      playProject(MethaneFromTitan, titanium = 6).expect("PROD[2 P, 2 H], -ProjectCard")
    }

    // "I add an extractor balloon."
    yellow.turn { cardAction1(ExtractorBalloons) }

    // "I'll go ahead and use Martian Zoo to take eight money."
    green.turn { cardAction1(MartianZoo) }

    yellow.turn {
      // "I'll use local heat trapping. One money. Spend five heat. And I will add two Venusian
      // animals."
      playProject(LocalHeatTrapping, 1) { addCardResources(VenusianAnimals) }
    }

    green.turn {
      // "Trading colony for my four titanium." "No, no, no. I'm gonna get three microbes." "Three
      // microbes which go on to Venusian insects."
      playProject(TradingColony, titanium = 4) {
            doTask("Colony<Enceladus>")
            addCardResources(VenusianInsects)
          }
          .expect("PROD[0 M], -ProjectCard")
    }

    yellow.turn {
      // "Airliners for 11 requires that you have three floaters." "Gain two money production, add
      // two floaters to another card, which will be Jovian lanterns."
      playProject(Airliners, 11) { addCardResources(JovianLanterns) }.expect("PROD[2 M]")
    }

    green.turn {
      // "Now, I'm going to fly my little boat to Angelatus." "I get three and one." "They all four
      // go on to the New Zealand insects."
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        doTask("ColonyProduction<Enceladus>")
        doTask("3 Microbe<$VenusianInsects>")
        doTask("Microbe<$VenusianInsects>")
      }
    }

    yellow.turn {
      // "My seven psychrophiles and three real." "Increase money production two steps. Increase
      // plant production three steps. Increase... No, gain two plants."
      playProject(KelpFarming, 3) {
            doTask("7 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>")
          }
          .expect("PROD[2 M, 3 P]")
    }

    green.turn {
      // "Just to be funny, I'm going to play one for land claim, just so you can go there."
      // The source does not identify the claimed area; Utopia_1_1 is a neutral test inference.
      playProject(LandClaim, 1) { doTask("LandClaimMarker<Utopia_1_1>") }
          .expect("-1 MC, -ProjectCard")
    }

    // "I sell a card for a money."
    yellow.turn { sellPatents(1).expect("1 MC, -ProjectCard") }

    // "I'll go ahead and sell five cards for five money. None of them have victory points on them."
    green.turn { sellPatents(5).expect("5 MC, -5 ProjectCard") }

    // "Wait. I sell a card for a money."
    yellow.turn { sellPatents(1).expect("1 MC, -ProjectCard") }

    // "I'll spend eight on lightning harvest. One energy product, one money product, and a point."
    green.turn { playProject(LightningHarvest, 8).expect("PROD[M, E], -ProjectCard") }

    // Yellow's ledger groups Media Archives' net thirteen-M€ gain with the twenty-five-M€ Water
    // Import from Europa payment below as one twelve-M€ debit at entry 325.
    yellow.turn {
      // "Sell three cards for three money. Wait, actually. Hold on. Just in case that can be useful
      // somehow. I'll sell two for two."
      sellPatents(2).expect("2 MC, -2 ProjectCard")
      // "I'm going to play Media Archives." "I have eight. I have 13, so that's 21 money for you."
      playProject(MediaArchives, 8).expect("13 MC, -ProjectCard")
    }

    // "Oh my god, I forgot to use my business network. Fine, I'll use that then."
    green.turn {
      cardAction1(BusinessNetwork) { /* Decline buying the revealed card. */
            green.buyCards(0)
          }
          .expect("0 ProjectCard")
    }

    // "Finally playing. Water Import from Europa."
    yellow.turn { playProject(WaterImportFromEuropa, 25).expect("-25 MC, -ProjectCard") }

    // The transcript identifies Sub-Zero Salt Fish in Green's hand, while the generic reconstructed
    // hand is one card short after the sourced plays and patent sales.
    green.exMachina("ProjectCard")
    green.turn {
      // "Okay, now I can use Energy Market. Get all up to 12 money."
      cardAction2(EnergyMarket).expect("PROD[-E], 8 MC")
      // "Play Sub-Zero Salt Fish." "Now you lose plant production. I spend five on that." "It's an
      // animal tag." "So I get an Ecomole."
      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Yellow>]") }
          .expect("-5 MC, PROD[-P<Yellow>], Animal<$EcologicalZone>, -ProjectCard")
    }
    // Green took Energy Market's eight M€ but did not record its energy-production decrease.
    green.exMachina("PROD[E]")

    yellow.turn {
      // "But, well, still, for the means, I can play Predators."
      // "Well, I guess you're going to take my Ecomal, then."
      playProject(Predators, 14).expect("0 Animal<Green, $EcologicalZone<Green>>")
      cardAction1(Predators) { doTask("-Animal<Green, $EcologicalZone<Green>>") }
          .expect("Animal<$Predators>, -Animal<Green, $EcologicalZone<Green>>")
    }

    // "And then, you know, for all the good it'll do, I'll just use the action to add another
    // animal."
    green.turn { cardAction1(SubZeroSaltFish).expect("Animal") }

    yellow.turn {
      // "Thanks to my steel, I can spend all six and three real for a point from Artificial Lake."
      playProject(ArtificialLake, 3, steel = 6).expect("0 OceanTile, -ProjectCard")
    }
    green.pass()
    yellow.pass()

    // Both ledgers: after the final production phase and before final greenery placement.
    with(green) {
      assertProduction(m = 34, s = 2, t = 7, p = 16, e = 2, h = 6)
      assertResources(m = 83, s = 3, t = 7, p = 17, e = 2, h = 16)
      assertCounts(42 to "TR")
    }
    with(yellow) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
      assertCounts(56 to "TR")
    }
    // The resource apps incremented their display to 12 during final production; the engine keeps
    // the completed action generation numbered 11.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Reconcile the mistakes still present in the physical game so final greenery and scoring use
    // the ordinary-rules state. This net delta is characterized by the separate exMachina-free
    // replay, not by the ledgers: retain Lava Flows' two TR and Kaguya Tech's Pets animal, retain
    // Immigrant City's M€-production trigger, and retain Energy Market's production decrease.
    green.exMachina("4 MC, 2 TR, PROD[M, -E], -E, Animal<$Pets>")
    with(green) {
      assertProduction(m = 35, s = 2, t = 7, p = 16, e = 1, h = 6)
      assertResources(m = 87, s = 3, t = 7, p = 17, e = 1, h = 16)
      assertCounts(44 to "TR", 11 to "Animal<$Pets>")
    }

    // "So I'm going to 1-2 and 1-3."
    green.convertPlants { placeTile(1, 2) }.expect("-8 P")
    green.convertPlants { placeTile(1, 3) }.expect("-8 P")
    // Decline another final greenery placement.
    green.declineTask()
    // Decline the final greenery placement.
    yellow.declineTask()

    val score = Summarizer(game)
    green.assertCounts(
        32 to "AwardTally<Green, Mogul>",
        11 to "AwardTally<Green, Traveller>",
        44 to "TR",
    )
    yellow.assertCounts(
        14 to "AwardTally<Yellow, Mogul>",
        8 to "AwardTally<Yellow, Traveller>",
        56 to "TR",
    )
    score.net("Milestone", "VP<Green>") shouldBe 10
    score.net("Milestone", "VP<Yellow>") shouldBe 5
    score.net("FirstPlace", "VP<Green>") shouldBe 10
    score.net("FirstPlace", "VP<Yellow>") shouldBe 0
    score.net("SecondPlace", "VP<Green>") shouldBe 0
    score.net("SecondPlace", "VP<Yellow>") shouldBe 0
    score.net("GreeneryTile", "VP<Green>") shouldBe 10
    score.net("GreeneryTile", "VP<Yellow>") shouldBe 3
    score.net("CityTile", "VP<Green>") shouldBe 15
    score.net("CityTile", "VP<Yellow>") shouldBe 6
    score.net("Card", "VP<Green>") shouldBe 35
    score.net("Card", "VP<Yellow>") shouldBe 46
    score.net("$Pets", "VP<Green>") shouldBe 5
    score.net("$VenusianInsects", "VP<Green>") shouldBe 5
    score.net("$EcologicalZone", "VP<Green>") shouldBe 1
    score.net("$Herbivores", "VP<Green>") shouldBe 4
    // "Resource points on cards. One, four. Holy shit. Yeah. One, four, eight, and fifteen."
    // The earlier explicit count, "I'm at 15 strato birds," identifies the last value.
    score.net("$Predators", "VP<Yellow>") shouldBe 1
    score.net("$JovianLanterns", "VP<Yellow>") shouldBe 4
    score.net("$VenusianAnimals", "VP<Yellow>") shouldBe 8
    score.net("$StratosphericBirds", "VP<Yellow>") shouldBe 15

    // The spoken 118-115 tally omitted Green's four Herbivores points and the two Lava Flows TR
    // that
    // the corrected scoring state retains. Yellow's spoken total is one point below the complete
    // replay categories, which sum to 116.
    green.assertCounts(124 to "VP", 1 to "Victory")
    yellow.assertCounts(116 to "VP", 0 to "Victory")

    with(green) {
      assertProduction(m = 35, s = 2, t = 7, p = 16, e = 1, h = 6)
      assertResources(m = 87, s = 3, t = 7, p = 1, e = 3, h = 16)
    }
    with(yellow) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
    }
  }

  private fun assertColonyProductions(vararg productions: Int) {
    require(productions.size == colonyTiles.size)
    engine.assertCounts(
        *productions
            .zip(colonyTiles) { production, colony -> production to "ColonyProduction<$colony>" }
            .toTypedArray()
    )
  }
}
