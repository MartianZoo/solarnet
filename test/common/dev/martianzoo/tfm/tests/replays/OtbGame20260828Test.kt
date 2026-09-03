package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.Parsing.parseClasses
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.canon.Canon
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals

// L1 Trade Terminal's general three-distinct-card selection is not yet modeled. This replay-local
// card preserves its ordinary play, trade effect, tags, and VP; the sourced resource destinations
// are supplied at the play site.
private val fakeL1TradeTerminal = cn("FakeL1TradeTerminal")
private val fakeL1TradeTerminalDefinition =
    parseClasses(
        """
        CLASS FakeL1TradeTerminal : ActiveCard<Class<ProjectCard>> {
          cost = 25
          This:: SpaceTag<This>
          This: Floater<FloatingHabs>, Floater<AerialMappers>, Floater<FloatingRefinery>
          Trade<ColonyTile>:: TradeBarrier<ColonyTile>
          Trade<ColonyTile>: (2 ColonyProduction<ColonyTile> OR Ok) THEN -TradeBarrier<ColonyTile>
          End: 2 VictoryPoint
        }
        """
    )

private val otbGame20260828Catalog = Canon.withNonstandardClasses(fakeL1TradeTerminalDefinition)

/** Three-player physical game begun Friday, 2026-08-28. */
internal class OtbGame20260828Test : AbstractFullGameTest() {
  private val colonyTiles = listOf("Ganymede", "Io", "Luna", "Miranda", "Titan")
  override val catalog = otbGame20260828Catalog

  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack
          FakeL1TradeTerminal

          Engineer, Fundraiser, Landshaper, Merchant, Metallurgist
          Benefactor, EstateDealer, Industrialist, Metropolist, SpaceBaron
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Blue",
          "Yellow",
      )

  @Test
  internal fun otbGame20260828() {
    TfmWorkflow.Auto(game).launch()
    val green = game.tfm(Player.PLAYER1)
    val blue = game.tfm(Player.PLAYER2)
    val yellow = game.tfm(Player.PLAYER3)

    // "Okay, I am the start player, and I play Paladin Shipping. That gives me 36 money and five
    // titanium. Then I use this little buy-cards slider to buy four cards."
    green.playCorp(PalladinShipping, 4)
    // "I play Celestic, so I start with 42 megacredits." "You do get 42 money and then buy how many
    // cards? Five cards?" "Oh, yes."
    blue.playCorp(Celestic, 5)
    // "And I am Point Luna." "Oh, man. That corp is strong." "Start with 38 money, titanium
    // production. It immediately gives me a card. And I slide five cards. Boop."
    yellow.playCorp(PointLuna, 5)

    green.turn {
      // "I've played Biofuels and Supplier. I get two energy production and four steel, and then I
      // get one energy production, one plant production, and two plants."
      playPrelude(Biofuels)
      playPrelude(Supplier)
    }
    blue.turn {
      // "First I'm going to play Great Aquifer. I place two ocean tiles." "I'll put one here.
      // That's row two, column one, for two titanium." The second placement is stated below.
      playPrelude(GreatAquifer) {
        doTask("OceanTile<Cimmeria_2_1>")
        doTask("OceanTile<Cimmeria_9_5>")
      }
      // "I'm going to put this one on row nine, column five. That gives me two plants." "And then
      // Atmospheric Enhancers. I'm enhancing our atmosphere. I'm gonna go ahead and raise Venus
      // two steps."
      playPrelude(AtmosphericEnhancers) { doTask("2 VenusStep") }
    }
    yellow.turn {
      // "Orbital Construction Yard: I gain titanium production and four titanium."
      playPrelude(OrbitalConstructionYard)
      // "Early Colonization: I place a colony on Luna, which means I gain 2 money production, I
      // gain 3 energy—not energy production, 3 energy—and raise all colony tracks 2 steps."
      playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
    }

    // board-17-33-49.jpg: post-Prelude setup, before the first project action.
    with(green) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 24, s = 4, t = 5, p = 2, e = 0, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(24 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertResources(m = 23, s = 0, t = 4, p = 0, e = 3, h = 0)
      assertCounts(20 to "TerraformRating", 1 to "Colony<Luna>")
    }
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 2, venus = 4)

    green.turn {
      // "I'm gonna play Titan Shuttles." "Oh, shit, that's competition." "Yep. Floater on floater
      // warfare. Floatfare." "I will spend three titanium, which counts as nine money, so I need
      // 14 more. That's my turn."
      playProject(TitanShuttles, 14, titanium = 3)
    }
    blue.turn {
      // "I had a plan; it's gone now." "Your first action should be to draw this stuff."
      // "There's one: Jet Stream Microscrappers. And Floater Technology. Very cool."
      stdAction("DoRequiredActions").expect("2 ProjectCard")
      // "Now I'm gonna play Local Shading. Pay four for it. And I guess that's my two actions."
      playProject(LocalShading, 4)
    }
    yellow.turn {
      playProject(RimFreighters, 1, titanium = 1)
    }
    green.turn {
      // "I'm going to use the Titan Shuttles action to put two floaters on Titan Shuttles."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      // "Where's my plan? FML. You have all these floater cards now. I'm going to play
      // Floater Technology for seven."
      playProject(FloaterTechnology, 7)
      // "I'm going to use the Local Shading action to add a floater to Local Shading. Wait, no,
      // you're right. I'm going to use the Floater Technology action to add it to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "For some reason, I thought I was rushing to trade. It occurs to me now that it's probably
      // not worth it for you guys to trade. I probably should have played Business Network first.
      // That's four real monies and an Earth-tag discount card."
      playProject(BusinessNetwork, 4)
    }

    // She forgot to reduce her money production (fixed later)
    yellow.exMachina("PROD[MC]")

    green.passWithUnusedActionCards(PalladinShipping)
    blue.turn {
      // "I'm going to play Nitrite Reducing Bacteria. That costs me 11 money."
      // "You immediately get three free microbes on Nitrite Reducing Bacteria."
      playProject(NitriteReducingBacteria, 11)
      // "For my next trick, I will use the Local Shading action to remove a floater and increase
      // my money production one step."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "I use Business Network action. I look at a card... not feeling it."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    blue.turn {
      // "I will use my Nitrite Reducing Bacteria action to spend three microbes and increase my TR
      // one step. I guess I pass." "No, you have one more action."
      cardAction2(NitriteReducingBacteria)
      // "Oh, my Celestic action. I forgot. I will use my Celestic action to add a floater to Local
      // Shading."
      cardAction1(Celestic) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "I will spend two energy to trade with Luna. That's seven from the track plus my bonus of
      // two, so nine." "Jesus."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 9 MC")
      // "One more thing. I spent one for Fuel Generators, lose a money production, and gain energy
      // production. And I think that's it."
      playProject(FueledGenerators, 1)
    }
    blue.passWithUnusedActionCards()
    yellow.passWithUnusedActionCards()

    // "I will raise the oxygen to one percent as my World Government step."
    green.wgt("OxygenStep").expect("0 TerraformRating")

    // board-17-45-16.jpg and all three app histories: Generation 2 before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 30, s = 4, t = 2, p = 3, e = 3, h = 0)
      assertCounts(20 to "TerraformRating")
      assertCardResources(2 to TitanShuttles)
    }
    with(blue) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 31, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(25 to "TerraformRating")
      assertCardResources(1 to LocalShading, 0 to NitriteReducingBacteria)
    }
    with(yellow) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 47, s = 0, t = 5, p = 0, e = 1, h = 1)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 2, venus = 4)

    blue.buyCards(2)
    yellow.buyCards(2)
    green.buyCards(3)

    blue.turn {
      // "I'm going to start by taking my Local Shading action, spending a floater to increase my
      // money production."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "I'm going to use my Business Network. And I will buy it."
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    green.turn {
      // "Minority Refuge. I'll spend one titanium and two real money. I lose two money production,
      // and I get to place a colony, which I'm going to place on Titan."
      playProject(MinorityRefuge, 2, titanium = 1) {
        doTask("Colony<Titan>")
        addCardResources(TitanShuttles)
      }
      // "Then I'm going to use my second action to grab all these floaters." "Remember to get 3
      // for your placing." "Holy shit. I get three and three and one. I get seven floaters."
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        addCardResources(TitanShuttles, 3)
        addCardResources(TitanShuttles)
      }
    }
    blue.turn {
      // "I'm going to play Jet Stream Microscrappers. Pay 12 for it." "You want me to actually pay
      // for the things that I place? You're crazy."
      playProject(JetStreamMicroscrappers, 12)
    }
    yellow.turn {
      // "I'm going to pay five titanium and five real for Solar Logistics." "Oh, man. She gets to
      // draw a card any time any one of us plays a space event. Plus she gets a discount on Earth
      // tags."
      playProject(SolarLogistics, 5, titanium = 5)
    }
    green.turn {
      // "You'll never imagine what I'm going to do. I'm going to use Titan Shuttles to spend nine
      // floaters and get nine titanium." "Gawah."
      cardAction2(TitanShuttles, x = 9)
    }
    blue.turn {
      // "I'm going to play Dirigibles, which costs 11 money."
      playProject(Dirigibles, 11)
    }
    yellow.turn {
      // "On top of Solar Logistics, I will place Optimal Aerobraking. For that, I will spend
      // seven." "We cannot let her get space events."
      playProject(OptimalAerobraking, 7)
    }
    green.turn {
      // "Here we go. Spache Elevator." "Am I spending all titanium for it? I'm gonna hold back one
      // titanium. I'm gonna spend nine titanium. Not 99, just nine." "Stupid fucking Spache
      // Elevator."
      playProject(SpaceElevator, titanium = 9)
    }
    blue.turn {
      // "I'm going to take my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      // "Solar Logistics also gives me a discount of two on my Earth tag. I'm gonna pay seven for
      // Imported Advanced GHG. Increase heat production two steps. I get a card for Solar
      // Logistics,
      // and from Optimal Aerobraking I get three money and three heat."
      playProject(ImportOfAdvancedGhg, 7).expect("-4 MC, 3 Heat, PROD[2 Heat]")
    }
    green.turn {
      // "I am going to use my Spache Elevator action to lose one steel and gain five real."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "Oh my god, I'm an idiot. Dude, I have plants. I can plant Potatoes. Why didn't I do this
      // last freaking generation? That cost me two money. I lose two plants, and I get two money
      // production." "Yep. Potatoes are mine. Andy Weir up in here."
      playProject(Potatoes, 2)
    }
    yellow.passWithUnusedActionCards()
    green.turn {
      // "I'm spending three steel and 12 real on Research Outpost, which lets me place a city tile.
      // I can place it right here, pay five money, and get another colony, which I'm just gonna
      // drop
      // on Luna. That gives me two money production, getting me back to zero again."
      playProject(ResearchOutpost, 12, steel = 3) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
      }
    }
    blue.turn {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    green.turn {
      // "[Yellow]'s already passed, so I'm the one holding things up. We play Peroxide Power, which
      // costs me six real money. I lose one money production and add two energy production."
      playProject(PeroxidePower, 6)
    }
    blue.turn {
      // "I'm going to take my Dirigibles action, which lets me add a floater to any card, and add
      // it
      // to Jet Stream Microscrappers. Then I take my Celestic action to add another floater to Jet
      // Stream Microscrappers." "I forgot I'm supposed to pay titanium for both of those." "You
      // don't have to pay." "I don't have to pay, thank God."
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    green.passWithUnusedActionCards(PalladinShipping)
    blue.turn {
      // "Then I'm going to use my Jet Stream Microscrappers action to remove two floaters and raise
      // Venus one step." "Ah shit, I made it easier for [Yellow] to get the bonus."
      cardAction2(JetStreamMicroscrappers)
    }
    blue.passWithUnusedActionCards()

    // "I'm going to raise Venus." "Venus is now at eight. No one gets the card. We're still at one
    // oxygen, two oceans, and no temperature raises yet."
    blue.wgt("VenusStep").expect("0 TerraformRating")

    with(green) {
      assertProduction(m = -1, s = 0, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 20, s = 0, t = 2, p = 4, e = 5, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(26 to "TerraformRating")
      assertCardResources(1 to LocalShading, 1 to NitriteReducingBacteria)
    }
    with(yellow) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 2)
      assertResources(m = 43, s = 0, t = 4, p = 0, e = 1, h = 7)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 2, venus = 8)

    yellow.buyCards(3)
    green.buyCards(2)
    blue.buyCards(1)

    yellow.turn {
      // "I think I will start by paying two titanium to trade with Io. That's ten heat for me."
      stdAction("TradeAction", 3) { doTask("Trade<Io>") }.expect("-2 Titanium, 10 Heat")
      // "Probably should have done this sooner, but Business Network."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn {
      // "What I'm gonna do is fly my boat. I'm gonna spend three energy, fly my boat to Ganymede,
      // take five planta. When you want to convert plants to greenery, you hit plants, but then
      // there's this 'plant forest'—for whatever reason they call it plant forest. It automatically
      // gives me the TR and takes away the eight plants and everything."
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }.expect("-3 Energy, 5 Plant")
      // "My forest is gonna go on 3-2, so that it's next to my city and gets two money from ocean.
      // That raises oxygen to two percent, and we got my TR already. That was my two actions."
      convertPlants { placeTile(3, 2) }.expect("-8 Plant, 2 MC, TerraformRating")
    }
    blue.turn {
      // "How much money do I have? Fuck. Okay, FML. I guess I'm playing Io Sulphur Research. I pay
      // for it first, then draw three cards. I have two Venus tags."
      // "I'll buy Reno."
      playProject(IoSulphurResearch, 17) { doTask("3 ProjectCard") }.expect("2 ProjectCard")
    }
    yellow.turn {
      // "For six monies, Carbonate Processing. Lose energy production, gain three heat production."
      playProject(CarbonateProcessing, 6)
    }
    green.turn {
      // "This is really cool, actually. I play Mining Area. It costs three money. That solves both
      // my—because I need steels for this thing, you know? It gives me one for this turn and solves
      // my problem for the future as well. That gives me one steel and one steel production."
      playProject(MiningArea, 3) { placeTile(4, 3) }.expect("Steel, PROD[Steel]")
      // "As my second action, I can spend eight on the Land Shaper."
      claimMilestone(cn("Landshaper"))
    }
    blue.turn {
      // "I'm going to play Mars University."
      // "Did you get that from the—"
      // "When I just drew three cards from Io Sulphur Research, I did draw Mars University, and I'm
      // playing it now. I lose eight money. I get a science tag and an Earth tag."
      // "It doesn't have anything that happens right now."
      // "Well, it does, if you would like to pitch a card and draw a card."
      // "Nah, I like my cards."
      // "The struggle is real."
      playProject(MarsUniversity, 8) { declineTask() }
    }
    yellow.turn {
      // "For Power Infrastructure."
      playProject(PowerInfrastructure, 4)
    }
    green.turn {
      // "Now I can use Spache Elevator to use one steel and get five real. Was there anything else
      // I was in a hurry to do? No."
      // "Look, guys, I'm jet-lagged. Jet lag makes me stupid, okay? We know this. This is a true
      // fact about me."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "I'm just gonna use my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      // "I now have two power tags, so I can play Fusion Tower [Fusion Power]."
      // "Dang it, I didn't think you were gonna be able to play that. I thought it might come back
      // to me."
      // "Increase energy production three steps."
      playProject(FusionPower, 14)
    }
    green.turn {
      // "I'm gonna use Titan Shuttles to add two floaters."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      // "You'll never guess: I'm gonna use my Local Shading action to spend a floater to increase
      // my
      // money production by one."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "I will use my remaining ten money to play House Printing, gain steel production."
      playProject(HousePrinting, 10)
    }
    green.turn {
      // "I will use my Paladin Shipping action to pay two titanium to raise the temperature to
      // minus
      // 28 and get my second TR."
      cardAction1(PalladinShipping)
    }
    blue.turn {
      // "I'm gonna use my Floater Technology action to add one floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "I have 17 heat."
      // Yellow's app and the photographed track require both ordinary heat conversions.
      convertHeat()
      convertHeat().expect("PROD[Heat]")
    }
    green.passWithUnusedActionCards()
    blue.turn {
      // The recording goes silent here. Blue's app adds one TR, and these three unused actions
      // are the ordinary card sequence that produces exactly that result.
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    yellow.passWithUnusedActionCards(PowerInfrastructure)
    blue.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    blue.passWithUnusedActionCards()

    // board-18-39-07.jpg: end of the Generation 3 action phase, before production.
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 2, venus = 10)
    green.assertCounts(
        22 to "TerraformRating",
        1 to "GreeneryTile<Cimmeria_3_2>",
        1 to "CityTile<Cimmeria_3_3>",
        1 to "MiningArea_SpecialTile<Cimmeria_4_3>",
        1 to "Landshaper",
    )
    blue.assertCounts(27 to "TerraformRating")
    yellow.assertCounts(22 to "TerraformRating")

    // Venus is the only World Government choice consistent with the stated Generation 4 values.
    yellow.wgt("VenusStep").expect("0 TerraformRating")

    with(green) {
      assertProduction(m = -1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 1, p = 2, e = 5, h = 2)
    }
    with(blue) {
      assertProduction(m = 5, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 34, s = 0, t = 2, p = 0, e = 0, h = 0)
    }
    with(yellow) {
      assertProduction(m = 1, s = 1, t = 2, p = 0, e = 3, h = 6)
      assertResources(m = 23, s = 1, t = 4, p = 0, e = 3, h = 8)
    }
    assertSidebar(gen = 4, temp = -24, oxygen = 2, oceans = 2, venus = 12)

    // "I think I'll buy just one."
    // "Much to think about."
    // "Is it crazy if I buy three?"
    // "Three is just a lot, because that's nine money."
    // "Three clicks."
    // "I think I'm also buying three. Three cards for nine money."
    green.buyCards(3)
    blue.buyCards(3)
    yellow.buyCards(1)

    green.turn {
      // "Research Colony. One titanium and 16 real money. Not only does it let me place another
      // colony, it lets me go somewhere that I'm already at. I get another two money production, so
      // I'm back to positive money production again."
      playProject(ResearchColony, 16, titanium = 1) { doTask("Colony<Luna>") }
      // "When I use my second action to fly my boat, I get ten plus two plus two, and [Yellow] gets
      // two. So I get 14. And that was my two actiones. Oh, and I draw two cards."
      // "They're just lousy Earth-tag cards."
      // "Are you saying that to piss me off?"
      // "Yes."
      // "Nice."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-3 Energy, 14 MC, 2 MC<Yellow>")
    }
    blue.turn {
      // "I'm gonna play Ice Moon Colony. I'm gonna spend two titanium, which equals six money, and
      // 17 money. I'm gonna place an ocean first: row eight, column nine. I get two plants and one
      // TR."
      // "I'm a little behind on my boops here. We have two oxygen, and we have three temperature
      // boops. We're at negative 24."
      // "Let the record show I just caught up on my boops."
      // "So did I."
      // "As did I. We all did."
      // "Then I place my colony on Titan. I immediately get three floaters for one card, so I'm
      // gonna
      // put them on Jet Stream Microscrappers."
      playProject(IceMoonColony, 17, titanium = 2) {
        placeTile(8, 9)
        doTask("Colony<Titan>")
        addCardResources(JetStreamMicroscrappers, 3)
      }
    }
    yellow.turn {
      // "This is a lot of colonies on the board."
      // "I need more money. Why do I have so little money? And so little everything? Why are all my
      // productions so low? I feel like I never draw any cards that just give me productions
      // without
      // also costing other productions that I already don't have."
      // "Business Network. A card to inspect. Inspect, protect, detect."
      // "You know what I just realized. Minus one money—"
      // "You never did that?"
      // "I never."
      // "Wow. Take yourself a minus one money production now and maybe minus two or three money."
      // "I know I played this first gen, and it is gen four. So that means minus three money.
      // Thanks
      // for noticing that. We would have caught it later."
      // "What are you going to do with that card?"
      // "Nah. Booped."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    // Yellow then corrected Business Network's previously omitted production loss and the three
    // generations of extra income it had caused.
    yellow.exMachina("-3 MC, PROD[-MC]")
    green.turn {
      // "Space Elevate. A steel for five money."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "I will do my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      // "I will pay 15 money for Love Tube Settlement [Lava Tube Settlement]. Lose energy
      // production, gain two money production, and place a city tile."
      // "I will place it on Hadriacus Mons, 6-2, for two cards."
      // "I'm 6'2”, by the way."
      // "Oh no, I dropped my feminist literature."
      // "You did all your stuff you had to do?"
      // "Yes."
      // "I have money."
      // "Yeah, yeah, fuck you."
      playProject(LavaTubeSettlement, 15) { placeTile(6, 2) }
    }
    green.turn {
      // "With this money that, as previously observed, I have. How did I get that money, by the
      // way?
      // Oh, Space Elevating. Spillivate."
      // "It's your lucky day, [Yellow]. I'm playing a space event. That cost me 22. Paladin
      // Shipping
      // gives me a titanium. You get a card. I'll take a titanium, take two plants, raise oxygen to
      // three and get a TR, and place an ocean tile and get a TR. The ocean tile will be 7-9 for
      // two
      // plants and two money. Au revoir."
      playProject(TowingAComet, 22) { placeTile(7, 9) }
    }
    blue.turn {
      // "I'm gonna take my Local Shading action to remove a floater and gain a money production."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "When did we get a third oxygen?"
      // "Just now."
      // "Good, because I wasn't paying attention."
      // "I pitch a card."
      // "You sell a Patento? As your turn?"
      // "Yes."
      sellPatents(1)
    }
    green.turn {
      // "I am going to Titan Shuttles to place two floaters."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    blue.turn {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      // "Keep in mind, it can be useful to park them on Dirigibles too, because then you have the
      // option of using them as money. Local Shading, you can always next turn put it on and take
      // it
      // off, so it doesn't really do any advantage to have it there this turn. On the other hand,
      // there might be a card that does even better that collects floaters."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "I use Power Infrastructure: spend an energy to gain money."
      cardAction1(PowerInfrastructure, x = 1)
    }
    green.passWithUnusedActionCards(PalladinShipping)
    blue.turn {
      // "I will take my Jet Stream Microscrappers action to remove two floaters and raise Venus to
      // 14."
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      // "Water to Venus. Spend three titanium."
      // "She always gets it. Such an asshole."
      // "I raise it to 16, which gives an extra TR. And I get three money and three heat from
      // Optimal
      // Aerobraking."
      // "I never told you that I hate you."
      playProject(WaterToVenus, titanium = 3)
    }
    blue.turn {
      // "I use my Dirigibles action to put a floater on Jet Stream Microscrappers, and my Celestic
      // action to put a floater on Dirigibles. And that's all, folks."
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(Dirigibles) }
    }
    yellow.passWithUnusedActionCards()
    blue.passWithUnusedActionCards()

    // board-18-58-23.jpg: end of the Generation 4 action phase.
    assertSidebar(gen = 4, temp = -24, oxygen = 3, oceans = 4, venus = 16)
    green.assertCounts(24 to "TerraformRating", 1 to "Milestone")
    blue.assertCounts(29 to "TerraformRating")
    yellow.assertCounts(24 to "TerraformRating", 1 to "CityTile<Cimmeria_6_2>")
    engine.assertCounts(
        1 to "OceanTile<Cimmeria_2_1>",
        1 to "OceanTile<Cimmeria_9_5>",
        1 to "OceanTile<Cimmeria_8_9>",
        1 to "OceanTile<Cimmeria_7_9>",
    )
    green.assertCardResources(4 to TitanShuttles)
    blue.assertCardResources(
        1 to LocalShading,
        3 to NitriteReducingBacteria,
        2 to JetStreamMicroscrappers,
        1 to Dirigibles,
    )

    // "I'll put another ocean out here at 1-1. Oceans go up to five."
    // "Oceans rise and pass fall."
    green.wgt("OceanTile<Cimmeria_1_1>").expect("0 TerraformRating")

    // All three app histories: post-production Generation 5 pause, before Research.
    with(green) {
      assertProduction(m = 1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 30, s = 1, t = 2, p = 7, e = 5, h = 4)
      assertCounts(24 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 6, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertCounts(29 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 2, h = 6)
      assertResources(m = 35, s = 2, t = 3, p = 0, e = 2, h = 19)
      assertCounts(24 to "TerraformRating")
    }
    assertSidebar(gen = 5, temp = -24, oxygen = 3, oceans = 5, venus = 16)
    // "Six, nine, and eight. Perfect. Perfect."
    green.assertCounts(6 to "ProjectCard")
    blue.assertCounts(9 to "ProjectCard")
    yellow.assertCounts(8 to "ProjectCard")

    // The resumed table drafts to the right. The app histories record the resulting purchases.
    blue.buyCards(2)
    yellow.buyCards(3)
    green.buyCards(3)

    blue.turn {
      // "I'm gonna play Protected Valley."
      // "We're gonna save the River Valley. Yay."
      // "I pay 23 money. I don't give myself 23 money. I pay 23 money. I gain two money production,
      // and I place a greenery on an area reserved for ocean."
      // "Row nine, column nine. This is a city. Oh, it flips. Yes. Row nine, column nine, greenery.
      // Number nine, which gets me a plant."
      // "And two money."
      playProject(ProtectedValley, 23) { placeTile(9, 9) }
    }
    yellow.turn {
      // "This feels like getting in the way of doing actual stuff, but I'm a bit paranoid. So,
      // Subterranean Reservoir."
      // "I know what you're doing."
      // "Yep. Pay 11 for an ocean to go top right, 1-5. Oceans are now at 6. Give myself TR as
      // well.
      // Can't forget."
      playProject(SubterraneanReservoir, 11) { placeTile(1, 5) }
      // "And then pay 8."
      // "Eight money."
      // "Yes."
      // "And you claim the Merchant."
      // "As you could see, I was very close to being able to get that."
      // "Yeah. I just needed to place a greenery there. [Yellow] gets the Merchant."
      claimMilestone(cn("Merchant"))
    }
    green.turn {
      // "Oh, yeah. I was going to try to steal [Yellow]'s milestone."
      // "I'm afraid I'm going to spend three energy and fly my boat to Titan. Three floaters and a
      // floater. And [Blue], you get one floater."
      // "Yay!"
      // "I put my three and one on my only floater card, Titan Travels."
      // "I'm going to put my floater on Dirigibles."
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(green) {
          doTask("3 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$Dirigibles>")
        }
      }
    }
    blue.turn {
      // "Okay, so let me think. So that's now six money. Using 16 money and two floaters for my
      // Dirigibles card, I am going to play Stratopolis."
      // "Nice."
      // "And so I pay my 16 money first. And then I gain two money production. And I place a city
      // on
      // Stratopolis."
      // "You are the floater queen."
      playProject(Stratopolis, 16) {
        doTask("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
      }
    }
    yellow.turn {
      // "Now that I have much enticed, spend two energies to trade with Luna. I get 12. You get 12
      // and I get four."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 12 MC, 4 MC<Green>")
      // "And I will Business Network."
      // "You are business networking."
      // "Nah."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn {
      // "Well, I'll do my thing where I spend one steel to get five real by using Spache Elevator.
      // That's my turn. Go off."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "I'm out of money, so I am going to use my Nitrite Reducing Bacteria action to remove three
      // microbes and raise my TR one step."
      cardAction2(NitriteReducingBacteria)
    }
    yellow.turn {
      // "Man, I want to do stuff, but—wait. Wait. I think I can. You know what? I can always just
      // start by—can't really lose anything if I—yes, I will spend my two steel and 13 real. Then
      // immediately spend a plant to gain seven."
      // "Oh, I should remember to lose an energy production."
      // "Oh, yeah. Almost wrong."
      playProject(ElectroCatapult, 13, steel = 2)
      cardAction1(ElectroCatapult)
    }
    green.turn {
      // "I think I should probably use Titan Shuttles to take eight titanium. Oyga."
      cardAction2(TitanShuttles, x = 8)
    }
    blue.turn {
      // "I am going to use my Local Shading action. I remove a floater and I get a money
      // production."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "Robotic Workforce."
      // "Yeah, I didn't like giving that to you."
      // "Spend nine. You have so many building production cards."
      // "Yeah, I will replicate Fusion Power."
      // "Fuck. I didn't see that one."
      playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$FusionPower>") }
    }
    green.turn {
      // "On my turn, it's time to play Io Mining Industries. That cost me ten titanium, which is
      // worth three, and I get two titanium production, two money production, and I'm going to get
      // bank on Jupiter tags."
      playProject(IoMiningIndustries, 10, titanium = 10)
    }
    blue.turn {
      // "I'm using Jet Stream Microscrappers to spend two floaters and raise the Venus."
      // "To 18."
      // "To 18, and I give myself a TR for that."
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      // "Alright, I think I've done the urgent stuff. Now I can double heat boop."
      // "Okay. Boop. That takes temp to minus 20, and I get a heat product."
      convertHeat()
      convertHeat().expect("PROD[Heat]")
    }
    green.turn {
      // "Let's just direct some impactors. Direct impact. I pay seven for that."
      playProject(DirectedImpactors, 7)
    }
    blue.turn {
      // "I'm going to use Floater Technology to add one floater to Dirigibles."
      // "Okay. Alright. Dirigibles."
      cardAction1(FloaterTechnology) { addCardResources(Dirigibles) }
    }
    yellow.turn {
      // "Actually, you know what the hell. Why not? I'm going to sell a card for a money and then
      // pay five for Floating Habs."
      // "Ah, that's the one that's two floaters to a victory point."
      // "Yep. I has it. I finally have one, two science tags."
      sellPatents(1)
      playProject(FloatingHabs, 5)
    }
    green.turn {
      // "I'm going to Release my Inert Gases, which costs all 13 of my money. And it just gives me
      // two TR. And it flips."
      // "Oh, I forgot to use Directed Impactors. Built it for nothing."
      // "Yeah, I think I'm kind of fading too. In terms of my awareness."
      playProject(ReleaseOfInertGases, 13)
    }
    blue.turn {
      // "I'm going to use my Stratopolis action to add two floaters to my Dirigibles. Sweet."
      cardAction1(Stratopolis) { addCardResources(Dirigibles, 2) }
    }
    yellow.turn {
      // "I'm going to pitch two cards for two money. Then spend two money to add a floater to any
      // card, and that will be Floating Habs."
      sellPatents(2)
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    green.passWithUnusedActionCards(PalladinShipping, DirectedImpactors)
    blue.turn {
      // "I'm going to use my Extremophiles action to add two floaters to my Dirigibles."
      // "I'm going to play Extremophiles."
      // "Does that have a requirement?"
      // "Two science tags?"
      // The app history shows that a Dirigibles floater paid the three-M€ cost; Blue had no M€.
      playProject(
          Extremophiles,
          payment = {
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
      // "Awesome. Okay, I'm going to use my Extremophiles action to add one microbe to Nitrite
      // Reducing Bacteria."
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    yellow.passWithUnusedActionCards(PowerInfrastructure)
    blue.turn {
      // "I guess I'm going to use my Celestic action to add a dirigible."
      cardAction1(Celestic) { addCardResources(Dirigibles) }
      // "And I'm probably going to use my Dirigibles action to add another dirigible. Right? Do I
      // even have enough Venus tags for that to be worth it? No. So I'm going to use my Dirigibles
      // action to add a floater to Local Shading."
      // "Wait, no, that's not true. I did math wrong. Actually, I am going to, instead of adding it
      // to Local Shading, I'm going to add this floater to Dirigibles. So be it."
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
      // "Now that's $12 worth. And then I'm going to use them to pay for Venus Trade Hub."
      // "Damn."
      playProject(
          VenusTradeHub,
          payment = {
            doTask("4 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
      pass()
    }

    // "Okay, Jaybird, what do you want to move for World Government? Venus, temperature, oxygen,
    // ocean."
    // "Let's move oxygen."
    // "Okay. Oxygen to five percent."
    // board-21-08-05.jpg is after production and this World Government step.
    blue.wgt("OxygenStep").expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 29, s = 1, t = 3, p = 8, e = 5, h = 6)
      assertCounts(26 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 11, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 3, e = 0, h = 0)
      assertCounts(32 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 4, h = 7)
      assertResources(m = 29, s = 1, t = 5, p = 1, e = 4, h = 10)
      assertCounts(27 to "TerraformRating", 1 to "Milestone")
    }
    assertSidebar(gen = 6, temp = -20, oxygen = 5, oceans = 6, venus = 18)

    // "Motherfucker. Bro, I don't want any of these fuck-ass things."
    // "Yeah, I like fuck-ass things."
    // "I'm going to buy two cards."
    // "You know what? I have too many fucking cards. I'm going to buy four cards."
    // "Yay! I'm going to live a little and do that, too, I think. I'm going to buy four cards for
    // 12 money. Boop."
    blue.buyCards(2)
    yellow.buyCards(4)
    green.buyCards(4)

    yellow.turn {
      // "Well, why don't I go ahead and pay eight for Engineer. And I think I will pay two energies
      // to trade with Luna. I'll collect my ten—no, twelve monies. You get your four."
      // "Oh, put your boat on."
      // "Yes, thank you."
      claimMilestone(cn("Engineer"))
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 12 MC, 4 MC<Green>")
    }
    green.turn {
      // "This kind of sucks. I shouldn't have taken so many cards. Okay, well, alright. I'm not
      // racing for anything anymore. I'll use Space Elevator to spend one steel and take five
      // real."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "I am going to buy some Red Ships. Oh, wait, that does—okay, which costs me two money."
      // "I should actually warn you that it doesn't pay out anything just yet."
      // "I know. Why would I not know that?"
      // "It will. It will, yeah."
      playProject(RedShips, 2)
    }
    yellow.turn {
      // "Alright, let me start with Imported GHG. Pay one titanium, two monies. Increase heat
      // production one, gain three heat, get a card from Earth card, from space event—yes, both
      // Earth
      // and space. Doesn't make any sense. And gain three money, three heat. Jesus Christ, that's
      // insane. Be nicer if I got more space events."
      playProject(ImportedGhg, 2, titanium = 1)
      // "Sure, Business Network to inspect the card."
      // "Wait, didn't we say we can't—"
      // "I decided to put it in."
      // "Aw, thank you."
      // "The thing is that the card I think would be best with that I'd kind of want to play soon,
      // so
      // I'm gonna reject anyways."
      // "Reject."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    green.turn {
      // "I'm gonna play Research Coordination for three."
      // "Oh God, you like the wild tags."
      // "I love them. They're so useful."
      // "You like to live on the wild side."
      playProject(ResearchCoordination, 3)
    }
    blue.turn {
      // "I'm going to play Snow Algae, which costs me 12 money, and it gives me one plant
      // production
      // and one heat production."
      playProject(SnowAlgae, 12)
    }
    yellow.turn {
      // "Spend a plant to gain seven money production—no, not money production, seven money
      // resource. That would be ridiculous."
      cardAction1(ElectroCatapult)
      // "I probably may as well Cartel. One, two, three, four. Be nice if I had more good Point
      // Luna, but whatever. Four money production, yes, and the card."
      playProject(Cartel, 6)
    }

    // She forgot to pay for Cartel (fixed later)
    yellow.exMachina("6 MC")

    green.turn {
      // "I am going to play Olympus Conferencia, which costs nine, and it gives me a science
      // resource
      // because it's a science tag."
      playProject(OlympusConference, 9)
    }
    blue.turn {
      // "Okay, I'm now going to play Red Spot Observatory, which costs 17 monies, and with Mars
      // University, can I draw the two cards that I get from this first and then decide how to
      // discard
      // one?"
      // "Okay, good. I draw two cards."
      // "Come on. Dropped card. Stupid-ass card."
      // "Let the record show that [Blue] dropped her stupid-ass card."
      // "Fuck you guys."
      // "I'm going to discard this one to draw a new card because of my Mars University effect.
      // Thanks. That is better."
      playProject(RedSpotObservatory, 17) {
        doTask("-ProjectCard")
      }
    }
    yellow.turn {
      // "I realize, you know, I got some cash flow. Stratospheric Expedition. Pay four titaniums."
      // "Add two floaters to any card. That will be Floating Habs. Draw two Venus cards. But also
      // save a card for me to draw because of—"
      // "Do you want that first or do you want the Venus cards first?"
      // "Venus cards first."
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(FloatingHabs, 2)
      }
    }
    green.turn {
      // "I am going to spend three energy to fly my boat to Titan."
      // "To Titan."
      // "And [Blue] gets one floater, and I get two plus one floaters."
      // "I guess I'll go ahead and put my floater onto Local Shading."
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(green) {
          doTask("2 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          green.selectTask("Floater<Blue>.")
          blue.doTask("Floater<$LocalShading>")
        }
      }
    }
    blue.turn {
      // "I don't have money to pay for any of my cards. I'm just doing actions."
      // "I'm going to take my Nitrate Reducing Bacteria action and add a microbe to my Nitrate
      // Reducing Bacteria. Bet you guys never would have guessed that I'm going to do something
      // crazy
      // like that."
      cardAction1(NitriteReducingBacteria)
    }
    yellow.turn {
      // "Can I see your player board?"
      // "You suck. Apparently not. No, you can't see it."
      // "You know what? I don't have shit for my ass. I don't have anything. You know what? Leave
      // me
      // alone."
      // "[Green], I'm hiring raiders. Stealing three money from you. Boop. Boop, boop, doot, doot.
      // And
      // pay them one money for it."
      playProject(HiredRaiders, 1) { doTask("3 M<Yellow> FROM M<Green>") }
    }
    green.turn {
      // "That might just possibly screw me up, actually."
      // "Yay! Everybody dance! Everybody dance! Life is good!"
      // "God, I keep forgetting. Every time I trade, I forget to play Market Manipulation first.
      // Over and over."
      // "I'm gonna use Titan Shuttles to take three titanium."
      cardAction2(TitanShuttles, x = 3).expect("-3 Floater<$TitanShuttles>, 3 Titanium")
    }
    blue.turn {
      // "I'm gonna use my Local Shading action to—not to add a floater—to spend a floater to add a
      // money production."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "I will play Cutting Edge Technology for 12."
      // "Nice. Love it?"
      // "Yeah."
      playProject(CuttingEdgeTechnology, 12)
    }
    green.turn {
      // "I'm gonna plant a forest, as they say. Oxygen is now six, and my forest will go right
      // where
      // you think it will go, 2-2."
      // "Then, for my next trick, what I'm gonna do is I'm gonna play Sky Docks."
      // "Do you actually say 'for my next trick'? Did you take that from me, or did I take that
      // from
      // you?"
      // "I used to say that all the time."
      // "Okay, I took it from you."
      // "I do have the two Earth tags. I have to assign my wild tag as an Earth tag when I play it.
      // I'm gonna pay four titanium and five real. And I get a trade fleet."
      // "Yeah, you do. Heck. H-E-K-K."
      convertPlants { placeTile(2, 2) }
      playProject(
          SkyDocks,
          5,
          titanium = 4,
          butFirst = assignAllWildTags("EarthTag"),
      )
    }
    blue.turn {
      // "I'm using my Floater Technology action to put a floater somewhere fun, I guess. I don't
      // know."
      // "You're starting to drown in floaters."
      // "Put it on Celestic."
      // The spoken choice conflicts with the later definite Observatory draw, which establishes the
      // physical floater's destination.
      cardAction1(FloaterTechnology) { addCardResources(RedSpotObservatory) }
    }
    yellow.turn {
      // "Expat Ishtar for four. I gain three titanium, and it requires Venus ten percent."
      // "Yeah, we got that."
      // "Draw two Venus cards. What gives you a two discount?"
      // "Cutting Edge."
      playProject(IshtarExpedition, 4)
    }
    green.turn {
      // "I'm gonna play Inventors' Guild. I call it Inventioner's Guild. That cost me seven money."
      // "And then I'm gonna use it."
      // "Use it tonight."
      // "Yeah, that's not bad. I'll pay three for that."
      playProject(InventorsGuild, 7) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
      cardAction1(InventorsGuild) { buyCards(1) }
    }
    blue.turn {
      // "I'm gonna use my Extremophiles action to put a microbe on my sulfite reducing bacteria.
      // Bet
      // you weren't expecting that."
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    // "I am now realizing I miscalculated. Bummer, dude. How dare I miscalculate."
    // "In multiple ways, I really could have played my cards better, quite literally."
    yellow.turn {
      // "We probably have at least three more generations left in this game, right?"
      // "Probably. Maybe even four? I don't know. It's hard to say."
      // "We're kind of focusing on not terraforming."
      // "Well, except for Venus, man."
      // "By the way, Venus doesn't need to be completed to end the game. Only the other three."
      // "But it's raising my terraforming."
      // "I know, I know. It's fun, yeah."
      // "11-4, Aerial Mappers. It's like the only way I have to conveniently raise my TR right now.
      // It's working wonders for me."
      playProject(AerialMappers, 11)
    }
    green.turn {
      // "I am gonna use Paladin Shipping for almost the first time ever to get a Temp Boop to minus
      // 18."
      // "I don't know about that. It's at least the second."
      cardAction1(PalladinShipping)
    }
    blue.turn {
      // "Wait a minute. When I played Inventor's Guild, I should have lost a Science Cube and drawn
      // a
      // card. But it's still your turn."
      // "I'm going to use my Red Spot Observatory action to spend a floater and draw a card. Card
      // me."
      // "Card. Cardi B."
      cardAction2(RedSpotObservatory)
    }
    yellow.turn {
      // "All right, for two, I had to actually get my second Venus tag down. Venus Governator."
      // "Governator."
      // "I gain two money per Earth."
      playProject(VenusGovernor, 2)
    }
    green.passWithUnusedActionCards(DirectedImpactors)
    blue.turn {
      // "I'm going to use my Stratopolis action to add two floaters to Jet Stream Microscrappers."
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
    }
    yellow.turn {
      // "You know, before I forget, I can do this. Power Infrastructure. Spend two monies. Get—no,
      // spend two energies. Get two monies."
      cardAction1(PowerInfrastructure, x = 2)
    }
    blue.turn {
      // "I'm going to use my Jet Stream Microscrappers action to raise the Venus. [Yellow], can you
      // move
      // the Venus? And I get a TR."
      // "Yay! Yay! Life is good."
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      // "I spend seven money on Floating Refinery. And I get one, two, three, four, five floaters
      // immediately added."
      // "Damn. And where are you adding them?"
      // "To Floating Refinery. It makes you add them there."
      playProject(FloatingRefinery, 7)
    }
    blue.turn {
      // "[Blue] would have really liked that card because it lets you pull floaters off of any card
      // you want."
      // "Oh, that would have been cool."
      // "Anyway, I'm going to use my Dirigibles action to add a floater to Dirigibles."
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
    }
    yellow.turn {
      // "You know what? Before I forget, I'm going to heat boop. Boop up to minus 16."
      convertHeat()
    }
    blue.turn {
      // "I'm going to use my Celestic action to add a cube to—to add a floater to, I guess, to
      // Celestic."
      // "I've never seen so many float boys."
      // "So many what?"
      // "Floater cards in my life. Did you say float boys?"
      // "Yeah, float boys. That's what [Yellow] calls them."
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "I'm going to add an Aerial Mapper."
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
    blue.turn {
      // "Okay, for one Dirigible and two monies, I'm gonna play Ishtar Mining, and I get a titanium
      // production."
      // "Yay! Yay! Yay! Yay! Kermit the Frog. Kermit the Frog goes, 'Yay!'"
      playProject(
          IshtarMining,
          2,
          payment = {
            pay(2)
            doTask("PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
          },
      )
    }
    yellow.turn {
      // "I am feeling like I did not play this generation optimally."
      // "Nope. Sure didn't."
      // "Floating Refinery. Remove two from Floating Refinery to gain a titanium and two money."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    blue.turn {
      // "Just to have something to do, I'm gonna take my Red Ships action, which doesn't give me
      // anything."
      cardAction1(RedShips)
    }
    yellow.turn {
      // "I'm gonna use Floating Habs. Spend two money, add to Floating Habs."
      // "How do you have so much shit to do? I freaking pass."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    blue.passWithUnusedActionCards()
    yellow.turn {
      // "Heat boop. Heat up to 14—minus 14."
      // "Oh shit, I have not been tracking the temperature or anything up to minus 14."
      // "And then six and six."
      // "There's six oceans now?"
      // "Yeah."
      // "Has been for a while. Always has been."
      convertHeat()
      pass()
    }

    // "We decided that we would do World Government, which is whose choice?"
    // "Me."
    // "Okay, what are you gonna choose?"
    // "Venus. Venus to 22. Then it's picture time. Picture pages, picture pages."
    // board-21-40-27.jpg follows production and this WGT step.
    yellow.wgt("VenusStep").expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 3, p = 1, e = 5, h = 8)
      assertCounts(28 to "TerraformRating", 8 to "ProjectCard")
    }
    with(blue) {
      assertProduction(m = 12, s = 0, t = 1, p = 1, e = 0, h = 1)
      assertResources(m = 49, s = 0, t = 1, p = 4, e = 0, h = 1)
      assertCounts(33 to "TerraformRating", 8 to "ProjectCard")
    }
    with(yellow) {
      assertProduction(m = 8, s = 1, t = 2, p = 0, e = 4, h = 8)
      assertResources(m = 37, s = 2, t = 6, p = 0, e = 4, h = 11)
      assertCounts(29 to "TerraformRating", 7 to "ProjectCard")
    }
    assertSidebar(gen = 7, temp = -14, oxygen = 6, oceans = 6, venus = 22)

    // "We made [Yellow] pay six to account for her previous mistake. And then I bought three cards
    // and [Yellow] bought three cards. Yeah, and [Blue]'s buying one card."
    yellow.exMachina("-6 MC")
    green.buyCards(3)
    yellow.buyCards(3)
    blue.buyCards(1)

    green.turn {
      // "Do you mind if we say that I played Market Manipulation just before I did that?" "Sure."
      // "I'll reduce Titan." Luna's raised track makes the following trade worth three more M€.
      playProject(MarketManipulation, 0) {
        doTask("ColonyProduction<Luna FROM Titan>")
      }
      // "I want to fly my boat to Luna. And I get 14 money and yellow gets two." After correcting
      // Market Manipulation's order, Green records the full 17 M€ trade result.
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-3 Energy, 17 MC, 2 MC<Yellow>")
    }
    blue.turn {
      // "I'm going to play Indentured Workers, Giant Ice Asteroid."
      playProject(IndenturedWorkers, 0)
      // "I'm going to pay one titanium and 25 money." "I took your one plant, and I'm going to
      // place two oceans." "Row nine, column ... six and seven."
      playProject(GiantIceAsteroid, 25, titanium = 1) {
        doTask("-Plant<Green>")
        placeTile(9, 6)
        placeTile(9, 7)
      }
    }
    yellow.turn {
      // "I will Business Network. Look at the card. Keep."
      cardAction1(BusinessNetwork) { buyCards(1) }
      // "Technology Demonstration. I paid five, which will be one titanium, two real. Draw two
      // cards, but actually I draw three because of that. And I gain my three money, three heat."
      playProject(TechnologyDemonstration, 2, titanium = 1)
    }
    green.turn {
      // "Play Invention Contest for free. That gives me a little ... science resource on Olympus
      // Conference. And then I look at three cards from the deck. And I get to keep ... this one."
      playProject(InventionContest, 0) {
        declineTask() // Research Coordination is not used for this play.
      }
    }
    blue.turn {
      // "Use my Nitrite Reducing Bacteria action to remove three microbes and raise my TR."
      cardAction2(NitriteReducingBacteria)
    }
    yellow.turn {
      // "I will Asteroid. That's four titanium and two real." "[Blue], you lose three plants."
      playProject(AsteroidCard, 2, titanium = 4) { doTask("-3 Plant<Blue>") }
    }
    green.turn {
      // "Quantum Extractor. That costs me 11. It gives me four energy production."
      playProject(QuantumExtractor, 11) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn {
      // "Use my Floater Technology action to add a floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "Use Floating Refinery. Remove two things ... to take a titanium and two money."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    green.assertCounts(3 to "Titanium") // Green applog entry 170 starts from three titanium.
    green.turn {
      // "Use two titanium and my Paladin action ... to raise the temperature to minus six."
      cardAction1(PalladinShipping)
    }
    blue.turn {
      // "Take my Local Shading action to turn that floater into a money production."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "Pay four titanium and thirteen real money for L1 Trade Terminal."
      // Yellow paid the printed cost despite her four M€ of applicable card discounts.
      intentionalOverpay(4)
      // "I add one to Floating Habs ... and add Aerial Mapper, add Floating Refineries."
      playProject(fakeL1TradeTerminal, 13, titanium = 4)
    }
    green.turn {
      // "Use my Space Elevator to destroy one steel and gain five real."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "Use my Stratopolis action to add two floaters to Jetstream Microscrapper."
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
    }
    yellow.turn {
      // "Spend two energies to trade with Io ... 13 heateroonies for me."
      stdAction("TradeAction", 2) {
            doTask("Trade<Io>")
            doTask("2 ColonyProduction<Io>")
          }
          .expect("-2 Energy, 13 Heat")
    }
    green.turn {
      // "Use my Inventors' Guild and decide whether to buy this ... card. Nothing."
      cardAction1(InventorsGuild) { buyCards(0) }
    }
    blue.turn {
      // "Spend two floaters and raise Venus one step. Venus is at 24 and I get a TR."
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      // "I will spend a steel to gain seven real."
      cardAction2(ElectroCatapult)
    }
    green.turn {
      // "I will play Molecular Printing. That cost me nine ... it gives me eight."
      playProject(MolecularPrinting, 9) {
        declineTask() // Research Coordination is not used for this play.
      }
    }
    blue.turn {
      // "Use my Extremophiles action to add a microbe to Nitrate Reducing Bacteria."
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    yellow.turn {
      // "Aerial Mappers remove to card."
      cardAction2(AerialMappers)
    }
    green.turn {
      // "Anti-Gravity Technology. I pay 12 for that."
      // "I have one, two, three, four, five, six, seven science tags."
      // The seven are Research Outpost, Research Colony, Olympus Conference, Inventors' Guild,
      // Quantum Extractor, Molecular Printing, and Research Coordination's wild tag.
      playProject(
          AntiGravityTechnology,
          12,
          butFirst = assignAllWildTags("ScienceTag"),
      ) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn {
      // "Get ready for some grass. Grass is grass production and three plants."
      playProject(Grass, 11)
    }
    yellow.turn {
      // "Comet for Venus. I pay 11. Raise Venus one step ... you lose four money."
      playProject(CometForVenus, 11) { doTask("-4 MC<Blue>") }
    }
    green.turn {
      // "Take my Titan Shuttles action to put two Jovian floaters on Titan Shuttles."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) }
    }
    blue.turn {
      // "Take my Red Spot Observatory action to add a floater to Red Spot Observatory."
      cardAction1(RedSpotObservatory)
    }
    yellow.turn {
      // "Five for Sister Planet Support ... increase money production three steps and take a card."
      playProject(SisterPlanetSupport, 3)
    }
    green.turn {
      // "This makes it four Earth tags. And that cost me seven money ... two titanium production."
      playProject(LunarMining, 7, butFirst = assignAllWildTags("EarthTag"))
    }
    blue.turn {
      // "Use the Dirigibles action to add a floater to Celestic."
      cardAction1(Dirigibles) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "Luna Governor ... for free. Two money production, two cards."
      playProject(LunaGovernor, 0)
    }
    green.turn {
      // "Atmoscoop ... one titanium and 13 real. ... two floaters on Titan Shuttles. I raise the
      // temperature to minus two and get my two TR."
      playProject(Atmoscoop, 13, titanium = 1) {
        doTask("2 TemperatureStep")
        addCardResources(TitanShuttles, 2)
      }
      // "I convert heat ... another TR for the ocean ... two-six for one plant and two money."
      convertHeat { placeTile(2, 6) }
    }
    blue.turn {
      // "Use my Celestic action to add a floater to Celestic."
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "I do two heat boops. Temp is at plus four now."
      convertHeat()
      convertHeat()
    }
    green.passWithUnusedActionCards(DirectedImpactors)
    blue.turn {
      // "Take my Red Ships action, for which I get nothing."
      cardAction1(RedShips)
    }
    yellow.turn {
      // "I'm going to do two heat boops. Another two heat? ... temp is maxed."
      convertHeat()
      convertHeat()
    }
    blue.passWithUnusedActionCards()
    yellow.turn {
      // "Power Infrastructure, spend two energy, gain two money."
      cardAction1(PowerInfrastructure, x = 2)
      // "Pay three for Energy Market. Use the effect of lose an energy production. Gain eight."
      playProject(EnergyMarket, 3)
      cardAction2(EnergyMarket)
      // "Floating Habs, spend two and add ... to itself."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      // "Spend my 11 monies on Cloud Tourism, increase money production one step per set of Earth
      // and Venus tags." "Cloud Tourism, add to self."
      playProject(CloudTourism, 11)
      cardAction1(CloudTourism)
    }

    // board-16-51-02.jpg: all players have passed in generation 7, before production.
    with(green) {
      assertProduction(m = 3, s = 1, t = 5, p = 1, e = 9, h = 0)
      assertResources(m = 2, s = 0, t = 0, p = 1, e = 2, h = 0)
      assertCounts(33 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 13, s = 0, t = 1, p = 2, e = 0, h = 1)
      assertResources(m = 10, s = 0, t = 0, p = 6, e = 0, h = 1)
      assertCounts(39 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 2, p = 0, e = 3, h = 8)
      assertResources(m = 2, s = 1, t = 0, p = 0, e = 0, h = 1)
      assertCounts(35 to "TerraformRating")
    }
    assertSidebar(gen = 7, temp = 8, oxygen = 6, oceans = 9, venus = 26)

    yellow.passWithUnusedActionCards()
    // "It is my turn to be the world government ... I'm going to eat up a Venus slot."
    green.wgt("VenusStep").expect("0 TerraformRating")
    with(green) {
      assertProduction(m = 3, s = 1, t = 5, p = 1, e = 9, h = 0)
      assertResources(m = 38, s = 1, t = 5, p = 2, e = 9, h = 2)
      assertCounts(33 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 13, s = 0, t = 1, p = 2, e = 0, h = 1)
      assertResources(m = 62, s = 0, t = 1, p = 8, e = 0, h = 2)
      assertCounts(39 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 2, p = 0, e = 3, h = 8)
      assertResources(m = 57, s = 2, t = 2, p = 0, e = 3, h = 9)
      assertCounts(35 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = 8, oxygen = 6, oceans = 9, venus = 28)

    // Research: "I buy one card." "I'm gonna buy two cards." "I buy one card."
    blue.buyCards(1)
    green.buyCards(2)
    yellow.buyCards(1)

    blue.turn {
      // "Use my Stratopolis action to add two floaters to Jetstream Microscrappers and then my
      // Jetstream Microscrappers action to raise Venus."
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
      cardAction2(JetStreamMicroscrappers)
    }
    yellow.turn {
      // "Suppose I can start with a Business Network."
      cardAction1(BusinessNetwork) { buyCards(0) }
      // "Atalanta Planitia Lab. For a ten. I draw two cards. ... Only eight because my cutting
      // edge."
      playProject(AtalantaPlanitiaLab, 8)
    }
    green.turn {
      // "I'm gonna pay 22. Damn, two awards. Industrialist and Space Baron."
      fundAward(cn("Industrialist"), 8)
      fundAward(cn("SpaceBaron"), 14)
    }
    blue.turn {
      // "I'm gonna play Gene Repair. So it costs 12 money and I gain two money production."
      playProject(GeneRepair, 12) { declineTask() }
    }
    yellow.turn {
      // "I'm gonna play Cry Yourself to Sleep for 10."
      playProject(CryoSleep, 10)
      // "Solar Probe: two titanium, three monies. ... I get two, but then a third for Solar, and I
      // also get three money, three heat from the thing."
      intentionalOverpay(4)
      playProject(SolarProbe, 3, titanium = 2)
    }
    green.turn {
      // "I pay three energy to fly my boat to Luna. I get 14 and [Yellow] gets two."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-3 Energy, 14 MC, 2 MC<Yellow>")
      // "Pay three energy to fly another boat to Ganymede for five plants."
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }.expect("-3 Energy, 5 Plant")
    }
    blue.turn {
      // "I'm gonna play Orbital Cleanup. ... I put 17. ... give myself three more money. ... my
      // titanium ... three more money."
      playProject(OrbitalCleanup, 11, titanium = 1)
    }
    yellow.turn {
      // "First remove an Aerial Mapper to draw another card."
      cardAction2(AerialMappers)
    }
    green.turn {
      // "Something's waiting for us in the bushes of love. And that cost me six money. It gives me
      // two plant production and two plants."
      playProject(Bushes, 6)
      // "Then I plant forest ... three, four to the right of my city."
      convertPlants { placeTile(3, 4) }
    }
    blue.turn {
      // "For 10 money, I'm going to play Bacto Viral Research. ... discard a card from hand to draw
      // a card. ... add all six of mine to my nitrate reducing bacteria."
      playProject(BactoviralResearch, 10) {
        doTask("-ProjectCard")
        addCardResources(NitriteReducingBacteria)
      }
    }
    yellow.turn {
      // "Pitch a steel for seven real."
      cardAction2(ElectroCatapult)
    }
    green.turn {
      // "Pitch a steel for five real."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "Now I'm going to play Titan Air Scrapping for 21 money."
      playProject(TitanAirScrapping, 21)
    }
    yellow.turn {
      // "Floating Refinery, spend two floaters ... gain a titanium and two monies."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    green.turn {
      // "Play Quantum Communications because it costs me four and it gives me five money
      // production."
      playProject(QuantumCommunications, 4)
    }
    blue.turn {
      // "Use my nitrate reducing bacteria action to ... increase my TR."
      cardAction2(NitriteReducingBacteria)
    }
    yellow.turn {
      // "Spend two energy, get two money."
      cardAction1(PowerInfrastructure, x = 2)
    }
    green.turn {
      // "I'm going to use Inventor's Guild. No. I'm not going to buy it."
      cardAction1(InventorsGuild) { buyCards(0) }
    }
    blue.turn {
      // "I use Floater Technology to add a floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    yellow.turn {
      // "Add a Cloud Tourism, if you will."
      cardAction1(CloudTourism)
    }
    green.turn {
      // "Water Splitting Plant. That costs me eight."
      playProject(WaterSplittingPlant, 8)
    }
    blue.turn {
      // "Take my Local Shading action ... increase my money production by one step."
      cardAction2(LocalShading)
    }
    yellow.turn {
      // "For 12 plus 4, Mangrove. ... that'll be 8-4. Get two money. Get a TR."
      playProject(Mangrove, 10) { placeTile(8, 4) }
    }
    green.turn {
      // "Use my Water Splitting Plant ... lose three energy."
      cardAction1(WaterSplittingPlant)
    }
    blue.turn {
      // "Use my Dirigibles action to add a floater to Titan Air Scrapping."
      cardAction1(Dirigibles) { addCardResources(TitanAirScrapping) }
    }
    yellow.turn {
      // "Tundra Farming. Right, for 16. ... increase plant production one step, increase money
      // production two steps, and gain a plant."
      playProject(TundraFarming, 14)
      // "I got Livestock. ... Decrease your plant production one step. Increase your money
      // production two steps."
      playProject(Livestock, 11)
    }
    green.turn {
      // "I've spent seven on Breathing Filters and I'm putting a science resource on Olympus
      // Conference."
      playProject(BreathingFilters, 7) {
        declineTask() // Research Coordination is not used for this play.
      }
    }
    blue.turn {
      // "I take my Extremophiles action and put a microbe on Nitrate Reducing Bacteria."
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    yellow.turn {
      // L1 Trade Terminal: "I boost the thing two things first."
      // "Because I have two of these bonuses, only pay one energy to trade. And it'll be with
      // Miranda. ... I get two animals to Livestock."
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        doTask("2 ColonyProduction<Miranda>")
        addCardResources(Livestock)
      }
    }
    green.turn {
      // "I'm going to play a Trans-Neptune Probe ... it's free. ... remove a science resource from
      // Olympus Conference and I draw a card."
      playProject(TransNeptuneProbe, 0) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    blue.turn {
      // "I'm going to take my Orbital Cleanup action and I gain six money because I have six
      // science tags."
      cardAction1(OrbitalCleanup)
    }
    yellow.turn {
      // "Spend 5 for Mineral Deposit, gain 5 steel."
      playProject(MineralDeposit, 5)
      // "Spend 4 steel and 1 real on Mining Rights. ... 8, 6."
      playProject(MiningRights, 1, steel = 4) { placeTile(8, 6) }
    }
    green.turn {
      // "Use Titan Shuttles to add 2 floaters to Titan Shuttles."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles, 2) }
    }
    blue.turn {
      // "Use my Celestic action to add a floater to Titan Air Scrapping."
      cardAction1(Celestic) { addCardResources(TitanAirScrapping) }
    }
    yellow.turn {
      // "Ants, pay 7, remember my Cutting Edge this time. ... immediately ants one of your
      // nitrites."
      playProject(Ants, 7)
      cardAction1(Ants)
    }
    green.passWithUnusedActionCards(PalladinShipping, DirectedImpactors)
    blue.turn {
      // "I take my Titan Air Scrapping action. I remove two floaters and I gain a TR."
      cardAction2(TitanAirScrapping)
    }
    yellow.turn {
      // "I add a Livestock."
      cardAction1(Livestock)
    }
    blue.turn {
      // "I take my Red Spot Observatory action and I draw a card, please."
      cardAction2(RedSpotObservatory)
    }
    yellow.turn {
      // "Paid two things for Floating Habs. Add to self."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    blue.turn {
      // "I'm gonna buy some Algae. Cost me 10 money. And I gain one plant and two plant
      // productions."
      playProject(Algae, 10)
    }
    yellow.turn {
      // "Energy Market, lose energy production, gain eight."
      cardAction2(EnergyMarket)
      // "Spend my thirteen on Asteroid Mining Consortium. ... decrease [Blue]'s titanium
      // production by one, increase my own."
      playProject(AsteroidMiningConsortium, 11) {
        doTask("PROD[-Titanium<Blue>]")
      }
    }
    blue.turn {
      // "I'm gonna play a greenery. ... row eight, column eight. For two money and two plants."
      convertPlants { placeTile(8, 8) }
    }
    yellow.passWithUnusedActionCards()
    blue.turn {
      // "I take my Red Ships action and I gain one money."
      cardAction1(RedShips)
      // "I can sell these three ... patents."
      sellPatents(3)
      // "I have seven money and I can play my Lichen! ... one more plant production."
      playProject(Lichen, 7)
    }
    // board-17-27-05.jpg: generation 8 is complete; app ledgers provide the exact pre-production
    // resource checkpoint because the photograph was captured while players were pressing Produce.
    with(green) {
      assertProduction(m = 8, s = 1, t = 5, p = 3, e = 9, h = 0)
      assertResources(m = 4, s = 0, t = 5, p = 1, e = 0, h = 2)
      assertCounts(35 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 14, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 0, s = 0, t = 0, p = 3, e = 0, h = 2)
      assertCounts(43 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 24, s = 1, t = 4, p = 0, e = 2, h = 8)
      assertResources(m = 2, s = 2, t = 2, p = 1, e = 0, h = 12)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = 8, oxygen = 10, oceans = 9, venus = 30)

    blue.passWithUnusedActionCards()
    // "[Blue] would be the world government, but there's no choice. ... raise the oxygen."
    blue.wgt("OxygenStep").expect("0 TerraformRating")
    with(green) {
      assertResources(m = 47, s = 1, t = 10, p = 4, e = 9, h = 2)
      assertCounts(35 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 57, s = 0, t = 0, p = 8, e = 0, h = 3)
      assertCounts(43 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 62, s = 3, t = 6, p = 1, e = 2, h = 20)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 11, oceans = 9, venus = 30)
    // Research: Yellow buys one, Green buys one, and Blue buys none.
    yellow.buyCards(1)
    green.buyCards(1)
    blue.buyCards(0)

    yellow.turn {
      // Yellow's applog records two 23-M€ greenery standard projects. "Oxygen to 13 ... 7-3 ...
      // and 7-4 for a titanium."
      stdProject("GreenerySP") { placeTile(7, 3) }
      stdProject("GreenerySP") { placeTile(7, 4) }
    }
    green.turn {
      // "Use Water Splitting Plant to use up three energy and take the last oxygen ... I give
      // myself a TR."
      cardAction1(WaterSplittingPlant)
      // "Build the Immigrant City ... cost me nine ... 9-8 for two money and a plant."
      playProject(ImmigrantCity, 9) { placeTile(9, 8) }
    }
    blue.turn {
      // "Take the city standard project ... this is 8-5."
      stdProject("CitySP") { placeTile(8, 5) }
      assertCounts(36 to "MC")
      // "Place a city for 25 more money ... 6-8." [sic]
      stdProject("CitySP") { placeTile(7, 8) }
    }
    yellow.turn {
      // "Play Kaguya Tech for 10 ... 7-3 ... flipping the tile ... becoming a city."
      playProject(KaguyaTech, 10) {
        doTask("CityTile<Cimmeria_7_3> FROM GreeneryTile<Cimmeria_7_3>")
      }
      // "Energy Market an energy production into eight monies."
      cardAction2(EnergyMarket)
    }
    green.turn {
      // "Play Open City. It cost me 19 ... go to 5-3."
      playProject(OpenCity, 19) { placeTile(5, 3) }
    }
    // Green forgot to take his Immigrant City effect
    green.exMachina("PROD[-MC]")

    blue.turn {
      // "I'm going to plant forest ... at 7-7 ... that gives you two plants."
      convertPlants { placeTile(7, 7) }
    }
    yellow.turn {
      // "Never did a Business Network. ... draw a card? No."
      cardAction1(BusinessNetwork) { buyCards(0) }
      // "Topsoil Contract for eight. ... no, no, no. Six. Solar Logistics."
      playProject(TopsoilContract, 6)
    }
    green.turn {
      // "Use my Space Elevator to consume a steel and take five real."
      cardAction1(SpaceElevator)
    }
    blue.turn {
      // "Take my Nitrate Reducing Bacteria action. Remove three microbes, increase my TR."
      cardAction2(NitriteReducingBacteria)
    }
    yellow.turn {
      // "Spend an energy to trade with Miranda ... get two animals ... Livestock."
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        doTask("2 ColonyProduction<Miranda>")
        addCardResources(Livestock)
      }
    }
    green.turn {
      // "Spend three energy to trade with Luna ... get 14 and yellow gets two."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-3 Energy, 14 MC, 2 MC<Yellow>")
      // "Spend three energy to fly to Ganymede and take one plant."
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }.expect("-3 Energy, Plant")
    }
    // Earliest: after Blue's generation-8 Observatory draw. Latest: before this draw, whose
    // narration explicitly says a floater is present. An omitted flexible-floater placement is
    // likelier than a second Observatory action because each card action is once per generation;
    // the exact source action is not identified.
    blue.exMachina("Floater<$RedSpotObservatory>")
    blue.turn {
      // "Take my Red Spot Observatory action ... draw a card."
      cardAction2(RedSpotObservatory)
      // "Sell this patent for one money."
      sellPatents(1)
    }
    yellow.turn {
      // "Take your final nitrite ... and my thing gives me a money."
      cardAction1(Ants) {
        doTask("-Microbe<Player2, $NitriteReducingBacteria<Player2>>")
      }
    }
    green.turn {
      // "Now I can plant forest ... six, four to get one titanium and two steel."
      convertPlants { placeTile(6, 4) }
    }
    blue.turn {
      // "Take my Stratopolis action to add two floaters to Titan Air Scrapping."
      // Resolve the action on its own eligible card before reproducing Blue's illegal
      // destination.
      cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) }
    }
    blue.exMachina("-2 Floater<$Stratopolis>, 2 Floater<$TitanAirScrapping>")
    yellow.turn {
      // "Power Infrastructure, spend an energy, gain a money."
      cardAction1(PowerInfrastructure, x = 1)
    }
    green.turn {
      // "Use Inventors' Guild ... two cards, out of cards."
      cardAction1(InventorsGuild) { buyCards(0) }
    }
    blue.turn {
      // "Titan Air Scrapping ... move two floaters and increase my TR."
      cardAction2(TitanAirScrapping)
    }
    yellow.turn {
      // "I'm gonna Livestock."
      cardAction1(Livestock)
    }
    green.turn {
      // The green tableau and the turn order identify Green as the Interstellar Colony Ship player.
      // "Cost me 18, which I'll spend at six titanium." Solar Logistics gives Yellow the card.
      playProject(InterstellarColonyShip, titanium = 6)
    }
    // Green forgets the Palladin effect for paying a space event
    green.exMachina("-Titanium")
    blue.turn {
      // "Orbital Cleanup. I gain six monies."
      cardAction1(OrbitalCleanup)
    }
    yellow.turn {
      // The clipped "Cloud..." response between Blue's cleanup and Green's turn is Yellow's
      // otherwise-unused Cloud Tourism action, corroborated by its final-tableau floater.
      cardAction1(CloudTourism)
    }
    green.turn {
      // "Use Titan Shuttles to take six titanium."
      cardAction2(TitanShuttles, x = 6)
    }
    blue.turn {
      // "I'm gonna fund Benefactor ... cost me 20."
      fundAward(cn("Benefactor"), 20)
    }
    yellow.turn {
      // "Add a floater to Floating Refinery."
      cardAction1(FloatingRefinery)
    }
    green.turn {
      // "I might as well play it ... spend two steel and ... only spent 13. ... draw two cards."
      playProject(AiCentral, 13, steel = 2) {
        declineTask() // Research Coordination is not used for this play.
      }
      cardAction1(AiCentral)
    }
    blue.turn {
      // "Use my Dirigibles action to add a floater to Celestic."
      cardAction1(Dirigibles) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "Add to Floating Habs."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    // Yellow did not announce or log Floating Habs' two-M€ action cost; every later app balance,
    // including the two M€ she spends on Vesta's Shipyard, retains it.
    yellow.exMachina("2 MC")
    green.turn {
      // "Plant forest ... here for a card and two money." The final board identifies 8-7.
      stdProject("GreenerySP") { placeTile(8, 7) }
    }
    blue.turn {
      // "Use my Floater Technology action to add a floater to Celestic."
      cardAction1(FloaterTechnology) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "I sell a patent."
      sellPatents(1)
    }
    green.turn {
      // "I'm going to sell 5 patentos."
      sellPatents(5)
    }
    blue.turn {
      // "Use my Celestic action to add a floater to Celestic."
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    yellow.turn {
      // "Plant a forest ... seven, six ... a plant and two steel."
      convertPlants { placeTile(7, 6) }
    }
    green.passWithUnusedActionCards(PalladinShipping, DirectedImpactors)
    blue.turn {
      // "Take my Red Ships action ... one, two, three, four."
      cardAction1(RedShips).expect("4 MC")
    }
    yellow.turn {
      // "Spend a steel to gain ... seven real."
      cardAction2(ElectroCatapult)
    }
    blue.turn {
      // "Take my Local Shading action to add a floater to Local Shading."
      cardAction1(LocalShading)
    }
    yellow.turn {
      // "Pay seven for Robot Pollinators ... three plants."
      playProject(RobotPollinators, 7)
    }
    blue.turn {
      // "Take my Extremophiles action and add a microbe to Extremophiles."
      cardAction1(Extremophiles) { addCardResources(Extremophiles) }
    }
    yellow.turn {
      // "Pay seven for Insects ... increase plant production ... for each plant tag ... three."
      playProject(Insects, 7)
    }
    blue.passWithUnusedActionCards(JetStreamMicroscrappers)
    yellow.turn {
      // "Stanford Torus ... four titanium ... reserved area."
      playProject(StanfordTorus, titanium = 4)
      // "Sell one, two, three, four, five patents."
      sellPatents(5)
      // "Carbon Nanosystems ... four steel ... and then six real."
      playProject(CarbonNanosystems, 6, steel = 4)
      // "Vesta's Shipyard ... three titanium ... Carbon Nano is worth four ... my two real."
      playProject(VestaShipyard, 2, titanium = 3) {
        doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
      }
      // Yellow's app ledger records one final M€ after Vesta and before Pass.
      sellPatents(1)
    }
    yellow.passWithUnusedActionCards(AerialMappers)

    // All three applogs after the final production phase and before final greenery placement.
    with(green) {
      assertProduction(m = 15, s = 1, t = 5, p = 3, e = 6, h = 0)
      assertResources(m = 59, s = 1, t = 16, p = 3, e = 6, h = 2)
      assertCounts(36 to "TerraformRating", 0 to "$InventionContest")
    }
    with(blue) {
      assertProduction(m = 16, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 67, s = 0, t = 0, p = 9, e = 0, h = 4)
      assertCounts(45 to "TerraformRating", 0 to "$CryoSleep")
    }
    with(yellow) {
      assertProduction(m = 26, s = 1, t = 5, p = 4, e = 1, h = 8)
      assertResources(m = 65, s = 1, t = 5, p = 8, e = 1, h = 28)
      assertCounts(
          38 to "TerraformRating",
          1 to "$CryoSleep",
          1 to "$AerialMappers",
          1 to "$Insects",
      )
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Final greenery placement, in start-player order.
    yellow.convertPlants { placeTile(6, 3) }
    yellow.declineTask()
    green.declineTask()
    blue.convertPlants { placeTile(6, 8) }
    blue.declineTask()

    green.assertCounts(
        0 to "ProjectCard",
        1 to "Science<$OlympusConference>",
        1 to "CardResource", // and that's it
    )
    blue.assertCounts(
        0 to "ProjectCard",
        6 to "Floater<$Celestic>",
        1 to "Floater<$LocalShading>",
        0 to "Floater<$JetStreamMicroscrappers>",
        0 to "Floater<$Dirigibles>",
        0 to "Floater<$Stratopolis>",
        0 to "Floater<$RedSpotObservatory>",
        0 to "Microbe<$NitriteReducingBacteria>",
        1 to "Microbe<$Extremophiles>",
        8 to "CardResource", // and that's it
    )
    yellow.assertCounts(
        0 to "ProjectCard",
        8 to "Floater<$FloatingHabs>",
        0 to "Floater<$AerialMappers>",
        1 to "Floater<$FloatingRefinery>",
        3 to "Floater<$CloudTourism>",
        2 to "Microbe<$Ants>",
        6 to "Animal<$Livestock>",
        0 to "Graphene<$CarbonNanosystems>",
        20 to "CardResource", // and that's it
    )

    val score = Summarizer(game)
    assertEquals(5, score.net("FirstPlace", "VictoryPoint<Blue>"))
    assertEquals(4, score.net("GreeneryTile", "VictoryPoint<Blue>"))
    assertEquals(5, score.net("CityTile", "VictoryPoint<Blue>"))
    assertEquals(13, score.net("Card", "VictoryPoint<Blue>"))
    green.assertCounts(88 to "VictoryPoint", 0 to "Victory")
    blue.assertCounts(72 to "VictoryPoint", 0 to "Victory")
    yellow.assertCounts(94 to "VictoryPoint", 1 to "Victory")

    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -             [O]    LP    VS    LP   [O]
        |
        | 2 -          [O]   [G1]   L     L    LPS   [O]
        |
        | 3 -        L    [G1]  [C1]  [G1]   L     LP    L
        |
        | 4 -     VS    L    [S1]   L    LSS    L    VTT    LC
        |
        | 5 -  L     L    [C1]   LS    LS    LC    L    LSC    W
        |
        | 6 -    [C3]  [G3]  [G1]   L     LT   LSS   [G2]  LSS
        |
        | 7 -       [C3]  [G3]   L    [G3]  [G2]  [C2]  [O]
        |
        | 8 -          [G3]  [C2]  [S3]  [G1]  [G2]  [O]
        |
        | 9 -             [O]   [O]   [O]   [C1]  [G2]
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
  }
}
