package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.giveTask
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import kotlin.test.Test
import kotlin.test.assertEquals

/** Physical game played November 11–12, 2026; sources: `_local/replays/Game20261111/`. */
internal class OtbGame20261111Test : FullGameTest() {
  override val config =
      // (11:51:55 AM) "Here begins our Veterans Day game of Terraforming Mars. We're playing on the
      // Vastitas Borealis board. Our milestones are Builder, Coast Guard, Legend, Merchant,
      // Pioneer, and Hoverlord. Our awards are Benefactor, Celebrity, Incorporator, Scientist,
      // Zoologist, and Venuphile."
      GameConfig(
          """
          VastitasMap
          CorporateEraExpansion, PreludeExpansion, Prelude2Expansion, VenusNextExpansion
          ColoniesExpansion, TurmoilExpansion, promos:cards, $FakeHelion
          1 SelectableCorporationCount
          Luna, Ceres, Triton, Titan, Enceladus
          Builder, Coastguard, Legend, Merchant, Pioneer, Hoverlord
          Benefactor, Celebrity, Incorporator, Scientist, Zoologist, Venuphile
          """,
          "Green",
          "Pink",
      )

  // "Our coming global event is Scientific Community and our distant global event is Global Dust
  // Storm. Reds are dominant. We pulled up Scientific Community first, and so that made Reds
  // dominant, and nobody else has taken the dominance away. So yeah, the order of those matters.
  // And our colonies are Luna, Ceres, Triton, Titan, and Enceladus, although Enceladus and Titan
  // are not really in play yet."

  override val globalEventDeck =
      listOf(
          "ExploreFirstDirective",
          "HeatFirstPolicy",
          "FreeAcademiaTreaty",
          "ViralModificationsApproved",
          "TerraformingLobbying",
          "DemocraticReform",
          "SelfSufficiencyProgram",
          "Diaspora",
          "IncreasedInterplanetaryTrade",
          "MoralMovement",
      )

  // (11:53:15 AM) "The start player is Green and the second player is Pink. You made your
  // discards?"
  // "Yeah, we're ready to play."
  private val green
    get() = p1

  private val pink
    get() = p2

  @Test
  fun game20261111() {
    startGame()
    // Colonies and Turmoil give both players their fleets and lobby delegates through Admin's setup
    // effects. Starting TR comes from each player's CorporationOption, so we submit it.
    green.doTask("20 TerraformRating")
    pink.doTask("20 TerraformRating")

    green.doTasks(
        "3 CorporationCard",
        "4 PreludeCard",
        "10 ProjectCard<Selecting>",
        "-2 CorporationCard",
        "-2 PreludeCard",
        "-5 ProjectCard<Selecting>",
    )
    pink.doTasks(
        "3 CorporationCard",
        "4 PreludeCard",
        "10 ProjectCard<Selecting>",
        "-2 CorporationCard",
        "-2 PreludeCard",
        "-3 ProjectCard<Selecting>",
    )
    with(green) {
      // (11:54:46 AM) "Manutech, suffering a building tag, but I get a steel production which gives
      // me a steel. I hit 35 minus 15."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Manutech>>",
          "$Manutech FROM CorporationCard",
          "35 MC",
          "PROD[Steel]",
          "Steel",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(pink) {
      // (11:55:24 AM) "Pink is Helion. Get 42 money. I buy seven cards."
      // 11:55:25: Helion is represented by the explicitly requested FakeHelion.
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$FakeHelion>>",
          "$FakeHelion FROM CorporationCard",
          "42 MC",
          "PROD[3 Heat]",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    with(green) {
      // (11:56:10 AM) "We're ready for preludies. For my first preludy, Orbital Construction Yard,
      // I get one titanium production and five titanium."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$OrbitalConstructionYard>>",
          "$OrbitalConstructionYard FROM PreludeCard",
          "PROD[Titanium]",
          "5 Titanium",
      )
      // "For my other one, Rise to Power, I get three megacredit production, three megacredits, and
      // I get to choose three delegates anywhere I want them to go. We know Scientists is going to
      // happen when they meet, so I'll put one in Scientists to get in there ahead of them. As far
      // as what I want to be in power, Unity, Mars First, or do I want to take over my existing
      // ones? I'm just going to put one in Scientists, one in Unity, and..."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$RiseToPower>>",
          "$RiseToPower FROM PreludeCard",
          "PROD[3 MC]",
          "3 MC",
          "PartyLeader<Scientists>",
          "PartyLeader<Unity>",
          "PartyLeader<MarsFirst>",
      )
    }
    with(pink) {
      // (11:58:11 AM) "I think I got good preludes. Power Generation: increase energy production
      // three steps."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>",
          "$PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
      // (11:58:20 AM) "Gonna be trading. And Old Mining Colony: increase titanium production one
      // step. Place a colony, which will go on Luna for two money production. Discard a card."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$OldMiningColony>>",
          "$OldMiningColony FROM PreludeCard",
          "PROD[Titanium]",
          "Colony<Luna>",
          "PROD[2 MC]",
          "-ProjectCard",
      )
    }
    // (11:58:53 AM) "Now it's time for a picture. I hope that's got everything in the frame."
    // board-11-59-13.jpg, before action play. Helion's heat production was entered late.
    pink.assertResources(m = 21, s = 0, t = 0, p = 0, e = 0, h = 0)
    pink.assertProduction(m = 2, s = 0, t = 1, p = 0, e = 3, h = 3)
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)

    green.turn {
      // (11:59:26 AM) "Even though it's a building tag, it's probably worth it. Research Outpost,
      // costing me 18 of my 23 money, and I place a shitty tile."
      // "Sometimes it's not worth it to try and not play building tags because they're generally
      // good; it's easy to get the five anyways."
      // "I'm placing it at six two on the edge of the board to draw two cards."
      doTasks(
          *playCardTasks("$ResearchOutpost"),
          "Ok", // no steel paid
          "18 Pay<> FROM MC",
          "$ResearchOutpost FROM ProjectCard",
          "CityTile<Vastitas_6_2>",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (12:01:23 PM) "Pink places a guy from lobby in Kelvinists."
      // "With the first placement, dominance moved from Reds to Kelvinists."
      // "Yes. Oh, I forgot to give myself the three production."
      // "Pretty sure you got everything else?"
      // "Yeah."

      doTasks(
              "UseAction<LobbyAction, Action1>",
              "PartyFollower<Kelvinists> FROM LobbyDelegate",
          )
          .expect("-Dominant<Reds>, Dominant<Kelvinists>")
      // "And pays five to place a dude in Kelvinists and kick the party leader out."
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "5 Pay<> FROM MC",
              "PartyLeader<Kelvinists>",
          )
          .expect("-PartyLeader<Kelvinists, Neutral>, PartyFollower<Kelvinists, Neutral>")
    }
    green.turn {
      // (12:02:31 PM) "I'll play Vesta Shipyard for 14, which will be 4 titanium and 2 real. That
      // gives me a titanium production and a titanium."
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
          "Titanium",
      )
      // Remembered and accepted at 12:06:16–12:06:29. User authorizes recording the agreed lobby
      // action before passing; use this unused second action.
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
    }
    pink.turn {
      // (12:03:21 PM) "Now I can play my own building tag. Sponsored Mohole requires Kelvinists are
      // ruling or that you have two delegates there. I pay five, get two heat production."
      doTasks(
          *playCardTasks("$SponsoredMohole"),
          "5 Pay<> FROM MC",
          "$SponsoredMohole FROM ProjectCard",
          "PROD[2 Heat]",
      )
    }
    // (12:03:33 PM) "I pass."
    green.doTask("Pass")

    pink.turn {
      // (12:03:45 PM) "I pay two for Aerial Lenses, which is the same thing, except that you don't
      // lose any plants, and it's minus a point."
      // "And you're going to make a bunch of money when Kelvinists come into power."
      doTasks(
          *playCardTasks("$AerialLenses"),
          "2 Pay<> FROM MC",
          "$AerialLenses FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("Ok") // no plants removed
    }
    // (12:04:26 PM) "Then you pass after that?"
    // "Yeah."
    pink.doTask("Pass")

    // (12:04:28 PM) "We hit generation plus. That does production."
    green.doTasks("23 MC", "Steel", "2 Titanium")
    pink.doTasks("22 MC", "Titanium", "3 Energy", "7 Heat")
    green.assertResources(m = 26, s = 2, t = 4, p = 0, e = 0, h = 0)
    // (12:04:39 PM) "Then I choose world governmentation. I think I'll stick an ocean at 5.3.
    // Oceans plus one. Then we start moving all the colonies."
    // 12:04:45: World Government ocean at 5,3, with no player placement bonus or TR. Turmoil
    // removes a TR from both; Pink immediately regains hers as new chairman.
    // (12:05:02 PM) "No current event. Do we need to refer to the rules to do this in the right
    // order?"
    // "It wouldn't hurt. Scientific Community moves to current. Neutral in Scientists. We reveal
    // Free Academia Treaty, also known as Diversity. Add neutral to Scientists."
    // "I got demoted. Wait, we're not doing things in order. We were supposed to start by losing a
    // TR after production, then do the global event, but there wasn't one. Now Kelvinists come into
    // power. Pink becomes chairperson. Pink gets a TR."
    // Green: "Oh, I forgot to ever..."
    // Pink: "Use your lobby."
    // Green: "I would just put them in Greens. Can I just do that?"
    // Pink: "Sure."
    // Green: "I meant to use my free lobby action to put a person in there. That doesn't affect
    // anything."
    // "Dominance might have briefly moved to Reds, but it would have soon moved to Scientists.
    // Scientists are dominant. Follow the order. We should change the policy tile."
    // Green: "Would now be a good time for you to take your seven freaking money and me to take my
    // nothing?"
    // Pink: "Yeah. Seven plus. And TR for being chairman."
    // Green: "So you're back to 20 TR?"
    // Pink: "I believe that is all good, and I start."
    green
        .doTask("OceanTile<Vastitas_5_3>! BY Admin")
        .expect(
            "-TerraformRating<Green>, 0 TerraformRating<Pink>, 0 Plant<Green>, 7 MC<Pink>, " +
                "-Chairman<Neutral>, Chairman<Pink>, 2 PartyDelegate<Neutral, Scientists>, " +
                "-PartyLeader<Scientists, Green>, -Dominant<Kelvinists>, Dominant<Scientists>, -Ruling<Greens>, " +
                "Ruling<Kelvinists>, Current<ExploreFirstDirective>, Coming<HeatFirstPolicy>, " +
                "Distant<FreeAcademiaTreaty>",
        )

    // green-applog-01 entry 27: we don't know why green did this, maybe a stray tap
    green.exMachina("PROD[-MC]")

    // (12:08:06 PM) "Generation becomes two, button moves to Pink. We would take cubes off cards if
    // we had them, and we've done all our colony stuff already. We're good for research. Two for
    // you, two for me, two for you, two for me. We usually would have taken a picture by now, so
    // we're taking the picture during research."
    // 12:08:25: both research offers were dealt before the photograph.
    green.doTask("4 ProjectCard<Selecting>")
    pink.doTask("4 ProjectCard<Selecting>")

    // board-12-08-47.jpg: Research has begun, neither player has bought cards yet.
    green.assertResources(m = 26, s = 2, t = 4, p = 0, e = 0, h = 0)
    green.assertProduction(m = 2, s = 1, t = 2, p = 0, e = 0, h = 0)
    green.assertCounts(
        19 to "TerraformRating",
        5 to "ProjectCard",
    )
    pink.assertResources(m = 38, s = 0, t = 1, p = 0, e = 3, h = 7)
    pink.assertProduction(m = 2, s = 0, t = 1, p = 0, e = 3, h = 7)
    pink.assertCounts(
        20 to "TerraformRating",
        4 to "ProjectCard",
    )

    // (12:10:55 PM) "Time to go into stupid mode. Buying all four cards."
    green.doTasks(
        "Ok", // no research cards rejected
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    // (12:11:08 PM) "I'm buying two. I will play Research Colony for titanium and citrine real."
    pink.doTasks(
        "-2 ProjectCard<Selecting>",
        "Ok", // no heat paid
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.turn {
      // (12:11:37 PM) "Place a colony; may be placed where you already have a colony. Luna for two
      // money production. Would have been fun to do that last gen. Draw two cards."
      // "You paid the 20 money for that? Draw yourself two cards."
      doTasks(
          *playCardTasks("$ResearchColony"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "17 Pay<> FROM MC",
          "$ResearchColony FROM ProjectCard",
          "Colony<Luna>",
          "PROD[2 MC]",
          "2 ProjectCard",
      )
    }
    green.turn {
      // (12:12:13 PM) "I'm playing Smasteroid Rites. It costs me nine, which I will pay as three
      // titanium. I gain two asteroids on Asteroid Rights."
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "3 Pay<Class<Titanium>> FROM Titanium",
      )

      doTask("Ok") // no MC paid
      doTasks(
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
      )
    }
    pink.turn {
      // (12:12:32 PM) "Do you need to refill the lobby?"
      // "Yes."
      // Pink: "Lobby and reserve guy both in Mars First."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<MarsFirst> FROM LobbyDelegate",
      )
      // Green: "That kicks me out of the leadership for Mars First, and you spend five. You did
      // spend five already?"
      // Pink: "I will get that spent."
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "Ok", // no heat paid
              "5 Pay<> FROM MC",
              "PartyLeader<MarsFirst>",
          )
          .expect("-PartyLeader<MarsFirst, Green>, PartyFollower<MarsFirst, Green>")
    }
    green.turn {
      // (12:13:12 PM) "Probably best if I spend 10 to raise my energy and heat production, which
      // gives me an energy and a heat."
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "PROD[Energy]",
          "PROD[Heat]",
          "Energy",
          "Heat",
      )
    }
    pink.turn {
      // (12:14:06 PM) "If I do that too, I have just enough. Spend 10 money, gain energy production
      // and heat production."
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "Ok", // no heat paid
          "10 Pay<> FROM MC",
          "PROD[Energy]",
          "PROD[Heat]",
      )
    }
    green.turn {
      // (12:14:15 PM) "I'm going to use Asteroid Rights to remove an asteroid. I think I'll take
      // the money production. And the money."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "PROD[MC]",
          "MC",
      )
    }
    // (12:14:33 PM) "Oh yeah, Manutech. I pass."
    pink.doTask("Pass")

    green.turn {
      // (12:14:50 PM) "I use my free lobby action. Let's put someone in Unity. I have a second
      // person in Unity now."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Unity> FROM LobbyDelegate",
      )
    }
    // "Then I think I'm out of stuff to do."
    // "So am I."
    green.doTask("Pass")

    // (12:15:18 PM) "We hit production."
    green.doTasks("22 MC", "Steel", "2 Titanium", "Heat FROM Energy", "Energy", "Heat")
    pink.doTasks("24 MC", "Titanium", "3 Heat FROM Energy", "4 Energy", "8 Heat")

    // "You choose your world governmentation, then I take a picture."
    // "Venus. Maybe that's not the smartest. Sure, why not? Venus."
    // "Venus to 2%. We don't have to type anything for that. Then the picture, and then we do
    // colonies."

    // WGT starts Turmoil, but its global-event payouts still await the players below. Check
    // government formation on the last payout, when it actually happens.
    pink.doTask("VenusStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // board-12-16-00.jpg: resource/tableau snapshot after Venus WGT, before event payouts.
    green.assertResources(m = 27, s = 3, t = 3, p = 0, e = 1, h = 3)
    green.assertProduction(m = 3, s = 1, t = 2, p = 0, e = 1, h = 1)
    green.assertCounts(
        8 to "ProjectCard",
        1 to "Asteroid<$AsteroidRights>",
    )
    pink.assertResources(m = 24, s = 0, t = 1, p = 0, e = 4, h = 18)
    pink.assertProduction(m = 4, s = 0, t = 1, p = 0, e = 4, h = 8)
    pink.assertCounts(7 to "ProjectCard")
    // (12:16:08 PM) "All players lose one TR. Perform the current global event: gain one money for
    // each card in hand, no limit, and influence."
    // "We each have one influence. You because you're chairman, me because I'm in Scientists."
    // Pink: "Seven cards, so I get eight monies."
    // Green: "I have eight cards, so I get nine monies."
    // 12:16:33–12:16:42: seven/eight cards, plus an influence each.
    green.doTask("9 MC")
    // (12:16:51 PM) "Dominant party becomes ruling. Change policy tile. Scientists. Chairman earns
    // a TR."
    // "That's nobody. Dominance is now Mars First. Move events; we get a neutral Green. Pandemic
    // was revealed, so I get kicked out of the leadership of Greens. I've gotten kicked out as the
    // leader of almost everything. We're getting punished for building tags again?"
    // "At least it's not later."
    // Completing the event forms the Scientists government: Research Outpost and Research Colony
    // each earn an automatic 1 MC ruling bonus.
    pink
        .doTask("8 MC")
        .expect(
            "-Ruling<Kelvinists>, Ruling<Scientists>, -Chairman<Pink>, " +
                "Chairman<Neutral>, -Dominant<Scientists>, Dominant<MarsFirst>, Current<HeatFirstPolicy>, " +
                "Coming<FreeAcademiaTreaty>, Distant<ViralModificationsApproved>, -PartyLeader<Greens, Green>, " +
                "PartyFollower<Greens, Green>",
        )
    // Neither final app log includes this bonus: Green 36, Pink 32 at the pause.
    green.exMachina("-MC")
    pink.exMachina("-MC")
    green.assertResources(m = 36, s = 3, t = 3, p = 0, e = 1, h = 3)
    pink.assertResources(m = 32, s = 0, t = 1, p = 0, e = 4, h = 18)
    green.assertCounts(18 to "TerraformRating", 8 to "ProjectCard")
    pink.assertCounts(19 to "TerraformRating", 7 to "ProjectCard")

    // (12:18:18 PM) "Generation moves, button moves, cubes come off cards. I'd like to pause and
    // have you send me screencaps and get my translation started. I'm pretty sure I've already made
    // some mistakes, and I want to find out what they are."
    // At the pause, the players confirm correcting the discrepancies found above. Keep the original
    // screenshots' values above and apply the repairs when they occurred.
    pink.exMachina("MC")
    green.exMachina("PROD[MC], 2 MC")
    pink.assertCounts(33 to "MC")
    green.assertCounts(38 to "MC")
    green.assertProduction(m = 4, s = 1, t = 2, p = 0, e = 1, h = 1)

    // (12:56:15 PM) "Green has eight cards in hand. Three of my cards have requirements. Pink has
    // seven cards in hand. We're ready to draft. Two for you, two for me, two for you, two for me."
    // Green: "I'll buy one card."
    // Pink: "I will buy two cards."
    // transcript-next.md, 12:56–12:58: eight cards for Green, seven for Pink. Green forgot this
    // payment at the table. Per user request, pay it normally here. His ledger remains 3 MC above
    // the replay through generation 3; the checkpoints below use the corrected balance, so no
    // manual grant restores the omitted payment.
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    // Pink forgot this 6 MC payment, then corrected it before production at 13:18–13:19 by
    // cancelling her last Martian Media Center action and paying the research bill. Per user
    // request, pay here and omit that cancelled action. Her ledger is 6 MC above this replay during
    // the generation (3 MC while the cancelled action is recorded), then agrees again before
    // production.
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "Ok", // no heat paid
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.turn {
      // (1:01:01 PM) "I think I might as well play Convoy from Europa for 14, which I'm going to
      // spend as 3 titanium and 5 real. I'm going to place an ocean tile, so ocean goes up and my
      // TR goes up. I'm probably going to place it 6,7 for a card. I'm drawing that card now. Oh,
      // and I get to draw a card because of Convoy from Europa. That's my turn."
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "OceanTile<Vastitas_6_7>",
          "TerraformRating",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (1:03:03 PM) "We should have refilled the lobby. We've been forgetting that."
      // "Next time let's follow the whole thing, because we missed two things at least."
      // "I'm gonna lobby in Kelvinists. I've taken the party leadership."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Kelvinists> FROM LobbyDelegate",
      )
    }
    green.turn {
      // (1:04:26 PM) "I should have taken my free titanium first, but I didn't. I'll use Asteroid
      // Rights, remove an asteroid, and take two titanium."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }
    pink.turn {
      // (1:04:43 PM) "I'm going to do a standard project asteroid. I'm going to hit the asteroid
      // standard project button, then manually remove two heat and gain two money. Temp is at minus
      // 28."
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "2 Pay<Class<Heat>> FROM Heat",
          "12 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (1:06:06 PM) "I think I'll play Mars Nomads for 12. I'm going to put it on 6-9."
      doTasks(
          *playCardTasks("$MarsNomads"),
          "12 Pay<> FROM MC",
          "$MarsNomads FROM ProjectCard",
          "NomadsMarker<Vastitas_6_9>",
      )
      // "For my second action, I'm going to use Mars Nomads to move it to 5-9 and draw my two
      // cards."
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_5_9 FROM Vastitas_6_9>",
          "2 ProjectCard",
      )
    }
    // Pink's accidental heat-production decrease is immediately reversed in the log. The actual
    // increase below comes from reaching -24 degrees.
    // (1:07:13 PM) "I have 16 heat with which to do two heat boops. Accidentally decreased my heat
    // production by one."
    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "Another heat boop, which increases my heat production one. We're at negative 24."
      // "And you got your bonus heat production?"
      // "Yeah."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }
    green.turn {
      // (1:07:39 PM) "I'm going to play Mineral Deposit and spend four to take five steel."
      doTasks(
          *playCardTasks("$MineralDeposit"),
          "4 Pay<> FROM MC",
          "$MineralDeposit FROM ProjectCard",
          "5 Steel",
      )
    }
    // (1:08:29 PM) "I may as well pay seven. I've got my two delegates in Mars First, so I gain two
    // money production."
    // 13:08: the spoken card name is missing; board-13-19-37.jpg identifies it.
    pink.turn {
      doTasks(
          *playCardTasks("$MartianMediaCenter"),
          "7 Pay<> FROM MC",
          "$MartianMediaCenter FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    green.turn {
      // (1:08:55 PM) "I want to play Corporate Stronghold, paying just five steel. I lose an energy
      // production. I gain three megacredit production, which gives me three megacredits. Then I
      // have to place a shitty tile. I think we'll go to 3-5 for one plant."
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "3 MC",
          "CityTile<Vastitas_3_5>",
          "Plant",
      )
    }
    pink.turn {
      // (1:10:31 PM) "I'm gonna spend three energy to trade with Luna for seven, two, two: eleven
      // money."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "11 MC",
      )
    }
    green.turn {
      // (1:11:23 PM) "Hermetic Order of Mars costs me nine. I get two money production, which gives
      // me two money, and then I get ten money."
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "9 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "12 MC",
      )
    }
    pink.turn {
      // (1:12:17 PM) "I will spend a titanium and three real for Rotator Impacts."
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
      )
    }
    green.turn {
      // (1:12:53 PM) "Corroder Suits for seven. It gives me another two money production and two
      // money."
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "7 Pay<> FROM MC",
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "2 MC",
      )
    }
    pink.turn {
      // (1:12:54 PM) "Get those tags going. I'm gonna spend six to add an asteroid to Rotator
      // Impacts."
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }
    green.turn {
      // (1:13:10 PM) "More importantly, it gives me a tag. I think I'll put my lobby dude in
      // Greens."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Greens> FROM LobbyDelegate",
      )
      // "And then spend five to take over the Greens. Greens also become dominant."
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "5 Pay<> FROM MC",
              "PartyLeader<Greens>",
          )
          .expect("-PartyLeader<Greens, Neutral>, PartyFollower<Greens, Neutral>")
    }
    pink.turn {
      // (1:14:20 PM) "Pay nine for Sponsored Academies, and draw a card. Oh wait, I draw three
      // cards, you draw one. I thought I was doing the University thing."
      // "There's your three, and here's my one."
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "3 ProjectCard",
          "EACH",
          "ProjectCard<Green, Hand>",
      )
    }
    green.turn {
      // (1:16:04 PM) "For my turn, I will spend the ten to get three more cards."
      doTasks(
          "UseAction<ScientistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "3 ProjectCard",
      )
    }
    pink.turn {
      // (1:16:30 PM) "I spend a money on CEO's Favorite Project. Add a resource to a card with at
      // least one. I add an asteroid to Rotator Impacts."
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "Pay<> FROM MC",
          "$CeosFavoriteProject FROM ProjectCard",
          "Asteroid<$RotatorImpacts>",
      )
    }
    // (1:17:42 PM) "I don't have many options left. I think I'm prepared to pass."
    green.doTask("Pass")

    // (1:17:52 PM) Pink: "Martian Media Center: spend three to place a delegate in Scientists, and
    // it becomes party leader. Now I pass."
    // Green: "We hit production."
    // Pink: "Whoops, wait. I forgot to buy my freaking cards."
    // Green: "Now you wouldn't have had enough money to do it. We'll have to let it go, but after
    // you do production... Is that enough to fix it? Okay, we're making it so that you did not use
    // Martian Media Center."
    // Pink: "I just don't use this."
    // Green: "Scientists is still empty, and because you bought two cards, that way you have just
    // enough."
    // Pink: "The slider has fewer options now. Don't want to cheat."
    // The players canceled this action to cover Pink's forgotten research payment. The replay pays
    // the 6 MC during research and omits this action and its delegate.
    pink.assertCounts(0 to "MC")
    // (1:19:15 PM) "Then hit your production."
    // "Now I pass. Pro Duck."
    pink.doTask("Pass")

    // Green's app groups Corroder +2, delegate -5, Scientists -10, production +30 as +17.
    green.doTasks("30 MC", "Steel", "2 Titanium", "Heat FROM Energy", "Heat")
    pink.doTasks("28 MC", "Titanium", "Heat FROM Energy", "4 Energy", "9 Heat")
    // (1:19:22 PM) "I am going to increase Venus to four percent for my world government. Then it's
    // picture time. Then colonies go zhwee, and trade fleet goes back. Let's do Turmoil carefully,
    // one step at a time. Read it off the thing."
    green.doTask("VenusStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // board-13-19-37.jpg and both app logs, before Global Dust Storm. Green's 30 MC is the
    // photographed/logged 33 MC less the retconned research payment.
    green.assertResources(m = 30, s = 4, t = 4, p = 1, e = 0, h = 5)
    green.assertProduction(m = 11, s = 1, t = 2, p = 0, e = 0, h = 1)
    green.assertCounts(0 to "Asteroid<$AsteroidRights>")
    pink.assertResources(m = 28, s = 0, t = 1, p = 0, e = 4, h = 10)
    pink.assertProduction(m = 6, s = 0, t = 1, p = 0, e = 4, h = 9)
    pink.assertCounts(2 to "Asteroid<$RotatorImpacts>")
    // (1:19:54 PM) "All players lose one TR. Perform the current global event."
    // Green: "I'm down to 18 TR. Looks like you have no influence, and I have two. So I only lose
    // two money."
    // Pink: "I lose my 10 heat and four monies."
    // Green: "Oh, I lose two money and five heat."
    // Influence 2 for Green, 0 for Pink; three/two building tags respectively.
    green.doTasks("-5 Heat", "-2 MC")
    pink.doTask("-10 Heat")
    // (1:20:36 PM) "The dominant party becomes ruling. Change policy tiles: Greens."
    // Green: "I get no bonus, but Greens are now in power."
    // Pink: "Me neither. Dominant party leader becomes chairman."
    // "Green becomes chairman and gains one TR. Remove the other delegates, shift dominance,
    // restore lobby. Move coming global to current, add neutral. We get a neutral Scientist.
    // Scientist party leader is neutral. Reveal and add a neutral Kelvinist. We revealed Generous
    // Funding."
    pink
        .doTask("-4 MC")
        .expect(
            "TerraformRating<Green>, -Ruling<Scientists>, Ruling<Greens>, -Chairman<Neutral>, " +
                "Chairman<Green>, -Dominant<Greens>, Dominant<MarsFirst>, Current<FreeAcademiaTreaty>, " +
                "Coming<ViralModificationsApproved>, Distant<TerraformingLobbying>",
        )
    green.assertResources(m = 28, s = 4, t = 4, p = 1, e = 0, h = 0)
    pink.assertResources(m = 24, s = 0, t = 1, p = 0, e = 4, h = 0)
    green.assertCounts(19 to "TerraformRating", 11 to "ProjectCard")
    pink.assertCounts(21 to "TerraformRating")

    // (1:21:47 PM) "I have so little TR. Generation marker moves, the button moves, cubes come off
    // cards. We would be ready to draft, but we're pausing. I have 11 cards in hand."
    // transcript-afternoon.md, 16:53–16:54: Pink buys a card; Green buys two.
    // (4:51:38 PM) "I'm gonna buy one card."
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    // (4:54:46 PM) Green: "I mean two cards."
    // Pink: "I started."
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.turn {
      // (4:56:05 PM) "Minority Refuge for five, which I will pay with a titanium and two real.
      // Decrease money production two steps, place a colony. This will be Triton for three
      // titanium."
      doTasks(
          *playCardTasks("$MinorityRefuge"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$MinorityRefuge FROM ProjectCard",
          "PROD[-2 MC]",
          "Colony<Triton>",
          "3 Titanium",
      )
      // "I'll spend three energy to trade with Triton for four titanium."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "4 Titanium",
      )
    }
    green.turn {
      // (4:56:47 PM) "I'm going to do Solar Logistics for 19, which I pay as 12 worth of titanium
      // and seven real, and it gives me two titanium."
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
      )
      // (4:57:19 PM) "Then Spin-Inducing Asteroid. 15, of which I pay six worth of titanium and
      // nine real. Venus goes up two steps to eight percent. I get two TR and two cards."
      // "One from the bonus track and one from Solar Logistics."
      doTasks(
          *playCardTasks("$SpinInducingAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "9 Pay<> FROM MC",
          "$SpinInducingAsteroid FROM ProjectCard",
          "2 VenusStep",
          "2 TerraformRating",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (4:57:57 PM) "Acquired Company: gain three money production."
      // The transcript omits the payment; pink-applog-05 entry 97 records ten MC.
      doTasks(
          *playCardTasks("$AcquiredCompany"),
          "10 Pay<> FROM MC",
          "$AcquiredCompany FROM ProjectCard",
          "PROD[3 MC]",
      )
    }
    green.turn {
      // (4:59:30 PM) "I'm going to need to sell a patent."
      // 16:59:30: the user-approved retcon sells an additional patent, making Legend affordable
      // after paying for research.
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
    }
    pink.turn {
      // (4:59:42 PM) "Use Rotator Impacts, remove an asteroid, raise Venus a step, and I get TR."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (5:00:06 PM) "Indentured Workers."
      assertCounts(8 to "MC")

      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
      )
      // "Can you guess what I'm gonna pay for now? Legend of Zelda. I played Indentured Workers and
      // then I funded the Legend milestone."
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Legend",
      )
      assertCounts(0 to "MC")
    }
    pink.turn {
      // (5:01:36 PM) "I will use Lobby to send a guy to Greens."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Greens> FROM LobbyDelegate",
      )
    }
    green.turn {
      // (5:02:25 PM) "Tectonic Stress Power costs nine, which I'm paying eight worth of steel. I
      // get three energy production and three energy."
      // At 17:02 the table played TSP without its required second science tag. User-approved
      // retcon: move Nomads earlier, then play Solar Wind Power instead. Research Outpost and
      // Indentured Workers reduce SWP to 2 MC; a titanium pays it.
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_6_9 FROM Vastitas_5_9>",
          "2 Titanium",
      )
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC paid; titanium deliberately overpays the 2 MC cost by 1
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "Energy",
          "2 Titanium",
      )
    }
    // (5:02:31 PM) "I don't really have much to do, so I'll pass."
    pink.doTask("Pass")

    green.turn {
      // (5:03:02 PM) "I might as well spend the three energy to pick up the six steel from Ceres.
      // I'm not going to use it yet, but I need to use my lobby action. And Asteroid Rights: I
      // gotta pay one to put an asteroid on it. And I gotta use Nomads. I could have used that
      // titanium earlier. I'm moving it to 6-9 to get two titanium."
      // "Dominance moves to your left, clockwise, right? It'll go to Kelvinists. Well, I'm going to
      // put my lobby person in Unity."
      // "Did I reach the end of everything I can do? I did my lobby action, I can't convert stuff,
      // I've used all my card actions, and I don't have enough money to do anything. And I flew my
      // boat. It's crazy that we still haven't gotten those colonies. Ready to hit production?"
      // Omit the Ceres trade that depended on TSP's energy and, as selected by the user, the 1 MC
      // Asteroid Rights refill. Nomads has already moved; retain the Unity lobby.
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Unity> FROM LobbyDelegate",
      )
    }
    green.doTask("Pass")

    green.doTasks("32 MC", "Steel", "2 Titanium", "Heat FROM Energy", "Energy", "Heat")
    pink.doTasks("29 MC", "Titanium", "Heat FROM Energy", "4 Energy", "9 Heat")
    green.assertCounts(21 to "TerraformRating")
    pink.assertCounts(22 to "TerraformRating")
    // (5:05:45 PM) "You must world governmentalize."
    // "I think I will oxygenate."
    // "Oxygen is up to one percent."
    pink.doTask("OxygenStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // (5:06:02 PM) "Now it's time for the picture, then colonies. Oops, why did I move it down? Up,
    // up, up. Luna is on the 10 money spot now. Walk us through Turmoil."
    // "All players lose one TR. Perform the current global event."
    // board-17-06-16.jpg and the final app logs anchor the original production checkpoint. Green's
    // resources/tableau below incorporate the agreed SWP/no-trade retcon; Pink's values still match
    // the photo. Automatic Turmoil has already removed each TR.
    green.assertResources(m = 32, s = 5, t = 5, p = 1, e = 1, h = 2)
    green.assertProduction(m = 11, s = 1, t = 2, p = 0, e = 1, h = 1)
    green.assertCounts(
        1 to "$SolarWindPower",
        0 to "$TectonicStressPower",
        1 to "Legend",
        0 to "Asteroid<$AsteroidRights>",
        2 to "ScienceTag",
    )
    pink.assertResources(m = 38, s = 0, t = 8, p = 0, e = 4, h = 10)
    pink.assertProduction(m = 7, s = 0, t = 1, p = 0, e = 4, h = 9)
    pink.assertCounts(1 to "Asteroid<$RotatorImpacts>")
    // (5:06:45 PM) Green: "Do we agree that I have two influence?"
    // Pink: "And I have two."
    // Green: "I have ten, in fact, so I made it easily. I get 10 money for having enough tags and
    // influence."
    // Pink: "I have five unique tag types. Just wasn't in the cards."
    // 17:06–17:08: Diversity still pays only Green. Without TSP's building tag, Mars First pays
    // Green 3 MC rather than the recorded 4; Pink's outcome is unchanged.
    green.doTask("10 MC")
    // (5:07:32 PM) Pink: "The dominant party becomes ruling. Change policy tile. We each get a
    // money per building. I have two."
    // Green: "I have four. I'm going to get screwed in a minute, but four for now."
    // Pink: "I become chairman. I earn TR."
    pink
        .doTask("Ok") // no Diversity payout: five distinct tags plus two influence
        .expect(
            "3 MC<Green>, 2 MC<Pink>, TerraformRating<Pink>, -Ruling<Greens>, Ruling<MarsFirst>, " +
                "-Chairman<Green>, Chairman<Pink>, -Dominant<MarsFirst>, Dominant<Unity>, " +
                "Current<ViralModificationsApproved>, Coming<TerraformingLobbying>, Distant<DemocraticReform>",
        )
    green.assertResources(m = 45, s = 5, t = 5, p = 1, e = 1, h = 2)
    // Corrected hand count: 11 before Research + 2 purchases + 2 draws - 4 plays - 2 sales.
    green.assertCounts(20 to "TerraformRating", 9 to "ProjectCard")
    pink.assertResources(m = 40, s = 0, t = 8, p = 0, e = 4, h = 10)
    pink.assertCounts(22 to "TerraformRating")
    // (5:08:08 PM) "Kelvinists become dominant. Nope, Unity. That was the point. Unity becomes
    // dominant. Restore lobby. Move coming to current. A neutral Mars First person, and a neutral
    // Mars First person. What was just revealed is Aquifer Released by Public Council. The current
    // event this next generation is going to be Pandemic. Generation moves. Button moves. Cubes off
    // cards."

    // Play resumed on November 12, before generation 5.
    // (3:34:23 PM) Green: "We're going to make it as if I didn't play Tectonic Stress Power, but
    // played Solar Wind Power instead, and undo the trade with Ceres. Return Tectonic Stress Power
    // to my hand, move Solar Wind Power into my tableau, and sell an extra patent. I should finish
    // with nine cards in hand; I do. Remove the asteroid from Asteroid Rights because I wouldn't
    // have had the one money to do that. Then I lose one money and two steel, because I never got
    // them from trade, but I never spent them either. Steel goes from seven to five. Titanium goes
    // from four to five. Plants unchanged. I have two fewer energy and one more heat, because of
    // the power generator, and my energy production is down too. Is that good enough for you for
    // now?"
    // Pink: "Yeah. I assume you wouldn't have spent your energy on the trade."
    // Green: "I wouldn't have done a trade, so we have to send this back to eight. I lost six
    // steel, but gained back the four I never ended up spending. Now we're ready for Generation 5."
    // transcript-middle.md, 15:35–15:37: both players confirm applying the repair above. Green
    // confirms nine cards in hand after the repair. green-applog-05 entries 110–115 confirm all six
    // repair deltas, including energy production.
    // (3:37:59 PM) "Two for you, two for me. We have building tags coming up."
    // "I noticed Red Appeasement has a red star in the requirements. I thought it was a requirement
    // relating to the Reds, but no. I love Vote of No Confidence in solo games."
    // "If I know you, there's never going to be a neutral chairman."
    // "Usually players keep doing the thing. And with Red Appeasement, you're the one who'd benefit
    // from the Reds. I don't like that you have that much money production."
    // "You make almost as much money as me anyway. I'm going to actually buy my cards. Buy two
    // cards."
    // "It goes max five, then reduced by influence. So once you have five already, there's no harm
    // in putting more."
    // "Yes, that is how that works."
    // 15:43–15:45: Green buys two; Pink buys three.
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // (3:45:34 PM) "Keep three cards."
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "Ok", // no heat paid
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.turn {
      // (3:46:00 PM) "Tectonic Stress Power. This time I do have the two science tags. It costs 17,
      // which I'll pay as 10 worth of steel and 7 real. That gives me three energy production and
      // three energy."
      doTasks(
          *playCardTasks("$TectonicStressPower"),
          "5 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$TectonicStressPower FROM ProjectCard",
          "PROD[3 Energy]",
          "3 Energy",
      )
      // "For my second action, I'm going to fly my boat to Ceres and get even more than before. I
      // lose three energy and get eight steel."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "8 Steel",
      )
    }
    pink.turn {
      // (3:46:50 PM) "I'm gonna use Rotator Impacts, remove the thingy, raise Venus a step, which
      // gives me a TR."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      // "Then Venus Allies."
      // "You made that card pay off. Pay for it first."
      // "I'm trying to decide if I hold back all my... I'm gonna live on the edge. Spend it all as
      // real money. Raise Venus two steps; the 16 gives me a bonus. Gain four money per colony, so
      // that's 12. That was two actions."
      doTasks(*playCardTasks("$VenusAllies"))
      // Titanium and heat payments both offer Ok; select titanium before declining it.
      selectTask("X! Pay<Class<Titanium>> FROM Titanium OR Ok")
      doTasks(
          "Ok", // no titanium paid
          "Ok", // no heat paid
          "30 Pay<> FROM MC",
          "$VenusAllies FROM ProjectCard",
          "2 VenusStep",
          "3 TerraformRating",
          "12 MC",
      )
    }
    green.turn {
      // (3:48:27 PM) "I'll play Floating Refinery for six. Because I have two Venus tags, it puts
      // two floaters on itself. Oops, those aren't floaters. Where are the squares? Titan has now
      // come into play."
      // 15:48: the diarization says Ellie, but all three photos put Floating Refinery in Green's
      // tableau.
      doTasks(
          *playCardTasks("$FloatingRefinery"),
          "6 Pay<> FROM MC",
          "$FloatingRefinery FROM ProjectCard",
          "2 Floater<$FloatingRefinery>",
      )
    }
    pink.turn {
      // (3:49:25 PM) "My lobby guy to Reds."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Reds> FROM LobbyDelegate",
      )
      // "Martian Media Center, spend three, go to Reds. That kicked out the neutral leader."
      doTasks(
              *cardActionTasks("$MartianMediaCenter"),
              "Ok", // no heat paid
              "3 Pay<> FROM MC",
              "PartyLeader<Reds>",
          )
          .expect("-PartyLeader<Reds, Neutral>, PartyFollower<Reds, Neutral>")
    }
    green.turn {
      // (3:50:22 PM) "I'm going to use Mars Nomads, move back to 5-9 again and draw my two cards.
      // They're not good."
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_5_9 FROM Vastitas_6_9>",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (3:50:28 PM) "I will spend three energy to trade with Luna. I get 10 plus two bonuses of
      // two, so 14 money."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "14 MC",
      )
      // "I may as well pay eighteen for Aquifer Pumping."
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "Ok", // no heat paid
          "18 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
    }
    green.turn {
      // (3:51:38 PM) "Cupola City for seven steel and one real. We are under nine percent. I lose
      // an energy production, gain three megacredit production and three megacredits, and place a
      // shitty tile. Where do I want to put it? At 2,3 for nothing. No bonus."
      doTasks(
          *playCardTasks("$CupolaCity"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Pay<> FROM MC",
          "$CupolaCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "3 MC",
          "CityTile<Vastitas_2_3>",
          "Steel",
      )
      // green-applog-05/06: Cupola leaves 1 steel; Mining adds 2, Power Plant spends 1. The Mars
      // First placement steel was omitted at the table.
      exMachina("-Steel")
    }
    pink.turn {
      // (3:52:36 PM) "Spend six money and two heat on Aquifer Pumping to place an ocean for two
      // money and a plant."
      // "Two money and a plant, and two money because it went on 6-6."
      // "No, two heat and a plant. The whole heat-for-money thing. Wait, one more action."
      // 15:52: six MC and two heat, ocean at 6,6. Its TR was remembered at 15:54; resolve it here,
      // with the same final balance and no intervening TR-dependent action.
      doTasks(*cardActionTasks("$AquiferPumping"))
      doTasks(
          "2 Pay<Class<Heat>> FROM Heat",
          "6 Pay<> FROM MC",
          "OceanTile<Vastitas_6_6>",
          "TerraformRating",
          "2 Heat",
          "Plant",
          "2 MC",
          "Steel",
      )
      // pink-applog-06 and board-15-58-19 show 0 steel: this Mars First bonus was also missed.
      exMachina("-Steel")
      // (3:53:30 PM) "Red Appeasement, and no other player has passed."
      // "I haven't passed yet, so you can't play that. Oh, no other player. Right."
      // "I pay no money, get two money production, and I pass."
      doTasks(
          // The required Pass below completes this card action.
          *playCardTasks("$RedAppeasement"),
          "$RedAppeasement FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("Pass")
    }
    green.turn {
      // Pink has passed through Red Appeasement; Green completes the generation.
      // (3:53:49 PM) Green: "I don't really have a plan here. I guess I'll play Mining Expedition
      // for 11, which raises oxygen to 2 and gives me a TR."
      // Pink: "I think I forgot to give myself the TR for my ocean."
      // Green: "We're at three oceans now?"
      // Pink: "Yes."
      // Green: "And you can lose your one plant."
      doTasks(
          *playCardTasks("$MiningExpedition"),
          "11 Pay<> FROM MC",
          "$MiningExpedition FROM ProjectCard",
          "OxygenStep",
          "TerraformRating",
          "-Plant<Pink>",
          "2 Steel",
      )
      // (3:54:34 PM) "Then Supermarkets for eight. That gives me two money production and two
      // money. We do have two city tiles in play."
      doTasks(
          *playCardTasks("$Supermarkets"),
          "8 Pay<> FROM MC",
          "$Supermarkets FROM ProjectCard",
          "PROD[2 MC]",
          "2 MC",
      )
      // (3:54:59 PM) "I might as well do Power Plant for three, except I'll spend one money and one
      // steel. I get an energy production and an energy. I have nothing to convert. I have a lobby
      // action to do."
      doTasks(
          *playCardTasks("$PowerPlant"),
          "Pay<Class<Steel>> FROM Steel",
          "Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
          "Energy",
      )
      // (3:55:21 PM) "And use Floating Refinery to remove two floaters, take one titanium and two
      // money."
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      // "Then use Asteroid Rights, spend one money to put an asteroid on it. Then my lobby dude."
      doTasks(
          *cardActionTasks("$AsteroidRights"),
          "Pay<> FROM MC",
          "Asteroid<$AsteroidRights>",
      )
      // (3:56:37 PM) "I'm gonna move the lobby dude to Reds, which makes Reds dominant."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Reds> FROM LobbyDelegate",
      )
      // (3:56:49 PM) "I could add two more lobby dudes. That'd be pretty expensive. What the hell,
      // I'm gonna do it. I'm spending 10, adding a regular dude and then a leader, kicking out the
      // Pink leader."
      doTasks(
          "UseAction<PaidLobbyAction, Action1>",
          "5 Pay<> FROM MC",
          "PartyFollower<Reds>",
      )
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "5 Pay<> FROM MC",
              "PartyLeader<Reds>",
          )
          .expect("-PartyLeader<Reds, Pink>, PartyFollower<Reds, Pink>")
    }
    // (3:57:17 PM) "Now I'm out of stuff to do. I've flown my boat, converted things I can convert,
    // I have money for cards, and I've used all my actions. Production?"
    green.doTask("Pass")

    green.doTasks("37 MC", "Steel", "2 Titanium", "2 Heat FROM Energy", "4 Energy", "Heat")
    pink.doTasks("36 MC", "Titanium", "Heat FROM Energy", "4 Energy", "9 Heat")
    // "Then I need to world governmentalize. How about an ocean at 5-8?"
    green
        .doTask("OceanTile<Vastitas_5_8>! BY Admin")
        .expect("-TerraformRating<Green>, -TerraformRating<Pink>, 0 MC<Green>, 0 Plant<Green>")
    // (3:58:06 PM) "Oceans are at four now. Picture time. Colonies: two boats return, everything
    // has slid up. Then Turmoil: all players lose one TR."
    // board-15-58-19.jpg: after production/WGT, before Pandemic and the Reds government.
    green.assertResources(m = 38, s = 3, t = 8, p = 1, e = 4, h = 5)
    green.assertProduction(m = 16, s = 1, t = 2, p = 0, e = 4, h = 1)
    pink.assertResources(m = 38, s = 0, t = 9, p = 0, e = 4, h = 20)
    pink.assertProduction(m = 9, s = 0, t = 1, p = 0, e = 4, h = 9)
    green.assertCounts(
        1 to "Asteroid<$AsteroidRights>",
        0 to "Floater<$FloatingRefinery>",
    )
    // (3:58:46 PM) Pink: "Perform the current global event, taking influence into account. I
    // believe we each have two, so I lose three."
    // Green: "I lose three times three. You mean you only lose three money? I lose nine money."
    green.doTask("-9 MC")
    // (3:59:22 PM) Pink: "The dominant party becomes ruling. Change policy tile. You get a TR
    // bonus. You become chairman and earn another. Move everything else out and restore the lobby."
    // Green: "That gives me a TR, right? I'm now at 21 TR. I become chairman, and dominance moves
    // to Unity."
    pink
        .doTask("-3 MC")
        .expect(
            "2 TerraformRating<Green>, -Ruling<MarsFirst>, Ruling<Reds>, -Chairman<Pink>, Chairman<Green>, " +
                "-Dominant<Reds>, Dominant<Unity>, Current<TerraformingLobbying>, Coming<DemocraticReform>, " +
                "Distant<SelfSufficiencyProgram>",
        )
    // The spoken "21 TR" conflicts with green-applog-06 entry 156: two TR gained, ending at 22.
    green.assertResources(m = 29, s = 3, t = 8, p = 1, e = 4, h = 5)
    pink.assertResources(m = 35, s = 0, t = 9, p = 0, e = 4, h = 20)
    green.assertCounts(22 to "TerraformRating")
    pink.assertCounts(26 to "TerraformRating")
    // (4:00:19 PM) "Move the event, add neutral Unity. It is still dominant. Shift over and reveal
    // Election. Add a dude to Greens. I think we know who's winning that one: influence plus
    // building tags plus cities."
    // "That's a shoo-in for me."
    // "I've added the neutral delegate. That's all the Turmoil stuff."

    // (4:01:02 PM) "Generation becomes six. Button moves to Pink. We're each taking three cubes off
    // cards. We're ready to research. Two for you, two for me, two for you, two for me. I want most
    // of these; I want two of these."
    // 16:03: the mixed-speaker research sentence is resolved by both logs: three purchases each.
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "Ok", // no heat paid
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    pink.turn {
      // (4:03:59 PM) Pink: "I may have been a little unsmart. I'm going to raise temp two times. I
      // have to lose six for it, but I do get the heat bonus."
      // Green: "You lost six what?"
      // Pink: "Money. Reds' bonus."
      // Green: "Of course I did."
      // Pink: "Did you forget? You just wanted the TR?"
      // Green: "Thank you for realizing that."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "-3 MC",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "-3 MC",
          "PROD[Heat]",
      )
    }
    green.turn {
      // (4:05:09 PM) "Lavatory Settlement costs 14, which I'll spend as 6 worth of steel and 8
      // real. I lose an energy production, gain 2 money production, which gains me 2 money, and
      // place a shitty tile. I don't even want these stupid cities, but I do want to claim Builder.
      // I'm placing it on Elysium Mons to draw one card. Oh great, another city."
      // 16:05: "Lavatory settlement" at Elysium Mons (photo:2,6), then Builder.
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "3 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "2 MC",
          "CityTile<Vastitas_2_6>",
          "ProjectCard",
      )
      // "I count seven. For my second action, I pay eight and claim Builder."
      // "I keep forgetting about Merchant, but I've never had two plants together at the same time
      // in this whole game."
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "8 Pay<> FROM MC",
          "Builder",
      )
    }
    pink.turn {
      // (4:06:19 PM) "Spend three energy and trade with Luna for 11."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "11 MC",
      )
    }
    green.turn {
      // (4:06:40 PM) "Use Asteroid Rights, remove an asteroid, and give myself a money production."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "PROD[MC]",
          "MC",
      )
    }
    pink.turn {
      // (4:07:06 PM) "Pay six real money for Rotator Impacts, add a resource to it."
      doTasks(*cardActionTasks("$RotatorImpacts"))
      // Titanium and heat payments both offer Ok; select titanium before declining it.
      selectTask("X! Pay<Class<Titanium>> FROM Titanium OR Ok")
      doTasks(
          "Ok", // no titanium paid
          "Ok", // no heat paid
          "6 Pay<> FROM MC",
          "Asteroid<$RotatorImpacts>",
      )
    }
    green.turn {
      // (4:07:24 PM) "I'm going to use Floating Refinery to add a floater."
      doTasks(
          *cardActionTasks("$FloatingRefinery"),
          "Floater<$FloatingRefinery>",
      )
    }
    pink.turn {
      // (4:07:47 PM) "Spend thirteen on Lunar Beam. Lose two money production, gain two energy
      // production, gain two heat production."
      doTasks(
          *playCardTasks("$LunarBeam"),
          "Ok", // no heat paid
          "13 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Energy]",
          "PROD[2 Heat]",
      )
    }
    green.turn {
      // (4:08:00 PM) "I'm going to use Mars Nomads, move to 4,8 for two money and place a delegate
      // for free, which I'll do in Reds to grab the leadership."
      // "I thought about placing my thing for that, but there's a good chance you'll get into Reds
      // again. That benefits you anyways."
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_4_8 FROM Vastitas_5_9>",
          "2 MC",
          "PartyLeader<Reds>",
      )
    }
    pink.turn {
      // (4:08:35 PM) "I'm going to place my lobby guy in Greens."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Greens> FROM LobbyDelegate",
      )
    }
    green.turn {
      // (4:08:46 PM) "I'll place my lobby guy in Mars First."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<MarsFirst> FROM LobbyDelegate",
      )
    }
    pink.turn {
      // (4:09:39 PM) "Aquifer Pumping. Spend eight, place an ocean."
      // "Don't forget your TR."
      // "Increase ocean and TR, for two plants and four money. 5,6. Then I lose three from the
      // thing. It's not great."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "Ok", // no heat paid
          "8 Pay<> FROM MC",
          "OceanTile<Vastitas_5_6>",
          "TerraformRating",
          "-3 MC",
          "2 Plant",
          "4 MC",
      )
    }
    // (4:10:14 PM) "I don't see anything worth trading with, so I think I'm done. I pass."
    green.doTask("Pass")

    pink.turn {
      // (4:10:35 PM) Pink: "I believe I have two energy tags, so I can pay five for Power Supply
      // Consortium."
      // Green: "I lost an energy production."
      // Pink: "And I gained one. Now I think I'm out."
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "Ok", // no heat paid
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[-Energy<Green>]",
          "PROD[Energy]",
      )
    }
    pink.doTask("Pass")

    // (4:11:22 PM) "Ready for production?"
    // "Yeah."
    green.doTasks("41 MC", "Steel", "2 Titanium", "4 Heat FROM Energy", "2 Energy", "Heat")
    pink.doTasks("36 MC", "Titanium", "Heat FROM Energy", "7 Energy", "12 Heat")
    // (4:11:27 PM) "Then world government."
    // Pink: "I will increase oxygen to three percent."
    // Green: "I'm belatedly raising the temp for my thing too."
    pink.doTask("OxygenStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // (4:11:47 PM) "Then picture time, then colonies. Take my ship back. Then walk us through
    // Turmoil."
    // board-16-11-57.jpg, green-applog-07 and pink-applog-08, before Generous Funding.
    green.assertResources(m = 50, s = 1, t = 10, p = 1, e = 2, h = 10)
    green.assertProduction(m = 19, s = 1, t = 2, p = 0, e = 2, h = 1)
    pink.assertResources(m = 36, s = 0, t = 10, p = 2, e = 7, h = 17)
    pink.assertProduction(m = 7, s = 0, t = 1, p = 0, e = 7, h = 12)
    green.assertCounts(
        1 to "Builder",
        0 to "Asteroid<$AsteroidRights>",
        1 to "Floater<$FloatingRefinery>",
    )
    // (4:12:18 PM) "All players lose one TR. Perform the current global event."
    // Green: "I have three influence plus one bundle of TR, so I get eight money."
    // Pink: "I have one bundle, so I get two."
    // Green: "You have two bundles over 15."
    // Pink: "28. Yes, I do. I thought I had to get to 30. Our new government: Unity."
    green.doTask("8 MC")
    pink.doTask("4 MC")
    // (4:13:09 PM) "Perform the ruling bonus."
    // Green: "I think I get five."
    // Pink: "I believe I get four. Dominant party... you gain a TR. Restore lobby. Move coming to
    // current. We get neutral in Greens."
    pink
        .doTask("Ok") // no influence payout
        .expect(
            "5 MC<Green>, 4 MC<Pink>, TerraformRating<Green>, -Ruling<Reds>, Ruling<Unity>, 0 " +
                "Chairman<Green>, -Dominant<Unity>, Dominant<Greens>, Current<DemocraticReform>, " +
                "Coming<SelfSufficiencyProgram>, Distant<Diaspora>",
        )
    green.assertResources(m = 63, s = 1, t = 10, p = 1, e = 2, h = 10)
    pink.assertResources(m = 44, s = 0, t = 10, p = 2, e = 7, h = 17)
    green.assertCounts(22 to "TerraformRating")
    pink.assertCounts(28 to "TerraformRating")
    // (4:13:32 PM) "Mars First becomes dominant. Now dominance moves to Greens. Another person to
    // Mars First. We revealed Microgravity Health Problems. Lose three money for it. Let's end the
    // game real quick."
    // "At the end of this round we're going to get stuff per influence, and you're going to place
    // an ocean. That's the end of Turmoil. Generation becomes seven. I'm the start player; we
    // haven't moved the button yet. Three cubes off cards for me. Ready to research."

    // (4:15:09 PM) Green: "I am going to purchase two cards."
    // Pink: "I as well purchase two. I believe I will pay a titanium for Rim Freighters."
    // Green: "Is it my turn?"
    // Pink: "Oh wait, it is. Sorry, your turn."
    // 16:16: both buy two.
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "Ok", // no heat paid
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.turn {
      // (4:18:22 PM) "Pay one for an asteroid on Asteroid Rights."
      doTasks(
          *cardActionTasks("$AsteroidRights"),
          "Pay<> FROM MC",
          "Asteroid<$AsteroidRights>",
      )
    }
    pink.turn {
      // (4:18:29 PM) "For now I'm not going to play Rim Freighters. I'll spend the full three
      // energy to fly my boat to Ceres for three steel."
      // The proposed early Rim Freighters play was withdrawn; the photographed card arrives later.
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "3 Steel",
      )
      // "I believe I have two of each resource. I claim Merchant."
      // "Did you pay your eight for that? Merchant is claimed. Now Coast Guard and Pioneer will not
      // be claimed. You're so close to Pioneer."
      // "Yeah, but you have those things."
      doTasks(
          "UseAction<ClaimMilestoneAction, Action1>",
          "Ok", // no heat paid
          "8 Pay<> FROM MC",
          "Merchant",
      )
    }
    green.turn {
      // (4:19:24 PM) "Let's play Standard Technology for five money. Oops, not 51."
      doTasks(
          *playCardTasks("$StandardTechnology"),
          "5 Pay<> FROM MC",
          "$StandardTechnology FROM ProjectCard",
      )
      // "Then standard project aquifer and take three money. I'm going to place it you know where:
      // 5,7, for six money and two plants, and a TR. That's our sixth ocean on the board."
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "3 MC",
          "OceanTile<Vastitas_5_7>",
          "6 MC",
          "2 Plant",
          "TerraformRating",
      )
      // green-applog-07 entries 188/189 already add ocean 6/TR 23 for the standard project. Entries
      // 192/193 add another ocean/TR; 194 reverses only the ocean, leaving TR 24.
      exMachina("TerraformRating")
    }
    pink.turn {
      // (4:20:22 PM) "You like to stall and I like to be aggressive. I want to do things in one
      // order, but I want to make sure I get to them. I guess I will play Sky Docks. Had it since
      // the start. Requires two Earth tags. Four titanium as 16, and two real. I get a universal
      // discount of one and another trade fleet."
      // 16:23: four titanium are worth 16 under Unity, plus two MC for Sky Docks.
      doTasks(
          *playCardTasks("$SkyDocks"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "2 Pay<> FROM MC",
          "$SkyDocks FROM ProjectCard",
          "TradeFleet",
      )
      // "I'm worried you'll want to trade right away, so I'll spend three energy to trade with
      // Triton, get four titanium."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "4 Titanium",
      )
    }
    green.turn {
      // (4:23:53 PM) "I am going to play L1 Trade Terminal for 24, which I'll spend as 6 titanium.
      // I add an asteroid to Asteroid Rights and a floater to Floating Refinery."
      doTasks(
          *playCardTasks("$L1TradeTerminal"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remains owed
          "$L1TradeTerminal FROM ProjectCard",
          "EACH",
          "Asteroid<$AsteroidRights>",
          "Floater<$FloatingRefinery>",
      )
      // (4:24:17 PM) "Then I'm going to buy a standard project power plant, which gives me three
      // back from Standard Technology, an energy production, and an energy."
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "3 MC",
          "PROD[Energy]",
          "Energy",
      )
      // green-applog-08 entries 198/200 each add energy production for the same standard project.
      // Entry 201 adds Manutech's energy only once. Keep that actual extra production explicit.
      exMachina("PROD[Energy]")
    }
    pink.turn {
      // (4:24:28 PM) "Steel and two real to use Aquifer Pumping. Place an ocean: two heat, two
      // money on 6,5. Ooh, not heat production."
      // "Did you get your TR?"
      // "Oh, thank you. Yeah."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "3 Pay<Class<Steel>> FROM Steel",
          "Ok", // no heat paid
          "2 Pay<> FROM MC",
          "OceanTile<Vastitas_6_5>",
          "2 Heat",
          "2 MC",
          "TerraformRating",
      )
      // (4:25:37 PM) "Five for Capital. A special tile, right on 5,5. I collect six money, then
      // lose four, so I get two money and a temp."
      doTasks(
          *playCardTasks("$Capital"),
          "Ok", // no heat paid
          "25 Pay<> FROM MC",
          "$Capital FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[5 MC]",
          "CapitalTile<Vastitas_5_5>",
          "2 MC", // six adjacency MC less the four MC placement cost
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (4:26:19 PM) Green: "I am ready to trade for three energy with Luna. I think I will raise
      // it two spots. I get 13, you get four."
      // Pink: "I get four."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "2 ColonyProduction<Luna>",
          "13 MC",
      )

      // Each of Pink's two colony bonuses is dispatched from Green's trade to Pink.
      giveTask(pink, "2 MC<Pink>", "2 MC")
      giveTask(pink, "2 MC<Pink>", "2 MC")
      // (4:26:41 PM) "Astromechanica for six. I want this one."
      // "Astromechanica" is Astra Mechanica. The two returned events are replayed below.
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "6 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$IndenturedWorkers>>",
          "ProjectCard FROM PlayedEvent<Class<$ConvoyFromEuropa>>",
      )
    }
    pink.turn {
      // (4:27:41 PM) "You bought Refugee Camps. I never buy that."
      // "I feel like now I'm getting somewhere production-wise."
      // pink-applog-09's net -5 combines Luna +4 with the discounted 9 MC card bill.
      doTasks(
          *playCardTasks("$RefugeeCamps"),
          "Ok", // no heat paid
          "9 Pay<> FROM MC",
          "$RefugeeCamps FROM ProjectCard",
      )
    }
    green.turn {
      // (4:27:51 PM) "Use Floating Refinery to remove two floaters, take a titanium and two money."
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      // "Now I'm going to play Indentured Workers for free."
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
      )
    }
    pink.turn {
      // (4:28:22 PM) "I'll use Refugee Camps, lose money production, gain a camp."
      doTasks(
          *cardActionTasks("$RefugeeCamps"),
          "PROD[-MC]",
          "Camp<$RefugeeCamps>",
      )
    }
    green.turn {
      // (4:28:30 PM) "Now I'm going to play 16 Psyche for twenty-two. Five titanium will count for
      // twenty, so I pay two real. I get three titanium, and two titanium production, which gives
      // me another two titanium. Come right back where I was with titanium."
      // "Might have been wrong for me to refrigerate."
      doTasks(
          *playCardTasks("$SixteenPsyche"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "3 Titanium",
          "PROD[2 Titanium]",
          "2 Titanium",
      )
    }
    pink.turn {
      // (4:29:16 PM) "I will use Rotator Impacts, lose titane."
      // The transcript's "lose titane" is the asteroid on Rotator Impacts; Venus reaches 18.
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (4:29:24 PM) "Venus is now at 18%. I'll play Nuclear Zone for nine."
      doTasks(
          *playCardTasks("$NuclearZone"),
          "7 Pay<> FROM MC",
      )
      // 16:29: Green paid 9 MC (entry 214), forgetting Solar Logistics' Earth-tag discount.
      exMachina("-2 MC")
      // (4:29:48 PM) "I'm going to place it at 6,8 for one plant and six money, and raise the temp
      // to minus 14, which gives me two TR. My cube on it. Let me check if there's anything else I
      // want to do. I'm fine."
      doTasks(
          "$NuclearZone FROM ProjectCard",
          "SpecialTile<Vastitas_6_8>",
          "Plant",
          "6 MC",
          "2 TemperatureStep",
          "2 TerraformRating",
      )
    }
    pink.turn {
      // (4:30:23 PM) "I can now spend five titanium on Luna Metropolis. I have six Earth tags. And
      // place a shitty tile."
      doTasks(
          *playCardTasks("$LunaMetropolis"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "Ok", // no MC remains owed
          "$LunaMetropolis FROM ProjectCard",
          "PROD[6 MC]",
          "CityTile<LunaMetropolis_RemoteArea>",
      )
    }
    green.turn {
      // (4:31:07 PM) "I'm going to play Miranda Resort. Costs 11. Two titanium counts for eight,
      // and three real. I have three Earth tags? Three megacredit production, three megacredits."
      doTasks(
          *playCardTasks("$MirandaResort"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[3 MC]",
          "3 MC",
      )
    }
    pink.turn {
      // (4:31:39 PM) "I've decided to spend the full titanium on Rim Freighters. Figure I'll want
      // to get aggressive next."
      // 16:31: the actual Rim Freighters play, after both of Pink's trades.
      doTasks(
          *playCardTasks("$RimFreighters"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "Ok", // no MC remains owed; titanium deliberately overpays by 1
          "$RimFreighters FROM ProjectCard",
      )
    }
    green.turn {
      // (4:31:50 PM) "Solar Reflectors for 22: 12 worth of titanium and 10 real. I get 5 heat
      // production and 5 heat."
      doTasks(
          *playCardTasks("$SolarReflectors"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "10 Pay<> FROM MC",
          "$SolarReflectors FROM ProjectCard",
          "PROD[5 Heat]",
          "5 Heat",
      )
    }
    pink.turn {
      // (4:32:31 PM) "Is it minus twelve?"
      // "Yeah."
      // 16:33: Pink's logged heat conversion raises temperature to -12.
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (4:33:46 PM) "I guess I'll do a heat boop too. Since you haven't done anything yet, I'm
      // going to go next."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "Convoy from Europa costs me 14. I have no titanium left, so I'm going to spend 14 real. I
      // place the penultimate ocean tile on 3-3 for two plants and a TR, and draw two cards."
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "14 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "OceanTile<Vastitas_3_3>",
          "2 Plant",
          "TerraformRating",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (4:34:46 PM) "I pay a heat for Virus, so you lose five plants. Wait, no, I don't pay for
      // Virus."
      // "That makes me sad."
      // Sky Docks reduces the Virus bill to 0.
      doTasks(
          *playCardTasks("$Virus"),
          "$Virus FROM ProjectCard",
          "-5 Plant<Green>",
      )
    }
    green.turn {
      // (4:35:31 PM) "Regardless of my sadness, I'm still going to play Grass. The Convoy is
      // flipped. Grass for 10 real gives me three plants, a plant production, and a plant."
      doTasks(
          *playCardTasks("$Grass"),
          "10 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "3 Plant",
          "PROD[Plant]",
          "Plant",
      )
    }
    pink.turn {
      // (4:36:18 PM) "Put my free dude in Greens just to hold the lead."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Greens> FROM LobbyDelegate",
      )
    }
    green.turn {
      // (4:36:29 PM) "Let's get weird. It's Jovian Envoys. Pay one. I finally have the two Jupiter
      // tags that I need. I place two delegates into Mars First."
      doTasks(
          *playCardTasks("$JovianEnvoys"),
          "Pay<> FROM MC",
          "$JovianEnvoys FROM ProjectCard",
          "PartyFollower<MarsFirst>",
          "PartyFollower<MarsFirst>",
      )
      // "Then use my lobby action in Mars First, which both takes the leadership and moves the
      // dominance."
      doTasks(
              "UseAction<LobbyAction, Action1>",
              "PartyLeader<MarsFirst> FROM LobbyDelegate",
          )
          .expect("-PartyLeader<MarsFirst, Neutral>, PartyFollower<MarsFirst, Neutral>")
    }
    pink.turn {
      // (4:37:12 PM) "Do another heat boop. Temp is at minus eight."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (4:37:54 PM) "I use Mars Nomads. I forgot to do this earlier. Move back to 5,9 again, take
      // two money and kick that out of there."
      // 16:37: two adjacent-ocean MC are spoken; Nomads also draws the two printed cards.
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_5_9 FROM Vastitas_4_8>",
          "2 MC",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (4:39:09 PM) "Is that all you're doing?"
      // "Yeah, for now."
      // 16:38: pink-applog-10 entry 227 confirms the 1 MC patent sale.
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "MC FROM ProjectCard",
      )
    }
    green.turn {
      // (4:39:22 PM) "I'm taking two money."
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
      // "Now spending all seven of my money on Permafrost Extraction. Puts oceans at nine, my TR at
      // twenty-nine. That ocean is going at 3.2 for two plants and two money."
      doTasks(
          *playCardTasks("$PermafrostExtraction"),
          "7 Pay<> FROM MC",
          "$PermafrostExtraction FROM ProjectCard",
          "OceanTile<Vastitas_3_2>",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
    }
    pink.turn {
      // (4:39:46 PM) "I spend three titanium and one real on Imported Nutrients. Gain four plants,
      // and the microbes don't happen."
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "4* Microbe",
      )
      // No microbe holder; Solar Logistics still awards Green a card for Pink's space event. Keep
      // the handoff separate so we can check Green's hand before the missed draw.
      doTask("NOW Green { ProjectCard<Green, Hand> }")

      green.assertCounts(4 to "ProjectCard")
      green.doTask("ProjectCard")
      // At 16:43 Green explicitly counts 4 cards, not 5. The user confirms forgetting this Solar
      // Logistics draw; retain the observed hand until the correction is actually applied.
      green.exMachina("-ProjectCard")
    }
    // (4:40:05 PM) "I am all out of stuff to do. Pass."
    green.doTask("Pass")

    // (4:40:18 PM) "I as well pass."
    pink.doTask("Pass")

    green.doTasks("51 MC", "Steel", "4 Titanium", "Plant", "4 Energy", "6 Heat")
    pink.doTasks("50 MC", "Titanium", "Heat FROM Energy", "5 Energy", "12 Heat")
    // board-16-40-53.jpg is after production, before WGT and Turmoil; boats are still out.
    green.assertResources(m = 53, s = 2, t = 4, p = 8, e = 4, h = 13)
    green.assertProduction(m = 22, s = 1, t = 4, p = 1, e = 4, h = 6)
    pink.assertResources(m = 50, s = 0, t = 2, p = 6, e = 5, h = 16)
    pink.assertProduction(m = 17, s = 0, t = 1, p = 0, e = 5, h = 12)
    green.assertCounts(
        29 to "TerraformRating",
        4 to "ProjectCard",
        2 to "Asteroid<$AsteroidRights>",
        0 to "Floater<$FloatingRefinery>",
    )
    pink.assertCounts(
        33 to "TerraformRating",
        1 to "ProjectCard",
        1 to "Merchant",
        1 to "Camp<$RefugeeCamps>",
        0 to "Asteroid<$RotatorImpacts>",
    )
    // (4:40:21 PM) "I will world governmentalize Venus to 20. This game needs to end sometime; I'm
    // gonna do oxygen to four instead. Then colonies: three boats go back. Everyone loses a TR."
    green.doTask("OxygenStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // (4:41:21 PM) Pink: "Perform the current global event. First player, no ocean placed. Gain one
    // plant and one steel per influence. You keep winning the Turmoil stuff."
    // Green: "That's three of each for me."
    // All nine oceans are already placed; Aquifer Released only awards influence resources.
    green.doTasks("3 Plant", "3 Steel")
    pink.doTask("0 Plant") // zero influence
    pink
        .doTask("0 Steel") // zero influence
        // (4:41:47 PM) "Dominant party becomes ruling. Change to Mars First tile. Do we do the
        // bonus now?"
        // Pink: "Ruling bonus. I get four. You become chairman and earn TR."
        // Green: "Seven for me. My dudes are going back, I get one TR, and dominance shifts to
        // Greens."
        .expect(
            "7 MC<Green>, 4 MC<Pink>, TerraformRating<Green>, -Ruling<Unity>, Ruling<MarsFirst>, 0 " +
                "Chairman<Green>, -Dominant<MarsFirst>, Dominant<Greens>, Current<SelfSufficiencyProgram>, " +
                "Coming<Diaspora>, Distant<IncreasedInterplanetaryTrade>",
        )
    green.assertResources(m = 60, s = 5, t = 4, p = 11, e = 4, h = 13)
    pink.assertResources(m = 54, s = 0, t = 2, p = 6, e = 5, h = 16)
    green.assertCounts(29 to "TerraformRating")
    pink.assertCounts(32 to "TerraformRating")
    // (4:42:13 PM) "Restore lobby. Election moves into current, and we get a Mars First leader. We
    // reveal Solar Flare, which adds a leader for Unity. Generation becomes eight."

    // (4:43:01 PM) Green: "Button moves. I'm removing three cubes from cards. We're adjourning with
    // four cards left in my hand."
    // Pink: "I as well remove three cubes from cards. I am adjourning with one card left in hand."
    // Generation 8, 18:43: both players confirm applying the previously identified corrections.
    // (6:43:34 PM) Green: "I've made the necessary adjustments. My resources are 61, 6, 4, 11, 3,
    // 13, and my productions are 22, 1, 4, 1, 3, 6."
    // Pink: "My resources are 54, 1, 2, 6, 5, 16. Productions 17, 0, 1, 0, 5, 12."
    green.exMachina("MC, Steel, -TerraformRating, PROD[-Energy], -Energy")
    pink.exMachina("Steel")
    green.assertResources(m = 61, s = 6, t = 4, p = 11, e = 3, h = 13)
    green.assertProduction(m = 22, s = 1, t = 4, p = 1, e = 3, h = 6)
    pink.assertResources(m = 54, s = 1, t = 2, p = 6, e = 5, h = 16)
    // (6:43:59 PM) Green: "I've got four cards in hand. To put things right, because I forgot Solar
    // Logistics, I get to draw a free card. Then we're ready to draft. Two for you, two for me, two
    // for you, two for me."
    // Pink: "And I have my one card."
    // 18:43:59: Green draws the forgotten Solar Logistics card before drafting.
    green.exMachina("ProjectCard")
    // (6:46:19 PM) "I'm buying two cards."
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // (6:47:09 PM) "Did you buy your cards?"
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-2 ProjectCard<Selecting>",
        "Ok", // no heat paid
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    pink.turn {
      // (6:48:10 PM) "I should block you. I'm gonna do a greenery standard project here. Oxygen is
      // now at five. At 5,4 for two plants and four money."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "Ok", // no heat paid
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_5_4>",
          "2 Plant",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
          "Steel",
      )
      // (6:48:46 PM) "Then I'll convert plants to greenery at 4,4 for a plant and two money.
      // Unfortunately, I do have to do this."
      // "Did you get two steel for placing?"
      // "Oh right, that was part of the point."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_4>",
          "Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
          "Steel",
      )
    }
    green.turn {
      // (6:49:52 PM) "I'm just going to win Election? I have two more cities than you and quite a
      // few more building tags."
      // "Pretty much."
      // "I'll use Asteroid Rights to remove an asteroid and take two titanium."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      // "I'll spend that titanium on Imported Nitrogen, 18 worth, plus two money because of Solar
      // Logistics. Then I get a TR, four plants. I don't get any of the other stuff, but I do get a
      // card. Those were my two actions, right?"
      doTasks(
          *playCardTasks("$ImportedNitrogen"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$ImportedNitrogen FROM ProjectCard",
          "TerraformRating",
          "4 Plant",
          "3* Microbe",
          "2* Animal",
          "ProjectCard",
      )
    }
    // Pink trades with Titan despite having no floater holder, explicitly to block Green.
    pink.turn {
      // (6:52:44 PM) "I'm gonna be a little mean: spend two energy to trade with Titan."
      // "Oh, you jerk. I didn't expect that. Anything else?"
      // "Your turn."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Titan>",
          "3* Floater",
      )
    }
    green.turn {
      // (6:53:13 PM) "I'm going to convert plants. That raises oxygen to seven, and I place a
      // greenery at 3-4 to get 2 money and 2 plants."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_3_4>",
          "2 Plant",
          "2 MC",
          "OxygenStep",
          "TerraformRating",
          "Steel",
      )
      // "Then I convert plants again. Oxygen is at 8 and temperature also increases. I raise
      // temperature one more time and give myself one more TR, because it gave me one. This
      // greenery goes at 4,6 to get a card and four money."
      // "We placed our greeneries before the thing that gives us money for that, but I was paranoid
      // about you making me lose plants."
      // "I guess I do get two steel."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_6>",
          "ProjectCard",
          "4 MC",
          "OxygenStep",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "Steel",
      )
    }
    pink.turn {
      // (6:54:29 PM) "Do I have a lead on any of the awards? I'm gonna spend two titanium to add to
      // Rotator Impacts."
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "Ok", // no MC remains owed
          "Asteroid<$RotatorImpacts>",
      )
    }
    green.turn {
      // (6:56:22 PM) "I'm going to Sabotage you for free. Lose seven money."
      doTasks(
          *playCardTasks("$Sabotage"),
          "$Sabotage FROM ProjectCard",
          "-7 MC<Pink>",
      )
    }
    pink.turn {
      // (6:56:39 PM) "I'm gonna send a free dude and use Martian Media Center: spend three money
      // and place two dudes in Scientists, which I am now leading."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Scientists> FROM LobbyDelegate",
      )
      doTasks(
              *cardActionTasks("$MartianMediaCenter"),
              "Ok", // no heat paid
              "3 Pay<> FROM MC",
              "PartyLeader<Scientists>",
          )
          .expect("-PartyLeader<Scientists, Neutral>, PartyFollower<Scientists, Neutral>")
    }
    green.turn {
      // (6:57:44 PM) "I'm going to pay 23 for a greenery. It raised oxygen to nine automatically
      // and gave me a TR, I think. Put it at 2,5 for one plant. I get a steel for placing that."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "3 MC",
          "GreeneryTile<Vastitas_2_5>",
          "Plant",
          "OxygenStep",
          "TerraformRating",
          "Steel",
      )
      // Green ledger entries 291–296 omit this Standard Technology rebate: 59−23+12−3−14=31 MC.
      exMachina("-3 MC")
      // "Harvest costs me three, and gives me 12."
      doTasks(
          *playCardTasks("$Harvest"),
          "3 Pay<> FROM MC",
          "$Harvest FROM ProjectCard",
          "12 MC",
      )
    }
    pink.turn {
      // (6:59:15 PM) "Refugee Camps: lose money production, add a camp."
      doTasks(
          *cardActionTasks("$RefugeeCamps"),
          "PROD[-MC]",
          "Camp<$RefugeeCamps>",
      )
    }
    green.turn {
      // (7:00:08 PM) "Atmo Collectors for 14, which automatically puts two floaters on itself."
      doTasks(
          *playCardTasks("$AtmoCollectors"),
          "14 Pay<> FROM MC",
          "$AtmoCollectors FROM ProjectCard",
          "2 Floater<$AtmoCollectors>",
      )
      // "Then I'll use it to remove a floater and take three energy."
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "3 Energy",
      )
    }
    pink.turn {
      // (7:00:44 PM) "Spend two energy, fly my boat to Luna, collect my eleven monies."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "11 MC",
      )
    }
    green.turn {
      // (7:00:49 PM) "I'm going to pay 14 for Steelworks, but really only pay seven steel."
      doTasks(
          *playCardTasks("$Steelworks"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC remains owed
          "$Steelworks FROM ProjectCard",
      )
      // "Then use it to lose four energy and get an oxygen, a TR, and two steel. Oxygen is on 10
      // now."
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
          "2 Steel",
      )
    }
    pink.turn {
      // (7:01:27 PM) "Heat boop."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (7:01:36 PM) "I'll do a heat boop too. At zero."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "I will use Floating Refinery to add a floater."
      doTasks(
          *cardActionTasks("$FloatingRefinery"),
          "Floater<$FloatingRefinery>",
      )
    }
    // Pink ledger entries 275–278 supply a second heat conversion omitted from the words at 19:03.
    pink.turn {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>*",
      )
      // (7:03:01 PM) "Methane from Titan. Increase heat production two steps, increase plant
      // production two."
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "27 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
    }
    green.turn {
      // (7:03:16 PM) "I will move Mars Nomads to the spot where I get two money and two titanium.
      // Should have done it before."
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_6_9 FROM Vastitas_5_9>",
          "2 MC",
          "2 Titanium",
      )
    }
    pink.turn {
      // (7:04:41 PM) "Money to put another delegate in Kelvinists and ensure my lead."
      doTasks(
          "UseAction<PaidLobbyAction, Action1>",
          "5 Pay<> FROM MC",
          "PartyFollower<Kelvinists>",
      )
    }
    // Pink says she is out by 19:06; record her pass after Green’s two lobby actions.
    green.turn {
      // (7:05:25 PM) "I'm gonna move my lobby dude to Unity."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Unity> FROM LobbyDelegate",
      )
      // "And pay five to put another dude in Unity. Now I took over Unity."
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "5 Pay<> FROM MC",
              "PartyLeader<Unity>",
          )
          .expect("-PartyLeader<Unity, Neutral>, PartyFollower<Unity, Neutral>")
    }
    // (7:06:08 PM) Pink: "Still out."
    // Green: "Oh, you're out."
    pink.doTask("Pass")

    green.turn {
      // (7:06:10 PM) "I'm going to play Cultural Metropolis. Costs 19, but I pay 8 worth of steel,
      // so that costs me 11. I lose an energy production, get 3 money production and 3 money, place
      // a shitty tile at 4,7 for one plant and four money."
      // "You've been gaining your steels?"
      // "I'm getting one now."
      doTasks(
              *playCardTasks("$CulturalMetropolis"),
              "4 Pay<Class<Steel>> FROM Steel",
              "11 Pay<> FROM MC",
              "$CulturalMetropolis FROM ProjectCard",
              "PROD[-Energy]",
              "PROD[3 MC]",
              "3 MC",
              "CityTile<Vastitas_4_7>",
              "Plant",
              "4 MC",
              "Steel",
              // (7:07:26 PM) "Then I place two delegates in the same party. I don't really know
              // what party I want. I could take stuff away from you, but then I'm going to be thin
              // on delegates. I'm just going to dump them in Mars First. I took it over from the
              // neutral."
              "PartyFollower<MarsFirst>",
              "PartyLeader<MarsFirst>",
          )
          .expect("-PartyLeader<MarsFirst, Neutral>, PartyFollower<MarsFirst, Neutral>")
      // "Then a power plant, which gives me an energy production, an energy, and three money from
      // Standard Technology."
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "3 MC",
          "PROD[Energy]",
          "Energy",
      )
      // "Then trade with Ceres. I get to increase it two, so I get eight steel. That's a lot of
      // steel."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "2 ColonyProduction<Ceres>",
          "4 Steel",
      )
    }
    // (7:09:08 PM) Pink: "It was at two, I believe. You increased it four."
    // Green: "Oh, I get four steel."
    // Pink: "Did you multiply it by two?"
    // The spoken 8-steel trade is immediately corrected to 4; ledger entries 324/325 combine that
    // correction with spending 5 steel.
    green.turn {
      // (7:09:19 PM) "I don't know what I did. Now I can play Jovian Embassy for thirteen, but
      // spend ten of it as steel. Three real. I get a TR. I might finally be done."
      doTasks(
          *playCardTasks("$JovianEmbassy"),
          "5 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$JovianEmbassy FROM ProjectCard",
          "TerraformRating",
      )
      // (7:09:58 PM) "Or I might pitch one card and then asteroid, get three money from that, and
      // temperature is now two."
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "MC FROM ProjectCard",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "3 MC",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    // (7:10:15 PM) "Now I'm out of stuff to do, and I've only one card left."
    green.doTask("Pass")

    // 19:10 production. Green ledger entry 333 combines the 3-MC asteroid rebate with 62-MC
    // production.
    green.doTasks(
        "62 MC",
        "Steel",
        "4 Titanium",
        "Plant",
        "3 Energy",
        "6 Heat",
    )
    pink.doTasks(
        "52 MC",
        "Titanium",
        "2 Plant",
        "Heat FROM Energy",
        "5 Energy",
        "14 Heat",
    )
    green.assertResources(m = 65, s = 1, t = 6, p = 4, e = 3, h = 11)
    pink.assertResources(m = 52, s = 3, t = 1, p = 3, e = 5, h = 15)
    green.assertCounts(37 to "TerraformRating", 1 to "ProjectCard")
    pink.assertCounts(36 to "TerraformRating")
    // (7:10:25 PM) Pink: "Part of me wants to raise Venus and see if we can keep the game going. I
    // don't feel confident about my chances of winning."
    // Green: "Nor do I. What's it going to be? Oxygen is at 11 now. It looks like I accidentally
    // reduced my heat production by one, so I'm going to increase it again. Now it's time for
    // colonies."
    // board-19-11-27.jpg: WGT oxygen 11, before Turmoil; Green 3 greeneries, Pink 2.
    pink.doTask("OxygenStep! BY Admin").expect("-TerraformRating<Green>, -TerraformRating<Pink>")
    // (7:11:59 PM) Green: "Then the global event. I get two TR. Does second place happen in a
    // two-player game?"
    // Pink: "Good question. Where's the rulebook?"
    // Green: "It doesn't say anything on the card. We're going with yes, you do get a TR."
    // Pink: "That's what I thought."
    green.doTask("2 TerraformRating")
    pink
        .doTask("TerraformRating")
        .expect(
            "2 TerraformRating<Pink>, MC<Green>, -Chairman<Green>, Chairman<Pink>, -Ruling<MarsFirst>, " +
                "Ruling<Greens>, -Dominant<Greens>, Dominant<Scientists>, Current<Diaspora>, " +
                "Coming<IncreasedInterplanetaryTrade>, Distant<MoralMovement>",
        )
    // (7:14:01 PM) "All players lose one TR, but we've already done the current event. New
    // government: dominant party becomes ruling, Greens."
    // Green: "That was supposed to happen first. Greens are ruling, so we count our bio tags. I
    // have one."
    // Pink: "I have zero."
    // Green: "This is a very unbiological game. You become chairman."
    // Pink: "Get TR back. Restore lobby. We get a neutral Scientist and dominance shifts."
    // "Scientists are dominant. Then we get a neutral Red, because we revealed Strong Society."
    green.assertResources(m = 66, s = 1, t = 6, p = 4, e = 3, h = 11)
    pink.assertResources(m = 52, s = 3, t = 1, p = 3, e = 5, h = 15)
    green.assertCounts(38 to "TerraformRating")
    pink.assertCounts(37 to "TerraformRating")

    // (7:15:21 PM) Green: "Generation moves to nine, button moves to me. Taking five off cards.
    // Ready to draft?"
    // Pink: "I'm taking three off cards."
    // (7:17:25 PM) "I'm buying one card."
    green.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    // (7:17:27 PM) "I'm keeping three. Living on the edge."
    pink.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "Ok", // no heat paid
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // The first award is identified indirectly: Green sings The Scientist while paying 8 MC.
    green.turn {
      // (7:17:36 PM) "I'm going to spend eight. Come up to meet you, tell you I'm sorry. You don't
      // know how lovely you are."
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "8 Pay<> FROM MC",
          "Scientist",
      )
      // "What do I want to hurry up and trade with? I'm gonna spend three energy to fly my boat to
      // Luna. You get four money, I get 13 money."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "2 ColonyProduction<Luna>",
          "13 MC",
      )
      giveTask(pink, "2 MC<Pink>", "2 MC")
      giveTask(pink, "2 MC<Pink>", "2 MC")
    }
    pink.turn {
      // (7:18:29 PM) "I'm gonna heat boop."

      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "Then play Asteroid for 13, which I will spend as a titanium and 10 real. Gain two titanium
      // back."
      doTasks(
          *playCardTasks("$AsteroidCard"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no heat paid
          "10 Pay<> FROM MC",
          "$AsteroidCard FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "2 Titanium",
          "-3 Plant<Green>",
      )
      giveTask(green, "ProjectCard")
    }
    green.turn {
      // (7:18:50 PM) "I lost three points. I better heat boop."

      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "Then use Atmo Collectors to remove a floater and take three energy."
      doTasks(
          *cardActionTasks("$AtmoCollectors", "Action2"),
          "-Floater<$AtmoCollectors>",
          "3 Energy",
      )
    }
    pink.turn {
      // (7:19:35 PM) "Before we forget, I'm gonna pop my lobby boy in Greens."
      // "Anything else?"
      // "Your turn."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Greens> FROM LobbyDelegate",
      )
    }
    green.turn {
      // (7:20:27 PM) "I'm going to buy a standard project power plant, which gives me an extra
      // energy and three money back."
      doTasks(
          *standardProjectTasks("PowerPlantProject"),
          "11 Pay<> FROM MC",
          "3 MC",
          "PROD[Energy]",
          "Energy",
      )
      // (7:20:44 PM) "Then use Steelworks to delete four energy, take two steel, raise oxygen to
      // 12, and give myself a TR."
      doTasks(
          *cardActionTasks("$Steelworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
          "2 Steel",
      )
    }
    pink.turn {
      // (7:21:23 PM) "Standard project greenery. Oxygen to 13. At 6,4 for a plant, four money, and
      // the Greens bonus gives four more."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "Ok", // no heat paid
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_6_4>",
          "Plant",
          "8 MC",
          "OxygenStep",
          "TerraformRating",
      )
      // (7:22:03 PM) "I remembered a big brain move: spend seven heat and 16 real. That's the end
      // of oxygen."
      // "Make sure to give yourself the TR manually."
      // "I go at 4,5 for two money, and then four from what's-its-face."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "7 Pay<Class<Heat>> FROM Heat",
          "16 Pay<> FROM MC",
          "GreeneryTile<Vastitas_4_5>",
          "6 MC",
          "OxygenStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (7:22:51 PM) "I should have done things in a different order. I'm going to spend 23 on a
      // greenery, which it should not have given me a TR for. Greenery goes at 3.6 for nothing, but
      // I do get four money."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "3 MC",
          "GreeneryTile<Vastitas_3_6>",
          "4 MC",
          "0 OxygenStep",
      )
      // The app briefly adds TR at maximum oxygen; Green reverses it immediately. The second missed
      // rebate appears in ledger entries 374/375: 37+4+2−14−10=19 MC.
      exMachina("-3 MC")
      // (7:23:32 PM) "Use Mars Nomads, move this guy back to take two money and two cards. Please
      // be good. No."
      doTasks(
          *cardActionTasks("$MarsNomads"),
          "NomadsMarker<Vastitas_5_9 FROM Vastitas_6_9>",
          "2 MC",
          "2 ProjectCard",
      )
    }
    pink.turn {
      // (7:23:51 PM) "Two energy to trade with Ceres for two steel."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Ceres>",
          "2 Steel",
      )
      // "Then two energy to trade with Triton for three titanium. I don't know why I did both those
      // trades so urgently; you don't have another trade fleet."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "3 Titanium",
      )
    }
    green.turn {
      // (7:24:29 PM) Green: "Pay 14 to fund Venuphile. You only have one Venus tag, right?"
      // Pink: "Yes."
      // Green: "And I have two. That's how badly I'm doing on all the other ones."
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "14 Pay<> FROM MC",
          "Venuphile",
      )
    }
    pink.turn {
      // (7:25:01 PM) "Two titanium and two real for Lagrange Observatory. Draw a card."
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
    }
    green.turn {
      // (7:25:22 PM) "I am going to play Research for ten."
      doTasks(
          *playCardTasks("$Research"),
          "10 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
    }
    // The unnamed 11-MC, two-city requirement play is Martian Zoo, named by its later
    // trigger/action.
    pink.turn {
      // (7:25:24 PM) "I'm going to play for five steel and one real. Requires two cities in play. I
      // think we have that."
      doTasks(
          *playCardTasks("$MartianZoo"),
          "5 Pay<Class<Steel>> FROM Steel",
          "Pay<> FROM MC",
          "$MartianZoo FROM ProjectCard",
      )
    }
    green.turn {
      // (7:26:11 PM) "Ganymede Colony for six titanium and one real. Place a shitty tile on the
      // reserved area. That wouldn't count for Mars First if it was Mars First, right?"
      // "Don't think so."
      doTasks(
          *playCardTasks("$GanymedeColony"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Pay<> FROM MC",
          "$GanymedeColony FROM ProjectCard",
          "CityTile<GanymedeColony_RemoteArea>",
      )
    }
    pink.turn {
      // (7:26:42 PM) "I'll spend three titanium as nine money and four real on Orbital Cleanup.
      // It's an Earth tag, so it gives Martian Zoo a marshmallow."
      doTasks(
          *playCardTasks("$OrbitalCleanup"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$OrbitalCleanup FROM ProjectCard",
          "PROD[-2 MC]",
          "Animal<$MartianZoo>",
      )
      // Pink ledger entry 334 records only Refugee Camps’ 16→15 production change. Orbital
      // Cleanup’s −2 is omitted.
      exMachina("PROD[2 MC]")
    }
    green.turn {
      // (7:27:24 PM) "Neutralizer Factory for six. Venus goes to 20, and I get a TR."
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "6 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
    }
    pink.turn {
      // (7:27:40 PM) "I use Rotator Impacts, remove the asteroid, and increase Venus."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
    }
    green.turn {
      // (7:27:50 PM) "Venus is at 22. I'll use Asteroid Rights to remove an asteroid and give
      // myself two titanium."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
    }
    pink.turn {
      // (7:27:59 PM) "I will use Refugee Camps, remove money production, and add a thing."
      doTasks(
          *cardActionTasks("$RefugeeCamps"),
          "PROD[-MC]",
          "Camp<$RefugeeCamps>",
      )
    }
    green.turn {
      // (7:28:52 PM) "I'll spend five on top of my lobby action and go to Greens for both, so
      // you're kicked out."
      doTasks(
          "UseAction<LobbyAction, Action1>",
          "PartyFollower<Greens> FROM LobbyDelegate",
      )
      doTasks(
              "UseAction<PaidLobbyAction, Action1>",
              "5 Pay<> FROM MC",
              "PartyLeader<Greens>",
          )
          .expect("-PartyLeader<Greens, Pink>, PartyFollower<Greens, Pink>")
    }
    pink.turn {
      // (7:29:00 PM) "I had the thought that I could use Martian Media to increase my lead. Great
      // job, Pink. Anyways, Martian Zoo: use action and take a money."
      doTasks(
          *cardActionTasks("$MartianZoo"),
          "MC",
      )
    }
    green.turn {
      // (7:29:24 PM) "Trans-Neptune Probe using two titanium."
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // no MC remains owed
          "$TransNeptuneProbe FROM ProjectCard",
      )
    }
    pink.turn {
      // (7:29:33 PM) "Orbital Cleanup for three."
      doTasks(
          *cardActionTasks("$OrbitalCleanup"),
          "3 MC",
      )
    }
    green.turn {
      // (7:29:41 PM) "I'm going to play City Parks because I do have three city tiles. Spend six,
      // and I get two plants."
      doTasks(
          *playCardTasks("$CityParks"),
          "6 Pay<> FROM MC",
          "$CityParks FROM ProjectCard",
          "2 Plant",
      )
    }
    pink.turn {
      // (7:29:44 PM) "I think I'm just gonna sell two patents and pass. There's nothing I can hope
      // for."
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
    }
    green.turn {
      // (7:30:09 PM) "Same here, two patents, and there's nothing I can do. I could pointlessly add
      // a floater to Floating Refinery, but it makes no difference. We're done, and we don't do any
      // Turmoil stuff."
      doTasks(
          *standardProjectTasks("SellPatentsProject"),
          "2 MC FROM ProjectCard",
      )
    }
    pink.doTask("Pass")

    green.doTask("Pass")

    // (7:30:35 PM) "We do hit production."
    // 19:30 final production. The apps label it generation 10; the game ends in generation 9.
    green.doTasks(
        "66 MC",
        "Steel",
        "4 Titanium",
        "Plant",
        "4 Energy",
        "6 Heat",
    )
    pink.doTasks(
        "57 MC",
        "Titanium",
        "2 Plant",
        "Heat FROM Energy",
        "5 Energy",
        "14 Heat",
    )
    green.assertResources(m = 69, s = 4, t = 4, p = 4, e = 4, h = 9)
    pink.assertResources(m = 68, s = 0, t = 1, p = 6, e = 5, h = 15)
    green.assertProduction(m = 25, s = 1, t = 4, p = 1, e = 4, h = 6)
    pink.assertProduction(m = 15, s = 0, t = 1, p = 2, e = 5, h = 14)
    green.assertCounts(
        41 to "TerraformRating",
        0 to "ProjectCard",
        4 to "GreeneryTile",
        4 to "PartyLeader",
    )
    pink.assertCounts(
        42 to "TerraformRating",
        0 to "ProjectCard",
        4 to "GreeneryTile",
        2 to "PartyLeader",
        1 to "Chairman",
    )
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9, venus = 22)
    // (7:30:41 PM) Pink: "I have six plants."
    // Green: "I have less than that, so no greeneries to place."
    // Neither player has eight plants. There is no final Turmoil phase.
    green.doTask("PassBaton")
    pink.doTask("PassBaton")
    // (7:30:53 PM) Green: "I think I won both awards."
    // Pink: "I think so. And I rejected Thermalist. Special tile points: I believe I get three."
    // Green: "No special tile points for me."
    // Pink: "Also three city points and four greeneries."
    // Green: "We only had four greeneries."
    // Pink: "You got a city almost completely surrounded."
    // Green: "No resource points."
    // Pink: "Three resource points."
    // Green: "I get one VP per Jupiter tag, and I have four Jupiter tags."
    // "Make sure your preludes don't have victory points on them. I'm not used to having to check
    // for that."
    // Pink: "I have 13 VP on cards, which takes me to 73."
    // Green: "I have 88 VP. 88 to 73. Oh, I didn't do these points. I get four, you get two, right?
    // Or do you get one for the chairman too?"
    // Pink: "I feel like I get one for the chairman."
    // Green: "No, I don't think so, because you already got TR when you went there. You can look it
    // up. I think I actually got 92. How did you add it?"
    // Pink: "75. I put it in VP on cards."
    // Green: "I'll go back to VP on cards and add four. Or I could have done it as resources. 92 to
    // 75."
    // The table announces 92–75. Pink omits the chairman point but overcounts printed cards as 13
    // instead of 12, consistent with overlooking Aerial Lenses’ −1. The errors cancel. Per user
    // instruction, let the chairman point score normally; no score adjustment is made.
    val score = Summarizer(game)
    assertEquals(1, score.net("Chairman", "VictoryPoint<Pink>"))
    assertEquals(4, score.net("PartyLeader", "VictoryPoint<Green>"))
    assertEquals(2, score.net("PartyLeader", "VictoryPoint<Pink>"))
    green.assertCounts(92 to "VictoryPoint", 1 to "Victory")
    assertEquals(-1, score.net("$AerialLenses", "VictoryPoint<Pink>"))
    assertEquals(3, score.net("$Capital", "VictoryPoint<Pink>"))
    assertEquals(3, score.net("$RefugeeCamps", "VictoryPoint<Pink>"))
    // The table counts Capital and camp resources separately from printed card points.
    assertEquals(12, score.net("Card", "VictoryPoint<Pink>") - 3 - 3)
    pink.assertCounts(75 to "VictoryPoint", 0 to "Victory")
  }
}
