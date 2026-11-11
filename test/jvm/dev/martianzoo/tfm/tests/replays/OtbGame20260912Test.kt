package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.Player
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlin.test.assertEquals

/** Three-player physical game begun Saturday, 2026-09-12. */
internal class OtbGame20260912Test : FullGameTest() {
  override val firstGlobalEvents =
      listOf(
          "ThawMining",
          "MartianProtectionism",
          "ScienceSummit",
          "BioengineeringBoom",
          "FreeAcademiaTreaty",
          "SecondEnergyCrisis",
          "WorldGovernmentDirectives",
          "MoholeLakeGlobalEvent",
          "ExperimentalLifeforms",
          "CharismaticWgPresident",
      )
  // The physical deck included Summit Logistics without Colonies, and the recorded game had no
  // colony board. Name the ordinary card explicitly because its colony count contributes zero,
  // while the Prelude 2 card pack selects it automatically only when Colonies is available.
  override val config =
      GameConfig(
          """
          VastitasMap
          VenusNextExpansion, PreludeExpansion, prelude2:cards, TurmoilExpansion, promos:cards
          SummitLogistics
          fakes:cards

          Farmer, Generalist, Lobbyist, Philantropist, Producer
          Collector, Forecaster, Landscaper, Politician, Suburbian
          """
      )

  // The second transcript states Blue's three-TR handicap; blue-applog-01.png records it before
  // Tycho Magnetics and the initial project purchase.
  override val playerClassPets =
      """
      CLASS Green : Player { SetupPhase: PreludeCard<This> }
      CLASS Yellow : Player { SetupPhase: PreludeCard<This> }
      CLASS Blue : Player { SetupPhase: 3 TerraformRating<This>, PreludeCard<This> }
      """
          .trimIndent()

  private val green
    get() = player(1)

  private val yellow
    get() = player(2)

  private val blue
    get() = player(3)

  @Test
  internal fun completeGame() {
    startGame()
    blue.doTask("3 TerraformRating")
    green.doTask("20 TerraformRating")
    yellow.doTask("20 TerraformRating")
    blue.doTask("20 TerraformRating")
    green.doTask("PreludeCard")
    yellow.doTask("PreludeCard")
    blue.doTask("PreludeCard")
    green.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    yellow.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    blue.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    green.doTask("Ok") // Keep all ten starting projects.
    yellow.doTask("-3 ProjectCard<Selecting>")
    blue.doTask("-2 ProjectCard<Selecting>")
    generation1()
    generation2()
    generation3()
    generation4()
    generation5()
    generation6()
    generation7()
    generation8()
    generation9()
  }

  private fun generation1() {
    // "Our coming global event is Mud Slides, and our distant global event is Venus
    // Infrastructure."
    admin.count("Coming<ThawMining>") shouldBe 1
    (admin.count("PartyLeader<Kelvinists, Neutral>") > 0) shouldBe true
    admin.count("Distant<MartianProtectionism>") shouldBe 1
    (admin.count("PartyLeader<MarsFirst, Neutral>") > 0) shouldBe true

    with(green) {
      // "Septim Triboos [Septem Tribus]. I get 36 money, and then I buy 10 cards. Don't tell me I
      // bought 10."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$SeptemTribus>>",
          "$SeptemTribus FROM CorporationCard",
          "36 MC",
          "30 Pay<> FROM MC",
          "10 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(yellow) {
      // "Pristar. I get 53 money. I lose two TR. Not super happy. And I buy seven cards."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Pristar>>",
          "$Pristar FROM CorporationCard",
          "53 MC",
          "-2 TerraformRating",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(blue) {
      // "My corporation is Tycho Magnetics. I start with 42. Not 42 money production. But one
      // energy
      // production."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$TychoMagnetics>>",
          "$TychoMagnetics FROM CorporationCard",
          "42 MC",
          "PROD[Energy]",
          "24 Pay<> FROM MC",
          "8 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      // "Nobel Prize. My second wild tag. I have no tags, but I have two wild tags."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NobelPrize>>",
          "$NobelPrize FROM PreludeCard",
          "5 MC",
      )
      doTask("2 SearchForCard<RequirementFilter>").expect("2 ProjectCard")
      // "Experimental Forest. My greenery. I'm going to go on six two. For dos cardos. And for
      // moneyos."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$ExperimentalForest>>",
          "$ExperimentalForest FROM PreludeCard",
      )
      doTask("2 SearchForCard<TagFilter<Class<PlantTag>>>").expect("2 ProjectCard")
      doTasks("GreeneryTile<Vastitas_6_2>", "OxygenStep", "2 ProjectCard", "TerraformRating")
      // The players acknowledged that paying the initial Greens policy during Prelude was a rule
      // mistake. Green nevertheless took 4 M€ here (transcript and Green ledger entry 6).
      green.exMachina("4 MC")

      // "Y'all ready for this?" "Is it a head start?" "I have 16 project cards in hand. Imagine if
      // we had a planner milestone. So I gain 32 money. Then I immediately take two actions. You
      // guys are never going to get to take any turns."
      // SF Memorial's sentence is missing from the transcript. Green's first ledger segment and
      // every tableau photo place it in Head Start's first action. The newer transcript restores
      // the second: "Let's just use my free delegate thingy ... put it in Scientists."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$FakeHeadStart>>",
          "$FakeHeadStart FROM PreludeCard",
          "2 Steel",
          "32 MC",
      )
      doTasks(
          *playCardTasks("$SfMemorial"),
          "2 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$SfMemorial FROM ProjectCard",
          "ProjectCard",
          "UseAction<LobbyAction, Action1>",
          "PartyLeader<Scientists> FROM LobbyDelegate",
      )
    }
    with(yellow) {
      // "Martian Industries." "Nice." "Gain energy production, steel production, and gain six
      // monies."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>",
          "$MartianIndustries FROM PreludeCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
      )
      // "Applied Science is a wild tag, not that it counts for anything yet. I immediately add six
      // science resources, and it's going to let me gain stuff. Stuff and stuff."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
          "$AppliedScience FROM PreludeCard",
          "6 Science<$AppliedScience>",
      )
      // "Established methods. Gain 30 monies." "Wow." "I'm gonna greenery for 23. I'm gonna put
      // it here for two cards... And that gave me the temperatura."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$EstablishedMethods>>",
          "$EstablishedMethods FROM PreludeCard",
          "30 MC",
          "UseAction<GreeneryProject, Action1>",
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_5_9>",
          "OxygenStep",
          "2 ProjectCard",
          "TerraformRating",
          "UseAction<AsteroidProject, Action1>",
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      // The players acknowledged this initial Greens-policy payout during Prelude as the same rule
      // mistake. Yellow nevertheless took 4 M€ here (transcript and Yellow ledger entry 15).
      yellow.exMachina("4 MC")
    }
    with(blue) {
      // "I got some boring ones, but hopefully they'll be good. Allied Bank." "Oh, that's a good
      // one." "I increased my money production by four and I give myself three loose monies. Dome
      // Farming. I increased my money production by two and plant production by one. And Power
      // Generator [Power Generation]. One, two, three energy production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AlliedBank>>",
          "$AlliedBank FROM PreludeCard",
          "PROD[4 MC]",
          "3 MC",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
          "PROD[Plant]",
          "PROD[2 MC]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>",
          "$PowerGeneration FROM PreludeCard",
          "PROD[3 Energy]",
      )
    }

    with(green) {
      // "Artificial Photosynthesis. I spent twelve, and I get two energy production. Actually,
      // no, I'm going to also spend five to put another one of my dudes in Unity."
      doTasks(
          *playCardTasks("$ArtificialPhotosynthesis"),
          "12 Pay<> FROM MC",
          "$ArtificialPhotosynthesis FROM ProjectCard",
          "PROD[2 Energy]",
      )
      // "Actually, no, I'm going to also spend five to put another one of my dudes in community
      // [Unity]."
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Unity>")
    }
    with(yellow) {
      // "I'm going to lava flows for 18. Raise temperature, two steps. Oh, and I get the heat. Oh,
      // this is the lava. Two, six."
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "LavaFlows_SpecialTile<Vastitas_2_6>",
          "ProjectCard",
      )
      // "I'm also going to place my delegate in the Grens [Greens] since that's coming into power.
      // I was going to try and go for red since that would work better with Prestar [Pristar], but
      // now I've terraformed. I screwed that up."
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
    }
    with(blue) {
      // "I'm going to pay five for Power Supply Consortium. I believe I'm increasing [Green's]
      // power production by one. And I gain one."
      doTasks(
          *playCardTasks("$PowerSupplyConsortium"),
          "5 Pay<> FROM MC",
          "$PowerSupplyConsortium FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Green>]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I'm not liking all of these options." "But I am liking this option." "So I pay seven for
      // that. The requirement is met and I get a plant production."
      // The latest photo identifies the otherwise unnamed card as Lichen.
      doTasks(
          *playCardTasks("$Lichen"),
          "7 Pay<> FROM MC",
          "$Lichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Hermetic Order of Mars. Pay 10. Oxygen must be 4% or lower." "Sure is." "Gain two money
      // production. Gain a money per empty area adjacent to my tiles. That is seven." "Heck yeah."
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "10 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "7 MC",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "I am paying 13." "Blue paying 13 and two money production. In order to gain two heat
      // production and two more energy production." "My goodness." "On lunar beam."
      doTasks(
          *playCardTasks("$LunarBeam"),
          "13 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I think I'm going to pay five to put another doodage in red."
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Reds>")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Titanium mine. Pay seven, gain titanium production."
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "7 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "I like science. Put me in the scientist party."
      // "The ruler's blue lobbyist is in the science party."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    // "Oh, dominance moved over to the Scientists." Green's recovered Head Start placement and
    // Blue's placement make Scientists the first party with two delegates.
    with(green) {
      // "Well, that's good in some ways. I think I'll go ahead and use my Septim Tribus [Septem
      // Tribus] action now. And that means I get six money, right?" "Six McSeas."
      doTasks(*cardActionTasks("$SeptemTribus"), "6 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Soych for life [Search for Life] for three."
      doTasks(
          *playCardTasks("$SearchForLife"),
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "Dang. You know what? I can't use it yet, but I can afford it right now. So I'm going to
      // pay one money for Directed Heat Usage."
      doTasks(
          *playCardTasks("$DirectedHeatUsage"),
          "1 Pay<> FROM MC",
          "$DirectedHeatUsage FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Play orbital cleanup. And I lose two money production."
      doTasks(
          *playCardTasks("$OrbitalCleanup"),
          "14 Pay<> FROM MC",
          "$OrbitalCleanup FROM ProjectCard",
          "PROD[-2 MC]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I use soych for life [Search for Life] for three."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag was revealed.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    with(green) {
      // "I will use Orbital Cleanup to take three money." Artificial Photosynthesis supplies one
      // printed Science tag, and both wild tags are assigned Science for this action.
      doTasks(*cardActionTasks("$OrbitalCleanup"), "3 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Decisions, decisions." "You should take mine." "We're reducing blue's energy production
      // by one."
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Blue>]",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    yellow.doTask("Pass")

    // Production.
    green.doTasks("19 MC", "Energy", "Plant")
    yellow.doTasks("24 MC", "2 Energy", "Steel", "Heat", "Titanium")
    blue.doTasks("27 MC", "6 Energy", "Plant", "2 Heat")

    // Complete phone-ledger resource checkpoint after Generation 1 production. World Government
    // is pending, so the Turmoil TR revision must not have happened yet.
    with(green) {
      assertResources(m = 29, s = 0, t = 0, p = 1, e = 1, h = 0)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 24, s = 1, t = 1, p = 0, e = 2, h = 1)
      assertCounts(22 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 29, s = 0, t = 0, p = 1, e = 6, h = 2)
      assertCounts(23 to "TerraformRating")
    }
    // "I will world government." "Five, eight." "So just increase oceans by one?"
    expectChanges(
        "0 TerraformRating<Green>, ScienceSummit, Distant<ScienceSummit>, PartyLeader<Scientists, Neutral>"
    ) {
      green.doTask("OceanTile<Vastitas_5_8>! BY Admin")
    }
    // "Oh, turmoil." "Walk us through it." "TR Revision, all players lose one TR." "Boo."
    // "New Government. The dominant party now becomes ruling. Change policy title to Scientists."
    // The Turmoil phase that follows performs TR revision, forms the Scientists government, and
    // advances both printed events before requesting only the next distant event.
    // "Changing times. Got the coming global event. Move a distant global event to coming. Turn the
    // top part of global event face up. It is sponsored projects."
    (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
    admin.count("Distant<ScienceSummit>") shouldBe 1
    (admin.count("PartyLeader<Scientists, Neutral>") > 0) shouldBe true
  }

  private fun generation2() {
    // "Really, because you're the chairman. Oh, did you give yourself TR for being chairman?" "No,
    // because you didn't say it." "Oh, yeah, that is what I skipped. Sorry."
    // Green placed the first Scientist delegate and remains its leader when Blue ties it. Green
    // therefore becomes chairman and receives the recorded TR normally. Mud Slides' neutral
    // delegate makes Greens uniquely dominant with Yellow plus neutral.

    // The three complete phone histories: Generation 2 before Research.
    with(green) {
      assertProduction(m = -2, s = 0, t = 0, p = 1, e = 1, h = 0)
      assertResources(m = 30, s = 0, t = 0, p = 1, e = 1, h = 0)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 1, p = 0, e = 2, h = 1)
      assertCounts(1 to "ScienceTag")
      assertResources(m = 25, s = 1, t = 1, p = 0, e = 2, h = 1)
      assertCounts(21 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 6, h = 2)
      assertResources(m = 30, s = 0, t = 0, p = 1, e = 6, h = 2)
      assertCounts(22 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -24, oxygen = 2, oceans = 1, venus = 0)
    admin.assertCounts(
        1 to "Ruling<Scientists>",
        1 to "Dominant<Greens>",
        1 to "Current<ThawMining>",
        1 to "Coming<MartianProtectionism>",
        1 to "Distant<ScienceSummit>",
    )

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 2 Research: "I buy four"; Yellow buys two; Blue buys three.
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I will pay three for Supported Research. Requires scientists are in power. They sure
      // are. Draw two cards."
      doTasks(
          *playCardTasks("$SupportedResearch"),
          "3 Pay<> FROM MC",
          "$SupportedResearch FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "Blue is paying six money ... to build Fuel Factory. So I'm going to lose one energy
      // production, but gain a titanium and a money production."
      doTasks(
          *playCardTasks("$FuelFactory"),
          "6 Pay<> FROM MC",
          "$FuelFactory FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I will play Investment Loan for three and get 10 and lose the money production."
      doTasks(
          *playCardTasks("$InvestmentLoan"),
          "3 Pay<> FROM MC",
          "$InvestmentLoan FROM ProjectCard",
          "PROD[-MC]",
          "10 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I will play Local Shading for four."
      doTasks(*playCardTasks("$LocalShading"), "4 Pay<> FROM MC", "$LocalShading FROM ProjectCard")
      doTask("PassBaton")
    }
    with(blue) {
      // "Put my blue lobbyist in the Unity party, please."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm gonna use my free action to put a dude into Greens."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I'll put my free guy in Reds."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Reds> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(blue) {
      // "Blue's going to pay five money to put another delegate in Unity."
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Unity>")
      doTask("PassBaton")
    }
    with(green) {
      // "Recruitment in the Greens ... I become the party leader. I paid two for that."
      doTasks(
          *playCardTasks("$Recruitment"),
          "2 Pay<> FROM MC",
          "$Recruitment FROM ProjectCard",
          "PartyLeader<Greens> FROM PartyFollower<Greens, Neutral>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "Pay five to place a dude in Reds." "Aww, that's no fair." "Yeah. I laid the track."
      // This is Yellow's turn between Green's Recruitment and Blue's House Printing. Her ledger
      // includes the 5 M€ payment, board-12-18-40.jpg shows both Yellow delegates in Reds, and
      // she later cites those two delegates as satisfying Red Appeasement.
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<Reds>")
      doTask("PassBaton")
    }
    with(blue) {
      // "Blue is paying ten for House Printing, which gives me a steel production."
      doTasks(
          *playCardTasks("$HousePrinting"),
          "10 Pay<> FROM MC",
          "$HousePrinting FROM ProjectCard",
          "PROD[Steel]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I'm gonna pay five and put a dude in Kelvinists."
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Kelvinists>")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Local Shading, add floater."
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }
    blue.doTask("Pass")

    with(green) {
      // "Oh, right. It's your turn." "You're gonna pay five ... to maybe go to Scientists? Wow."
      // Green has six delegates deployed and the seventh still in reserve. The later photograph
      // shows Green represented in Scientists, and Green's aggregated ledger includes this 5 M€
      // payment along with Recruitment, Kelvinists, and Lobbyist.
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Scientists>")
      doTask("PassBaton")
    }

    with(yellow) {
      // "Search for Life. Pay one ... It was Public Celebrations."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag was revealed.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      doTask("PassBaton")
    }
    with(green) {
      // "I am going to pay eight for the Lobbyist."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Lobbyist")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Applied Science floater onto Local Shading."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Floater<$LocalShading>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I am going to use my Septem Tribus action to take ten money." Green is represented in
      // five parties; the recovered Generation 1 Scientist delegate became chairman rather than
      // remaining in Mars First.
      doTasks(*cardActionTasks("$SeptemTribus"), "10 MC")
      doTask("PassBaton")
    }
    yellow.doTask("Pass")

    with(green) {
      // "I am going to play Event Analysts, which costs five. You have an extra influence
      // permanently."
      doTasks(
          *playCardTasks("$EventAnalysts"),
          "5 Pay<> FROM MC",
          "$EventAnalysts FROM ProjectCard",
      )
    }
    with(green) {
      // "Use Orbital Cleanup to take four money."
      // Artificial Photosynthesis and Event Analysts supply two printed Science tags; Septem
      // Tribus and Nobel Prize supply the two chosen wild tags.
      doTasks(*cardActionTasks("$OrbitalCleanup"), "4 MC")
    }
    with(green) {
      // "Play Peroxide Power for seven money ... minus four [M€ production] ... two energy
      // production."
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "7 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
    }
    with(green) {
      // "Sell a card for money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
    }
    with(green) {
      // "I'm going to play Lightning Harvest for all eight of my money. I do have the three
      // science tags I need." The same two wild tags satisfy the printed requirement.
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "8 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
      )
    }
    green.doTask("Pass")

    // Production.
    green.doTasks("18 MC", "4 Energy", "Plant", "Heat FROM Energy")
    yellow.doTask("6 MC", Pristar)
    yellow.doTasks(
        "23 MC",
        "Preservation<$Pristar>",
        "2 Energy",
        "Steel",
        "Heat",
        "Titanium",
        "2 Heat FROM Energy",
    )
    blue.doTasks("27 MC", "5 Energy", "Plant", "2 Heat", "6 Heat FROM Energy", "Titanium", "Steel")

    // Reds, Greens, and Unity each have three delegates. The existing marker therefore remains on
    // Greens, matching the spoken government formation and board-12-18-40.jpg.
    admin.assertCounts(
        3 to "PartyDelegate<Reds>",
        3 to "PartyDelegate<Greens>",
        3 to "PartyDelegate<Unity>",
        1 to "Dominant<Greens>",
    )

    // Complete phone-ledger checkpoint after Generation 2 production. Pristar's preservation
    // reward is included because the players apply it immediately before World Government. The
    // game is now waiting for that World Government choice.
    with(green) {
      assertProduction(m = -3, s = 0, t = 0, p = 1, e = 4, h = 0)
      assertResources(m = 18, s = 0, t = 0, p = 2, e = 4, h = 1)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 1, p = 0, e = 2, h = 1)
      assertResources(m = 35, s = 2, t = 2, p = 0, e = 2, h = 4)
      assertCounts(21 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 5, s = 1, t = 1, p = 1, e = 5, h = 2)
      assertResources(m = 27, s = 1, t = 1, p = 2, e = 5, h = 10)
      assertCounts(22 to "TerraformRating")
    }
    // "Ellie does World Government ... ocean ... five seven." The Admin ocean awards Yellow no TR;
    // the Turmoil phase then begins at once, and its TR revision costs every player one.
    expectChanges(
        "-1 TerraformRating<Yellow>, BioengineeringBoom, Distant<BioengineeringBoom>, PartyLeader<Greens, Neutral>"
    ) {
      yellow.doTask("OceanTile<Vastitas_5_7>! BY Admin")
      green.doTask("-0 MC")
      yellow.doTask("-0 MC")
      blue.doTask("-0 MC")
    }
    // Mud Slides charges nobody because influence covers every adjacent owned tile. Greens forms
    // the government and Green becomes chairman.
    (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
    admin.count("Distant<BioengineeringBoom>") shouldBe 1
    (admin.count("PartyLeader<Greens, Neutral>") > 0) shouldBe true
    // "So you're saying this moves to Unity now?" During Changing Times, Venus Infrastructure's
    // neutral delegate raises Unity from three delegates to four, ahead of Reds' three. The engine
    // therefore moves the dominance marker to Unity normally, matching board-12-18-40.jpg.
    admin.assertCounts(
        3 to "PartyDelegate<Reds>",
        4 to "PartyDelegate<Unity>",
        1 to "Dominant<Unity>",
    )
  }

  private fun generation3() {
    // The three complete phone histories: Generation 3 before Research.
    with(green) {
      assertProduction(m = -3, s = 0, t = 0, p = 1, e = 4, h = 0)
      assertResources(m = 20, s = 0, t = 0, p = 2, e = 4, h = 1)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 2, s = 1, t = 1, p = 0, e = 2, h = 1)
      assertResources(m = 35, s = 2, t = 2, p = 0, e = 2, h = 4)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 5, s = 1, t = 1, p = 1, e = 5, h = 2)
      assertResources(m = 28, s = 1, t = 1, p = 2, e = 5, h = 10)
      assertCounts(21 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -24, oxygen = 2, oceans = 2, venus = 0)
    admin.assertCounts(
        1 to "Ruling<Greens>",
        1 to "Dominant<Unity>",
        1 to "Current<MartianProtectionism>",
        1 to "Coming<ScienceSummit>",
        1 to "Distant<BioengineeringBoom>",
    )

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 3 Research: all three players buy three projects.
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      // "I'm definitely spending three heat in order to gain four money. And ... that was Blue."
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to use my free action to put a dude into Mars First." The former Green chairman
      // and one non-leader Green delegate returned when Greens formed, so this placement is legal.
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Sponsored Academies. I pay nine. I pitch a card and draw three and everyone else gets
      // one."
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Yellow) { ProjectCard<Other@Player, Hand>! }",
          "3 ProjectCard",
          "ProjectCard<Green, Hand>",
          "ProjectCard<Blue, Hand>",
      )
      // "I guess I have Local Shading. Remove floater to gain money production."
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
    }
    with(blue) {
      // "Blue ... spending eight money to get the Generalist."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Generalist")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to use my Septem Tribus action to take 10 money."
      doTasks(*cardActionTasks("$SeptemTribus"), "10 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "I spend two titanium and six real on Space Hotels. Increase money production four
      // steps." Sponsored Academies supplies one Earth tag; Applied Science's wild tag is the
      // second Earth tag for the requirement.
      doTasks(
          *playCardTasks("$SpaceHotels"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "6 Pay<> FROM MC",
          "$SpaceHotels FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "Blue is spending one steel and seven money ... Natural Preserve ... up here for two
      // steel."
      doTasks(
          *playCardTasks("$NaturalPreserve"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$NaturalPreserve FROM ProjectCard",
          "PROD[MC]",
          "NaturalPreserve_SpecialTile<Vastitas_4_1>",
          "2 Steel",
      )
      // "For my second action, I'm going to pay eight money ... Producer."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Producer")
    }
    with(green) {
      // "Water Splitting Plant. We have the two oceans. I pay the 12 money."
      doTasks(
          *playCardTasks("$WaterSplittingPlant"),
          "12 Pay<> FROM MC",
          "$WaterSplittingPlant FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Apply Science floater onto Local Shading. And I will pay two steel for Mine to get a
      // steel production."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Floater<$LocalShading>",
      )
      doTasks(
          *playCardTasks("$Mine"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
    }
    with(blue) {
      // "Blue, spending five energy for five cards. And I will keep one."
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-5 Energy",
          "5 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Use my Water Splitting Plant, spend three energy, raise oxygen to three, and get a TR."
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I add a delegate in Unity ... for an influence."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
      // "Red Appeasement for zero ... gain two money production and this counts as me passing."
      doTasks(*playCardTasks("$RedAppeasement"), "$RedAppeasement FROM ProjectCard", "PROD[2 MC]")
      doTask("Pass")
    }
    with(blue) {
      // "Blue is going to send her lobbyist to ... Greens."
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to use Orbital Cleanup ... and get four money." Green again chooses both wild
      // tags as Science.
      doTasks(*cardActionTasks("$OrbitalCleanup"), "4 MC")
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    green.doTask("Pass")

    // Production.
    green.doTasks("19 MC", "4 Energy", "Plant", "Heat FROM Energy")
    yellow.doTask("6 MC", Pristar)
    yellow.doTasks(
        "29 MC",
        "Preservation<$Pristar>",
        "2 Energy",
        "2 Steel",
        "Heat",
        "Titanium",
        "2 Heat FROM Energy",
    )
    blue.doTasks("27 MC", "5 Energy", "Plant", "2 Heat", "Titanium", "Steel")

    // board-12-36-50.jpg and the phone histories show these complete physical ledgers after
    // production and before World Government or Turmoil. Yellow's phone still reads 40 M€ because
    // its 6 M€ Pristar reward is entered later with the Solar payouts; the engine has already
    // awarded it and therefore reads 46 M€ here. The game is waiting for World
    // Government, so no Turmoil TR revision has happened yet.
    with(green) {
      assertProduction(m = -3, s = 0, t = 0, p = 1, e = 4, h = 0)
      assertResources(m = 32, s = 0, t = 0, p = 3, e = 4, h = 2)
      assertCounts(22 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 9, s = 2, t = 1, p = 0, e = 2, h = 1)
      assertResources(m = 46, s = 2, t = 1, p = 0, e = 2, h = 7)
      assertCounts(20 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 5, h = 2)
      assertResources(m = 27, s = 3, t = 2, p = 3, e = 5, h = 9)
      assertCounts(21 to "TerraformRating")
    }
    assertSidebar(gen = 3, temp = -24, oxygen = 3, oceans = 2, venus = 0)
    // "Why not Venus? Venus it is. ... Venus is now at two." The Turmoil phase follows at once:
    // Blue loses one TR to revision and regains it as chairman.
    expectChanges(
        "0 TerraformRating<Blue>, FreeAcademiaTreaty, Distant<FreeAcademiaTreaty>, PartyFollower<Scientists, Neutral>"
    ) {
      blue.doTask("VenusStep! BY Admin")
      green.doTasks("0 MC", "6 MC")
      yellow.selectTask("2! MC<Yellow> / VenusTag<Yellow> MAX 5")
      yellow.doTasks("2 MC", "2 MC")
      blue.doTasks("0 MC", "4 MC")
    }
    // Venus Infrastructure pays Green 6 M€, Yellow 4 M€, and Blue 4 M€ from their recorded
    // Venus tags and influence. Unity then pays 1/3/2 M€ for planetary tags and makes Blue
    // chairman. Diversity—the event headed "Free Academia Treaty"—is revealed with its neutral
    // Scientist delegate.
    (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
    admin.count("Distant<FreeAcademiaTreaty>") shouldBe 1
    (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
  }

  private fun generation4() {
    // Complete Generation 3 Solar ledgers.
    with(green) {
      assertProduction(m = -3, s = 0, t = 0, p = 1, e = 4, h = 0)
      assertResources(m = 39, s = 0, t = 0, p = 3, e = 4, h = 2)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 9, s = 2, t = 1, p = 0, e = 2, h = 1)
      assertResources(m = 53, s = 2, t = 1, p = 0, e = 2, h = 7)
      assertCounts(19 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 6, s = 1, t = 1, p = 1, e = 5, h = 2)
      assertResources(m = 33, s = 3, t = 2, p = 3, e = 5, h = 9)
      assertCounts(21 to "TerraformRating")
    }
    assertSidebar(gen = 4, temp = -24, oxygen = 3, oceans = 2, venus = 2)
    admin.assertCounts(
        1 to "Ruling<Unity>",
        1 to "Dominant<Reds>",
        1 to "Current<ScienceSummit>",
        1 to "Coming<BioengineeringBoom>",
        1 to "Distant<FreeAcademiaTreaty>",
    )

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 4 Research: Green buys zero, Yellow buys one, and Blue buys three. The complete
    // ledgers disambiguate which voice owns each purchase.
    green.doTask("-4 ProjectCard<Selecting>")
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "Cultural Metropolis, for 20. No discounts." The photographed city at Vastitas 6-9
      // supplies the recorded two titanium and two M€ placement bonuses.
      doTasks(
          *playCardTasks("$CulturalMetropolis"),
          "20 Pay<> FROM MC",
          "$CulturalMetropolis FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Vastitas_6_9>",
          "2 Titanium",
          "2 MC",
          "PartyFollower<Reds>",
          "PartyLeader<Reds>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I'm gonna go place my free delegate in Punity." The board photo shows that the otherwise
      // unlabeled speaker placed Yellow's delegate, not Green's.
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(blue) {
      // "Mining Area. I'm going to pay two steel for it."
      doTasks(
          *playCardTasks("$MiningArea"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$MiningArea FROM ProjectCard",
          "MiningArea_SpecialTile<Vastitas_4_2>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningArea_SpecialTile)>)>",
          "Titanium",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I am going to design some microorganisms. Pay 16 ... two plant production."
      doTasks(
          *playCardTasks("$DesignedMicroorganisms"),
          "16 Pay<> FROM MC",
          "$DesignedMicroorganisms FROM ProjectCard",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I'm gonna pay six for Archaebacteria, for a plant production."
      doTasks(
          *playCardTasks("$Archaebacteria"),
          "6 Pay<> FROM MC",
          "$Archaebacteria FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // "Asteroid Hollowing ... I need one titanium to actually put on the card, so I'll take one
      // back and pay four more money." Final payment was two titanium and eight M€.
      doTasks(
          *playCardTasks("$AsteroidHollowing"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "8 Pay<> FROM MC",
          "$AsteroidHollowing FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "I will use Orbital Cleanup to get five money."
      doTasks(*cardActionTasks("$OrbitalCleanup"), "5 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Security Fleet for 12." "I'm gonna spend a titanium to put a fighter on it."
      doTasks(
          *playCardTasks("$SecurityFleet"),
          "Ok", // No titanium paid.
          "12 Pay<> FROM MC",
          "$SecurityFleet FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
    }
    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Free lobby into Kelvinists"; then "Sponsored Mohole for five ... two heat production."
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Kelvinists> FROM LobbyDelegate")
      doTasks(
          *playCardTasks("$SponsoredMohole"),
          "5 Pay<> FROM MC",
          "$SponsoredMohole FROM ProjectCard",
          "PROD[2 Heat]",
      )
    }
    with(yellow) {
      // "Research Coordination for four. And then Interplanetary Trade." Its nine-step increase
      // brings Yellow's M€ production from nine to the photographed 18.
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$InterplanetaryTrade"),
          "27 Pay<> FROM MC",
          "$InterplanetaryTrade FROM ProjectCard",
          "EACH @WildTagElection { Tag<@WildTagElection>? }",
          "JovianTag<WildTagElection<WildTag<$AppliedScience>>>",
          "PlantTag<WildTagElection<WildTag<$ResearchCoordination>>>",
          "PROD[9 MC]",
      )
    }
    with(blue) {
      // "Bioprinting Facility ... one steel and five money." "Spend two energy, gain two plants."
      doTasks(
          *playCardTasks("$BioPrintingFacility"),
          "1 Pay<Class<Steel>> FROM Steel",
          "5 Pay<> FROM MC",
          "$BioPrintingFacility FROM ProjectCard",
      )
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
    }

    with(green) {
      doTasks(*cardActionTasks("$SeptemTribus"), "8 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag was revealed.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      doTask("PassBaton")
    }
    with(blue) {
      // Blue's ledger separately records two energy for Bioprinting and one here for Tycho.
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    green.doTask("Pass")

    with(yellow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(blue) {
      // "Business Contacts for seven"; the two retained projects are not named in the record.
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTasks(
          *playCardTasks("$RadChemFactory"),
          "8 Pay<> FROM MC",
          "$RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
      )
    }

    with(blue) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
    }
    blue.doTask("Pass")

    // Production.
    green.doTasks("22 MC", "3 Energy", "3 Plant", "Heat FROM Energy", "2 Heat")
    yellow.doTask("6 MC", Pristar)
    yellow.doTasks(
        "38 MC",
        "Preservation<$Pristar>",
        "2 Energy",
        "2 Steel",
        "Heat",
        "Titanium",
        "2 Heat FROM Energy",
        "Plant",
    )
    blue.doTasks(
        "30 MC",
        "4 Energy",
        "Plant",
        "2 Heat",
        "2 Titanium",
        "Steel",
        "2 Heat FROM Energy",
    )

    // Yellow added Pristar's preservation resource, which is visible when Sponsored Projects is
    // resolved, but neither the complete ledger nor the spoken production includes its 6 M€.
    yellow.exMachina("-6 MC")

    // Complete photographed phone ledgers after Generation 4 production and before World
    // Government. The board photograph still shows oxygen at four.
    with(green) {
      assertProduction(m = 0, s = 0, t = 0, p = 3, e = 3, h = 2)
      assertResources(m = 35, s = 0, t = 2, p = 6, e = 3, h = 5)
      assertCounts(22 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 19, s = 2, t = 1, p = 1, e = 2, h = 1)
      assertResources(m = 38, s = 4, t = 1, p = 1, e = 2, h = 10)
      assertCounts(19 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 7, s = 1, t = 2, p = 1, e = 4, h = 2)
      assertResources(m = 32, s = 1, t = 2, p = 6, e = 4, h = 10)
      assertCounts(23 to "TerraformRating")
    }
    assertSidebar(gen = 4, temp = -24, oxygen = 4, oceans = 2, venus = 2)
    // "World Government ... oxygen." Sponsored Projects resolves before Reds form government;
    // the automatic Turmoil phase then advances Spin-Off Products and Diversity. The newly drawn
    // card is Improved Energy Templates, headed "Second Energy Crisis" in the physical deck.
    expectChanges(
        "SecondEnergyCrisis, Distant<SecondEnergyCrisis>, 2 PartyFollower<Scientists, Neutral>"
    ) {
      green.doTask("OxygenStep! BY Admin")
      green.doTasks(
          "EACH Card@ResourceHolder<Player>(HAS CardResource) { CardResource<Card@ResourceHolder>* BY Admin }",
          "3 ProjectCard",
      )
      yellow.doTask("ProjectCard")
      blue.doTask("ProjectCard")
    }
    // At 6:38:48 pm, the table notes that Search for Life has no science to receive a bonus.
    yellow.assertCardResources(0 to SearchForLife)
    (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
    admin.count("Distant<SecondEnergyCrisis>") shouldBe 1
    (admin.count("PartyFollower<Scientists, Neutral>") > 0) shouldBe true
  }

  private fun generation5() {
    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 2, venus = 2)
    admin.assertCounts(
        1 to "Ruling<Reds>",
        1 to "Dominant<Scientists>",
        1 to "Current<BioengineeringBoom>",
        1 to "Coming<FreeAcademiaTreaty>",
        1 to "Distant<SecondEnergyCrisis>",
    )
    // "Yellow, six. Blue has ten. Green has fourteen."
    yellow.assertCounts(6 to "ProjectCard")
    blue.assertCounts(10 to "ProjectCard")
    green.assertCounts(14 to "ProjectCard")

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 5 Research after the dinner break: Yellow and Green buy three; Blue buys one.
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    // Blue says she buys one, but Blue's continuous ledger has no 3 M€ purchase and every later
    // payment reaches the photographed balance only if that charge was missed physically.
    blue.exMachina("3 MC")
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Collector")
      doTask("PassBaton")
    }
    with(blue) {
      // "Blue is going to pay most of her money, 22, to build Stratopolis."
      doTasks(
          *playCardTasks("$Stratopolis"),
          "22 Pay<> FROM MC",
          "$Stratopolis FROM ProjectCard",
          "PROD[2 MC]",
          "NormalCityTile<Stratopolis_RemoteArea>",
      )
      // The physical game omitted Stratopolis's two M€ production; Blue's ledger and the
      // board-21-13-22.jpg production track both retain eight.
      exMachina("PROD[-2 MC]")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to put my free delegate in reds."
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Reds> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(yellow) {
      // Dusk Laser Mining: one titanium and five M€. Satellites: three titanium and one M€.
      doTasks(
          *playCardTasks("$DuskLaserMining"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$DuskLaserMining FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "4 Titanium",
      )
      doTasks(
          *playCardTasks("$Satellites"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$Satellites FROM ProjectCard",
          "PROD[7 MC]",
      )
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // No titanium paid.
          "1 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SeptemTribus"), "8 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      // Symbiotic Fungus is revealed, so Search for Life succeeds.
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "ClaimCardReward<TagFilter<Class<MicrobeTag>>, $SearchForLife>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Stratopolis"), "2 Floater<$Stratopolis>")
      doTask("PassBaton")
    }
    with(green) {
      // Earth Catapult reduces Deuterium Export to nine; Mercurian Alloys makes each titanium
      // worth four, so the recorded payment is two titanium and one M€.
      doTasks(
          *playCardTasks("$DeuteriumExport"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$DeuteriumExport FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks(
          *playCardTasks("$AsteroidDeflectionSystem"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<Class<Steel>> FROM Steel",
          "8 Pay<> FROM MC",
          "$AsteroidDeflectionSystem FROM ProjectCard",
          "PROD[-Energy]",
      )
      // The physical game also omitted Asteroid Deflection System's energy-production decrease.
      exMachina("PROD[Energy]")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$DeuteriumExport"), "Floater<$DeuteriumExport>")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$ImportOfAdvancedGhg"),
          "9 Pay<> FROM MC",
          "$ImportOfAdvancedGhg FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*playCardTasks("$DustSeals"), "2 Pay<> FROM MC", "$DustSeals FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "5 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*playCardTasks("$RedShips"), "2 Pay<> FROM MC", "$RedShips FROM ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$HeatTrappers"),
          "4 Pay<> FROM MC",
          "$HeatTrappers FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-2 Heat<Yellow>]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$GhgFactories"),
          "4 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$GhgFactories FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 Heat]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // Venus Shuttles has no Space tag, so the reveal adds no asteroid.
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // No space tag was revealed.
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$CarbonateProcessing"),
          "4 Pay<> FROM MC",
          "$CarbonateProcessing FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 Heat]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$RedShips"), "MC")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<MarsFirst>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$Supercapacitors"),
          "2 Pay<> FROM MC",
          "$Supercapacitors FROM ProjectCard",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-2 Energy",
          "2 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    yellow.doTask("Pass")

    with(blue) {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
    }
    blue.doTask("Pass")

    // Production.
    green.doTasks("23 MC", "3 Energy", "3 Plant", "3 Heat FROM Energy", "5 Heat")
    yellow.doTask("6 MC", Pristar)
    yellow.doTasks(
        "46 MC",
        "Preservation<$Pristar>",
        "2 Steel",
        "5 Heat",
        "2 Titanium",
        "2 Heat FROM Energy",
        "Plant",
    )
    blue.doTasks("30 MC", "4 Energy", "Plant", "2 Heat", "2 Titanium", "Steel")
    // Green's photographed post-production energy and heat show that Supercapacitors was declined.
    green.doTask("Ok") // Keep the energy on Supercapacitors.
    with(green) {
      assertProduction(m = 1, s = 0, t = 0, p = 3, e = 3, h = 5)
      assertResources(m = 27, s = 0, t = 0, p = 9, e = 3, h = 13)
      assertCounts(22 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 27, s = 2, t = 2, p = 1, e = 0, h = 5)
      assertResources(m = 53, s = 2, t = 2, p = 2, e = 0, h = 17)
      assertCounts(19 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 8, s = 1, t = 2, p = 1, e = 4, h = 2)
      assertResources(m = 31, s = 1, t = 2, p = 9, e = 4, h = 9)
      assertCounts(22 to "TerraformRating")
    }
    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 2, venus = 2)

    // Yellow gained no TR during Generation 5, so this is the legitimate Pristar preservation
    // reward, not a table mistake. The engine suppresses it because its generation watcher still
    // sees Yellow's chairman TR from the preceding Solar transition.
    yellow.exMachina("6 MC")
    // The Admin ocean awards Yellow no TR; the Turmoil phase's TR revision then costs one.
    expectChanges(
        "-1 TerraformRating<Yellow>, WorldGovernmentDirectives, Distant<WorldGovernmentDirectives>, PartyFollower<Unity, Neutral>"
    ) {
      yellow.doTask("OceanTile<Vastitas_4_3>! BY Admin")
      green.selectTask("2! MC<Green> / ScienceTag<Green> MAX 5")
      green.doTasks("6 MC", "6 MC")
      yellow.doTasks("6 MC", "0 MC")
      blue.doTasks("4 MC", "0 MC")
    }
    // Spin-Off Products and the Scientists ruling bonus pay for science tags. Diversity becomes
    // current, Improved Energy Templates becomes coming, and Revolution is revealed.
    admin.count("PartyLeader<Scientists, Neutral>") shouldBe 1
    admin.count("Distant<WorldGovernmentDirectives>") shouldBe 1
    (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
    // Blue has two printed Science tags, so Spin-Off Products and the Scientists ruling bonus pay
    // 4 M€ and 2 M€. The transcript instead says, consecutively, "you get four money and you get
    // six money" for Blue's and Yellow's event awards; Blue's lone +10 row is exactly 4 + 6. The
    // likeliest entry mistake is therefore recording both announced event awards for Blue while
    // omitting Blue's own 2 M€ ruling bonus, a net overpayment of four.
    blue.exMachina("4 MC")
  }

  private fun generation6() {
    with(green) {
      assertResources(m = 42, s = 0, t = 0, p = 9, e = 3, h = 13)
      assertCounts(21 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 68, s = 2, t = 2, p = 2, e = 0, h = 17)
      assertCounts(18 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 41, s = 1, t = 2, p = 9, e = 4, h = 9)
      assertCounts(21 to "TerraformRating")
    }
    assertSidebar(gen = 6, temp = -24, oxygen = 5, oceans = 3, venus = 2)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 6 Research: Blue buys two, Green three, and Yellow four.
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      // Asteroid Hollowing should raise M€ production from eight to nine. The phone next records
      // Food Factory raising it directly from eight to twelve, so Blue added the asteroid and spent
      // the titanium but omitted this production step.
      exMachina("PROD[-MC]")
      doTask("PassBaton")
    }
    with(green) {
      // "Which cost me five. And then I'm going to raise Venus to four percent."
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "5 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      // Green's phone omitted this spoken five-M€ payment; preserve its continuous balance.
      exMachina("5 MC")

      doTasks(
          *playCardTasks("$CometForVenus"),
          "9 Pay<> FROM MC",
          "$CometForVenus FROM ProjectCard",
          "VenusStep",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "Ok", // Do not remove another player’s M€.
      )
    }
    with(yellow) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
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
    }

    with(blue) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    // The transcript resumes with Yellow although Green had not passed. To preserve legal turn
    // order, Green's next narrated action is advanced into that skipped physical turn; the
    // remaining Green actions stay in their sourced order through the otherwise silent Toll
    // Station play.
    with(green) {
      // "I'm gonna plant forest. Obviously here on 6-8 for one plant and four money."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_6_8>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "I'm going to envoys from Venus ... put two delegates in one party ... unity."
      doTasks(
          *playCardTasks("$EnvoysFromVenus"),
          "1 Pay<> FROM MC",
          "$EnvoysFromVenus FROM ProjectCard",
          "PartyFollower<Unity>",
          "PartyFollower<Unity>",
      )
      // board-21-13-22.jpg shows Yellow leading an otherwise empty player delegation in Unity
      // before Generation 6. Yellow then announces placing two Yellow Envoys delegates there, but
      // board-21-53-02.jpg instead shows Green as leader with another Green non-leader, while the
      // displaced original Yellow leader remains as Yellow's only Unity delegate. The exact color
      // counts show that the two Envoys cubes were physically taken from Green's supply.
      exMachina("-2 PartyFollower<Unity>")

      green.exMachina("PartyFollower<Unity>, PartyLeader<Unity>")

      // The table called Envoys free, and Yellow's ledger contains no payment.
      exMachina("MC")

      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "EcologicalZone_SpecialTile<Vastitas_4_8>",
          "2 MC",
          "PartyFollower<Greens>",
      )
    }
    with(blue) {
      // "One steel as two money. And then 10 monies ... to play Food Factory."
      doTasks(
          *playCardTasks("$FoodFactory"),
          "1 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$FoodFactory FROM ProjectCard",
          "PROD[-Plant]",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$MartianLumberCorp"),
          "4 Pay<> FROM MC",
          "$MartianLumberCorp FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // Scientists' ruling policy: "I suppose I'll spend ten to draw three cards."
      doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(yellow) {
      // Summit Logistics: two steel, one titanium, three M€, then five planetary tags.
      doTasks(
          *playCardTasks("$SummitLogistics"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$SummitLogistics FROM ProjectCard",
          "5 MC",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "9 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "$DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // Stanford Tours has a Space tag, so the reveal adds an asteroid.
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Remove a floater ... and gain an energy production." This is Deuterium Export's second
      // action; it supplies Green ledger entry 151 without any state repair.
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // "Moholy Lake, 31"; Yellow explicitly kept the two available steel.
      doTasks(
          *playCardTasks("$MoholeLake"),
          "31 Pay<> FROM MC",
          "$MoholeLake FROM ProjectCard",
          "TemperatureStep",
          "3 Plant",
          "TerraformRating",
          "OceanTile<Vastitas_6_7>",
          "TerraformRating",
          "ProjectCard",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Stratopolis"), "2 Floater<$Stratopolis>")
      doTask("PassBaton")
    }

    with(green) {
      // The wrong-color Envoys placement above supplies Green's photographed Unity representation
      // and the fifth party counted by this Septem Tribus action.
      doTasks(*cardActionTasks("$SeptemTribus"), "10 MC")
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "7 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$DiversitySupport>>",
          "ProjectCard FROM PlayedEvent<Class<$EnvoysFromVenus>>",
      )
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "$DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "4 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Scientists> FROM LobbyDelegate")
      doTask("PassBaton")
    }
    with(green) {
      // "I'm going to spend sixteen on aquifer pumping"; Green kept its plants.
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "Ok", // Keep the plants; pay with M€.
          "16 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Unity>")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Vastitas_5_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "6 MC")
      // Toll Station is visible in board-21-53-02.jpg and its cost/production are the remaining
      // Green ledger changes, although the transcript does not name the play.
      doTasks(
          *playCardTasks("$TollStation"),
          "10 Pay<> FROM MC",
          "$TollStation FROM ProjectCard",
          "PROD[8 MC]",
      )
    }

    with(yellow) {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // The otherwise unnamed +3 M€ row is exactly Red Ships' Generation 6 count: its sourced
      // actions pay one in Generation 5 and four in Generation 7, while the intervening map has
      // three
      // eligible wet cities/special tiles. Yellow has one extra loose heat by production. Repeating
      // the already-awarded -20°C threshold heat is the strongest candidate, but the surviving
      // records cannot exclude an earlier missed one-heat gain or manual correction in this
      // interval.
      doTasks(*cardActionTasks("$RedShips"), "3 MC")
      doTask("PassBaton")
    }
    yellow.exMachina("Heat")
    green.doTask("Pass")
    yellow.doTask("Pass")

    // Production.
    green.doTasks("34 MC", "4 Energy", "4 Plant", "3 Heat FROM Energy", "5 Heat")
    yellow.doTasks("52 MC", "2 Steel", "6 Heat", "2 Titanium", "Plant")
    blue.doTasks("36 MC", "4 Energy", "2 Heat", "2 Titanium", "Steel", "Heat FROM Energy")

    // Blue's TR loss for Diversity was entered before production at the table, reducing the
    // physical M€ payout by one; the engine correctly applies the TR loss later in Solar.
    blue.exMachina("-MC")
    // Neither Green's spoken intent correction nor its ledger preserved energy this production.
    green.doTask("Ok") // Keep the energy on Supercapacitors.

    with(green) {
      assertProduction(m = 9, s = 0, t = 0, p = 4, e = 4, h = 5)
      assertResources(m = 34, s = 0, t = 0, p = 8, e = 4, h = 16)
      assertCounts(25 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 28, s = 2, t = 2, p = 1, e = 0, h = 6)
      assertResources(m = 55, s = 2, t = 2, p = 6, e = 0, h = 8)
      assertCounts(24 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 12, s = 1, t = 2, p = 0, e = 4, h = 2)
      assertResources(m = 41, s = 1, t = 3, p = 11, e = 4, h = 4)
      assertCounts(24 to "TerraformRating")
    }
    assertSidebar(gen = 6, temp = -14, oxygen = 6, oceans = 5, venus = 4)
    // The Turmoil phase's TR revision follows World Government at once and costs Blue one TR.
    expectChanges(
        "-1 TerraformRating<Blue>, MoholeLakeGlobalEvent, Distant<MoholeLakeGlobalEvent>, PartyLeader<Kelvinists, Neutral>"
    ) {
      blue.doTask("VenusStep! BY Admin")
      green.doTask("10 MC")
      yellow.doTask("10 MC")
      blue.doTask("10 MC")
    }
    // Diversity resolves, Greens takes government, and Snow Cover is revealed.
    (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    admin.count("Distant<MoholeLakeGlobalEvent>") shouldBe 1
    (admin.count("PartyLeader<Kelvinists, Neutral>") > 0) shouldBe true
    // The physical Greens delegation makes Yellow chairman; the engine retained the neutral
    // tie-break. The spoken transition awards Yellow the corresponding TR.
    yellow.exMachina("-Chairman<Neutral>, Chairman, TerraformRating")
    // This is the table's explicit make-good for the one M€ Blue lost by entering the Diversity
    // TR reduction before production above: "I realized she hadn't produced ... and gave her a
    // money to compensate." It is not an unexplained political payout.
    blue.exMachina("MC")
    admin.assertCounts(
        1 to "Ruling<Greens>",
        1 to "Dominant<Unity>",
        1 to "Chairman<Yellow>",
    )
  }

  private fun generation7() {
    with(green) {
      assertResources(m = 51, s = 0, t = 0, p = 8, e = 4, h = 16)
      assertCounts(24 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 68, s = 2, t = 2, p = 6, e = 0, h = 8)
      assertCounts(24 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 53, s = 1, t = 3, p = 11, e = 4, h = 4)
      assertCounts(23 to "TerraformRating")
    }
    assertSidebar(gen = 7, temp = -14, oxygen = 6, oceans = 5, venus = 6)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 7 Research: Green buys three, Yellow four, and Blue two.
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_7_9>",
          "OxygenStep",
          "4 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$WgProject"),
          "9 Pay<> FROM MC",
          "$WgProject FROM ProjectCard",
          "3 PreludeCard<Selecting>",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$CorporateArchives>>",
          "$CorporateArchives FROM PreludeCard<Selecting>",
          "13 MC",
          "7 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // The phone call obscures this play in the transcript. Blue's continuous ledger records its
      // exact cost and production, and the next photograph isolates its city at 7-8.
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "1 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Vastitas_7_8>",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_7_7>",
          "OxygenStep",
          "4 MC",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
      // Unlike Blue's later greeneries, the ledger omits this 4 M€ Greens ruling bonus.
      exMachina("-4 MC")
    }

    with(green) {
      doTasks(
          *playCardTasks("$OrbitalReflectors"),
          "24 Pay<> FROM MC",
          "$OrbitalReflectors FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "PROD[2 Heat]",
          "TerraformRating",
          "ProjectCard",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$AntiGravityTechnology"),
          "14 Pay<> FROM MC",
          "$AntiGravityTechnology FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*cardActionTasks("$DeuteriumExport"), "Floater<$DeuteriumExport>")
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Vastitas_6_6>",
          "TerraformRating",
          "2 Heat",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$SpaceStation"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$SpaceStation FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SeptemTribus"), "10 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$MartianMediaCenter"),
          "7 Pay<> FROM MC",
          "$MartianMediaCenter FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$Grass"),
          "9 Pay<> FROM MC",
          "$Grass FROM ProjectCard",
          "PROD[Plant]",
          "3 Plant",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*playCardTasks("$Virus"), "$Virus FROM ProjectCard", "-4 Plant<Green>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$ProtectedValley"),
          "23 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "GreeneryTile<Vastitas_3_2>",
          "OxygenStep",
          "4 MC",
          "2 Plant",
          "2 MC",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "6 MC")
      // The name is lost under "brain says" in the transcript; Green's ledger records its exact
      // seven-M€ cost and production, and the next photograph shows Adapted Lichen in the tableau.
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "7 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$ImportOfAdvancedGhg"),
          "5 Pay<> FROM MC",
          "$ImportOfAdvancedGhg FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks(*cardActionTasks("$Stratopolis"), "2 Floater<$Stratopolis>")
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Greens> FROM LobbyDelegate")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$MartianMediaCenter"), "3 Pay<> FROM MC", "PartyFollower<Unity>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$SnowAlgae"),
          "10 Pay<> FROM MC",
          "$SnowAlgae FROM ProjectCard",
          "PROD[Plant]",
          "PROD[Heat]",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_8_9>",
          "OxygenStep",
          "4 MC",
          "Plant",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$Moss"),
          "2 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "Ok", // No space tag was revealed.
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$TopsoilContract"),
          "6 Pay<> FROM MC",
          "$TopsoilContract FROM ProjectCard",
          "3 Plant",
      )
      // Topsoil costs six after Earth Catapult and its own Microbe tag refunds one, for a legal net
      // cost of five. The phone records both a five-M€ debit and a later one-M€ credit, so the
      // narrow mistake is double-crediting that self-trigger; no later Green Microbe tag exists in
      // the generation.
      exMachina("MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")

    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*playCardTasks("$EnergyMarket"), "1 Pay<> FROM MC", "$EnergyMarket FROM ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "2 Plant",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$Greenhouses"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$Greenhouses FROM ProjectCard",
          "3 Plant",
          "Animal<$EcologicalZone>",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_7>",
          "OxygenStep",
          "4 MC",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    with(blue) {
      doTasks(
          *playCardTasks("$OutdoorSports"),
          "8 Pay<> FROM MC",
          "$OutdoorSports FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(*cardActionTasks("$RedShips"), "4 MC")
    }
    blue.doTask("Pass")
    with(yellow) {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Trees"),
          "11 Pay<> FROM MC",
          "$Trees FROM ProjectCard",
          "PROD[3 Plant]",
          "Plant",
          "Animal<$EcologicalZone>",
      )
    }
    yellow.doTask("Pass")

    // Production.
    green.doTasks("41 MC", "4 Energy", "6 Plant", "Heat FROM Energy", "7 Heat")
    yellow.doTasks("55 MC", "2 Steel", "9 Heat", "2 Titanium", "6 Plant")
    blue.doTasks("49 MC", "3 Energy", "2 Heat", "2 Titanium", "Steel", "Heat FROM Energy")
    // Green converted its remaining energy normally in the photographed production.
    green.doTask("Ok") // Keep the energy on Supercapacitors.

    with(green) {
      assertProduction(m = 9, s = 0, t = 0, p = 6, e = 4, h = 7)
      assertResources(m = 55, s = 0, t = 0, p = 9, e = 4, h = 10)
      assertCounts(32 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 28, s = 2, t = 2, p = 6, e = 0, h = 9)
      assertResources(m = 65, s = 2, t = 2, p = 8, e = 0, h = 9)
      assertCounts(27 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 22, s = 1, t = 2, p = 0, e = 3, h = 2)
      assertResources(m = 49, s = 1, t = 4, p = 3, e = 3, h = 4)
      assertCounts(27 to "TerraformRating")
    }
    assertSidebar(gen = 7, temp = -4, oxygen = 12, oceans = 6, venus = 12)
    expectChanges(
        "ExperimentalLifeforms, Distant<ExperimentalLifeforms>, PartyFollower<Greens, Neutral>"
    ) {
      green.doTask("OxygenStep! BY Admin")
      green.doTask("PROD[4 Energy]")
      yellow.doTask("PROD[2 Energy]")
      blue.doTask("PROD[2 Energy]")
    }
    (admin.count("PartyFollower<MarsFirst, Neutral>") > 0) shouldBe true
    admin.count("Distant<ExperimentalLifeforms>") shouldBe 1
    (admin.count("PartyFollower<Greens, Neutral>") > 0) shouldBe true
    // The two wrong-color Envoys cubes make Green Unity leader with a Green non-leader still in the
    // party. Those supply two rules-correct influence; Event Analysts supplies the third. Five
    // Power tags plus three influence produce four Improved Energy Templates steps, after which
    // the same physical Unity leader becomes chairman normally. Nothing new goes wrong here.
    admin.assertCounts(
        1 to "Ruling<Unity>",
        1 to "Dominant<Kelvinists>",
        1 to "Chairman<Green>",
    )
  }

  private fun generation8() {
    with(green) {
      assertProduction(m = 9, s = 0, t = 0, p = 6, e = 8, h = 7)
      assertResources(m = 60, s = 0, t = 0, p = 9, e = 4, h = 10)
      assertCounts(32 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 28, s = 2, t = 2, p = 6, e = 2, h = 9)
      assertResources(m = 69, s = 2, t = 2, p = 8, e = 0, h = 9)
      assertCounts(26 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 22, s = 1, t = 2, p = 0, e = 5, h = 2)
      assertResources(m = 53, s = 1, t = 4, p = 3, e = 3, h = 4)
      assertCounts(26 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = -4, oxygen = 13, oceans = 6, venus = 12)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 8 Research: Yellow buys two projects, Blue one, and Green one.
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_6>",
          "OxygenStep",
          "ProjectCard",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "19 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
      )
    }
    with(blue) {
      doTasks(
          *playCardTasks("$MagneticFieldGeneratorsPromo"),
          "1 Pay<Class<Steel>> FROM Steel",
          "20 Pay<> FROM MC",
          "$MagneticFieldGeneratorsPromo FROM ProjectCard",
          "PROD[-4 Energy]",
          "PROD[2 Plant]",
          "3 TerraformRating",
          "MagneticFieldGeneratorsPromo_SpecialTile<Vastitas_5_4>",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // Green says this is a Lobby delegate; the app stays at 57 M€ until Frontier Town.
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<MarsFirst> FROM LobbyDelegate")
      // Frontier Town repeats the temperature/-4 M€ bonus printed at 5-5 and crosses the
      // zero-degree ocean threshold; the table placed that ocean at 3-3.
      doTasks(
          *playCardTasks("$FrontierTown"),
          "Ok", // Keep the plants; pay with M€.
          "9 Pay<> FROM MC",
          "$FrontierTown FROM ProjectCard",
          "FrontierTownBonus",
          "PROD[-Energy]",
          "CityTile<Vastitas_5_5>",
          "-FrontierTownBonus",
          "TemperatureStep",
          "-4 MC",
          "2 MC",
          "2 MC",
          "RepeatPlacementBonus<Vastitas_5_5>",
          "TerraformRating",
          "TemperatureStep",
          "-4 MC",
          "TemperatureStep",
          "-4 MC",
          "TerraformRating",
          "TerraformRating",
          "OceanTile<Vastitas_3_3>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$OpenCity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "17 Pay<> FROM MC",
          "$OpenCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "2 Plant",
          "CityTile<Vastitas_3_6>",
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }
    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "12 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater<$Stratopolis>",
      )
    }
    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_3_4>",
          "2 Plant",
          "2 MC",
      )
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$Research"),
          "9 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$VenusianAnimals"),
          "15 Pay<> FROM MC",
          "$VenusianAnimals FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Vastitas_5_3>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_4>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
      )
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Stratopolis"), "2 Floater<$Stratopolis>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SeptemTribus"), "8 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // The revealed project has a Space tag, so Asteroid Deflection System succeeds.
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(
          *playCardTasks("$TundraFarming"),
          "14 Pay<> FROM MC",
          "$TundraFarming FROM ProjectCard",
          "PROD[Plant]",
          "PROD[2 MC]",
          "Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_4_5>",
          "OxygenStep",
          "2 MC",
      )
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$DawnCity"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$DawnCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "NormalCityTile<DawnCity_RemoteArea>",
      )
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
    }
    with(blue) {
      // The transcript skips Blue in this rotation and narrates this action after Yellow's next
      // turn. Advance that next Blue action here to preserve the legal turn sequence. It considers
      // Greens and Scientists without saying which was chosen; the next photograph shows Blue
      // leading Greens.
      doTasks(*cardActionTasks("$MartianMediaCenter"), "3 Pay<> FROM MC", "PartyFollower<Greens>")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "6 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$DeuteriumExport", "Action2"),
          "-Floater<$DeuteriumExport>",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Fighter<$SecurityFleet>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$TychoMagnetics"),
          "-Energy",
          "ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$ParliamentHall"),
          "6 Pay<> FROM MC",
          "$ParliamentHall FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$RedShips"), "7 MC")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "4 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*playCardTasks("$LuxuryFoods"), "6 Pay<> FROM MC", "$LuxuryFoods FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$Farming"),
          "14 Pay<> FROM MC",
          "$Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks(
          *cardActionTasks("$BioPrintingFacility"),
          "2 Pay<Class<Energy>> FROM Energy",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")

    with(yellow) {
      doTasks(
          *playCardTasks("$Heather"),
          "4 Pay<> FROM MC",
          "$Heather FROM ProjectCard",
          "PROD[Plant]",
          "Plant",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "4 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading", "Action2"), "-Floater<$LocalShading>", "PROD[MC]")
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    yellow.doTask("Pass")

    // Production.
    green.doTasks("54 MC", "8 Energy", "7 Plant", "4 Heat FROM Energy", "7 Heat")
    yellow.doTasks("67 MC", "2 Steel", "9 Heat", "3 Titanium", "9 Plant", "Energy")
    blue.doTasks("52 MC", "Energy", "2 Heat", "2 Titanium", "Steel", "2 Plant")
    // Green again retained Supercapacitors' energy instead of converting it to heat.
    green.doTask("Ok") // Keep the energy on Supercapacitors.

    with(green) {
      assertProduction(m = 16, s = 0, t = 0, p = 7, e = 8, h = 7)
      assertResources(m = 61, s = 0, t = 0, p = 7, e = 8, h = 13)
      assertCounts(38 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 35, s = 2, t = 3, p = 9, e = 1, h = 9)
      assertResources(m = 68, s = 2, t = 3, p = 14, e = 1, h = 10)
      assertCounts(32 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 23, s = 1, t = 2, p = 2, e = 1, h = 2)
      assertResources(m = 62, s = 1, t = 5, p = 7, e = 1, h = 3)
      assertCounts(29 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = 6, oxygen = 14, oceans = 8, venus = 18)
    expectChanges(
        "CharismaticWgPresident, Distant<CharismaticWgPresident>, PartyFollower<Unity, Neutral>"
    ) {
      yellow.doTask("TemperatureStep! BY Admin")
      green.doTask("-2 TerraformRating")
      blue.doTask("-TerraformRating")
    }
    // Revolution resolves, Mars First takes government, and the final photograph identifies
    // Interplanetary Trade as the newly revealed distant event.
    (admin.count("PartyFollower<Kelvinists, Neutral>") > 0) shouldBe true
    admin.count("Distant<CharismaticWgPresident>") shouldBe 1
    (admin.count("PartyFollower<Unity, Neutral>") > 0) shouldBe true
    // The table announces Green/Yellow counts of eleven/seven; their face-up cards contain
    // twelve/eight Building tags. Yellow's missing tag is most likely Martian Industries, a Prelude
    // separated from the project tableau. Green's omitted card cannot be isolated from the spoken
    // aggregate; SF Memorial is the strongest candidate because it was played during Head Start
    // and sits among the oldest stacked cards, while Parliament Hall is the latest alternative.
    green.exMachina("-MC")
    yellow.exMachina("-MC")

    admin.assertCounts(
        1 to "Ruling<MarsFirst>",
        1 to "Dominant<Kelvinists>",
        1 to "Chairman<Blue>",
    )
  }

  private fun generation9() {
    with(green) {
      assertResources(m = 72, s = 0, t = 0, p = 7, e = 8, h = 13)
      assertCounts(35 to "TerraformRating")
    }
    with(yellow) {
      assertResources(m = 75, s = 2, t = 3, p = 14, e = 1, h = 10)
      assertCounts(31 to "TerraformRating")
    }
    with(blue) {
      assertResources(m = 75, s = 1, t = 5, p = 7, e = 1, h = 3)
      assertCounts(28 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 8, venus = 18)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")

    // Generation 9 Research: Blue buys one project; Green and Yellow buy two each.
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Vastitas_7_6>",
          "TerraformRating",
          "Steel",
          "2 Heat",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyLeader<MarsFirst>")
    }
    with(yellow) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_8_7>",
          "Steel",
          "Plant",
          "Steel",
          "2 MC",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_9_7>",
          "OxygenStep",
          "Steel",
          "ProjectCard",
      )
    }
    with(blue) {
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "2 Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_2_2>",
          "OxygenStep",
          "Steel",
          "2 Plant",
          "2 MC",
      )
    }
    with(green) {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Vastitas_3_5>",
          "OxygenStep",
          "Steel",
          "Plant",
      )
      // The transcript records one-ocean adjacency and no printed resource bonus; 5-2 is the
      // photographed legal space adjacent to Green's 6-2 greenery.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_5_2>",
          "OxygenStep",
          "Steel",
          "2 MC",
      )
    }
    with(yellow) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_2_1>",
          "Steel",
          "2 Plant",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_9_8>",
          "OxygenStep",
          "Steel",
          "Titanium",
      )
    }
    with(blue) {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Vastitas_6_4>",
          "Steel",
          "Plant",
          "2 MC",
      )
      // Blue's phone and final production omit the standard city's production increase.
      exMachina("PROD[-MC]")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "7 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // The transcript compresses several final-generation rotations. Keep each player's sourced
      // action order while advancing the next available action into the otherwise silent turns. The
      // final tableau and Yellow's combined 11 M€ debit assign Decomposers and Bactoviral Research
      // to Yellow despite the transcript's missing speaker labels.
      doTasks(
          *playCardTasks("$Decomposers"),
          "3 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "Microbe<$Decomposers>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidHollowing"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Asteroid<$AsteroidHollowing>",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*cardActionTasks("$OrbitalCleanup"), "7 MC")
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Landscaper")
    }
    with(yellow) {
      // 16:13:50: twelve Science tags including both wilds, plus Decomposers' printed-tag
      // reaction, give the spoken thirteen microbes.
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "8 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "Microbe<$Decomposers>",
      )
      yellow.assertCounts(12 to "ScienceTag")
      doTask("12 Microbe<$Decomposers>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$MartianMediaCenter"), "3 Pay<> FROM MC", "PartyFollower<Reds>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *playCardTasks("$Mangrove"),
          "10 Pay<> FROM MC",
          "$Mangrove FROM ProjectCard",
          "GreeneryTile<Vastitas_6_5>",
          "OxygenStep",
          "Steel",
          "2 Heat",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(
          *playCardTasks("$NoctisFarming"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$NoctisFarming FROM ProjectCard",
          "PROD[MC]",
          "2 Plant",
          "Animal<$EcologicalZone>",
          "Microbe<$Decomposers>",
      )
      doTasks(
          *cardActionTasks("$SecurityFleet"),
          "Pay<Class<Titanium>> FROM Titanium",
          "Fighter<$SecurityFleet>",
      )
    }
    with(blue) {
      doTasks(
          *playCardTasks("$DevelopmentCenter"),
          "4 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
    }
    with(green) {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Reds>")
      // Both physical wild tags are chosen as Jovian for MicrogravityHealthProblems Movement.
      // Together with the
      // card's own Jovian tag, they explain the spoken three-M€ payout.
      doTasks(
          *playCardTasks("$DiasporaMovement"),
          "5 Pay<> FROM MC",
          "$DiasporaMovement FROM ProjectCard",
          "3 MC",
      )
    }

    with(yellow) {
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$SeptemTribus"), "8 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Politician")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$PhobosSpaceHaven"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$PhobosSpaceHaven FROM ProjectCard",
          "PROD[Titanium]",
          "NormalCityTile<PhobosSpaceHaven_RemoteArea>",
      )
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$Stratopolis"), "2 Floater<$Stratopolis>")
      doTask("PassBaton")
    }
    green.doTask("Pass")

    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(
          *playCardTasks("$UnexpectedApplication"),
          "4 Pay<> FROM MC",
          "$UnexpectedApplication FROM ProjectCard",
          "-ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *playCardTasks("$ArtificialLake"),
          "3 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$ArtificialLake FROM ProjectCard",
          "0 OceanTile<LandArea>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      doTasks(
          *playCardTasks("$SubCrustMeasurements"),
          "20 Pay<> FROM MC",
          "$SubCrustMeasurements FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      // The transcript and printed card both say 20 M€, while Blue's phone falls by 22 in this
      // interval. Every named intervening cost is already represented and none costs two. The
      // extra debit may nevertheless be a delayed correction of an earlier G9 entry; only if it
      // was not does this row represent a 22-M€ Sub-Crust payment. Preserve the bounded difference,
      // not the unproved local cause.
      exMachina("-2 MC")

      doTasks(*cardActionTasks("$SubCrustMeasurements"), "ProjectCard")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$EcologicalZone>")
      doTask("PassBaton")
    }

    with(blue) {
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(blue) {
      // The hand count is exact at the Generation 5 checkpoint and every later named source/sink is
      // accounted for. One extra card therefore entered Blue's hand between that checkpoint and
      // this sale. The strongest candidate is Space Elevator, successfully revealed by Asteroid
      // Deflection System minutes earlier: retaining that reveal instead of discarding it supplies
      // exactly the second patent. Stanford Tours (Generation 6) or the unnamed Generation 8
      // successful reveal are earlier alternatives.
      exMachina("ProjectCard")

      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks(*cardActionTasks("$RedShips"), "9 MC")
      // The table debates ten candidates, excludes the non-wet tile, and settles on nine. Yellow's
      // phone later groups 11 M€ where Red Ships plus the adjacent one-card patent sale explain
      // only ten. Entering ten despite the verbal correction is the strongest candidate, though a
      // delayed unrecorded one-M€ gain from earlier in the interval cannot be excluded.
      exMachina("MC")
      doTask("PassBaton")
    }
    with(blue) {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
      doTask("PassBaton")
    }
    with(yellow) {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Greens> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Greens>")
    }
    blue.doTask("Pass")
    with(yellow) {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<Kelvinists>")
    }
    yellow.doTask("Pass")

    // Production.
    green.doTasks("51 MC", "8 Energy", "7 Plant", "8 Heat FROM Energy", "7 Heat")
    yellow.doTask("6 MC", Pristar)
    yellow.doTasks(
        "69 MC",
        "Preservation<$Pristar>",
        "2 Steel",
        "9 Heat",
        "4 Titanium",
        "9 Plant",
        "Heat FROM Energy",
    )
    blue.doTasks("54 MC", "Energy", "2 Heat", "2 Titanium", "Steel", "2 Plant")
    // Green once more retained Supercapacitors' energy during final production.
    green.doTask("Ok") // Keep the energy on Supercapacitors.
    // Once MicrogravityHealthProblems Movement receives the two evidenced wild Jovian tags above,
    // Green reaches the
    // photographed final cash without any residual adjustment.
    // The final photograph has twelve animals although the final Mohole Lake action explicitly
    // adds an "eagle" to Ecological Zone; an animal was missed or removed sometime after Ecological
    // Zone's Generation 6 play, but the surviving records cannot identify the responsible trigger.
    yellow.exMachina("-Animal<$EcologicalZone>")

    with(green) {
      assertProduction(m = 16, s = 0, t = 0, p = 7, e = 8, h = 7)
      assertResources(m = 52, s = 3, t = 0, p = 7, e = 8, h = 30)
      assertCounts(35 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 38, s = 2, t = 4, p = 9, e = 0, h = 9)
      assertResources(m = 76, s = 2, t = 4, p = 12, e = 0, h = 20)
      assertCounts(31 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 24, s = 1, t = 2, p = 2, e = 1, h = 2)
      assertResources(m = 54, s = 1, t = 6, p = 6, e = 1, h = 4)
      assertCounts(30 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 8, oxygen = 14, oceans = 9, venus = 20)
    // Final greenery placement in start-player order. Blue and Green each have fewer than eight
    // plants; Yellow's photographed greenery is adjacent to Dawn City at 8-6.
    blue.doTask("PassBaton")
    green.doTask("PassBaton")

    with(yellow) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Vastitas_8_6>",
          "Plant",
          "2 MC",
      )
    }
    yellow.doTask("PassBaton")

    val score = Summarizer(game)
    assertEquals(
        """
        |                      1     2     3     4     5     6     7     8     9
        |                     /     /     /     /     /     /     /     /     /
        |
        | 1 -              LP    L     VS    L     L
        |
        | 2 -          [C2]  [G3]   L     L     LP   [S2]
        |
        | 3 -        LC   [G3]  [O]   [C1]  [G1]  [C2]   L
        |
        | 4 -    [S3]  [S3]  [O]   [G1]  [G1]  [G2]  [G2]  [S2]
        |
        | 5 -  L    [G1]  [O]   [S3]  [C1]  [O]   [O]   [O]   [G2]
        |
        | 6 -    [G1]   L    [C3]  [G1]  [O]   [O]   [G1]  [C1]
        |
        | 7 -        VT    LS    W    [O]   [G3]  [C3]  [G1]
        |
        | 8 -           LP    L    [G2]  [C2]   LS   [G3]
        |
        | 9 -              LD    L    [G2]  [G2]   LS
        """
            .trimMargin(),
        TfmMapRenderer(game.reader, game.actors.filterIsInstance<Player>(), useAnsiColors = false)
            .render()
            .joinToString("\n"),
    )
    yellow.assertCardResources(
        15 to Decomposers,
        12 to EcologicalZone,
        12 to SecurityFleet,
        1 to SearchForLife,
    )

    // The photograph gives Green eight greeneries and nine city points, and assigns the
    // Decomposers tableau to Yellow. Those rule-correct categories total 89, not the spoken 90.
    green.assertCounts(89 to "VictoryPoint", 0 to "Victory")
    // Green and Yellow each have a six-tile largest group in the final photograph, so both win
    // Landscaper. The table gave Yellow second place; rule-correct scoring is therefore 109 rather
    // than the spoken 106.
    yellow.assertCounts(109 to "VictoryPoint", 1 to "Victory")
    blue.assertCounts(74 to "VictoryPoint", 0 to "Victory")
    assertEquals(10, score.net("Milestone", "VictoryPoint<Blue>"))
    assertEquals(10, score.net("FirstPlace", "VictoryPoint<Green>"))
    assertEquals(10, score.net("FirstPlace", "VictoryPoint<Yellow>"))
    assertEquals(0, score.net("FirstPlace", "VictoryPoint<Blue>"))
    assertEquals(0, score.net("SecondPlace", "VictoryPoint<Green>"))
    assertEquals(2, score.net("SecondPlace", "VictoryPoint<Yellow>"))
    assertEquals(4, score.net("SecondPlace", "VictoryPoint<Blue>"))
    assertEquals(8, score.net("GreeneryTile", "VictoryPoint<Green>"))
    assertEquals(6, score.net("GreeneryTile", "VictoryPoint<Yellow>"))
    assertEquals(4, score.net("GreeneryTile", "VictoryPoint<Blue>"))
    assertEquals(9, score.net("CityTile", "VictoryPoint<Green>"))
    assertEquals(9, score.net("CityTile", "VictoryPoint<Yellow>"))
    assertEquals(5, score.net("CityTile", "VictoryPoint<Blue>"))
    assertEquals(19, score.net("Card", "VictoryPoint<Green>"))
    assertEquals(50, score.net("Card", "VictoryPoint<Yellow>"))
    assertEquals(19, score.net("Card", "VictoryPoint<Blue>"))
    assertEquals(5, score.net("$Decomposers", "VictoryPoint<Yellow>"))
    assertEquals(3, score.net("PartyLeader", "VictoryPoint<Green>"))
    assertEquals(1, score.net("PartyLeader", "VictoryPoint<Yellow>"))
    assertEquals(1, score.net("PartyLeader", "VictoryPoint<Blue>"))
    assertEquals(1, score.net("Chairman", "VictoryPoint<Blue>"))
  }
}
