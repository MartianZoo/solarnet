package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

// Sources: _local/replays/Game20261009/{transcript-afternoon,transcript-evening,
// transcript-middle,transcript-end,sources}.md.
// Player tasks are explicit. Admin retains normal autoexecution, as in Game20230521Test.
internal class OtbGame20261009Test : AbstractFullGameTest() {
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
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    admin.startGame()
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

    p1.turn {
      doTasks("DoRequiredActions", "Community<Vastitas_7_8>")
      doTasks(
          "PlayProject<Class<$NuclearPower>>",
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
    }

    // "Giant ice asteroid. Crazy play for first turn." "I knew you had something that would get
    // you two temperature, obviously." "Amazing play."
    p2.turn {
      doTasks(
          "PlayProject<Class<$GiantIceAsteroid>>",
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
    }
    // "I just want you to see how sad this is and it may even make you wonder why I'm even doing
    // it." "Immigrant city. Can you believe that three cities already got played before it?"
    p1.turn {
      doTasks(
          "PlayProject<Class<$ImmigrantCity>>",
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
    }
    // Pink log entry 27 records +1 heat before Pets' payment at entry 28.
    p2.exMachina("Heat")
    // "I think I may as well pets for a sec." "Oh, pets. I can see why you would not have expected
    // me to be playing more cities already."
    p2.turn {
      doTasks(
          "PlayProject<Class<$Pets>>",
          "7 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
    }
    p1.turn {
      doTasks(
          "UseCardAction<$ArcadianCommunities, Action1>",
          "Community<Vastitas_7_7>",
      )
    }
    p2.turn {
      doTasks("Pass")
    }
    p1.turn {
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
    p2.turn {
      doTasks(
          "PlayProject<Class<$ElectroCatapult>>",
          "17 Pay<> FROM MC",
          "$ElectroCatapult FROM ProjectCard",
          "PROD[-Energy]",
      )
      doTasks(
          "UseCardAction<$ElectroCatapult, Action1>",
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$ResearchOutpost>>",
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Vastitas_9_9>",
          "Steel",
          "PROD[MC]",
      )
      p2.doTasks("Animal<$Pets>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$Casinos>>",
          "5 Pay<> FROM MC",
          "$Casinos FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
      )
    }
    // "It's so weird that I did this Boomtown thing in a game where the only decent colony is
    // Triton." "I shouldn't be laughing."
    // Include the five-MC payment correction acknowledged at 22:00:27–22:00:47 here.
    p1.turn {
      doTasks(
          "PlayProject<Class<$TradingColony>>",
          "5 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$TradingColony FROM ProjectCard",
          "Colony<Triton>",
          "3 Titanium",
      )
    }
    p2.turn {
      doTasks("Pass")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$GiantSpaceMirror>>",
          "4 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
      // "Then, my Arcadian community, let me put a little cubie bobber right here on 6, 8."
      doTasks(
          "UseCardAction<$ArcadianCommunities, Action1>",
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
    p1.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?")
      narrowTask("ColonyProduction<Io>")
      doTasks("8 Heat")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$IndustrialCenter>>",
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Vastitas_8_8>",
          "Steel",
      )
    }
    // "Oh, I should have taken my turn to put the Arcadian down." "To do the Arcadian thing and
    // just grab more of the spots that you could take away from me."
    // The coordinate is unspoken at 22:07:33; board-22-11-07.jpg shows the cube at 9-8.
    p1.turn {
      doTasks(
          "UseCardAction<$ArcadianCommunities, Action1>",
          "Community<Vastitas_9_8>",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$IndustrialCenter, Action1>",
          "7 Pay<> FROM MC",
          "PROD[Steel]",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$InventionContest>>",
          "1 Pay<> FROM MC",
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$ElectroCatapult, Action1>",
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
    }
    p1.turn {
      doTasks("Pass")
    }
    p2.turn {
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
    p2.turn {
      doTasks(
          "PlayProject<Class<$LocalShading>>",
          "4 Pay<> FROM MC",
          "$LocalShading FROM ProjectCard",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$MarketManipulation>>",
          "$MarketManipulation FROM ProjectCard",
      )
      selectTask("ColonyProduction FROM ColonyProduction")
      narrowTask("ColonyProduction<Triton> FROM ColonyProduction<Miranda>")

      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      doTasks("6 Titanium")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$ElectroCatapult, Action1>",
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
    }
    // "I will also play a catapult." "Is it earth?" "Earth catapult. Earth catapult. Cost me
    // 22. Back to you."
    p1.turn {
      doTasks(
          "PlayProject<Class<$EarthCatapult>>",
          "22 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$LocalShading, Action1>",
          "Floater<$LocalShading>",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$ResearchCoordination>>",
          "1 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTasks(
          "UseCardAction<$ArcadianCommunities, Action1>",
          "Community<Vastitas_8_9>",
      )
    }
    // "I'm gonna pay nine. For viral enhancement-ers, which the effect automatically gives me a
    // planta. Now, I have building energy, earth, Venus, science, animal, microbe, and city.
    // That's eight thingy thing eight." "So it is done."
    p2.turn {
      doTasks(
          "PlayProject<Class<$ViralEnhancers>>",
          "9 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
      )
      doTask("ClaimMilestone<Class<Diversifier>>")
      // P2 has no wilds: finish this claim's empty allocation prompt.
      selectTask("EACH @WildTagElection { Tag<@WildTagElection>? }")
      doTasks("8 Pay<> FROM MC", "Diversifier")
    }
    p1.turn {
      // 22:18:07: the wild tag supplies Conscription's second Earth tag.
      doTasks(
          "PlayProject<Class<$Conscription>>",
          "2 Pay<> FROM MC",
          "$Conscription FROM ProjectCard",
          "Conscription_NextCardEffect",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$IndustrialCenter, Action1>",
          "7 Pay<> FROM MC",
          "PROD[Steel]",
      )
    }
    // "Titan floating launch pad for free. That doesn't happen every day, does it? And add two
    // floaters and a Joveen card. I choose to add them to Titan floating launch pad."
    p1.turn {
      doTasks(
          "PlayProject<Class<$TitanFloatingLaunchPad>>",
          "$TitanFloatingLaunchPad FROM ProjectCard",
          "2 Floater<$TitanFloatingLaunchPad>",
      )
    }
    p2.turn {
      doTasks("Pass")
    }
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action1>",
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
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Callisto>",
      )
      selectTask("ColonyProduction<Callisto>?")
      narrowTask("ColonyProduction<Callisto>")
      doTasks("13 Energy")
    }
    // "Requires six ocean tiles. Thank you, me. Gain two money production, gain three plant
    // production, gain two plants, and viral enhancements gives me a third."
    p2.turn {
      doTasks(
          "PlayProject<Class<$KelpFarming>>",
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
          "Plant",
      )
    }
    // "So I did it both through the app, so it would have given me TR for both of them. Wow, this
    // is the first TR I've ever gotten."
    p1.turn {
      doTasks(
          "UseStandardProject<AsteroidProject>",
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          "ConvertHeat",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
      // Aqua never recorded the −20°C production bonus: entries 97 and 106 show
      // 6 heat before production, then 24 from converting 18 energy, with no heat income.
      exMachina("PROD[-Heat]")
    }
    p2.turn {
      doTasks(
          "ClaimMilestone<Class<Forester>>",
          "8 Pay<> FROM MC",
          "Forester",
      )
    }
    p1.turn {
      doTasks(
          "UseCardAction<$ArcadianCommunities, Action1>",
          "Community<Vastitas_7_9>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$AdvancedAlloys>>",
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
    }
    p1.turn {
      doTasks("Pass")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$IndustrialMicrobes>>",
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "Plant",
      )
      doTasks(
          "PlayProject<Class<$CorporateStronghold>>",
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
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
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
          "UseCardAction<$ElectroCatapult, Action1>",
          "Pay<Class<Plant>> FROM Plant",
          "7 MC",
      )
      doTasks(
          "UseCardAction<$LocalShading, Action2>",
          "-Floater<$LocalShading>",
          "PROD[MC]",
      )
      doTasks(
          "PlayProject<Class<$TerraformingContract>>",
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

    p2.turn {
      doTasks(
          "PlayProject<Class<$Research>>",
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
      doTasks("ClaimMilestone<Class<Researcher>>", "8 Pay<> FROM MC", "Researcher")
    }

    // "Since I have so much titanium, I took spaceport colony and it cost me 24 and I can do that
    // as 12 titanium." "Oh, are you just not getting a lot of space cards?"
    // "I still have 10 left." Space Port Colony costs twelve titanium after discounts.
    p1.turn {
      doTasks(
          "PlayProject<Class<$SpacePortColony>>",
          "12 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$SpacePortColony FROM ProjectCard",
          "Colony<Io>",
          "PROD[Heat]",
          "TradeFleet",
      )
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?")
      narrowTask("ColonyProduction<Io>")
      doTasks("2 Heat", "8 Heat")
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
    }

    p1.turn {
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_5_9>")
      // "And I'm very sorry for this, but it just looks too good for you."
      // "Goodbye animals from Miranda, they all go away." "Well played."
      // "I have to push this colony advantage, that's kind of all I got. Your milestone advantage."
      // Miranda's trade empties its track; Aqua has no animal card to receive animals.
      doTasks(
          "TradeAction<Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Miranda>",
      )
      selectTask("ColonyProduction<Miranda>?")
      narrowTask("ColonyProduction<Miranda>")
      // The printed three-animal trade reward had no legal holder and yielded nothing.
      agents[p1.actor].dropTask(game.tasks.extract { it }.single { !it.deferred }.id)
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$SixteenPsyche>>",
          "31 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "PROD[2 Titanium]",
          "3 Titanium",
      )
    }

    p1.turn {
      doTasks("PlayProject<Class<$EnergyMarket>>", "$EnergyMarket FROM ProjectCard")
    }
    p2.turn {
      doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    }
    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }
    p2.turn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$CommunityServices>>",
          "10 Pay<> FROM MC",
          "$CommunityServices FROM ProjectCard",
          "PROD[4 MC]",
      )
    }
    p2.turn {
      doTasks("Pass")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$LunarBeam>>",
          "10 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
      repeat(4) {
        p1.doTasks(
            "ConvertHeat",
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
    p1.turn {
      doTasks(
          "PlayProject<Class<$RotatorImpacts>>",
          "Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
      )
    }
    // "Oh, I hate that card when I don't have it."
    p2.turn {
      doTasks(
          "PlayProject<Class<$SolarLogistics>>",
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      doTasks(
          "PlayProject<Class<$ImportedGhg>>",
          "Ok", // no titanium paid
          "2 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard", // Solar Logistics sees the space event
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$Dirigibles>>",
          "8 Pay<> FROM MC",
          "$Dirigibles FROM ProjectCard",
      )
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
      doTasks(
          "PlayProject<Class<$AstraMechanica>>",
          "7 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$GiantIceAsteroid>>",
          "ProjectCard FROM PlayedEvent<Class<$ImportedGhg>>",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }

    // "There goes my plan." "The final ocean already?" "Oh my god."
    // "Yeah, this game is wild." "If by wild you mean I'm losing badly, yeah."
    // Giant Ice Asteroid's two oceans are 3-2 and 6-5. The transcript and app
    // record their bonuses separately, although the app combines nearby money changes.
    p2.turn {
      doTasks(
          "PlayProject<Class<$GiantIceAsteroid>>",
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
          "PlayProject<Class<$LavaFlows>>",
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
    p1.turn {
      doTasks(
          "UseCardAction<$RotatorImpacts, Action1>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "Asteroid<$RotatorImpacts>",
      )
    }
    p2.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_4_6>",
          "ProjectCard",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_9_7>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$FloatingRefinery>>",
          "7 Pay<> FROM MC",
          "$FloatingRefinery FROM ProjectCard",
          "2 Floater<$FloatingRefinery>",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$FloatingRefinery, Action2>",
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
    }
    // "I've come to my big play, which is so silly. But I have all this titanium."
    // "Which as you can see I just spent the last several turns making sure I would be able to
    // do that."
    // The L1 gift is placed on the three cards Aqua prepared in the preceding turns.
    p1.turn {
      doTasks(
          "PlayProject<Class<$L1TradeTerminal>>",
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
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      declineTask("ColonyProduction<Triton>?") // decline Trading Colony's one step
      doTasks("Titanium", "5 Titanium")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$ImportedGhg>>",
          "Ok", // no titanium paid
          "2 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard", // Solar Logistics
      )
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    p1.turn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }

    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "3 MC FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$SolarProbe>>",
          "Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$SolarProbe FROM ProjectCard",
          "2 ProjectCard", // six science tags
          "ProjectCard", // Solar Logistics
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$InventorsGuild>>",
          "6 Pay<> FROM MC",
          "$InventorsGuild FROM ProjectCard",
      )
    }
    p2.turn {
      doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    }

    p1.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
      )
      selectTask("ColonyProduction<Io>?") // Trading Colony
      narrowTask("ColonyProduction<Io>")
      selectTask("2 ColonyProduction<Io> OR Ok") // L1 Trade Terminal
      narrowTask("2 ColonyProduction<Io>")
      doTasks("2 Heat", "10 Heat")
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
    }
    p2.turn {
      doTasks("Pass")
    }
    p1.turn {
      doTasks("ConvertHeat", "8 Pay<Class<Heat>> FROM Heat", "TemperatureStep", "TerraformRating")
      // "Check out this badass card. It's potatoes. I can't build it because it needs plants."
      // Inventors' Guild reveals Potatoes; Aqua declines the purchase.
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
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
    p2.turn {
      doTasks(
          "PlayProject<Class<$RedSpotObservatory>>",
          "17 Pay<> FROM MC",
          "$RedSpotObservatory FROM ProjectCard",
          "2 ProjectCard",
      )
    }

    p1.turn {
      doTasks(
          "UseStandardProject<AirScrappingProject>",
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          "UseCardAction<$RotatorImpacts, Action2>",
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard", // Venus 8% bonus
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$SulphurEatingBacteria>>",
          "6 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
          "Microbe<$SulphurEatingBacteria>", // Viral Enhancers on the microbe tag
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$HiredRaiders>>",
          "$HiredRaiders FROM ProjectCard",
          "3 MC FROM MC<Pink>",
      )
      doTasks(
          "PlayProject<Class<$Decomposers>>",
          "2 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "Microbe<$Decomposers>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$MartianLumberCorp>>",
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$MartianLumberCorp FROM ProjectCard",
          "PROD[Plant]",
          "Plant", // Viral Enhancers on the plant tag
      )
    }

    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
    }
    p1.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
      )
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$VenusShuttles>>",
          "9 Pay<> FROM MC",
          "$VenusShuttles FROM ProjectCard",
          "2 Floater<$FloatingRefinery>",
      )
    }
    // "I don't love how this game is going. Okay. I'm gonna use Inventor's Guild to look at the
    // best card in the game." "And it requires two Jovian."
    // Inventors' Guild reveals an unaffordable card; Aqua lets it go.
    p1.turn {
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$CoronaExtractor>>",
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$CoronaExtractor FROM ProjectCard",
          "PROD[4 Energy]",
      )
    }
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$VenusShuttles, Action1>",
          "8 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
    }

    // The agreed Dirigibles discount replaces Aqua's initial four-M€ payment.
    p1.turn {
      doTasks(
          "PlayProject<Class<$NeutralizerFactory>>",
          "PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          "PlayProject<Class<$OrbitalReflectors>>",
          "PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "10 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "$OrbitalReflectors FROM ProjectCard",
          "2 VenusStep",
          "3 TerraformRating", // includes the Venus 16% threshold
          "PROD[2 Heat]",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$FloatingRefinery, Action2>",
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
    }
    // This floater was placed later than Aqua first recalled in the conversation.
    p1.turn {
      doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$LocalShading, Action2>",
          "-Floater<$LocalShading>",
          "PROD[MC]",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$UrbanDecomposers>>",
          "3 Pay<> FROM MC",
          "$UrbanDecomposers FROM ProjectCard",
          "PROD[Plant]",
          "2 Microbe<$Decomposers>",
          "Microbe<$Decomposers>", // Decomposers reacts to the microbe tag
      )
    }
    p2.turn {
      doTasks("UseCardAction<$RedSpotObservatory, Action1>", "Floater<$RedSpotObservatory>")
    }
    // Viking 2's delegate space is visible in the final photograph; "5-9" was a slip.
    p1.turn {
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_4_8>")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$SulphurEatingBacteria, Action1>",
          "Microbe<$SulphurEatingBacteria>",
      )
    }
    // "How are we ever going to get this oxygen up? It's going to be stuck like this forever."
    p1.turn {
      doTasks("Pass")
    }
    // "So you have 23 mega credit production and I have one."
    // "I get 36. Hey, they're the same with the digits reversed." "You're the inverse."
    p2.turn {
      doTasks(
          "PlayProject<Class<$Sponsors>>",
          "Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTasks(
          "PlayProject<Class<$Cartel>>",
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
    p1.turn {
      doTasks("TradeAction<Action2>", "3 Pay<Class<Energy>> FROM Energy", "Trade<Triton>")
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTasks("TradeAction<Action3>", "3 Pay<Class<Titanium>> FROM Titanium", "Trade<Enceladus>")
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    // "I think I will fund the investor and the traveler, which will be a total of 22."
    // "You're winning in all five awards, so except the one we're tied."
    p2.turn {
      doTasks("FundAward<Class<Investor>>", "8 Pay<> FROM MC", "Investor")
      doTasks("FundAward<Class<Traveller>>", "14 Pay<> FROM MC", "Traveller")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$HeavyTaxation>>",
          "$HeavyTaxation FROM ProjectCard",
          "PROD[2 MC]",
          "4 MC",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$RedSpotObservatory, Action2>",
          "-Floater<$RedSpotObservatory>",
          "ProjectCard",
      )
    }
    // "I'm gonna spend the twenty three to plant a forest, which raised the oxygen to seven."
    // "Seven seven?" "Which gives me seven money and two plants."
    p1.turn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_7_7>",
          "7 MC",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
    }
    p2.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "2 Animal<$Pets>",
      )
    }
    // "I'm going to use my inventor's guild to look at a shitty card. It is a shitty card."
    // "But it's a shitty time."
    p1.turn {
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    p2.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_6_4>",
          "4 MC",
          "Plant",
          "OxygenStep",
          "TerraformRating",
          "0 TemperatureStep", // decline TemperatureStep. at the oxygen threshold
      )
      // "You got the non-existent bonus." The table does not take the 8% temperature bonus.
    }
    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
    }
    p1.turn {
      doTasks(
          "UseCardAction<$RotatorImpacts, Action2>",
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }
    p2.turn {
      doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    }
    // Aqua first says Titan Floating Launch-Pad, then corrects the floater to Dirigibles.
    p1.turn {
      doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$FloatingRefinery, Action2>",
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
    }
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action1>",
          "Floater<$TitanFloatingLaunchPad>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$JovianLanterns>>",
          "20 Pay<> FROM MC",
          "$JovianLanterns FROM ProjectCard",
          "TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$IoSulphurResearch>>",
          "14 Pay<> FROM MC",
          "$IoSulphurResearch FROM ProjectCard",
          "3 ProjectCard",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$JovianLanterns, Action1>",
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$ImmigrationShuttles>>",
          "11 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$ImmigrationShuttles FROM ProjectCard",
          "PROD[5 MC]",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$VenusShuttles, Action1>",
          "8 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_4_7>")
    }
    p2.turn {
      doTasks("UseCardAction<$SulphurEatingBacteria, Action1>", "Microbe<$SulphurEatingBacteria>")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$AdaptedLichen>>",
          "6 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
          "Microbe<$Decomposers>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$MethaneFromTitan>>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "16 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
    }
    p1.turn {
      doTasks("Pass")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$MedicalLab>>",
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
    p2.turn {
      doTasks(
          "UseStandardProject<BuildColonyProject>",
          "17 Pay<> FROM MC",
          "Colony<Miranda>",
          "Animal<$Pets>",
      )
      doTasks(
          "PlayProject<Class<$ResearchColony>>",
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
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
      doTasks("TradeAction<Action3>", "3 Pay<Class<Titanium>> FROM Titanium", "Trade<Enceladus>")
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$MediaGroup>>",
          "Pay<> FROM MC",
          "$MediaGroup FROM ProjectCard",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$InventorsGuild, Action1>", "ProjectCard<Selecting>")
      doTasks(
          "-0 ProjectCard<Selecting>",
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$VenusianInsects>>",
          "5 Pay<> FROM MC",
          "$VenusianInsects FROM ProjectCard",
          "Microbe<$VenusianInsects>", // Viral Enhancers
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$SubZeroSaltFish>>",
          "2 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "PROD[-Plant<Pink>]",
          "Microbe<$Decomposers>",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$JovianLanterns, Action1>",
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
    }
    p1.turn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_6_8>",
          "7 MC",
          "Plant",
          "OxygenStep",
          "TerraformRating",
      )
    }
    p2.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_3_4>",
          "2 MC",
          "2 Plant",
          "OxygenStep",
          "TerraformRating",
      )
    }
    p1.turn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_5_9>",
          "3 MC",
          "2 ProjectCard",
          "OxygenStep",
          "TerraformRating",
      )
    }
    // "I am doubting you'll have two more greeneries up your sleeve."
    // "With 12 money that would be a little difficult."
    p2.turn {
      doTasks(
          "PlayProject<Class<$HydrogenToVenus>>",
          "11 Pay<> FROM MC",
          "$HydrogenToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$LocalShading>",
          "3 MC", // Media Group
          "ProjectCard", // Solar Logistics
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$MineralDeposit>>",
          "2 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_3_6>")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$FloatingRefinery, Action2>",
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }
    p2.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "Animal<$Pets>",
          "2 ProjectCard",
      )
    }
    p1.turn {
      doTasks(
          "UseCardAction<$RotatorImpacts, Action1>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid
          "Asteroid<$RotatorImpacts>",
      )
    }
    p2.turn {
      doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    }
    p1.turn {
      doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$TitanFloatingLaunchPad>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$Atmoscoop>>",
          "Pay<Class<Titanium>> FROM Titanium",
          "18 Pay<> FROM MC",
          "$Atmoscoop FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
    }
    // "I pass. Wish I hadn't passed now."
    p1.turn {
      doTasks("Pass")
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action2>", "Pay<Class<Steel>> FROM Steel", "7 MC")
      doTasks(
          "UseCardAction<$VenusShuttles, Action1>",
          "7 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTasks("UseCardAction<$RedSpotObservatory, Action1>", "Floater<$RedSpotObservatory>")
      doTasks("UseCardAction<$VenusianInsects, Action1>", "Microbe<$VenusianInsects>")
      doTasks("UseCardAction<$SulphurEatingBacteria, Action1>", "Microbe<$SulphurEatingBacteria>")
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
    p1.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_9_8>",
          "Titanium",
          "3 MC",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseStandardProject<GreeneryProject>",
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
    p2.turn {
      doTasks(
          "UseCardAction<$RedSpotObservatory, Action2>",
          "-Floater<$RedSpotObservatory>",
          "ProjectCard",
      )
      doTasks("FundAward<Class<Landscaper>>", "20 Pay<> FROM MC", "Landscaper")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$LunaGovernor>>",
          "Pay<> FROM MC",
          "$LunaGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    // "This time I get two animals, so I get gold pets and two cards. Cool."
    p2.turn {
      doTasks(
          "TradeAction<Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "2 Animal<$Pets>",
          "2 ProjectCard",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$AsteroidRights>>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
    }
    // "Flip a greenery into a city."
    // "Oh, I should have used my thing to block you. Damn it. I gotta think more clearly."
    p2.turn {
      doTasks(
          "PlayProject<Class<$KaguyaTech>>",
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
      declineTask("Plant<Pink>! OR Ok")
      p1.doTasks("PROD[MC]")
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks("GreeneryTile<Vastitas_3_5>", "Plant")
      selectTask("OxygenStep") // oxygen is already maxed
    }
    p1.turn {
      doTasks(
          "UseCardAction<$TitanFloatingLaunchPad, Action2>",
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Triton>",
      )
      selectTask("ColonyProduction<Triton>?")
      narrowTask("ColonyProduction<Triton>")
      selectTask("2 ColonyProduction<Triton> OR Ok")
      narrowTask("2 ColonyProduction<Triton>")
      doTasks("Titanium", "4 Titanium")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$WarpDrive>>",
          "14 Pay<> FROM MC",
          "$WarpDrive FROM ProjectCard",
      )
    }
    p1.turn {
      doTasks("TradeAction<Action3>", "3 Pay<Class<Titanium>> FROM Titanium", "Trade<Enceladus>")
      selectTask("ColonyProduction<Enceladus>?")
      narrowTask("ColonyProduction<Enceladus>")
      selectTask("2 ColonyProduction<Enceladus> OR Ok")
      narrowTask("2 ColonyProduction<Enceladus>")
      doTasks("4 Microbe<$Decomposers>")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$ImportedNutrients>>",
          "Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "4 Microbe<$VenusianInsects>",
          "3 MC", // Media Group
          "ProjectCard", // Solar Logistics
      )
    }
    // "Whoo, this is what I'm reduced to." "You are reduced to produce."
    p1.turn {
      doTasks("UseCardAction<$AsteroidRights, Action2>", "-Asteroid<$AsteroidRights>", "PROD[MC]")
    }
    p2.turn {
      doTasks(
          "UseCardAction<$FloatingRefinery, Action2>",
          "-2 Floater<$LocalShading>",
          "Titanium",
          "2 MC",
      )
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$Advertising>>",
          "Pay<> FROM MC",
          "$Advertising FROM ProjectCard",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$JovianLanterns, Action1>",
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
    }
    p1.turn {
      doTasks(
          "UseStandardProject<GreeneryProject>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_7_9>",
          "3 MC",
          "0 OxygenStep", // decline OxygenStep.; oxygen is already maxed
      )
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$BactoviralResearch>>",
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "10 Microbe<$VenusianInsects>",
          "Plant", // Viral Enhancers
      )
    }
    p1.turn {
      doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$TerraformingGanymede>>",
          "Pay<Class<Titanium>> FROM Titanium",
          "25 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "5 TerraformRating",
      )
      // 16:34:22: Pink pays one titanium and 29 MC, missing Warp Drive's 4 MC discount.
      exMachina("-4 MC")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$MolecularPrinting>>",
          "8 Pay<> FROM MC",
          "$MolecularPrinting FROM ProjectCard",
          "11 MC",
      )
    }
    p2.turn {
      doTasks("UseCardAction<$VenusianInsects, Action1>", "Microbe<$VenusianInsects>")
    }
    // "This card is going to be the thing that saves everything. Natural preserve. That did not
    // save everything." "I don't buy it."
    p1.turn {
      doTasks(
          "UseCardAction<$InventorsGuild, Action1>",
          "ProjectCard<Selecting>",
          "-ProjectCard<Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "UseCardAction<$SulphurEatingBacteria, Action2>",
          "-4 Microbe<$SulphurEatingBacteria>",
          "12 MC",
      )
    }
    p1.turn {
      doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    }
    p2.turn {
      doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
      doTasks(
          "PlayProject<Class<$MagneticFieldGeneratorsPromo>>",
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
    p1.turn {
      doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_8_6>")
    }
    p2.turn {
      doTasks("UseCardAction<$ElectroCatapult, Action1>", "Pay<Class<Plant>> FROM Plant", "7 MC")
    }
    p1.turn {
      doTasks(
          "PlayProject<Class<$MediaArchives>>",
          "5 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "11 MC",
      )
    }
    // "I'm kind of surprised I'm even able to do this." "I pitched two cards."
    // "Oh my. You spent all your money exactly." "I should have waited."
    // Pink sells two cards to pay for Large Convoy.
    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "2 MC FROM ProjectCard")
      doTasks(
          "PlayProject<Class<$LargeConvoy>>",
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
    p1.turn {
      doTasks(
          "UseStandardProject<CityProject>",
          "25 Pay<> FROM MC",
          "CityTile<Vastitas_3_6>",
          "3 MC", // Arcadian community
          "PROD[MC]", // Immigrant City
      )
      p2.doTasks("Animal<$Pets>")
      doTasks("PROD[MC]") // the city's own production, in addition to Immigrant City
    }
    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    p1.turn {
      doTasks(
          "UseCardAction<$RotatorImpacts, Action2>",
          "-Asteroid<$RotatorImpacts>",
          "0 VenusStep", // Venus is already maxed
      )
    }
    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    // "It's all about the tiebreaker, which I'm definitely going to need in this game for sure."
    p1.turn {
      doTasks(
          "PlayProject<Class<$CorroderSuits>>",
          "2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>",
          "Ok", // Nothing remains to pay in MC.
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "Floater<$Dirigibles>",
      )
    }
    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    p1.turn {
      doTasks("UseCardAction<$SubZeroSaltFish, Action1>", "Animal<$SubZeroSaltFish>")
    }
    p2.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    // "I pitch a card." "So looks like you're pretty much out."
    // "Yeah, I think there's nothing else I can do. Now what are you gonna do?"
    // Aqua's final sale accounts for the recorded extra M€.
    p1.turn {
      doTasks("UseStandardProject<SellPatentsProject>", "MC FROM ProjectCard")
    }
    p2.turn {
      doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
      doTasks(
          "GreeneryTile<Vastitas_2_3>",
          "2 MC",
          "0 OxygenStep",
      ) // decline OxygenStep.; oxygen is maxed
      doTasks("UseStandardProject<SellPatentsProject>", "5 MC FROM ProjectCard")
    }
    p1.turn {
      doTasks("Pass")
    }
    p2.turn {
      doTasks(
          "PlayProject<Class<$BreathingFilters>>",
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
    p1.selectTask("-Baton") // only three plants; no final greenery
    // "Do you replace, you don't." "I do actually." "You do." "Probably on two four."
    // The final photo shows Pink's last greenery here.
    p2.doTasks("ConvertPlants", "8 Pay<Class<Plant>> FROM Plant")
    p2.doTasks("GreeneryTile<Vastitas_2_4>")
    p2.selectTask("-Baton") // no more plants to convert
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
