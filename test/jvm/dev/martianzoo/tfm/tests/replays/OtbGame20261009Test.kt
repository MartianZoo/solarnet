package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

// Sources: _local/replays/Game20261009/{transcript-afternoon,transcript-evening,sources}.md.
// Recorded through generation 8; the players stopped before generation 9 Research.
// Player tasks are explicit. Admin retains normal autoexecution, as in Game20230521Test.
internal class OtbGame20261009Test : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          VastitasMap
          CorporateEraExpansion, PreludeExpansion, Prelude2CardPack, VenusNextExpansion
          ColoniesExpansion, PromoCardPack, FakeStuffBundle
          Callisto, Enceladus, Io, Miranda, Triton
          Diversifier, Engineer, Forester, Legend, Researcher
          Botanist, Investor, Landscaper, Manufacturer, Traveller
          """,
          "Aqua",
          "Pink",
      )

  @Test
  internal fun game20261009ThroughGeneration8() {
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    TfmWorkflow.Automatic(agents).launch()
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
    p1.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$ArcadianCommunities>>",
        "$ArcadianCommunities FROM CorporationCard",
        "40 MC",
        "10 Steel",
        "ArcadianCommunities_RequiredAction",
        "-15 MC",
        "5 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "PlayCard<Class<CorporationCard>, Class<$Teractor>>",
        "$Teractor FROM CorporationCard",
        "60 MC",
        "-24 MC",
        "8 ProjectCard<Hand FROM Selecting>",
    )

    // "I chose my preludes because they rhyme. I'm just kidding."
    p1.doTasks(
        "PlayCard<Class<PreludeCard>, Class<$BoomTown>>",
        "$BoomTown FROM PreludeCard",
        "PROD[2 Titanium]",
        "CityTile<Vastitas_6_9>",
        "2 Titanium",
    )
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

    p1.doTasks("RequiredActionsSignal", "Community<Vastitas_7_8>")
    p1.doTasks(
        "PlayProject<Class<$NuclearPower>>",
        "-5 Steel",
        "Ok", // no MC paid
        "$NuclearPower FROM ProjectCard",
        "PROD[-2 MC]",
        "PROD[3 Energy]",
    )

    // "Giant ice asteroid. Crazy play for first turn."
    p2.doTasks(
        "PlayProject<Class<$GiantIceAsteroid>>",
        "-36 MC",
        "Ok", // no titanium paid
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
    p2.exMachina("PROD[-Heat]")
    p2.doTasks("Ok") // end turn
    // "Can you believe that three cities already got played before it?"
    p1.doTasks(
        "PlayProject<Class<$ImmigrantCity>>",
        "-6 Steel",
        "-1 MC",
        "$ImmigrantCity FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[-2 MC]",
        "CityTile<Vastitas_7_8>",
        "PROD[MC]",
        "5 MC",
        "Plant",
    )
    p1.doTasks("Ok") // end turn
    // Pink log entry 27 records +1 heat before Pets' payment at entry 28.
    p2.exMachina("Heat")
    p2.doTasks(
        "PlayProject<Class<$Pets>>",
        "-7 MC",
        "$Pets FROM ProjectCard",
        "Animal<$Pets>",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "UseCardAction<$ArcadianCommunities, Action1>",
        "Community<Vastitas_7_7>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("Pass")
    p1.doTasks("Pass")
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
    p1.doTasks("-3 ProjectCard<Selecting>", "-3 MC", "ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    // "Just in case you have any ideas, I'm gonna immediately spend plant gain seven."
    p2.doTasks(
        "PlayProject<Class<$ElectroCatapult>>",
        "-17 MC",
        "Ok", // no steel paid
        "$ElectroCatapult FROM ProjectCard",
        "PROD[-Energy]",
    )
    p2.doTasks(
        "UseCardAction<$ElectroCatapult, Action1>",
        "-Plant",
        "7 MC",
    )
    p1.doTasks(
        "PlayProject<Class<$ResearchOutpost>>",
        "-18 MC",
        "Ok", // no steel paid
        "$ResearchOutpost FROM ProjectCard",
        "CityTile<Vastitas_9_9>",
        "Steel",
        "PROD[MC]",
    )
    p1.selectTask("Animal<$Pets<Pink>>")
    p2.doTasks("Animal<$Pets>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$Casinos>>",
        "-5 MC",
        "Ok", // no steel paid
        "$Casinos FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[4 MC]",
    )
    p2.doTasks("Ok") // end turn
    // Include the five-MC payment correction acknowledged at 22:00:27–22:00:47 here.
    p1.doTasks(
        "PlayProject<Class<$TradingColony>>",
        "-5 Titanium",
        "-7 MC",
        "$TradingColony FROM ProjectCard",
        "Colony<Triton>",
        "3 Titanium",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("Pass")
    p1.doTasks(
        "PlayProject<Class<$GiantSpaceMirror>>",
        "-4 Titanium",
        "-8 MC",
        "$GiantSpaceMirror FROM ProjectCard",
        "PROD[3 Energy]",
    )
    p1.doTasks(
        "UseCardAction<$ArcadianCommunities, Action1>",
        "Community<Vastitas_6_8>",
    )
    p1.doTasks("Pass")
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
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p1.doTasks(
        "TradeAction<Action2>",
        "-3 Energy",
        "Trade<Io>",
    )
    p1.selectTask("ColonyProduction<Io>?")
    p1.narrowTask("ColonyProduction<Io>")
    p1.doTasks("8 Heat")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$IndustrialCenter>>",
        "-4 MC",
        "Ok", // no steel paid
        "$IndustrialCenter FROM ProjectCard",
        "IndustrialCenter_SpecialTile<Vastitas_8_8>",
        "Steel",
    )
    p2.doTasks("Ok") // end turn
    // The coordinate is unspoken at 22:07:33; board-22-11-07.jpg shows the cube at 9-8.
    p1.doTasks(
        "UseCardAction<$ArcadianCommunities, Action1>",
        "Community<Vastitas_9_8>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$IndustrialCenter, Action1>",
        "-7 MC",
        "PROD[Steel]",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$InventionContest>>",
        "-1 MC",
        "$InventionContest FROM ProjectCard",
        "3 ProjectCard<Selecting>",
        "ProjectCard<Hand FROM Selecting>",
        "-2 ProjectCard<Selecting>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$ElectroCatapult, Action1>",
        "-Plant",
        "7 MC",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks("Pass")
    p2.doTasks("Pass")
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
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks(
        "Ok", // keep all four research cards
        "-12 MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "PlayProject<Class<$LocalShading>>",
        "-4 MC",
        "$LocalShading FROM ProjectCard",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$MarketManipulation>>",
        "$MarketManipulation FROM ProjectCard",
    )
    p1.selectTask("ColonyProduction(NOT Source@) FROM Source@ColonyProduction")
    p1.narrowTask("ColonyProduction<Triton> FROM ColonyProduction<Miranda>")
    p1.doTasks(
        "TradeAction<Action2>",
        "-3 Energy",
        "Trade<Triton>",
    )
    p1.selectTask("ColonyProduction<Triton>?")
    p1.narrowTask("ColonyProduction<Triton>")
    p1.doTasks("6 Titanium")
    p2.doTasks(
        "UseCardAction<$ElectroCatapult, Action1>",
        "-Plant",
        "7 MC",
    )
    p2.doTasks("Ok") // end turn
    // "I will also play a catapult. Earth catapult."
    p1.doTasks(
        "PlayProject<Class<$EarthCatapult>>",
        "-22 MC",
        "$EarthCatapult FROM ProjectCard",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$LocalShading, Action1>",
        "Floater<$LocalShading>",
    )
    p2.doTasks("Ok") // end turn
    // Research Coordination uses the existing inert wild-tag stand-in.
    p1.doTasks(
        "PlayProject<Class<$FakeResearchCoordination>>",
        "-1 MC",
        "$FakeResearchCoordination FROM ProjectCard",
    )
    p1.doTasks(
        "UseCardAction<$ArcadianCommunities, Action1>",
        "Community<Vastitas_8_9>",
    )
    p2.doTasks(
        "PlayProject<Class<$ViralEnhancers>>",
        "-9 MC",
        "$ViralEnhancers FROM ProjectCard",
        "Plant",
    )
    p2.doTasks(
        "ClaimMilestone<Class<Diversifier>>",
        "-8 MC",
        "Diversifier",
    )
    // 22:18:07: the wild tag supplies Conscription's second Earth tag.
    p1.exMachina("FakeWildTagUse, EarthTag<FakeWildTagUse>")
    p1.doTasks(
        "PlayProject<Class<$Conscription>>",
        "-2 MC",
        "$Conscription FROM ProjectCard",
        "Conscription_NextCardEffect",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$IndustrialCenter, Action1>",
        "-7 MC",
        "PROD[Steel]",
    )
    p2.doTasks("Ok") // end turn
    // "Titan floating launch pad for free. That doesn't happen every day, does it?"
    p1.doTasks(
        "PlayProject<Class<$TitanFloatingLaunchPad>>",
        "$TitanFloatingLaunchPad FROM ProjectCard",
        "2 Floater<$TitanFloatingLaunchPad>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("Pass")
    p1.doTasks(
        "UseCardAction<$TitanFloatingLaunchPad, Action1>",
        "Floater<$TitanFloatingLaunchPad>",
    )
    // Pink's phone in board-22-21-43.jpg still displays this pre-production balance.
    p2.assertResources(m = 24, s = 2, t = 0, p = 3, e = 0, h = 5)
    p1.doTasks("Pass")
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
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    // At 22:25:50 both players agree to undo the titanium payment for this trade and
    // use the Launch-Pad instead. Replay the accepted choice at the original trade.
    p1.doTasks(
        "UseCardAction<$TitanFloatingLaunchPad, Action2>",
        "-Floater<$TitanFloatingLaunchPad>",
        "Trade<Callisto>",
    )
    p1.selectTask("ColonyProduction<Callisto>?")
    p1.narrowTask("ColonyProduction<Callisto>")
    p1.doTasks("13 Energy")
    p1.doTasks("Ok") // end turn
    // "Requires six ocean tiles. Thank you, me."
    p2.doTasks(
        "PlayProject<Class<$KelpFarming>>",
        "-17 MC",
        "$KelpFarming FROM ProjectCard",
        "PROD[2 MC]",
        "PROD[3 Plant]",
        "2 Plant",
        "Plant",
    )
    p2.doTasks("Ok") // end turn
    // "Wow, this is the first TR I've ever gotten."
    p1.doTasks(
        "UseStandardProject<AsteroidProject>",
        "-14 MC",
        "TemperatureStep",
        "TerraformRating",
    )
    p1.doTasks(
        "ConvertHeat",
        "-8 Heat",
        "TemperatureStep",
        "TerraformRating",
        "PROD[Heat]",
    )
    // Aqua never recorded the −20°C production bonus: entries 97 and 106 show
    // 6 heat before production, then 24 from converting 18 energy, with no heat income.
    p1.exMachina("PROD[-Heat]")
    p2.doTasks(
        "ClaimMilestone<Class<Forester>>",
        "-8 MC",
        "Forester",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "UseCardAction<$ArcadianCommunities, Action1>",
        "Community<Vastitas_7_9>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$AdvancedAlloys>>",
        "-9 MC",
        "$AdvancedAlloys FROM ProjectCard",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks("Pass")
    // "My four steel is 12 money."
    p2.doTasks(
        "PlayProject<Class<$IndustrialMicrobes>>",
        "-4 Steel",
        "Ok", // no MC paid
        "$IndustrialMicrobes FROM ProjectCard",
        "PROD[Energy]",
        "PROD[Steel]",
        "Plant",
    )
    p2.doTasks(
        "PlayProject<Class<$CorporateStronghold>>",
        "-11 MC",
        "Ok", // no steel paid
        "$CorporateStronghold FROM ProjectCard",
        "PROD[-Energy]",
        "PROD[3 MC]",
        "CityTile<Vastitas_5_4>",
        "2 MC",
        "2 Plant",
        "Animal<$Pets>",
    )
    p2.selectTask("PROD[MC<Aqua>]")
    p1.doTasks("PROD[MC]")
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Vastitas_4_4>",
        "Plant",
        "4 MC",
        "OxygenStep",
        "TerraformRating",
    )
    // 22:31:08–22:31:24: both greeneries' oxygen is finally recorded. This greenery's
    // step has just resolved normally; restore the missed Project Eden step without its TR.
    p2.exMachina("OxygenStep")
    p2.doTasks(
        "UseCardAction<$ElectroCatapult, Action1>",
        "-Plant",
        "7 MC",
    )
    p2.doTasks(
        "UseCardAction<$LocalShading, Action2>",
        "-Floater<$LocalShading>",
        "PROD[MC]",
    )
    p2.doTasks(
        "PlayProject<Class<$TerraformingContract>>",
        "-8 MC",
        "$TerraformingContract FROM ProjectCard",
        "PROD[4 MC]",
    )
    p2.doTasks("Pass")
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

    // Generation 6 Research: Aqua buys two cards; Pink buys four.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("-2 ProjectCard<Selecting>", "-6 MC", "2 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("Ok", "-12 MC", "4 ProjectCard<Hand FROM Selecting>") // keep all four

    p2.doTasks(
        "PlayProject<Class<$Research>>",
        "-11 MC",
        "$Research FROM ProjectCard",
        "2 ProjectCard",
    )
    p2.doTasks("ClaimMilestone<Class<Researcher>>", "-8 MC", "Researcher")

    // "I still have 10 left." Space Port Colony costs twelve titanium after discounts.
    p1.doTasks(
        "PlayProject<Class<$SpacePortColony>>",
        "-12 Titanium",
        "Ok", // no MC paid
        "$SpacePortColony FROM ProjectCard",
        "Colony<Io>",
        "PROD[Heat]",
        "TradeFleet",
    )
    p1.doTasks(
        "UseCardAction<$TitanFloatingLaunchPad, Action2>",
        "-Floater<$TitanFloatingLaunchPad>",
        "Trade<Io>",
    )
    p1.selectTask("ColonyProduction<Io>?")
    p1.narrowTask("ColonyProduction<Io>")
    p1.doTasks("2 Heat", "8 Heat")

    p2.doTasks("UseCardAction<$ElectroCatapult, Action2>", "-Steel", "7 MC")
    p2.doTasks("Ok") // end turn

    p1.doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_5_9>")
    // Miranda's trade empties its track; Aqua has no animal card to receive animals.
    p1.doTasks(
        "TradeAction<Action3>",
        "-3 Titanium",
        "Trade<Miranda>",
    )
    p1.selectTask("ColonyProduction<Miranda>?")
    p1.narrowTask("ColonyProduction<Miranda>")
    // The printed three-animal trade reward had no legal holder and yielded nothing.
    agents[p1.actor].dropTask(game.tasks.extract { it }.single().id)

    p2.doTasks(
        "PlayProject<Class<$SixteenPsyche>>",
        "-31 MC",
        "Ok", // no titanium paid
        "$SixteenPsyche FROM ProjectCard",
        "PROD[2 Titanium]",
        "3 Titanium",
    )
    p2.doTasks("Ok") // end turn

    p1.doTasks("PlayProject<Class<$EnergyMarket>>", "$EnergyMarket FROM ProjectCard")
    p1.doTasks("Ok") // end turn
    p2.doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    p2.doTasks("Ok") // end turn
    p1.doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    p1.doTasks("Ok") // end turn
    p2.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$CommunityServices>>",
        "-10 MC",
        "$CommunityServices FROM ProjectCard",
        "PROD[4 MC]",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("Pass")
    p1.doTasks(
        "PlayProject<Class<$LunarBeam>>",
        "-10 MC",
        "$LunarBeam FROM ProjectCard",
        "PROD[-2 MC]",
        "PROD[2 Heat]",
        "PROD[2 Energy]",
    )
    repeat(4) {
      p1.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    }
    p1.doTasks("Pass")
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

    // Generation 7 Research. Aqua keeps four cards; Pink keeps one.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("Ok", "-12 MC", "4 ProjectCard<Hand FROM Selecting>") // keep all four
    p2.doTasks("-3 ProjectCard<Selecting>", "-3 MC", "ProjectCard<Hand FROM Selecting>")

    // The transcript's "Rune Chatter in Tax" is Rotator Impacts: its three-M€
    // discounted price and the asteroid action used later identify it.
    p1.doTasks(
        "PlayProject<Class<$RotatorImpacts>>",
        "-Titanium",
        "-MC",
        "$RotatorImpacts FROM ProjectCard",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$SolarLogistics>>",
        "-4 Titanium",
        "-MC",
        "$SolarLogistics FROM ProjectCard",
        "2 Titanium",
    )
    p2.doTasks(
        "PlayProject<Class<$ImportedGhg>>",
        "-2 MC",
        "Ok", // no titanium paid
        "$ImportedGhg FROM ProjectCard",
        "PROD[Heat]",
        "3 Heat",
        "ProjectCard", // Solar Logistics sees the space event
    )
    p1.doTasks(
        "PlayProject<Class<$Dirigibles>>",
        "-8 MC",
        "$Dirigibles FROM ProjectCard",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("UseCardAction<$ElectroCatapult, Action2>", "-Steel", "7 MC")
    p2.doTasks(
        "PlayProject<Class<$AstraMechanica>>",
        "-7 MC",
        "$AstraMechanica FROM ProjectCard",
        "ProjectCard FROM PlayedEvent<Class<$GiantIceAsteroid>>",
        "ProjectCard FROM PlayedEvent<Class<$ImportedGhg>>",
    )
    p1.doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    p1.doTasks("Ok") // end turn

    // Giant Ice Asteroid's two oceans are 3-2 and 6-5. The transcript and app
    // record their bonuses separately, although the app combines nearby money changes.
    p2.doTasks(
        "PlayProject<Class<$GiantIceAsteroid>>",
        "-3 Titanium",
        "-24 MC",
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
    p2.doTasks(
        "PlayProject<Class<$LavaFlows>>",
        "-18 MC",
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

    p1.doTasks(
        "UseCardAction<$RotatorImpacts, Action1>",
        "-3 Titanium",
        "Ok", // no MC paid
        "Asteroid<$RotatorImpacts>",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("ConvertPlants", "-8 Plant")
    p2.selectTask("MAX 0 Billing: DefaultGreeneryTile")
    p2.doTasks(
        "GreeneryTile<Vastitas_4_6>",
        "ProjectCard",
        "4 MC",
        "OxygenStep",
        "TerraformRating",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_9_7>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$FloatingRefinery>>",
        "-7 MC",
        "$FloatingRefinery FROM ProjectCard",
        "2 Floater<$FloatingRefinery>",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$FloatingRefinery, Action2>",
        "-2 Floater<$FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    p2.doTasks("Ok") // end turn
    // The L1 gift is placed on the three cards Aqua prepared in the preceding turns.
    p1.doTasks(
        "PlayProject<Class<$L1TradeTerminal>>",
        "-7 Titanium",
        "-8 MC",
        "$L1TradeTerminal FROM ProjectCard",
    )
    p1.selectTask(game.tasks.extract { it }.single().id) // L1's three distinct holders
    p1.doTasks(
        "Asteroid<$RotatorImpacts>",
        "Floater<$Dirigibles>",
        "Floater<$TitanFloatingLaunchPad>",
    )
    // Choose L1's two track steps and decline Trading Colony's optional third step.
    p1.doTasks(
        "UseCardAction<$TitanFloatingLaunchPad, Action2>",
        "-Floater<$TitanFloatingLaunchPad>",
        "Trade<Triton>",
    )
    p1.selectTask("2 ColonyProduction<Triton> OR Ok")
    p1.narrowTask("2 ColonyProduction<Triton>")
    p1.doTasks("Ok") // decline Trading Colony's one step
    p1.doTasks("Titanium", "5 Titanium")

    p2.doTasks(
        "PlayProject<Class<$ImportedGhg>>",
        "-2 MC",
        "Ok", // no titanium paid
        "$ImportedGhg FROM ProjectCard",
        "PROD[Heat]",
        "3 Heat",
        "ProjectCard", // Solar Logistics
    )
    p2.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    p1.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    p1.doTasks("Ok") // end turn

    p2.doTasks("UseStandardProject<SellPatentsProject>", "3 MC FROM ProjectCard")
    p2.doTasks(
        "PlayProject<Class<$SolarProbe>>",
        "-Titanium",
        "-5 MC",
        "$SolarProbe FROM ProjectCard",
        "2 ProjectCard", // six science tags
        "ProjectCard", // Solar Logistics
    )
    p1.doTasks(
        "PlayProject<Class<$InventorsGuild>>",
        "-6 MC",
        "$InventorsGuild FROM ProjectCard",
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("UseCardAction<$LocalShading, Action1>", "Floater<$LocalShading>")
    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "TradeAction<Action2>",
        "-3 Energy",
        "Trade<Io>",
    )
    p1.selectTask("ColonyProduction<Io>?") // Trading Colony
    p1.narrowTask("ColonyProduction<Io>")
    p1.selectTask("2 ColonyProduction<Io> OR Ok") // L1 Trade Terminal
    p1.narrowTask("2 ColonyProduction<Io>")
    p1.doTasks("2 Heat", "10 Heat")
    p1.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    p2.doTasks("Pass")
    p1.doTasks("ConvertHeat", "-8 Heat", "TemperatureStep", "TerraformRating")
    // Inventors' Guild reveals Potatoes; Aqua declines the purchase.
    p1.doTasks("UseCardAction<$InventorsGuild, Action1>", "ProjectCard<Selecting>")
    p1.doTasks("-ProjectCard<Selecting>")
    p1.doTasks("Pass")
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

    // Generation 8 Research. Aqua buys four cards; Pink buys three.
    p1.doTasks("4 ProjectCard<Selecting>")
    p2.doTasks("4 ProjectCard<Selecting>")
    p1.doTasks("Ok", "-12 MC", "4 ProjectCard<Hand FROM Selecting>")
    p2.doTasks("-ProjectCard<Selecting>", "-9 MC", "3 ProjectCard<Hand FROM Selecting>")
    p2.doTasks(
        "PlayProject<Class<$RedSpotObservatory>>",
        "-17 MC",
        "$RedSpotObservatory FROM ProjectCard",
        "2 ProjectCard",
    )
    p2.doTasks("Ok") // end turn

    p1.doTasks(
        "UseStandardProject<AirScrappingProject>",
        "-15 MC",
        "VenusStep",
        "TerraformRating",
    )
    p1.doTasks(
        "UseCardAction<$RotatorImpacts, Action2>",
        "-Asteroid<$RotatorImpacts>",
        "VenusStep",
        "TerraformRating",
        "ProjectCard", // Venus 8% bonus
    )
    p2.doTasks(
        "PlayProject<Class<$SulphurEatingBacteria>>",
        "-6 MC",
        "$SulphurEatingBacteria FROM ProjectCard",
        "Microbe<$SulphurEatingBacteria>", // Viral Enhancers on the microbe tag
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$HiredRaiders>>",
        "$HiredRaiders FROM ProjectCard",
        "3 MC FROM MC<Pink>",
    )
    p1.doTasks(
        "PlayProject<Class<$Decomposers>>",
        "-2 MC",
        "$Decomposers FROM ProjectCard",
        "Microbe<$Decomposers>",
    )
    p2.doTasks(
        "PlayProject<Class<$MartianLumberCorp>>",
        "-2 Steel",
        "Ok", // no MC paid
        "$MartianLumberCorp FROM ProjectCard",
        "PROD[Plant]",
        "Plant", // Viral Enhancers on the plant tag
    )
    p2.doTasks("Ok") // end turn

    p1.doTasks("UseCardAction<$EnergyMarket, Action2>", "PROD[-Energy]", "8 MC")
    p1.doTasks("Ok") // end turn
    p2.doTasks("UseCardAction<$ElectroCatapult, Action2>", "-Steel", "7 MC")
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "TradeAction<Action2>",
        "-3 Energy",
        "Trade<Enceladus>",
    )
    p1.selectTask("ColonyProduction<Enceladus>?")
    p1.narrowTask("ColonyProduction<Enceladus>")
    p1.selectTask("2 ColonyProduction<Enceladus> OR Ok")
    p1.narrowTask("2 ColonyProduction<Enceladus>")
    p1.doTasks("4 Microbe<$Decomposers>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$VenusShuttles>>",
        "-9 MC",
        "$VenusShuttles FROM ProjectCard",
        "2 Floater<$FloatingRefinery>",
    )
    p2.doTasks("Ok") // end turn
    // Inventors' Guild reveals an unaffordable card; Aqua lets it go.
    p1.doTasks("UseCardAction<$InventorsGuild, Action1>", "ProjectCard<Selecting>")
    p1.doTasks("-ProjectCard<Selecting>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "PlayProject<Class<$CoronaExtractor>>",
        "-2 Titanium",
        "-2 MC",
        "$CoronaExtractor FROM ProjectCard",
        "PROD[4 Energy]",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "UseCardAction<$TitanFloatingLaunchPad, Action2>",
        "-Floater<$TitanFloatingLaunchPad>",
        "Trade<Triton>",
    )
    p1.selectTask("ColonyProduction<Triton>?")
    p1.narrowTask("ColonyProduction<Triton>")
    p1.selectTask("2 ColonyProduction<Triton> OR Ok")
    p1.narrowTask("2 ColonyProduction<Triton>")
    p1.doTasks("Titanium", "4 Titanium")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$VenusShuttles, Action1>",
        "-8 MC",
        "VenusStep",
        "TerraformRating",
    )
    p2.doTasks("Ok") // end turn

    // The agreed Dirigibles discount replaces Aqua's initial four-M€ payment.
    p1.doTasks(
        "PlayProject<Class<$NeutralizerFactory>>",
        "-Floater<$Dirigibles>",
        "-MC",
        "$NeutralizerFactory FROM ProjectCard",
        "VenusStep",
        "TerraformRating",
    )
    p1.doTasks(
        "PlayProject<Class<$OrbitalReflectors>>",
        "-Floater<$Dirigibles>",
        "-10 Titanium",
        "Ok", // no MC paid
        "$OrbitalReflectors FROM ProjectCard",
        "2 VenusStep",
        "3 TerraformRating", // includes the Venus 16% threshold
        "PROD[2 Heat]",
    )
    p2.doTasks(
        "UseCardAction<$FloatingRefinery, Action2>",
        "-2 Floater<$FloatingRefinery>",
        "Titanium",
        "2 MC",
    )
    p2.doTasks("Ok") // end turn
    // This floater was placed later than Aqua first recalled in the conversation.
    p1.doTasks("UseCardAction<$Dirigibles, Action1>", "Floater<$Dirigibles>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$LocalShading, Action2>",
        "-Floater<$LocalShading>",
        "PROD[MC]",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks(
        "PlayProject<Class<$UrbanDecomposers>>",
        "-3 MC",
        "$UrbanDecomposers FROM ProjectCard",
        "PROD[Plant]",
        "2 Microbe<$Decomposers>",
        "Microbe<$Decomposers>", // Decomposers reacts to the microbe tag
    )
    p1.doTasks("Ok") // end turn
    p2.doTasks("UseCardAction<$RedSpotObservatory, Action1>", "Floater<$RedSpotObservatory>")
    p2.doTasks("Ok") // end turn
    // Viking 2's delegate space is visible in the final photograph; "5-9" was a slip.
    p1.doTasks("UseCardAction<$ArcadianCommunities, Action1>", "Community<Vastitas_4_8>")
    p1.doTasks("Ok") // end turn
    p2.doTasks(
        "UseCardAction<$SulphurEatingBacteria, Action1>",
        "Microbe<$SulphurEatingBacteria>",
    )
    p2.doTasks("Ok") // end turn
    p1.doTasks("Pass")
    p2.doTasks(
        "PlayProject<Class<$Sponsors>>",
        "-MC",
        "$Sponsors FROM ProjectCard",
        "PROD[2 MC]",
    )
    p2.doTasks(
        "PlayProject<Class<$Cartel>>",
        "-3 MC",
        "$Cartel FROM ProjectCard",
        "PROD[6 MC]",
    )
    p2.doTasks("Pass")

    // Production 8 -> 9: Aqua's 2 remaining energy becomes heat.
    p1.doTasks("2 Heat FROM Energy", "36 MC", "4 Titanium", "Plant", "4 Energy", "6 Heat")
    p2.doTasks("63 MC", "3 Steel", "2 Titanium", "4 Plant", "4 Energy", "3 Heat")
    p2.doTasks("OxygenStep! BY Admin")
    // board-12-00-57.jpg; stop before generation 9 Research.
    p1.assertResources(m = 50, s = 1, t = 9, p = 1, e = 4, h = 15)
    p1.assertProduction(m = 1, s = 0, t = 4, p = 1, e = 4, h = 6)
    p2.assertResources(m = 67, s = 7, t = 3, p = 10, e = 4, h = 8)
    p2.assertProduction(m = 23, s = 3, t = 2, p = 4, e = 4, h = 3)
    p1.assertCounts(35 to "TerraformRating")
    p2.assertCounts(40 to "TerraformRating")
    assertSidebar(gen = 9, temp = 8, oxygen = 6, oceans = 9, venus = 16)
  }
}
