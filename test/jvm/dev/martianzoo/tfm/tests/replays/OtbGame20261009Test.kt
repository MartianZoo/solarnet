package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test

// Sources: _local/replays/Game20261009/{transcript-afternoon,transcript-evening,
// transcript-middle,transcript-end,sources}.md.
// Player tasks are explicit. Admin retains normal autoexecution, as in Game20230521Test.
internal class OtbGame20261009Test : FullGameTest() {
  override val config =
      GameConfig(
          """
          VastitasMap
          CorporateEraExpansion, PreludeExpansion, prelude2:cards, VenusNextExpansion
          ColoniesExpansion, promos:cards, fakes:cards
          Callisto, Enceladus, Io, Miranda, Triton
          Diversifier, Engineer, Forester, Legend, Researcher
          Botanist, Investor, Landscaper, Manufacturer, Traveller
          """,
          "Aqua",
          "Pink",
      )

  @Test
  internal fun game20261009() {
    startGame()
    // Each player takes their starting TR before their other setup tasks.
    p1.doTasks("20 TerraformRating")
    p2.doTasks("20 TerraformRating")
    p1.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-5 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-2 ProjectCard<Selecting>",
    )

    // Afternoon 13:44:57–13:45:37: Kevin/Aqua keeps five; Ellie/Pink keeps eight.
    // "Arcadian communities. That gives me 40 money and 10 steel. And then I'm keeping five
    // cards, so I spend 15." "I believe I am Tractor [Teractor], and I'm buying eight cards.
    // I know, crazy." "Oh, damn, money bags."
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$ArcadianCommunities>>",
        "$ArcadianCommunities FROM CorporationCard",
        "40 MC",
        "10 Steel",
        "15 Pay<> FROM MC",
        "5 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$Teractor>>",
        "$Teractor FROM CorporationCard",
        "60 MC",
        "24 Pay<> FROM MC",
        "8 ProjectCard<Hand FROM Selecting>",
    )

    // "I chose my preludes because they rhyme. I'm just kidding."
    // "Boomtown!" "Oh wow." "You need to help me remember that my titanium is only worth two,
    // just like my steel."
    p1.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$BoomTown>>",
        "$BoomTown FROM PreludeCard",
        "PROD[2 Titanium]",
        "CityTile<Vastitas_6_9>",
        "2 Titanium",
    )
    // "Oh, is it double down?" "Double down!" "It's actually pretty funny."
    p1.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
        "$DoubleDown FROM PreludeCard",
        "CopyPrelude<$BoomTown>",
        "PROD[2 Titanium]",
        "CityTile<Vastitas_8_7>",
        "Plant",
        "Steel",
    )
    p1.assertCounts(4 to "PROD[Titanium]", 2 to "ResourceValue<Class<Titanium>>")

    // "That has altered my plans. This is a little wild." "Yeah, that's why I bought a bunch."
    p2.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$AquiferTurbines>>",
        "$AquiferTurbines FROM PreludeCard",
        "OceanTile<Vastitas_5_6>",
        "TerraformRating",
        "2 Plant",
        "PROD[2 Energy]",
        "-3 MC",
    )
    // 13:52:07: revised ocean choice is 6-6; city 4-5; greenery 5-5.
    p2.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$ProjectEden>>",
        "$ProjectEden FROM PreludeCard",
        "OceanTile<Vastitas_6_6>",
        "TerraformRating",
        "2 MC",
        "2 Heat",
        "Plant",
        "CityTile<Vastitas_4_5>",
        "2 MC",
        "GreeneryTile<Vastitas_5_5>",
        "2 MC",
        "2 MC",
        "-4 MC",
        "TemperatureStep",
        "OxygenStep",
        "2 TerraformRating",
        "-3 ProjectCard",
    )
    // The table omitted Project Eden's oxygen and TR. The 13:53:05 photo shows oxygen 0,
    // TR 23; at 22:31:08 they notice the missing oxygen, but never restore this TR.
    p2.exMachina("-OxygenStep, -TerraformRating")
    // board-13-53-05.jpg, after Preludes.
    p2.assertResources(m = 37, s = 0, t = 0, p = 3, e = 0, h = 2)
    p2.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 2, h = 0)
    p2.assertCounts(23 to "TerraformRating", 5 to "ProjectCard")
    assertSidebar(gen = 1, temp = -28, oxygen = 0, oceans = 2, venus = 0)

    with(p1) {
      doTasks("DoRequiredActions", "Community<Vastitas_7_8>")
      doTasks(
          *playCardTasks("$NuclearPower"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
    }

    // "Giant ice asteroid. Crazy play for first turn." "I knew you had something that would get
    // you two temperature, obviously." "Amazing play."
    with(p2) {
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "36 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "OceanTile<Vastitas_6_7>",
          "TerraformRating",
          "4 MC",
          "ProjectCard",
          "OceanTile<Vastitas_5_7>",
          "TerraformRating",
          "4 MC",
          "2 Plant",
          "-Plant<Aqua>",
          "PROD[Heat]",
      )
      // Pink's logs show no heat production and +1 heat (entry 27), apparently confusing
      // the −24°C production bonus with a resource.
      exMachina("PROD[-Heat]")
      doTask("PassBaton") // decline the second action
    }
    // "I just want you to see how sad this is and it may even make you wonder why I'm even doing
    // it." "Immigrant city. Can you believe that three cities already got played before it?"
    with(p1) {
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "6 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Vastitas_7_8>",
          "PROD[MC]",
          "5 MC",
          "Plant",
      )
      doTask("PassBaton") // decline the second action
    }
    // Pink log entry 27 records +1 heat before Pets' payment at entry 28.
    p2.exMachina("Heat")
    // "I think I may as well pets for a sec." "Oh, pets. I can see why you would not have expected
    // me to be playing more cities already."
    with(p2) {
      doTasks(
          *playCardTasks("$Pets"),
          "7 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$ArcadianCommunities"),
          "Community<Vastitas_7_7>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("Pass")
    }
    with(p1) {
      doTasks("Pass")
    }
    p1.doTasks("17 MC", "4 Titanium", "2 Energy")
    p2.doTasks("27 MC")
    p2.doTasks("2 Energy")
    p1.doTasks("VenusStep! BY Admin")

    // board-13-59-21.jpg; app logs: Aqua 25–30, Pink 30–35. Break before Research.
    p1.assertResources(m = 46, s = 0, t = 6, p = 1, e = 2, h = 0)
    p1.assertProduction(m = -3, s = 0, t = 4, p = 0, e = 2, h = 0)
    p2.assertResources(m = 29, s = 0, t = 0, p = 5, e = 2, h = 3)
    p2.assertProduction(m = 0, s = 0, t = 0, p = 0, e = 2, h = 0)
    assertSidebar(gen = 2, temp = -24, oxygen = 0, oceans = 4, venus = 2)

    // Generation 2. Evening 21:55:19–21:55:55; drafting is represented by its kept counts.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // "Just in case you have any ideas, I'm gonna immediately spend plant gain seven."
    // "I think it's time for me to play a city tile, maybe. Yep. Research outpost."
    with(p2) {
      doTasks(
          *playCardTasks("$ElectroCatapult"),
          "17 Pay<> FROM MC",
          "$ElectroCatapult FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks(
          *cardActionTasks("$ElectroCatapult"),
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Vastitas_9_9>",
          "Steel",
          "PROD[MC]",
      )
      p2.doTasks("Animal<$Pets>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$Casinos"),
          "5 Pay<> FROM MC",
          "$Casinos FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    // "It's so weird that I did this Boomtown thing in a game where the only decent colony is
    // Triton." "I shouldn't be laughing."
    // Include the five-MC payment correction acknowledged at 22:00:27–22:00:47 here.
    with(p1) {
      doTasks(
          *playCardTasks("$TradingColony"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$TradingColony FROM ProjectCard",
          "Colony<Triton>",
          "3 Titanium",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("Pass")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$GiantSpaceMirror"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
      // "Then, my Arcadian community, let me put a little cubie bobber right here on 6, 8."
      doTasks(
          *cardActionTasks("$ArcadianCommunities"),
          "Community<Vastitas_6_8>",
      )
      doTasks("Pass")
    }
    // Production 2 -> 3: each player's 2 remaining energy becomes heat.
    p1.doTasks("2 Heat FROM Energy", "18 MC", "4 Titanium", "5 Energy")
    p2.doTasks("2 Heat FROM Energy", "31 MC")
    p2.doTasks("OceanTile<Vastitas_3_3>! BY Admin")
    // board-22-02-46.jpg; Aqua entries 46–51, Pink 49–54.
    p1.assertResources(m = 28, s = 1, t = 4, p = 1, e = 5, h = 2)
    p1.assertProduction(m = -2, s = 0, t = 4, p = 0, e = 5, h = 0)
    p2.assertResources(m = 39, s = 0, t = 0, p = 4, e = 0, h = 5)
    p2.assertProduction(m = 4, s = 0, t = 0, p = 0, e = 0, h = 0)
    p2.assertCounts(2 to "Animal<$Pets>")
    assertSidebar(gen = 3, temp = -24, oxygen = 0, oceans = 5, venus = 2)

    // Generation 3, 22:05:39–22:11:19.
    // "It is now round three. That was a really fast generation."
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    with(p1) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?")
      narrowTask("ColonyProduction<Io>")
      doTasks("8 Heat")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Vastitas_8_8>",
          "Steel",
      )
      doTask("PassBaton") // decline the second action
    }
    // "Oh, I should have taken my turn to put the Arcadian down." "To do the Arcadian thing and
    // just grab more of the spots that you could take away from me."
    // The coordinate is unspoken at 22:07:33; board-22-11-07.jpg shows the cube at 9-8.
    with(p1) {
      doTasks(
          *cardActionTasks("$ArcadianCommunities"),
          "Community<Vastitas_9_8>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$IndustrialCenter"),
          "7 Pay<> FROM MC",
          "PROD[Steel]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$InventionContest"),
          "1 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult"),
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks("Pass")
    }
    with(p2) {
      doTasks("Pass")
    }
    // Production 3 -> 4: Aqua's 2 remaining energy becomes heat.
    p1.doTasks("2 Heat FROM Energy", "18 MC", "4 Titanium", "5 Energy")
    p2.doTasks("31 MC", "Steel")
    p1.doTasks("OxygenStep! BY Admin")
    // board-22-11-07.jpg; Aqua entries 62–67, Pink 68–73.
    p1.assertResources(m = 36, s = 1, t = 8, p = 1, e = 5, h = 12)
    p1.assertProduction(m = -2, s = 0, t = 4, p = 0, e = 5, h = 0)
    p2.assertResources(m = 57, s = 2, t = 0, p = 3, e = 0, h = 5)
    p2.assertProduction(m = 4, s = 1, t = 0, p = 0, e = 0, h = 0)
    assertSidebar(gen = 4, temp = -24, oxygen = 1, oceans = 5, venus = 2)

    // Generation 4, 22:13:17–22:22:15.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-0 ProjectCard<Selecting>", // keep all four research cards
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    with(p2) {
      doTasks(
          *playCardTasks("$LocalShading"),
          "4 Pay<> FROM MC",
          "$LocalShading FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$MarketManipulation"),
          "$MarketManipulation FROM ProjectCard",
      )
      selectTask("ColonyProduction FROM ColonyProduction")
      narrowTask("ColonyProduction<Triton> FROM ColonyProduction<Miranda>")

      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      doTasks("6 Titanium")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult"),
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I will also play a catapult." "Is it earth?" "Earth catapult. Earth catapult. Cost me
    // 22. Back to you."
    with(p1) {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "22 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$LocalShading"),
          "Floater<$LocalShading>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "1 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$ArcadianCommunities"),
          "Community<Vastitas_8_9>",
      )
    }
    // "I'm gonna pay nine. For viral enhancement-ers, which the effect automatically gives me a
    // planta. Now, I have building energy, earth, Venus, science, animal, microbe, and city.
    // That's eight thingy thing eight." "So it is done."
    with(p2) {
      doTasks(
          *playCardTasks("$ViralEnhancers"),
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
      )
      doTask("UseAction<ClaimMilestoneAction, Action1>")
      doTasks("8 Pay<> FROM MC", "Diversifier")
    }
    with(p1) {
      // 22:18:07: the wild tag supplies Conscription's second Earth tag.
      doTasks(
          *playCardTasks("$Conscription"),
          "2 Pay<> FROM MC",
          "$Conscription FROM ProjectCard",
          "Conscription_NextCardEffect",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$IndustrialCenter"),
          "7 Pay<> FROM MC",
          "PROD[Steel]",
      )
      doTask("PassBaton") // decline the second action
    }
    // "Titan floating launch pad for free. That doesn't happen every day, does it? And add two
    // floaters and a Joveen card. I choose to add them to Titan floating launch pad."
    with(p1) {
      doTasks(
          *playCardTasks("$TitanFloatingLaunchPad"),
          "$TitanFloatingLaunchPad FROM ProjectCard",
          "2 Floater<$TitanFloatingLaunchPad>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("Pass")
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad"),
          "Floater<$TitanFloatingLaunchPad>",
      )
      // Pink's phone in board-22-21-43.jpg still displays this pre-production balance.
      p2.assertResources(m = 24, s = 2, t = 0, p = 3, e = 0, h = 5)
      doTasks("Pass")
    }
    // Production 4 -> 5: Aqua's 2 remaining energy becomes heat.
    p1.doTasks("2 Heat FROM Energy", "18 MC", "4 Titanium", "5 Energy")
    p2.doTasks("31 MC", "2 Steel")
    p2.doTasks("OceanTile<Vastitas_4_3>! BY Admin")
    // Aqua entries 77–82; Pink 88–93. Spoken production is at 22:20:10;
    // Pink's app timestamps this at 22:12:12 and its photographed display lags the ledger.
    p1.assertResources(m = 23, s = 1, t = 18, p = 1, e = 5, h = 14)
    p1.assertProduction(m = -2, s = 0, t = 4, p = 0, e = 5, h = 0)
    p2.assertResources(m = 55, s = 4, t = 0, p = 3, e = 0, h = 5)
    p2.assertProduction(m = 4, s = 2, t = 0, p = 0, e = 0, h = 0)
    p1.assertCounts(3 to "Floater<$TitanFloatingLaunchPad>")
    p2.assertCounts(1 to "Diversifier")
    assertSidebar(gen = 5, temp = -24, oxygen = 1, oceans = 6, venus = 2)

    // Generation 5, 22:24:37–22:33:16.
    // "Oh my God, I have eighteen titanium. I can't believe I used energy to trade last time."
    // "I have got to stop laughing at you." "I know."
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // At 22:25:50 both players agree to undo the titanium payment for this trade and
    // use the Launch-Pad instead. Replay the accepted choice at the original trade.
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Callisto>",
      )
      selectTask("ColonyProduction<Callisto>?")
      narrowTask("ColonyProduction<Callisto>")
      doTasks("13 Energy")
      doTask("PassBaton") // decline the second action
    }
    // "Requires six ocean tiles. Thank you, me. Gain two money production, gain three plant
    // production, gain two plants, and viral enhancements gives me a third."
    with(p2) {
      doTasks(
          *playCardTasks("$KelpFarming"),
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
          "Plant",
      )
      doTask("PassBaton") // decline the second action
    }
    // "So I did it both through the app, so it would have given me TR for both of them. Wow, this
    // is the first TR I've ever gotten."
    with(p1) {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      // Aqua never recorded the −20°C production bonus: entries 97 and 106 show
      // 6 heat before production, then 24 from converting 18 energy, with no heat income.
      exMachina("PROD[-Heat]")
    }
    with(p2) {
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Forester",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$ArcadianCommunities"),
          "Community<Vastitas_7_9>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks("Pass")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "Plant",
      )
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "11 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Vastitas_5_4>",
          "2 MC",
          "2 Plant",
          "Animal<$Pets>",
      )
      p1.doTasks("PROD[MC]")
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_4_4>",
          "Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      // "Oh good, there won't be any more six times as many cities as there are greeneries. Now
      // it's only three times as many cities."
      // "Oh gosh! We need to raise this two times." "They both should have raised the oxygen."
      // 22:31:08–22:31:24: both greeneries' oxygen is finally recorded. This greenery's
      // step has just resolved normally; restore the missed Project Eden step without its TR.
      exMachina("OxygenStep")
      // "You haven't even used your electrocatapult yet?" "Who are you?" "I know, I'm crazy."
      doTasks(
          *cardActionTasks("$ElectroCatapult"),
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
      doTasks(
          *cardActionTasks("$LocalShading", "Action2"),
          "-Floater<$LocalShading>",
          "PROD[MC]",
      )
      doTasks(
          *playCardTasks("$TerraformingContract"),
          "5 Pay<> FROM MC",
          "$TerraformingContract FROM ProjectCard",
          "PROD[4 MC]",
      )
      // 22:31:26–22:32:20: Pink says "pay eight", missing Teractor's 3 MC discount.
      exMachina("-3 MC")
      doTasks("Pass")
    }
    // Production 5 -> 6: Aqua's 18 remaining energy becomes heat.
    p1.doTasks("18 Heat FROM Energy", "21 MC", "4 Titanium", "5 Energy")
    p2.doTasks("42 MC", "3 Steel", "3 Plant")
    p1.doTasks("TemperatureStep! BY Admin")

    // board-22-32-56.jpg; Aqua entries 102–107; Pink 126–131. No generation-6 draws yet.
    p1.assertResources(m = 24, s = 1, t = 22, p = 1, e = 5, h = 24)
    p1.assertProduction(m = -1, s = 0, t = 4, p = 0, e = 5, h = 0)
    p2.assertResources(m = 51, s = 3, t = 0, p = 4, e = 0, h = 5)
    p2.assertProduction(m = 14, s = 3, t = 0, p = 3, e = 0, h = 0)
    p1.assertCounts(
        22 to "TerraformRating",
        2 to "Floater<$TitanFloatingLaunchPad>",
        4 to "CityTile",
        1 to "Colony<Triton>",
        0 to "Milestone",
    )
    p2.assertCounts(
        28 to "TerraformRating",
        3 to "Animal<$Pets>",
        0 to "Floater<$LocalShading>",
        2 to "CityTile",
        2 to "GreeneryTile",
        1 to "Diversifier",
        1 to "Forester",
    )
    assertSidebar(gen = 6, temp = -18, oxygen = 3, oceans = 6, venus = 2)
    // Counts from the recorded purchases, draws, discards, and plays; identities are incomplete.
    p1.assertCounts(3 to "ProjectCard<Hand>", 0 to "ProjectCard<Selecting>")
    p2.assertCounts(5 to "ProjectCard<Hand>", 0 to "ProjectCard<Selecting>")
    admin.assertCounts(1 to "ResearchPhase", 0 to "End")

    // October 10, before generation 6 Research: the players apply the agreed corrections.
    // Aqua's -20 C bonus was missed at generation 5 production.
    p1.exMachina("PROD[Heat], Heat")
    // Project Eden's generation-1 TR missed five productions; Pink's -24 C bonus was
    // entered as one heat resource rather than production, missing four further heat.
    p2.exMachina("TerraformRating, 5 MC, PROD[Heat], 4 Heat")
    p1.assertResources(m = 24, s = 1, t = 22, p = 1, e = 5, h = 25)
    p2.assertResources(m = 56, s = 3, t = 0, p = 4, e = 0, h = 9)

    // "I pay 11 for research. I get two cards."
    // "I believe I have 1, 2, 3, 4 science tags. Yep. So I'd also spend eight to claim Risocha."
    // "How do you feel about that?" "I guess I won't do those things."
    // Generation 6 Research: Aqua buys two cards; Pink buys four.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-0 ProjectCard<Selecting>",
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    ) // keep all four

    with(p2) {
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Researcher")
    }

    // "Since I have so much titanium, I took spaceport colony and it cost me 24 and I can do that
    // as 12 titanium." "Oh, are you just not getting a lot of space cards?"
    // "I still have 10 left." Space Port Colony costs twelve titanium after discounts.
    with(p1) {
      doTasks(
          *playCardTasks("$SpacePortColony"),
          "12 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$SpacePortColony FROM ProjectCard",
          "Colony<Io>",
          "PROD[Heat]",
          "TradeFleet",
      )
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?")
      narrowTask("ColonyProduction<Io>")
      doTasks("2 Heat", "8 Heat")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTask("PassBaton") // decline the second action
    }

    with(p1) {
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_5_9>")
      // "And I'm very sorry for this, but it just looks too good for you."
      // "Goodbye animals from Miranda, they all go away." "Well played."
      // "I have to push this colony advantage, that's kind of all I got. Your milestone advantage."
      // Miranda's trade empties its track; Aqua has no animal card to receive animals.
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Miranda>",
      )
      selectTask("ColonyProduction<Miranda>?")
      narrowTask("ColonyProduction<Miranda>")
      // The printed three-animal trade reward had no legal holder and yielded nothing.
      agents[p1.actor].dropTask(game.tasks.extract { it }.single { !it.deferred }.id)
    }
    with(p2) {
      doTasks(
          *playCardTasks("$SixteenPsyche"),
          "31 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "PROD[2 Titanium]",
          "3 Titanium",
      )
      doTask("PassBaton") // decline the second action
    }

    with(p1) {
      doTasks(*playCardTasks("$EnergyMarket"), "$EnergyMarket FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$CommunityServices"),
          "10 Pay<> FROM MC",
          "$CommunityServices FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("Pass")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$LunarBeam"),
          "10 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
      repeat(4) {
        p1.doTasks(
            "UseAction<ConvertHeatAction, Action1>",
            "8 Pay<Class<Heat>> FROM Heat",
            "TemperatureStep",
            "TerraformRating",
        )
      }
      doTasks("Pass")
    }
    // Production 6 -> 7: Aqua's 5 remaining energy becomes heat.
    p1.doTasks("5 Heat FROM Energy", "27 MC", "4 Titanium", "6 Energy", "4 Heat")
    p2.doTasks("44 MC", "3 Steel", "2 Titanium", "3 Plant", "Heat")
    p2.doTasks("VenusStep! BY Admin")
    // board-11-23-10.jpg; phone logs at the generation 7 checkpoint.
    p1.assertResources(m = 33, s = 1, t = 11, p = 1, e = 6, h = 12)
    p1.assertProduction(m = 1, s = 0, t = 4, p = 0, e = 6, h = 4)
    p2.assertResources(m = 45, s = 5, t = 5, p = 7, e = 0, h = 2)
    p2.assertProduction(m = 14, s = 3, t = 2, p = 3, e = 0, h = 1)
    p1.assertCounts(26 to "TerraformRating")
    p2.assertCounts(30 to "TerraformRating")
    assertSidebar(gen = 7, temp = -8, oxygen = 3, oceans = 6, venus = 4)

    // "They're all reasonable." "How terrible. I think I'll buy one." "I'm buying them all."
    // Generation 7 Research. Aqua keeps four cards; Pink keeps one.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-0 ProjectCard<Selecting>",
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    ) // keep all four
    p2.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    // The transcript's "Rune Chatter in Tax" is Rotator Impacts: its three-M€
    // discounted price and the asteroid action used later identify it.
    with(p1) {
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    // "Oh, I hate that card when I don't have it."
    with(p2) {
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "Ok", // no titanium paid
          "2 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard", // Solar Logistics sees the space event
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$Dirigibles"),
          "8 Pay<> FROM MC",
          "$Dirigibles FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "7 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$GiantIceAsteroid>>",
          "ProjectCard FROM PlayedEvent<Class<$ImportedGhg>>",
      )
    }
    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }

    // "There goes my plan." "The final ocean already?" "Oh my god."
    // "Yeah, this game is wild." "If by wild you mean I'm losing badly, yeah."
    // Giant Ice Asteroid's two oceans are 3-2 and 6-5. The transcript and app
    // record their bonuses separately, although the app combines nearby money changes.
    with(p2) {
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "24 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "OceanTile<Vastitas_3_2>",
          "TerraformRating",
          "4 MC",
          "2 Plant",
          "OceanTile<Vastitas_6_5>",
          "TerraformRating",
          "2 MC",
          "2 Heat",
          "-Plant<Aqua>",
          "ProjectCard", // Solar Logistics sees the space event
      )
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "2 TemperatureStep",
          "2 TerraformRating",
          "LavaFlows_SpecialTile<Vastitas_2_6>",
          "ProjectCard",
          "OceanTile<Vastitas_5_3>", // the 0 C threshold grants the final ocean
          "TerraformRating",
          "Plant",
          "2 MC",
      )
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "Asteroid<$RotatorImpacts>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_4_6>",
          "ProjectCard",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_9_7>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$FloatingRefinery"),
          "7 Pay<> FROM MC",
          "$FloatingRefinery FROM ProjectCard",
          "2 Floater<$FloatingRefinery>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$Dirigibles>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I've come to my big play, which is so silly. But I have all this titanium."
    // "Which as you can see I just spent the last several turns making sure I would be able to
    // do that."
    // The L1 gift is placed on the three cards Aqua prepared in the preceding turns.
    with(p1) {
      doTasks(
          *playCardTasks("$L1TradeTerminal"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$L1TradeTerminal FROM ProjectCard",
      )
      selectTask(
          game.tasks.extract { it }.single { !it.deferred }.id
      ) // L1's three distinct holders
      doTasks(
          "Asteroid<$RotatorImpacts>",
          "Floater<$Dirigibles>",
          "Floater<$TitanFloatingLaunchPad>",
      )
      // Choose L1's two track steps and decline Trading Colony's optional third step.
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTask("Ok") // decline Trading Colony's one step
      doTasks("Titanium", "5 Titanium")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "Ok", // no titanium paid
          "2 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard", // Solar Logistics
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }

    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$SolarProbe"),
          "Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$SolarProbe FROM ProjectCard",
          "2 ProjectCard", // six science tags
          "ProjectCard", // Solar Logistics
      )
    }
    with(p1) {
      doTasks(
          *playCardTasks("$InventorsGuild"),
          "6 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton") // decline the second action
    }

    with(p1) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?") // Trading Colony
      narrowTask("ColonyProduction<Io>")
      selectTask("2 ColonyProduction<Io> OR Ok") // L1 Trade Terminal
      narrowTask("2 ColonyProduction<Io>")
      doTasks("2 Heat", "10 Heat")
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(p2) {
      doTasks("Pass")
    }
    with(p1) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "Check out this badass card. It's potatoes. I can't build it because it needs plants."
      // Inventors' Guild reveals Potatoes; Aqua declines the purchase.
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      doTasks("Pass")
    }
    // Production 7 -> 8: Aqua's 3 remaining energy becomes heat.
    p1.doTasks("3 Heat FROM Energy", "30 MC", "4 Titanium", "5 Energy", "4 Heat")
    p2.doTasks("53 MC", "3 Steel", "2 Titanium", "3 Plant", "3 Heat")
    p1.doTasks("OxygenStep! BY Admin")
    // board-11-44-28.jpg; the logs show these balances before generation 8 Research.
    p1.assertResources(m = 36, s = 1, t = 10, p = 0, e = 5, h = 7)
    p1.assertProduction(m = 1, s = 0, t = 4, p = 0, e = 5, h = 4)
    p2.assertResources(m = 53, s = 7, t = 2, p = 5, e = 0, h = 5)
    p2.assertProduction(m = 14, s = 3, t = 2, p = 3, e = 0, h = 3)
    p1.assertCounts(29 to "TerraformRating")
    p2.assertCounts(39 to "TerraformRating")
    assertSidebar(gen = 8, temp = 8, oxygen = 5, oceans = 9, venus = 4)

    // "Show me greenery tiles." "Show me the greenery."
    // Generation 8 Research. Aqua buys four cards; Pink buys three.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-0 ProjectCard<Selecting>",
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    with(p2) {
      doTasks(
          *playCardTasks("$RedSpotObservatory"),
          "17 Pay<> FROM MC",
          "$RedSpotObservatory FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }

    with(p1) {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard", // Venus 8% bonus
      )
    }
    with(p2) {
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "6 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
          "Microbe<$SulphurEatingBacteria>", // Viral Enhancers on the microbe tag
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "$HiredRaiders FROM ProjectCard",
          "3 MC FROM MC<Pink>",
      )
      doTasks(
          *playCardTasks("$Decomposers"),
          "2 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "Microbe<$Decomposers>",
      )
    }
    with(p2) {
      doTasks(
          *playCardTasks("$MartianLumberCorp"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$MartianLumberCorp FROM ProjectCard",
          "PROD[Plant]",
          "Plant", // Viral Enhancers on the plant tag
      )
      doTask("PassBaton") // decline the second action
    }

    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
      )
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$VenusShuttles"),
          "9 Pay<> FROM MC",
          "$VenusShuttles FROM ProjectCard",
          "2 Floater<$FloatingRefinery>",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I don't love how this game is going. Okay. I'm gonna use Inventor's Guild to look at the
    // best card in the game." "And it requires two Jovian."
    // Inventors' Guild reveals an unaffordable card; Aqua lets it go.
    with(p1) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$CoronaExtractor"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$CoronaExtractor FROM ProjectCard",
          "PROD[4 Energy]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$VenusShuttles"),
          "8 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }

    // The agreed Dirigibles discount replaces Aqua's initial four-M€ payment.
    with(p1) {
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$OrbitalReflectors"),
          "PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "10 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$OrbitalReflectors FROM ProjectCard",
          "2 VenusStep",
          "3 TerraformRating", // includes the Venus 16% threshold
          "PROD[2 Heat]",
      )
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    // This floater was placed later than Aqua first recalled in the conversation.
    with(p1) {
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$Dirigibles>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$LocalShading", "Action2"),
          "-Floater<$LocalShading>",
          "PROD[MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$UrbanDecomposers"),
          "3 Pay<> FROM MC",
          "$UrbanDecomposers FROM ProjectCard",
          "PROD[Plant]",
          "2 Microbe<$Decomposers>",
          "Microbe<$Decomposers>", // Decomposers reacts to the microbe tag
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$RedSpotObservatory"), "Floater<$RedSpotObservatory>")
      doTask("PassBaton") // decline the second action
    }
    // Viking 2's delegate space is visible in the final photograph; "5-9" was a slip.
    with(p1) {
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_4_8>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria"),
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton") // decline the second action
    }
    // "How are we ever going to get this oxygen up? It's going to be stuck like this forever."
    with(p1) {
      doTasks("Pass")
    }
    // "So you have 23 mega credit production and I have one."
    // "I get 36. Hey, they're the same with the digits reversed." "You're the inverse."
    with(p2) {
      doTasks(
          *playCardTasks("$Sponsors"),
          "Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTasks(
          *playCardTasks("$Cartel"),
          "3 Pay<> FROM MC",
          "$Cartel FROM ProjectCard",
          "PROD[6 MC]",
      )
      doTasks("Pass")
    }

    // Production 8 -> 9: Aqua's 2 remaining energy becomes heat.
    p1.doTasks("2 Heat FROM Energy", "36 MC", "4 Titanium", "Plant", "4 Energy", "6 Heat")
    p2.doTasks("63 MC", "3 Steel", "2 Titanium", "4 Plant", "4 Energy", "3 Heat")
    p2.doTasks("OxygenStep! BY Admin")
    // board-12-00-57.jpg; before generation 9 Research.
    p1.assertResources(m = 50, s = 1, t = 9, p = 1, e = 4, h = 15)
    p1.assertProduction(m = 1, s = 0, t = 4, p = 1, e = 4, h = 6)
    p2.assertResources(m = 67, s = 7, t = 3, p = 10, e = 4, h = 8)
    p2.assertProduction(m = 23, s = 3, t = 2, p = 4, e = 4, h = 3)
    p1.assertCounts(35 to "TerraformRating")
    p2.assertCounts(40 to "TerraformRating")
    assertSidebar(gen = 9, temp = 8, oxygen = 6, oceans = 9, venus = 16)

    // Generation 9. Both players buy three of the drafted cards.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    // "I'll go to Triton, bump it up three times, which gives me five, gives me four plus one,
    // and then might as well spend three titanium too, to then go to enchiladas, bump it up three
    // times, and get four. Microbials." "Pretty crazy."
    with(p1) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Enceladus>",
      )
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    // "I think I will fund the investor and the traveler, which will be a total of 22."
    // "You're winning in all five awards, so except the one we're tied."
    with(p2) {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Investor")
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Traveller")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$HeavyTaxation"),
          "$HeavyTaxation FROM ProjectCard",
          "PROD[2 MC]",
          "4 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$RedSpotObservatory", "Action2"),
          "-Floater<$RedSpotObservatory>",
          "ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I'm gonna spend the twenty three to plant a forest, which raised the oxygen to seven."
    // "Seven seven?" "Which gives me seven money and two plants."
    with(p1) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_7_7>",
          "7 MC",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "2 Animal<$Pets>",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I'm going to use my inventor's guild to look at a shitty card. It is a shitty card."
    // "But it's a shitty time."
    with(p1) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_6_4>",
          "4 MC",
          "Plant",
          "OxygenStep",
          "TerraformRating",
          "0 TemperatureStep", // decline TemperatureStep. at the oxygen threshold
      )
      // "You got the non-existent bonus." The table does not take the 8% temperature bonus.
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton") // decline the second action
    }
    // Aqua first says Titan Floating Launch-Pad, then corrects the floater to Dirigibles.
    with(p1) {
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$Dirigibles>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad"),
          "Floater<$TitanFloatingLaunchPad>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$JovianLanterns"),
          "20 Pay<> FROM MC",
          "$JovianLanterns FROM ProjectCard",
          "TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$IoSulphurResearch"),
          "14 Pay<> FROM MC",
          "$IoSulphurResearch FROM ProjectCard",
          "3 ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$ImmigrationShuttles"),
          "11 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$ImmigrationShuttles FROM ProjectCard",
          "PROD[5 MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$VenusShuttles"),
          "8 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_4_7>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "6 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
          "Microbe<$Decomposers>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks("Pass")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$MedicalLab"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // Keep the plants.
          "Pay<> FROM MC",
          "$MedicalLab FROM ProjectCard",
          "PROD[3 MC]",
      )
      doTasks("Pass")
    }

    // Production 9 -> 10; Aqua chooses oxygen in the world-government phase.
    p1.doTasks("Heat FROM Energy", "45 MC", "4 Titanium", "2 Plant", "3 Energy", "6 Heat")
    p2.doTasks(
        "Heat FROM Energy",
        "69 MC",
        "3 Steel",
        "2 Titanium",
        "6 Plant",
        "4 Energy",
        "5 Heat",
    )
    p1.doTasks("OxygenStep! BY Admin")
    // board-16-04-56.jpg, before generation 10 Research.
    p1.assertResources(m = 56, s = 1, t = 4, p = 5, e = 3, h = 22)
    p2.assertResources(m = 73, s = 5, t = 2, p = 9, e = 4, h = 14)
    assertSidebar(gen = 10, temp = 8, oxygen = 9, oceans = 9, venus = 20)

    // Generation 10. Aqua says "I'm buying one card"; Pink's phone log records two purchases.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // "Oh my god." "You're placing so many late colonies."
    with(p2) {
      doTasks(
          *standardProjectTasks("BuildColonyProject"),
          "17 Pay<> FROM MC",
          "Colony<Miranda>",
          "Animal<$Pets>",
      )
      doTasks(
          *playCardTasks("$ResearchColony"),
          "Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "$ResearchColony FROM ProjectCard",
          "Colony<Miranda>",
          "Animal<$Pets>",
          "2 ProjectCard",
      )
    }
    // "I have considered flying my boat to Miranda, but not that mean."
    // "Why is that so hard to say? And then my boat will go, let's do the same thing as last
    // time. It goes to Triton and it gets five titanium."
    // "I'm not gonna win the game like this, but I'm not gonna win the game anyway."
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Enceladus>",
      )
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$MediaGroup"),
          "Pay<> FROM MC",
          "$MediaGroup FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$InventorsGuild"), "ProjectCard<Selecting>")
      doTasks(
          "-0 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$VenusianInsects"),
          "5 Pay<> FROM MC",
          "$VenusianInsects FROM ProjectCard",
          "Microbe<$VenusianInsects>", // Viral Enhancers
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$SubZeroSaltFish"),
          "2 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "PROD[-Plant<Pink>]",
          "Microbe<$Decomposers>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_6_8>",
          "7 MC",
          "Plant",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_3_4>",
          "2 MC",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_5_9>",
          "3 MC",
          "2 ProjectCard",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I am doubting you'll have two more greeneries up your sleeve."
    // "With 12 money that would be a little difficult."
    with(p2) {
      doTasks(
          *playCardTasks("$HydrogenToVenus"),
          "11 Pay<> FROM MC",
          "$HydrogenToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$LocalShading>",
          "3 MC", // Media Group
          "ProjectCard", // Solar Logistics
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "2 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_3_6>")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "Animal<$Pets>",
          "2 ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "Asteroid<$RotatorImpacts>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$TitanFloatingLaunchPad>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$Atmoscoop"),
          "Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I pass. Wish I hadn't passed now."
    with(p1) {
      doTasks("Pass")
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$ElectroCatapult", "Action2"),
          "Pay<Class<Steel>> FROM Steel",
          "7 MC",
      )
      doTasks(
          *cardActionTasks("$VenusShuttles"),
          "7 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RedSpotObservatory"), "Floater<$RedSpotObservatory>")
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      doTasks("Pass")
    }

    // Production 10 -> 11. The phone logs combine energy conversion and new production.
    p1.doTasks("3 Heat FROM Energy", "47 MC", "4 Titanium", "2 Plant", "2 Energy", "6 Heat")
    p2.doTasks(
        "Heat FROM Energy",
        "74 MC",
        "3 Steel",
        "2 Titanium",
        "5 Plant",
        "4 Energy",
        "5 Heat",
    )
    p2.doTasks("VenusStep! BY Admin")
    // board-16-22-18.jpg, before generation 11 Research.
    p1.assertResources(m = 65, s = 6, t = 7, p = 8, e = 2, h = 31)
    p2.assertResources(m = 80, s = 7, t = 2, p = 8, e = 4, h = 20)
    assertSidebar(gen = 11, temp = 8, oxygen = 12, oceans = 9, venus = 30)

    // "This will surely be the last generation." "These are all so bad."
    // "I think I'm buying none of them." "I think I'm buying three of them."
    // Generation 11. Pink buys nothing; Aqua keeps three cards.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks(
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks("-4 ProjectCard<Selecting>")
    // "Well, I think I'm going to plant a forest. She takes oxygen to 13."
    // "Then I'd better hurry up and pay 23 as well. That maxes out the oxygen."
    with(p1) {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_9_8>",
          "Titanium",
          "3 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_8_9>",
          "Plant",
          "3 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    // "I'm a landscaper, so you had to do it."
    // "Well you swept every award and every milestone. Amazing."
    with(p2) {
      doTasks(
          *cardActionTasks("$RedSpotObservatory", "Action2"),
          "-Floater<$RedSpotObservatory>",
          "ProjectCard",
      )
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Landscaper")
    }
    with(p1) {
      doTasks(
          *playCardTasks("$LunaGovernor"),
          "Pay<> FROM MC",
          "$LunaGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    // "This time I get two animals, so I get gold pets and two cards. Cool."
    with(p2) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "2 Animal<$Pets>",
          "2 ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
      doTask("PassBaton") // decline the second action
    }
    // "Flip a greenery into a city."
    // "Oh, I should have used my thing to block you. Damn it. I gotta think more clearly."
    with(p2) {
      doTasks(
          *playCardTasks("$KaguyaTech"),
          "10 Pay<> FROM MC",
          "$KaguyaTech FROM ProjectCard",
          "PROD[2 MC]",
          "ProjectCard",
          "CityTile<Vastitas_3_4> FROM GreeneryTile<Vastitas_3_4>",
          "2 Plant",
          "2 MC",
          "Animal<$Pets>",
      )
      // Pink records only the two placement plants, missing Viral Enhancers' optional plant.
      doTask("Ok") // decline Viral Enhancers' plant
      p1.doTasks("PROD[MC]")
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks("GreeneryTile<Vastitas_3_5>", "Plant")
      selectTask("OxygenStep") // oxygen is already maxed
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$WarpDrive"),
          "14 Pay<> FROM MC",
          "$WarpDrive FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Enceladus>",
      )
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "4 Microbe<$VenusianInsects>",
          "3 MC", // Media Group
          "ProjectCard", // Solar Logistics
      )
      doTask("PassBaton") // decline the second action
    }
    // "Whoo, this is what I'm reduced to." "You are reduced to produce."
    with(p1) {
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "PROD[MC]",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$Advertising"),
          "Pay<> FROM MC",
          "$Advertising FROM ProjectCard",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_7_9>",
          "3 MC",
          "0 OxygenStep", // decline OxygenStep.; oxygen is already maxed
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "10 Microbe<$VenusianInsects>",
          "Plant", // Viral Enhancers
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *playCardTasks("$TerraformingGanymede"),
          "Pay<Class<Titanium>> FROM Titanium",
          "25 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "5 TerraformRating",
      )
      // 16:34:22: Pink pays one titanium and 29 MC, missing Warp Drive's 4 MC discount.
      exMachina("-4 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$MolecularPrinting"),
          "8 Pay<> FROM MC",
          "$MolecularPrinting FROM ProjectCard",
          "11 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      doTask("PassBaton") // decline the second action
    }
    // "This card is going to be the thing that saves everything. Natural preserve. That did not
    // save everything." "I don't buy it."
    with(p1) {
      doTasks(
          *cardActionTasks("$InventorsGuild"),
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-4 Microbe<$SulphurEatingBacteria>",
          "12 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$Dirigibles"), "Floater<$Dirigibles>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTasks(
          *playCardTasks("$MagneticFieldGeneratorsPromo"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Ok", // Keep the plants.
          "Pay<> FROM MC",
          "$MagneticFieldGeneratorsPromo FROM ProjectCard",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "3 TerraformRating",
          "MagneticFieldGeneratorsPromo_SpecialTile<Vastitas_2_2>",
          "4 MC",
          "2 Plant",
      )
    }
    with(p1) {
      doTasks(*cardActionTasks("$ArcadianCommunities"), "Community<Vastitas_8_6>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*cardActionTasks("$ElectroCatapult"), "Pay<Class<Plant>> FROM Plant", "7 MC")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *playCardTasks("$MediaArchives"),
          "5 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "11 MC",
      )
      doTask("PassBaton") // decline the second action
    }
    // "I'm kind of surprised I'm even able to do this." "I pitched two cards."
    // "Oh my. You spent all your money exactly." "I should have waited."
    // Pink sells two cards to pay for Large Convoy.
    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$LargeConvoy"),
          "27 Pay<> FROM MC",
          "$LargeConvoy FROM ProjectCard",
      )
      doTasks(
          "Ok", // all oceans are already placed
          "2 ProjectCard",
          "4 Animal<$Pets>",
          "3 MC", // Media Group
          "ProjectCard", // Solar Logistics
      )
    }
    // "Probably not actually better but I'm just gonna do it anyway to be different."
    // "I think if not for your pet, it would have been a total wash which way I went."
    // "I don't get more animals. I just like that my four dinky little bronzes are upgraded to a
    // silver."
    with(p1) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Vastitas_3_6>",
          "3 MC", // Arcadian community
          "PROD[MC]", // Immigrant City
      )
      p2.doTasks("Animal<$Pets>")
      doTasks("PROD[MC]") // the city's own production, in addition to Immigrant City
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "0 VenusStep", // Venus is already maxed
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    // "It's all about the tiebreaker, which I'm definitely going to need in this game for sure."
    with(p1) {
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "Ok", // Nothing remains to pay in MC.
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "Floater<$Dirigibles>",
      )
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    with(p1) {
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    // "I pitch a card." "So looks like you're pretty much out."
    // "Yeah, I think there's nothing else I can do. Now what are you gonna do?"
    // Aqua's final sale accounts for the recorded extra M€.
    with(p1) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTask("PassBaton") // decline the second action
    }
    with(p2) {
      doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_2_3>",
          "2 MC",
          "0 OxygenStep",
      ) // decline OxygenStep.; oxygen is maxed
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
    }
    with(p1) {
      doTasks("Pass")
    }
    with(p2) {
      doTasks(
          *playCardTasks("$BreathingFilters"),
          "11 Pay<> FROM MC",
          "$BreathingFilters FROM ProjectCard",
      )
      doTasks("Pass")
    }

    // The last production is recorded before Pink's final greenery.
    p1.doTasks("2 Heat FROM Energy", "57 MC", "4 Titanium", "2 Plant", "Energy", "6 Heat")
    p2.doTasks("Heat FROM Energy", "84 MC", "3 Steel", "2 Titanium", "7 Plant", "5 Heat")
    p1.assertResources(m = 69, s = 6, t = 11, p = 3, e = 1, h = 39)
    p2.assertResources(m = 87, s = 3, t = 2, p = 8, e = 0, h = 26)
    // The phone app advances its display to generation 12; Solarnet keeps the final played
    // generation.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)
    p1.selectTask("PassBaton") // only three plants; no final greenery
    // "Do you replace, you don't." "I do actually." "You do." "Probably on two four."
    // The final photo shows Pink's last greenery here.
    p2.doTasks("UseAction<ConvertPlantsAction, Action1>", "8 Pay<Class<Plant>> FROM Plant")
    p2.doTasks("GreeneryTile<Vastitas_2_4>")
    p2.selectTask("PassBaton") // no more plants to convert
    // board-16-47-03.jpg, after final greenery.
    p2.assertCounts(7 to "GreeneryTile", 3 to "CityTile", 56 to "TerraformRating")
    // "Almost completely shut me out, but not quite."
    // "Wow, this is a really meager pile of cards I've got." "It was a fun game." "Always is."
    // "138." "Well, if you take off the one and you reverse the other two digits, you have my
    // score. Congratulations, son." "Good game."
    p1.assertCounts(83 to "VictoryPoint", 0 to "Victory")
    // Engine categories total 139: 56 TR, 15 milestones, 10 awards, 7 greeneries,
    // 11 cities, and 40 card points. The spoken 138 has no supplied category breakdown.
    p2.assertCounts(139 to "VictoryPoint", 1 to "Victory")
  }
}
