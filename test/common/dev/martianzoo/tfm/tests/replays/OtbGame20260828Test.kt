package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

/** Three-player physical game begun Friday, 2026-08-28. */
internal class OtbGame20260828Test : AbstractFullGameTest() {
  private val colonyTiles = listOf("Ganymede", "Io", "Luna", "Miranda", "Titan")

  override val config =
      GameConfig(
          """
          CimmeriaMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, ColoniesExpansion, PromoCardPack

          Engineer, Fundraiser, Landshaper, Merchant, Metallurgist
          Benefactor, EstateDealer, Industrialist, Metropolist, SpaceBaron
          ${colonyTiles.joinToString()}
          """,
          "Dad",
          "Joanna",
          "Ellie",
      )

  @Test
  internal fun otbGame20260828() {
    TfmWorkflow.Auto(game).launch()
    val dad = game.tfm(Player.PLAYER1)
    val joanna = game.tfm(Player.PLAYER2)
    val ellie = game.tfm(Player.PLAYER3)

    // "Okay, I am the start player, and I play Paladin Shipping. That gives me 36 money and five
    // titanium. Then I use this little buy-cards slider to buy four cards."
    dad.playCorp(PalladinShipping, 4)
    // "I play Celestic, so I start with 42 megacredits." "You do get 42 money and then buy how many
    // cards? Five cards?" "Oh, yes."
    joanna.playCorp(Celestic, 5)
    // "And I am Point Luna." "Oh, man. That corp is strong." "Start with 38 money, titanium
    // production. It immediately gives me a card. And I slide five cards. Boop."
    ellie.playCorp(PointLuna, 5)

    dad.turn {
      // "I've played Biofuels and Supplier. I get two energy production and four steel, and then I
      // get one energy production, one plant production, and two plants."
      playPrelude(Biofuels)
      playPrelude(Supplier)
    }
    joanna.turn {
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
    ellie.turn {
      // "Orbital Construction Yard: I gain titanium production and four titanium."
      playPrelude(OrbitalConstructionYard)
      // "Early Colonization: I place a colony on Luna, which means I gain 2 money production, I
      // gain 3 energy—not energy production, 3 energy—and raise all colony tracks 2 steps."
      playPrelude(EarlyColonization) { doTask("Colony<Luna>") }
    }

    // board-17-33-49.jpg: post-Prelude setup, before the first project action.
    with(dad) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 24, s = 4, t = 5, p = 2, e = 0, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(joanna) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(24 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 2, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertResources(m = 23, s = 0, t = 4, p = 0, e = 3, h = 0)
      assertCounts(20 to "TerraformRating", 1 to "Colony<Luna>")
    }
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 2, venus = 4)

    dad.turn {
      // "I'm gonna play Titan Shuttles." "Oh, shit, that's competition." "Yep. Floater on floater
      // warfare. Floatfare." "I will spend three titanium, which counts as nine money, so I need
      // 14 more. That's my turn."
      playProject(TitanShuttles, 14, titanium = 3)
    }
    joanna.turn {
      // "I had a plan; it's gone now." "Your first action should be to draw this stuff."
      // "There's one: Jet Stream Microscrappers. And Floater Technology. Very cool."
      stdAction("DoRequiredActions").expect("2 ProjectCard")
      // "Now I'm gonna play Local Shading. Pay four for it. And I guess that's my two actions."
      playProject(LocalShading, 4)
    }
    ellie.turn {
      playProject(RimFreighters, 1, titanium = 1)
    }
    dad.turn {
      // "I'm going to use the Titan Shuttles action to put two floaters on Titan Shuttles."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      // "Where's my plan? FML. You have all these floater cards now. I'm going to play
      // Floater Technology for seven."
      playProject(FloaterTechnology, 7)
      // "I'm going to use the Local Shading action to add a floater to Local Shading. Wait, no,
      // you're right. I'm going to use the Floater Technology action to add it to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      // "For some reason, I thought I was rushing to trade. It occurs to me now that it's probably
      // not worth it for you guys to trade. I probably should have played Business Network first.
      // That's four real monies and an Earth-tag discount card."
      playProject(BusinessNetwork, 4)
    }

    // She forgot to reduce her money production, and fixed it later
    ellie.exMachina("PROD[MC]")

    dad.pass()
    joanna.turn {
      // "I'm going to play Nitrite Reducing Bacteria. That costs me 11 money."
      // "You immediately get three free microbes on Nitrite Reducing Bacteria."
      playProject(NitriteReducingBacteria, 11)
      // "For my next trick, I will use the Local Shading action to remove a floater and increase
      // my money production one step."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "I use Business Network action. I look at a card... not feeling it."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    joanna.turn {
      // "I will use my Nitrite Reducing Bacteria action to spend three microbes and increase my TR
      // one step. I guess I pass." "No, you have one more action."
      cardAction2(NitriteReducingBacteria)
      // "Oh, my Celestic action. I forgot. I will use my Celestic action to add a floater to Local
      // Shading."
      cardAction1(Celestic) { addCardResources(LocalShading) }
    }
    ellie.turn {
      // "I will spend two energy to trade with Luna. That's seven from the track plus my bonus of
      // two, so nine." "Jesus."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 9 MC")
      // "One more thing. I spent one for Fuel Generators, lose a money production, and gain energy
      // production. And I think that's it."
      playProject(FueledGenerators, 1)
    }
    joanna.pass()
    ellie.pass()

    // "I will raise the oxygen to one percent as my World Government step."
    dad.wgt("OxygenStep").expect("0 TerraformRating")

    // board-17-45-16.jpg and all three app histories: Generation 2 before Research.
    with(dad) {
      assertProduction(m = 0, s = 0, t = 0, p = 1, e = 3, h = 0)
      assertResources(m = 30, s = 4, t = 2, p = 3, e = 3, h = 0)
      assertCounts(20 to "TerraformRating")
      assertCardResources(2 to TitanShuttles)
    }
    with(joanna) {
      assertProduction(m = 1, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 31, s = 0, t = 2, p = 2, e = 0, h = 0)
      assertCounts(25 to "TerraformRating")
      assertCardResources(1 to LocalShading, 0 to NitriteReducingBacteria)
    }
    with(ellie) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 0)
      assertResources(m = 47, s = 0, t = 5, p = 0, e = 1, h = 1)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 2, venus = 4)

    joanna.buyCards(2)
    ellie.buyCards(2)
    dad.buyCards(3)

    joanna.turn {
      // "I'm going to start by taking my Local Shading action, spending a floater to increase my
      // money production."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "I'm going to use my Business Network. And I will buy it."
      cardAction1(BusinessNetwork) { buyCards(1) }
    }
    dad.turn {
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
    joanna.turn {
      // "I'm going to play Jet Stream Microscrappers. Pay 12 for it." "You want me to actually pay
      // for the things that I place? You're crazy."
      playProject(JetStreamMicroscrappers, 12)
    }
    ellie.turn {
      // "I'm going to pay five titanium and five real for Solar Logistics." "Oh, man. She gets to
      // draw a card any time any one of us plays a space event. Plus she gets a discount on Earth
      // tags."
      playProject(SolarLogistics, 5, titanium = 5)
    }
    dad.turn {
      // "You'll never imagine what I'm going to do. I'm going to use Titan Shuttles to spend nine
      // floaters and get nine titanium." "Gawah."
      cardAction2(TitanShuttles, x = 9)
    }
    joanna.turn {
      // "I'm going to play Dirigibles, which costs 11 money."
      playProject(Dirigibles, 11)
    }
    ellie.turn {
      // "On top of Solar Logistics, I will place Optimal Aerobraking. For that, I will spend
      // seven." "We cannot let her get space events."
      playProject(OptimalAerobraking, 7)
    }
    dad.turn {
      // "Here we go. Spache Elevator." "Am I spending all titanium for it? I'm gonna hold back one
      // titanium. I'm gonna spend nine titanium. Not 99, just nine." "Stupid fucking Spache
      // Elevator."
      playProject(SpaceElevator, titanium = 9)
    }
    joanna.turn {
      // "I'm going to take my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      // "Solar Logistics also gives me a discount of two on my Earth tag. I'm gonna pay seven for
      // Imported Advanced GHG. Increase heat production two steps. I get a card for Solar
      // Logistics,
      // and from Optimal Aerobraking I get three money and three heat."
      playProject(ImportOfAdvancedGhg, 7).expect("-4 MC, 3 Heat, PROD[2 Heat]")
    }
    dad.turn {
      // "I am going to use my Spache Elevator action to lose one steel and gain five real."
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      // "Oh my god, I'm an idiot. Dude, I have plants. I can plant Potatoes. Why didn't I do this
      // last freaking generation? That cost me two money. I lose two plants, and I get two money
      // production." "Yep. Potatoes are mine. Andy Weir up in here."
      playProject(Potatoes, 2)
    }
    ellie.pass()
    dad.turn {
      // "I'm spending three steel and 12 real on Research Outpost, which lets me place a city tile.
      // I can place it right here, pay five money, and get another colony, which I'm just gonna
      // drop
      // on Luna. That gives me two money production, getting me back to zero again."
      playProject(ResearchOutpost, 12, steel = 3) {
        placeTile(3, 3)
        doTask("Colony<Luna>")
      }
    }
    joanna.turn {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    dad.turn {
      // "Ellie's already passed, so I'm the one holding things up. We play Peroxide Power, which
      // costs me six real money. I lose one money production and add two energy production."
      playProject(PeroxidePower, 6)
    }
    joanna.turn {
      // "I'm going to take my Dirigibles action, which lets me add a floater to any card, and add
      // it
      // to Jet Stream Microscrappers. Then I take my Celestic action to add another floater to Jet
      // Stream Microscrappers." "I forgot I'm supposed to pay titanium for both of those." "You
      // don't have to pay." "I don't have to pay, thank God."
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    dad.pass()
    joanna.turn {
      // "Then I'm going to use my Jet Stream Microscrappers action to remove two floaters and raise
      // Venus one step." "Ah shit, I made it easier for Ellie to get the bonus."
      cardAction2(JetStreamMicroscrappers)
    }
    joanna.pass()

    // "I'm going to raise Venus." "Venus is now at eight. No one gets the card. We're still at one
    // oxygen, two oceans, and no temperature raises yet."
    joanna.wgt("VenusStep").expect("0 TerraformRating")

    with(dad) {
      assertProduction(m = -1, s = 0, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 20, s = 0, t = 2, p = 4, e = 5, h = 0)
      assertCounts(20 to "TerraformRating")
    }
    with(joanna) {
      assertProduction(m = 4, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 30, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(26 to "TerraformRating")
      assertCardResources(1 to LocalShading, 1 to NitriteReducingBacteria)
    }
    with(ellie) {
      assertProduction(m = 1, s = 0, t = 2, p = 0, e = 1, h = 2)
      assertResources(m = 43, s = 0, t = 4, p = 0, e = 1, h = 7)
      assertCounts(20 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 2, venus = 8)

    ellie.buyCards(3)
    dad.buyCards(2)
    joanna.buyCards(1)

    ellie.turn {
      // "I think I will start by paying two titanium to trade with Io. That's ten heat for me."
      stdAction("TradeAction", 3) { doTask("Trade<Io>") }.expect("-2 Titanium, 10 Heat")
      // "Probably should have done this sooner, but Business Network."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    dad.turn {
      // "What I'm gonna do is fly my boat. I'm gonna spend three energy, fly my boat to Ganymede,
      // take five planta. When you want to convert plants to greenery, you hit plants, but then
      // there's this 'plant forest'—for whatever reason they call it plant forest. It automatically
      // gives me the TR and takes away the eight plants and everything."
      stdAction("TradeAction", 2) { doTask("Trade<Ganymede>") }.expect("-3 Energy, 5 Plant")
      // "My forest is gonna go on 3-2, so that it's next to my city and gets two money from ocean.
      // That raises oxygen to two percent, and we got my TR already. That was my two actions."
      convertPlants { placeTile(3, 2) }.expect("-8 Plant, 2 MC, TerraformRating")
    }
    joanna.turn {
      // "How much money do I have? Fuck. Okay, FML. I guess I'm playing Io Sulphur Research. I pay
      // for it first, then draw three cards. I have two Venus tags."
      // "I'll buy Reno."
      playProject(IoSulphurResearch, 17) { doTask("3 ProjectCard") }.expect("2 ProjectCard")
    }
    ellie.turn {
      // "For six monies, Carbonate Processing. Lose energy production, gain three heat production."
      playProject(CarbonateProcessing, 6)
    }
    dad.turn {
      // "This is really cool, actually. I play Mining Area. It costs three money. That solves both
      // my—because I need steels for this thing, you know? It gives me one for this turn and solves
      // my problem for the future as well. That gives me one steel and one steel production."
      playProject(MiningArea, 3) { placeTile(4, 3) }.expect("Steel, PROD[Steel]")
      // "As my second action, I can spend eight on the Land Shaper."
      claimMilestone(cn("Landshaper"))
    }
    joanna.turn {
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
    ellie.turn {
      // "For Power Infrastructure."
      playProject(PowerInfrastructure, 4)
    }
    dad.turn {
      // "Now I can use Spache Elevator to use one steel and get five real. Was there anything else
      // I was in a hurry to do? No."
      // "Look, guys, I'm jet-lagged. Jet lag makes me stupid, okay? We know this. This is a true
      // fact about me."
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      // "I'm just gonna use my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      // "I now have two power tags, so I can play Fusion Tower [Fusion Power]."
      // "Dang it, I didn't think you were gonna be able to play that. I thought it might come back
      // to me."
      // "Increase energy production three steps."
      playProject(FusionPower, 14)
    }
    dad.turn {
      // "I'm gonna use Titan Shuttles to add two floaters."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      // "You'll never guess: I'm gonna use my Local Shading action to spend a floater to increase
      // my
      // money production by one."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "I will use my remaining ten money to play House Printing, gain steel production."
      playProject(HousePrinting, 10)
    }
    dad.turn {
      // "I will use my Paladin Shipping action to pay two titanium to raise the temperature to
      // minus
      // 28 and get my second TR."
      cardAction1(PalladinShipping)
    }
    joanna.turn {
      // "I'm gonna use my Floater Technology action to add one floater to Local Shading."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      // "I have 17 heat."
      // Ellie's app and the photographed track require both ordinary heat conversions.
      convertHeat()
      convertHeat().expect("PROD[Heat]")
    }
    dad.pass()
    joanna.turn {
      // The recording goes silent here. Joanna's app adds one TR, and these three unused actions
      // are the ordinary card sequence that produces exactly that result.
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(JetStreamMicroscrappers) }
    }
    ellie.pass()
    joanna.turn {
      cardAction2(JetStreamMicroscrappers)
    }
    joanna.pass()

    // board-18-39-07.jpg: end of the Generation 3 action phase, before production.
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 2, venus = 10)
    dad.assertCounts(
        22 to "TerraformRating",
        1 to "GreeneryTile<Cimmeria_3_2>",
        1 to "CityTile<Cimmeria_3_3>",
        1 to "MiningArea_SpecialTile<Cimmeria_4_3>",
        1 to "Landshaper",
    )
    joanna.assertCounts(27 to "TerraformRating")
    ellie.assertCounts(22 to "TerraformRating")

    // Venus is the only World Government choice consistent with the stated Generation 4 values.
    ellie.wgt("VenusStep").expect("0 TerraformRating")

    with(dad) {
      assertProduction(m = -1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 1, p = 2, e = 5, h = 2)
    }
    with(joanna) {
      assertProduction(m = 5, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 34, s = 0, t = 2, p = 0, e = 0, h = 0)
    }
    with(ellie) {
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
    dad.buyCards(3)
    joanna.buyCards(3)
    ellie.buyCards(1)

    dad.turn {
      // "Research Colony. One titanium and 16 real money. Not only does it let me place another
      // colony, it lets me go somewhere that I'm already at. I get another two money production, so
      // I'm back to positive money production again."
      playProject(ResearchColony, 16, titanium = 1) { doTask("Colony<Luna>") }
      // "When I use my second action to fly my boat, I get ten plus two plus two, and Ellie gets
      // two. So I get 14. And that was my two actiones. Oh, and I draw two cards."
      // "They're just lousy Earth-tag cards."
      // "Are you saying that to piss me off?"
      // "Yes."
      // "Nice."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-3 Energy, 14 MC, 2 MC<Ellie>")
    }
    joanna.turn {
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
    ellie.turn {
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
    // Ellie then corrected Business Network's previously omitted production loss and the three
    // generations of extra income it had caused.
    ellie.exMachina("-3 MC, PROD[-MC]")
    dad.turn {
      // "Space Elevate. A steel for five money."
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      // "I will do my Nitrite Reducing Bacteria action to add a microbe."
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
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
    dad.turn {
      // "With this money that, as previously observed, I have. How did I get that money, by the
      // way?
      // Oh, Space Elevating. Spillivate."
      // "It's your lucky day, Ellie. I'm playing a space event. That cost me 22. Paladin Shipping
      // gives me a titanium. You get a card. I'll take a titanium, take two plants, raise oxygen to
      // three and get a TR, and place an ocean tile and get a TR. The ocean tile will be 7-9 for
      // two
      // plants and two money. Au revoir."
      playProject(TowingAComet, 22) { placeTile(7, 9) }
    }
    joanna.turn {
      // "I'm gonna take my Local Shading action to remove a floater and gain a money production."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "When did we get a third oxygen?"
      // "Just now."
      // "Good, because I wasn't paying attention."
      // "I pitch a card."
      // "You sell a Patento? As your turn?"
      // "Yes."
      sellPatents(1)
    }
    dad.turn {
      // "I am going to Titan Shuttles to place two floaters."
      cardAction1(TitanShuttles) { addCardResources(TitanShuttles) }
    }
    joanna.turn {
      // "I'm going to use my Floater Technology action to add a floater to Local Shading."
      // "Keep in mind, it can be useful to park them on Dirigibles too, because then you have the
      // option of using them as money. Local Shading, you can always next turn put it on and take
      // it
      // off, so it doesn't really do any advantage to have it there this turn. On the other hand,
      // there might be a card that does even better that collects floaters."
      cardAction1(FloaterTechnology) { addCardResources(LocalShading) }
    }
    ellie.turn {
      // "I use Power Infrastructure: spend an energy to gain money."
      cardAction1(PowerInfrastructure, x = 1)
    }
    dad.pass()
    joanna.turn {
      // "I will take my Jet Stream Microscrappers action to remove two floaters and raise Venus to
      // 14."
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      // "Water to Venus. Spend three titanium."
      // "She always gets it. Such an asshole."
      // "I raise it to 16, which gives an extra TR. And I get three money and three heat from
      // Optimal
      // Aerobraking."
      // "I never told you that I hate you."
      playProject(WaterToVenus, titanium = 3)
    }
    joanna.turn {
      // "I use my Dirigibles action to put a floater on Jet Stream Microscrappers, and my Celestic
      // action to put a floater on Dirigibles. And that's all, folks."
      cardAction1(Dirigibles) { addCardResources(JetStreamMicroscrappers) }
      cardAction1(Celestic) { addCardResources(Dirigibles) }
    }
    ellie.pass()
    joanna.pass()

    // board-18-58-23.jpg: end of the Generation 4 action phase.
    assertSidebar(gen = 4, temp = -24, oxygen = 3, oceans = 4, venus = 16)
    dad.assertCounts(24 to "TerraformRating", 1 to "Milestone")
    joanna.assertCounts(29 to "TerraformRating")
    ellie.assertCounts(24 to "TerraformRating", 1 to "CityTile<Cimmeria_6_2>")
    engine.assertCounts(
        1 to "OceanTile<Cimmeria_2_1>",
        1 to "OceanTile<Cimmeria_9_5>",
        1 to "OceanTile<Cimmeria_8_9>",
        1 to "OceanTile<Cimmeria_7_9>",
    )
    dad.assertCardResources(4 to TitanShuttles)
    joanna.assertCardResources(
        1 to LocalShading,
        3 to NitriteReducingBacteria,
        2 to JetStreamMicroscrappers,
        1 to Dirigibles,
    )

    // "I'll put another ocean out here at 1-1. Oceans go up to five."
    // "Oceans rise and pass fall."
    dad.wgt("OceanTile<Cimmeria_1_1>").expect("0 TerraformRating")

    // All three app histories: post-production Generation 5 pause, before Research.
    with(dad) {
      assertProduction(m = 1, s = 1, t = 1, p = 1, e = 5, h = 0)
      assertResources(m = 30, s = 1, t = 2, p = 7, e = 5, h = 4)
      assertCounts(24 to "TerraformRating")
    }
    with(joanna) {
      assertProduction(m = 6, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertCounts(29 to "TerraformRating")
    }
    with(ellie) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 2, h = 6)
      assertResources(m = 35, s = 2, t = 3, p = 0, e = 2, h = 19)
      assertCounts(24 to "TerraformRating")
    }
    assertSidebar(gen = 5, temp = -24, oxygen = 3, oceans = 5, venus = 16)
    // "Six, nine, and eight. Perfect. Perfect."
    dad.assertCounts(6 to "ProjectCard")
    joanna.assertCounts(9 to "ProjectCard")
    ellie.assertCounts(8 to "ProjectCard")

    // The resumed table drafts to the right. The app histories record the resulting purchases.
    joanna.buyCards(2)
    ellie.buyCards(3)
    dad.buyCards(3)

    joanna.turn {
      // "I'm gonna play Protected Valley."
      // "We're gonna save the River Valley. Yay."
      // "I pay 23 money. I don't give myself 23 money. I pay 23 money. I gain two money production,
      // and I place a greenery on an area reserved for ocean."
      // "Row nine, column nine. This is a city. Oh, it flips. Yes. Row nine, column nine, greenery.
      // Number nine, which gets me a plant."
      // "And two money."
      playProject(ProtectedValley, 23) { placeTile(9, 9) }
    }
    ellie.turn {
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
      // "Yeah. I just needed to place a greenery there. Ellie gets the Merchant."
      claimMilestone(cn("Merchant"))
    }
    dad.turn {
      // "Oh, yeah. I was going to try to steal Ellie's milestone."
      // "I'm afraid I'm going to spend three energy and fly my boat to Titan. Three floaters and a
      // floater. And Joanna, you get one floater."
      // "Yay!"
      // "I put my three and one on my only floater card, Titan Travels."
      // "I'm going to put my floater on Dirigibles."
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(dad) {
          doTask("3 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          dad.selectTask("Floater<Joanna>.")
          joanna.doTask("Floater<$Dirigibles>")
        }
      }
    }
    joanna.turn {
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
    ellie.turn {
      // "Now that I have much enticed, spend two energies to trade with Luna. I get 12. You get 12
      // and I get four."
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 12 MC, 4 MC<Dad>")
      // "And I will Business Network."
      // "You are business networking."
      // "Nah."
      cardAction1(BusinessNetwork) { buyCards(0) }
    }
    dad.turn {
      // "Well, I'll do my thing where I spend one steel to get five real by using Spache Elevator.
      // That's my turn. Go off."
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      // "I'm out of money, so I am going to use my Nitrite Reducing Bacteria action to remove three
      // microbes and raise my TR one step."
      cardAction2(NitriteReducingBacteria)
    }
    ellie.turn {
      // "Man, I want to do stuff, but—wait. Wait. I think I can. You know what? I can always just
      // start by—can't really lose anything if I—yes, I will spend my two steel and 13 real. Then
      // immediately spend a plant to gain seven."
      // "Oh, I should remember to lose an energy production."
      // "Oh, yeah. Almost wrong."
      playProject(ElectroCatapult, 13, steel = 2)
      cardAction1(ElectroCatapult)
    }
    dad.turn {
      // "I think I should probably use Titan Shuttles to take eight titanium. Oyga."
      cardAction2(TitanShuttles, x = 8)
    }
    joanna.turn {
      // "I am going to use my Local Shading action. I remove a floater and I get a money
      // production."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "Robotic Workforce."
      // "Yeah, I didn't like giving that to you."
      // "Spend nine. You have so many building production cards."
      // "Yeah, I will replicate Fusion Power."
      // "Fuck. I didn't see that one."
      playProject(RoboticWorkforce, 9) { doTask("CopyProductionBox<$FusionPower>") }
    }
    dad.turn {
      // "On my turn, it's time to play Io Mining Industries. That cost me ten titanium, which is
      // worth three, and I get two titanium production, two money production, and I'm going to get
      // bank on Jupiter tags."
      playProject(IoMiningIndustries, 10, titanium = 10)
    }
    joanna.turn {
      // "I'm using Jet Stream Microscrappers to spend two floaters and raise the Venus."
      // "To 18."
      // "To 18, and I give myself a TR for that."
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      // "Alright, I think I've done the urgent stuff. Now I can double heat boop."
      // "Okay. Boop. That takes temp to minus 20, and I get a heat product."
      convertHeat()
      convertHeat().expect("PROD[Heat]")
    }
    dad.turn {
      // "Let's just direct some impactors. Direct impact. I pay seven for that."
      playProject(DirectedImpactors, 7)
    }
    joanna.turn {
      // "I'm going to use Floater Technology to add one floater to Dirigibles."
      // "Okay. Alright. Dirigibles."
      cardAction1(FloaterTechnology) { addCardResources(Dirigibles) }
    }
    ellie.turn {
      // "Actually, you know what the hell. Why not? I'm going to sell a card for a money and then
      // pay five for Floating Habs."
      // "Ah, that's the one that's two floaters to a victory point."
      // "Yep. I has it. I finally have one, two science tags."
      sellPatents(1)
      playProject(FloatingHabs, 5)
    }
    dad.turn {
      // "I'm going to Release my Inert Gases, which costs all 13 of my money. And it just gives me
      // two TR. And it flips."
      // "Oh, I forgot to use Directed Impactors. Built it for nothing."
      // "Yeah, I think I'm kind of fading too. In terms of my awareness."
      playProject(ReleaseOfInertGases, 13)
    }
    joanna.turn {
      // "I'm going to use my Stratopolis action to add two floaters to my Dirigibles. Sweet."
      cardAction1(Stratopolis) { addCardResources(Dirigibles, 2) }
    }
    ellie.turn {
      // "I'm going to pitch two cards for two money. Then spend two money to add a floater to any
      // card, and that will be Floating Habs."
      sellPatents(2)
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    dad.pass()
    joanna.turn {
      // "I'm going to use my Extremophiles action to add two floaters to my Dirigibles."
      // "I'm going to play Extremophiles."
      // "Does that have a requirement?"
      // "Two science tags?"
      // The app history shows that a Dirigibles floater paid the three-M€ cost; Joanna had no M€.
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
    ellie.pass()
    joanna.turn {
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
    joanna.wgt("OxygenStep").expect("0 TerraformRating")
    with(dad) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 29, s = 1, t = 3, p = 8, e = 5, h = 6)
      assertCounts(26 to "TerraformRating")
    }
    with(joanna) {
      assertProduction(m = 11, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 43, s = 0, t = 0, p = 3, e = 0, h = 0)
      assertCounts(32 to "TerraformRating")
    }
    with(ellie) {
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
    joanna.buyCards(2)
    ellie.buyCards(4)
    dad.buyCards(4)

    ellie.turn {
      // "Well, why don't I go ahead and pay eight for Engineer. And I think I will pay two energies
      // to trade with Luna. I'll collect my ten—no, twelve monies. You get your four."
      // "Oh, put your boat on."
      // "Yes, thank you."
      claimMilestone(cn("Engineer"))
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("-2 Energy, 12 MC, 4 MC<Dad>")
    }
    dad.turn {
      // "This kind of sucks. I shouldn't have taken so many cards. Okay, well, alright. I'm not
      // racing for anything anymore. I'll use Space Elevator to spend one steel and take five
      // real."
      cardAction1(SpaceElevator)
    }
    joanna.turn {
      // "I am going to buy some Red Ships. Oh, wait, that does—okay, which costs me two money."
      // "I should actually warn you that it doesn't pay out anything just yet."
      // "I know. Why would I not know that?"
      // "It will. It will, yeah."
      playProject(RedShips, 2)
    }
    ellie.turn {
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
    dad.turn {
      // "I'm gonna play Research Coordination for three."
      // "Oh God, you like the wild tags."
      // "I love them. They're so useful."
      // "You like to live on the wild side."
      playProject(ResearchCoordination, 3)
    }
    joanna.turn {
      // "I'm going to play Snow Algae, which costs me 12 money, and it gives me one plant
      // production
      // and one heat production."
      playProject(SnowAlgae, 12)
    }
    ellie.turn {
      // "Spend a plant to gain seven money production—no, not money production, seven money
      // resource. That would be ridiculous."
      cardAction1(ElectroCatapult)
      // "I probably may as well Cartel. One, two, three, four. Be nice if I had more good Point
      // Luna, but whatever. Four money production, yes, and the card."
      playProject(Cartel, 6)
    }

    // Earliest: Cartel's payment. Latest: before Optimal Aerobraking's next recorded M€ balance.
    // Most likely: Ellie forgot Cartel's six-M€ effective cost; the transcript announces its
    // production and draw but no payment, and placing the mistake here produces perfect agreement
    // with every later app balance despite the app's ordinary transaction grouping.
    ellie.exMachina("6 MC")

    dad.turn {
      // "I am going to play Olympus Conferencia, which costs nine, and it gives me a science
      // resource
      // because it's a science tag."
      playProject(OlympusConference, 9)
    }
    joanna.turn {
      // "Okay, I'm now going to play Red Spot Observatory, which costs 17 monies, and with Mars
      // University, can I draw the two cards that I get from this first and then decide how to
      // discard
      // one?"
      // "Okay, good. I draw two cards."
      // "Come on. Dropped card. Stupid-ass card."
      // "Let the record show that Joanna dropped her stupid-ass card."
      // "Fuck you guys."
      // "I'm going to discard this one to draw a new card because of my Mars University effect.
      // Thanks. That is better."
      playProject(RedSpotObservatory, 17) {
        doTask("-ProjectCard")
      }
    }
    ellie.turn {
      // "I realize, you know, I got some cash flow. Stratospheric Expedition. Pay four titaniums."
      // "Add two floaters to any card. That will be Floating Habs. Draw two Venus cards. But also
      // save a card for me to draw because of—"
      // "Do you want that first or do you want the Venus cards first?"
      // "Venus cards first."
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(FloatingHabs, 2)
      }
    }
    dad.turn {
      // "I am going to spend three energy to fly my boat to Titan."
      // "To Titan."
      // "And Joanna gets one floater, and I get two plus one floaters."
      // "I guess I'll go ahead and put my floater onto Local Shading."
      stdAction("TradeAction", 2) {
        doTask("Trade<Titan>")
        doWithoutAutoExec(dad) {
          doTask("2 Floater<$TitanShuttles>")
          doTask("Floater<$TitanShuttles>")
          dad.selectTask("Floater<Joanna>.")
          joanna.doTask("Floater<$LocalShading>")
        }
      }
    }
    joanna.turn {
      // "I don't have money to pay for any of my cards. I'm just doing actions."
      // "I'm going to take my Nitrate Reducing Bacteria action and add a microbe to my Nitrate
      // Reducing Bacteria. Bet you guys never would have guessed that I'm going to do something
      // crazy
      // like that."
      cardAction1(NitriteReducingBacteria)
    }
    ellie.turn {
      // "Can I see your player board?"
      // "You suck. Apparently not. No, you can't see it."
      // "You know what? I don't have shit for my ass. I don't have anything. You know what? Leave
      // me
      // alone."
      // "Dad, I'm hiring raiders. Stealing three money from you. Boop. Boop, boop, doot, doot. And
      // pay them one money for it."
      playProject(HiredRaiders, 1) { doTask("3 M<Ellie> FROM M<Dad>") }
    }
    dad.turn {
      // "That might just possibly screw me up, actually."
      // "Yay! Everybody dance! Everybody dance! Life is good!"
      // "God, I keep forgetting. Every time I trade, I forget to play Market Manipulation first.
      // Over and over."
      // "I'm gonna use Titan Shuttles to take three titanium."
      cardAction2(TitanShuttles, x = 3).expect("-3 Floater<$TitanShuttles>, 3 Titanium")
    }
    joanna.turn {
      // "I'm gonna use my Local Shading action to—not to add a floater—to spend a floater to add a
      // money production."
      cardAction2(LocalShading)
    }
    ellie.turn {
      // "I will play Cutting Edge Technology for 12."
      // "Nice. Love it?"
      // "Yeah."
      playProject(CuttingEdgeTechnology, 12)
    }
    dad.turn {
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
      assignWildTag(ResearchCoordination, "EarthTag")
      playProject(SkyDocks, 5, titanium = 4)
    }
    joanna.turn {
      // "I'm using my Floater Technology action to put a floater somewhere fun, I guess. I don't
      // know."
      // "You're starting to drown in floaters."
      // "Put it on Celestic."
      // The spoken choice conflicts with the later definite Observatory draw, which establishes the
      // physical floater's destination.
      cardAction1(FloaterTechnology) { addCardResources(RedSpotObservatory) }
    }
    ellie.turn {
      // "Expat Ishtar for four. I gain three titanium, and it requires Venus ten percent."
      // "Yeah, we got that."
      // "Draw two Venus cards. What gives you a two discount?"
      // "Cutting Edge."
      playProject(IshtarExpedition, 4)
    }
    dad.turn {
      // "I'm gonna play Inventors' Guild. I call it Inventioner's Guild. That cost me seven money."
      // "And then I'm gonna use it."
      // "Use it tonight."
      // "Yeah, that's not bad. I'll pay three for that."
      playProject(InventorsGuild, 7) {
        doTask("Science<Player1, OlympusConference<Player1>>")
      }
      cardAction1(InventorsGuild) { buyCards(1) }
    }
    joanna.turn {
      // "I'm gonna use my Extremophiles action to put a microbe on my sulfite reducing bacteria.
      // Bet
      // you weren't expecting that."
      cardAction1(Extremophiles) { addCardResources(NitriteReducingBacteria) }
    }
    // "I am now realizing I miscalculated. Bummer, dude. How dare I miscalculate."
    // "In multiple ways, I really could have played my cards better, quite literally."
    ellie.turn {
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
    dad.turn {
      // "I am gonna use Paladin Shipping for almost the first time ever to get a Temp Boop to minus
      // 18."
      // "I don't know about that. It's at least the second."
      cardAction1(PalladinShipping)
    }
    joanna.turn {
      // "Wait a minute. When I played Inventor's Guild, I should have lost a Science Cube and drawn
      // a
      // card. But it's still your turn."
      // "I'm going to use my Red Spot Observatory action to spend a floater and draw a card. Card
      // me."
      // "Card. Cardi B."
      cardAction2(RedSpotObservatory)
    }
    ellie.turn {
      // "All right, for two, I had to actually get my second Venus tag down. Venus Governator."
      // "Governator."
      // "I gain two money per Earth."
      playProject(VenusGovernor, 2)
    }
    dad.pass()
    joanna.turn {
      // "I'm going to use my Stratopolis action to add two floaters to Jet Stream Microscrappers."
      cardAction1(Stratopolis) { addCardResources(JetStreamMicroscrappers, 2) }
    }
    ellie.turn {
      // "You know, before I forget, I can do this. Power Infrastructure. Spend two monies. Get—no,
      // spend two energies. Get two monies."
      cardAction1(PowerInfrastructure, x = 2)
    }
    joanna.turn {
      // "I'm going to use my Jet Stream Microscrappers action to raise the Venus. Ellie, can you
      // move
      // the Venus? And I get a TR."
      // "Yay! Yay! Life is good."
      cardAction2(JetStreamMicroscrappers)
    }
    ellie.turn {
      // "I spend seven money on Floating Refinery. And I get one, two, three, four, five floaters
      // immediately added."
      // "Damn. And where are you adding them?"
      // "To Floating Refinery. It makes you add them there."
      playProject(FloatingRefinery, 7)
    }
    joanna.turn {
      // "Joanna would have really liked that card because it lets you pull floaters off of any card
      // you want."
      // "Oh, that would have been cool."
      // "Anyway, I'm going to use my Dirigibles action to add a floater to Dirigibles."
      cardAction1(Dirigibles) { addCardResources(Dirigibles) }
    }
    ellie.turn {
      // "You know what? Before I forget, I'm going to heat boop. Boop up to minus 16."
      convertHeat()
    }
    joanna.turn {
      // "I'm going to use my Celestic action to add a cube to—to add a floater to, I guess, to
      // Celestic."
      // "I've never seen so many float boys."
      // "So many what?"
      // "Floater cards in my life. Did you say float boys?"
      // "Yeah, float boys. That's what Ellie calls them."
      cardAction1(Celestic) { addCardResources(Celestic) }
    }
    ellie.turn {
      // "I'm going to add an Aerial Mapper."
      cardAction1(AerialMappers) { addCardResources(AerialMappers) }
    }
    joanna.turn {
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
    ellie.turn {
      // "I am feeling like I did not play this generation optimally."
      // "Nope. Sure didn't."
      // "Floating Refinery. Remove two from Floating Refinery to gain a titanium and two money."
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>") }
    }
    joanna.turn {
      // "Just to have something to do, I'm gonna take my Red Ships action, which doesn't give me
      // anything."
      cardAction1(RedShips)
    }
    ellie.turn {
      // "I'm gonna use Floating Habs. Spend two money, add to Floating Habs."
      // "How do you have so much shit to do? I freaking pass."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
    }
    joanna.pass()
    ellie.turn {
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
    ellie.wgt("VenusStep").expect("0 TerraformRating")
    with(dad) {
      assertProduction(m = 3, s = 1, t = 3, p = 1, e = 5, h = 0)
      assertResources(m = 31, s = 1, t = 3, p = 1, e = 5, h = 8)
      assertCounts(28 to "TerraformRating", 7 to "ProjectCard")
    }
    with(joanna) {
      assertProduction(m = 12, s = 0, t = 1, p = 1, e = 0, h = 1)
      assertResources(m = 49, s = 0, t = 1, p = 4, e = 0, h = 1)
      assertCounts(33 to "TerraformRating", 8 to "ProjectCard")
    }
    with(ellie) {
      assertProduction(m = 8, s = 1, t = 2, p = 0, e = 4, h = 8)
      assertResources(m = 37, s = 2, t = 6, p = 0, e = 4, h = 11)
      assertCounts(29 to "TerraformRating", 7 to "ProjectCard")
    }
    assertSidebar(gen = 7, temp = -14, oxygen = 6, oceans = 6, venus = 22)
  }
}
