package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.script.TfmMapRenderer
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test
import kotlin.test.assertEquals

/** Three-player physical game begun Saturday, 2026-09-12. */
internal class OtbGame20260912Test : AbstractFullGameTest() {
  // The physical deck incorrectly included the Colonies-dependent Summit Logistics without using
  // colonies. Selecting the dependency with no colony tiles keeps that evidenced card available
  // while introducing no colony board or trade opportunities.
  override val config =
      GameConfig(
          """
          VastitasMap
          VenusNextExpansion, PreludeExpansion, Prelude2CardPack, TurmoilExpansion, PromoCardPack
          ColoniesExpansion
          FakeStuffBundle

          Farmer, Generalist, Lobbyist, Philantropist, Producer
          Collector, Forecaster, Landscaper, Politician, Suburbian
          """
      )

  // The second transcript states Blue's three-TR handicap; blue-applog-01.png records it before
  // Tycho Magnetics and the initial project purchase.
  override val playerClassPets =
      """
      CLASS Green : Player { SetupPhase: PreludeCard }
      CLASS Yellow : Player { SetupPhase: PreludeCard }
      CLASS Blue : Player { SetupPhase: 3 TerraformRating, PreludeCard }
      """
          .trimIndent()

  @Test
  internal fun completeGame() {
    TfmWorkflow.Automatic(agents).launch()
    val green = p1.requireExplicitUnusedActionCards()
    val yellow = p2.requireExplicitUnusedActionCards()
    val blue = p3.requireExplicitUnusedActionCards()

    // "Our coming global event is Mud Slides, and our distant global event is Venus
    // Infrastructure."
    admin.doTask("MudSlides")
    admin.doTask("VenusInfrastructure")

    // "Septim Triboos [Septem Tribus]. I get 36 money, and then I buy 10 cards. Don't tell me I
    // bought 10."
    green.playCorp(FakeSeptemTribus, 10)
    // "Pristar. I get 53 money. I lose two TR. Not super happy. And I buy seven cards."
    yellow.playCorp(Pristar, 7)
    // "My corporation is Tycho Magnetics. I start with 42. Not 42 money production. But one energy
    // production."
    blue.playCorp(TychoMagnetics, 8)

    green.turn {
      // "Nobel Prize. My second wild tag. I have no tags, but I have two wild tags."
      playPrelude(FakeNobelPrize)
      // "Experimental Forest. My greenery. I'm going to go on six two. For dos cardos. And for
      // moneyos."
      playPrelude(ExperimentalForest) { placeTile(6, 2) }
      // The players acknowledged that paying the initial Greens policy during Prelude was a rule
      // mistake. Green nevertheless took 4 M€ here (transcript and Green ledger entry 6).
      green.exMachina("4 MC")
      // "Y'all ready for this?" "Is it a head start?" "I have 16 project cards in hand. Imagine if
      // we had a planner milestone. So I gain 32 money. Then I immediately take two actions. You
      // guys are never going to get to take any turns."
      // SF Memorial's sentence is missing from the transcript. Green's first ledger segment and
      // every tableau photo place it in Head Start's first action. The newer transcript restores
      // the second: "Let's just use my free delegate thingy ... put it in Scientists."
      playPrelude(FakeHeadStart) {
        useStdAction("PlayCardFromHandAction", payment = {}) {
          this.playProject(SfMemorial, 3, steel = 2)
        }
        useStdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
      }
    }
    yellow.turn {
      // "Martian Industries." "Nice." "Gain energy production, steel production, and gain six
      // monies."
      playPrelude(MartianIndustries)
      // "Applied Science is a wild tag, not that it counts for anything yet. I immediately add six
      // science resources, and it's going to let me gain stuff. Stuff and stuff."
      playPrelude(FakeAppliedScience)
      // "Established methods. Gain 30 monies." "Wow." "I'm gonna greenery for 23. I'm gonna put
      // it here for two cards... And that gave me the temperatura."
      playPrelude(FakeEstablishedMethods) {
        useStdProject("GreeneryProject") { placeTile(5, 9) }
        useStdProject("AsteroidProject")
      }
      // The players acknowledged this initial Greens-policy payout during Prelude as the same rule
      // mistake. Yellow nevertheless took 4 M€ here (transcript and Yellow ledger entry 15).
      yellow.exMachina("4 MC")
    }
    blue.turn {
      // "I got some boring ones, but hopefully they'll be good. Allied Bank." "Oh, that's a good
      // one." "I increased my money production by four and I give myself three loose monies. Dome
      // Farming. I increased my money production by two and plant production by one. And Power
      // Generator [Power Generation]. One, two, three energy production."
      playPrelude(AlliedBank)
      playPrelude(DomeFarming)
      playPrelude(PowerGeneration)
    }

    green.turn {
      // "Artificial Photosynthesis. I spent twelve, and I get two energy production. Actually,
      // no, I'm going to also spend five to put another one of my dudes in Unity."
      playProject(ArtificialPhotosynthesis, 12) { doTask("PROD[2 Energy]") }
      // "Actually, no, I'm going to also spend five to put another one of my dudes in community
      // [Unity]."
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
    }
    yellow.turn {
      // "I'm going to lava flows for 18. Raise temperature, two steps. Oh, and I get the heat. Oh,
      // this is the lava. Two, six."
      playProject(LavaFlows, 18) { placeTile(2, 6) }
      // "I'm also going to place my delegate in the Grens [Greens] since that's coming into power.
      // I was going to try and go for red since that would work better with Prestar [Pristar], but
      // now I've terraformed. I screwed that up."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    blue.turn {
      // "I'm going to pay five for Power Supply Consortium. I believe I'm increasing [Green's]
      // power production by one. And I gain one."
      playProject(PowerSupplyConsortium, 5) { doTask("PROD[-Energy<Green>]") }
    }
    green.turn {
      // "I'm not liking all of these options." "But I am liking this option." "So I pay seven for
      // that. The requirement is met and I get a plant production."
      // The latest photo identifies the otherwise unnamed card as Lichen.
      playProject(Lichen, 7)
    }
    yellow.turn {
      // "Hermetic Order of Mars. Pay 10. Oxygen must be 4% or lower." "Sure is." "Gain two money
      // production. Gain a money per empty area adjacent to my tiles. That is seven." "Heck yeah."
      playProject(HermeticOrderOfMars, 10)
    }
    blue.turn {
      // "I am paying 13." "Blue paying 13 and two money production. In order to gain two heat
      // production and two more energy production." "My goodness." "On lunar beam."
      playProject(LunarBeam, 13)
    }
    green.turn {
      // "I think I'm going to pay five to put another doodage in red."
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
    }
    yellow.turn {
      // "Titanium mine. Pay seven, gain titanium production."
      playProject(TitaniumMine, 7)
    }
    blue.turn {
      // "I like science. Put me in the scientist party."
      // "The ruler's blue lobbyist is in the science party."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
    }
    // "Oh, dominance moved over to the Scientists." Green's recovered Head Start placement and
    // Blue's placement make Scientists the first party with two delegates.
    green.turn {
      // "Well, that's good in some ways. I think I'll go ahead and use my Septim Tribus [Septem
      // Tribus] action now. And that means I get six money, right?" "Six McSeas."
      cardAction1(FakeSeptemTribus).expect("6 MC")
    }
    yellow.turn {
      // "Soych for life [Search for Life] for three."
      playProject(SearchForLife, 3)
    }
    blue.turn {
      // "Dang. You know what? I can't use it yet, but I can afford it right now. So I'm going to
      // pay one money for Directed Heat Usage."
      playProject(DirectedHeatUsage, 1)
    }
    green.turn {
      // "Play orbital cleanup. And I lose two money production."
      playProject(OrbitalCleanup, 14)
    }
    yellow.turn {
      // "I use soych for life [Search for Life] for three."
      cardAction1(SearchForLife) { declineTask() }.expect("-MC, 0 Science")
    }
    blue.pass(unused = DirectedHeatUsage, TychoMagnetics)
    green.turn {
      // "I will use Orbital Cleanup to take three money." Artificial Photosynthesis supplies one
      // printed Science tag; the fake wild tags are reconciled at the pre-WGT checkpoint.
      cardAction1(OrbitalCleanup).expect("MC")
    }
    yellow.turn {
      // "Decisions, decisions." "You should take mine." "We're reducing blue's energy production
      // by one."
      playProject(EnergyTapping, 3) { doTask("PROD[-Energy<Blue>]") }
    }
    green.pass()
    yellow.pass(unused = FakeAppliedScience)

    // Green chose the two inert wild tags on Septem Tribus and Nobel Prize as Science, making the
    // recorded Orbital Cleanup payout 3 M€ rather than the engine's printed-tag payout of 1 M€.
    green.exMachina("2 MC")

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
    green.wgt("OceanTile<Vastitas_5_8>").expect("0 TerraformRating<Green>")
    // "Oh, turmoil." "Walk us through it." "TR Revision, all players lose one TR." "Boo."
    // "New Government. The dominant party now becomes ruling. Change policy title to Scientists."
    // The automatic Solar operation performs TR revision, forms the Scientists government, and
    // advances both printed events before requesting only the next distant event.
    // "Changing times. Got the coming global event. Move a distant global event to coming. Turn the
    // top part of global event face up. It is sponsored projects."
    admin.doTask("SponsoredProjects")

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
        1 to "Current<Class<MudSlides>>",
        1 to "Coming<Class<VenusInfrastructure>>",
        1 to "Distant<Class<SponsoredProjects>>",
    )

    // Green consistently uses the inert wild tags on Septem Tribus and Nobel Prize as Science for
    // every sourced Generation 2-3 action that observes them. No intervening rule observes Science
    // tags, so materialize that choice once and remove it after the final such action.
    green.exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")

    // Generation 2 Research: "I buy four"; Yellow buys two; Blue buys three.
    green.buyCards(4)
    yellow.buyCards(2)
    blue.buyCards(3)

    yellow.turn {
      // "I will pay three for Supported Research. Requires scientists are in power. They sure
      // are. Draw two cards."
      playProject(SupportedResearch, 3).expect("-3 MC, ProjectCard")
    }
    blue.turn {
      // "Blue is paying six money ... to build Fuel Factory. So I'm going to lose one energy
      // production, but gain a titanium and a money production."
      playProject(FuelFactory, 6).expect("-6 MC, PROD[-Energy, Titanium, 1 MC]")
    }
    green.turn {
      // "I will play Investment Loan for three and get 10 and lose the money production."
      playProject(InvestmentLoan, 3).expect("7 MC, PROD[-1 MC]")
    }
    yellow.turn {
      // "I will play Local Shading for four."
      playProject(LocalShading, 4)
    }
    blue.turn {
      // "Put my blue lobbyist in the Unity party, please."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    green.turn {
      // "I'm gonna use my free action to put a dude into Greens."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    yellow.turn {
      // "I'll put my free guy in Reds."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
    }
    blue.turn {
      // "Blue's going to pay five money to put another delegate in Unity."
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") }
    }
    green.turn {
      // "Recruitment in the Greens ... I become the party leader. I paid two for that."
      playProject(Recruitment, 2) {
        doTask("PartyDelegate<Greens, Owner FROM Neutral>")
      }
    }

    yellow.turn {
      // "Pay five to place a dude in Reds." "Aww, that's no fair." "Yeah. I laid the track."
      // This is Yellow's turn between Green's Recruitment and Blue's House Printing. Her ledger
      // includes the 5 M€ payment, board-12-18-40.jpg shows both Yellow delegates in Reds, and
      // she later cites those two delegates as satisfying Red Appeasement.
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
    }
    blue.turn {
      // "Blue is paying ten for House Printing, which gives me a steel production."
      playProject(HousePrinting, 10)
    }
    green.turn {
      // "I'm gonna pay five and put a dude in Kelvinists."
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") }
    }
    yellow.turn {
      // "Local Shading, add floater."
      cardAction1(LocalShading).expect("Floater<$LocalShading>")
    }
    blue.pass(unused = DirectedHeatUsage, TychoMagnetics)

    green.turn {
      // "Oh, right. It's your turn." "You're gonna pay five ... to maybe go to Scientists? Wow."
      // Green has six delegates deployed and the seventh still in reserve. The later photograph
      // shows Green represented in Scientists, and Green's aggregated ledger includes this 5 M€
      // payment along with Recruitment, Kelvinists, and Lobbyist.
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
    }

    yellow.turn {
      // "Search for Life. Pay one ... It was Public Celebrations."
      cardAction1(SearchForLife) { declineTask() }.expect("-MC, 0 Science")
    }
    green.turn {
      // "I am going to pay eight for the Lobbyist."
      claimMilestone(cn("Lobbyist")).expect("-8 MC, Lobbyist")
    }
    yellow.turn {
      // "Applied Science floater onto Local Shading."
      cardAction1(FakeAppliedScience) { addCardResources(LocalShading) }
          .expect("-Science<$FakeAppliedScience>, Floater<$LocalShading>")
    }
    green.turn {
      // "I am going to use my Septem Tribus action to take ten money." Green is represented in
      // five parties; the recovered Generation 1 Scientist delegate became chairman rather than
      // remaining in Mars First.
      cardAction1(FakeSeptemTribus).expect("10 MC")
    }
    yellow.pass()

    green.turn {
      // "I am going to play Event Analysts, which costs five. You have an extra influence
      // permanently."
      playProject(EventAnalysts, 5)
    }
    green.turn {
      // "Use Orbital Cleanup to take four money."
      // Artificial Photosynthesis and Event Analysts supply two printed Science tags; Septem
      // Tribus and Nobel Prize supply the two chosen wild tags.
      cardAction1(OrbitalCleanup).expect("4 MC")
    }
    green.turn {
      // "Play Peroxide Power for seven money ... minus four [M€ production] ... two energy
      // production."
      playProject(PeroxidePower, 7)
    }
    green.turn {
      // "Sell a card for money."
      sellPatents(1)
    }
    green.turn {
      // "I'm going to play Lightning Harvest for all eight of my money. I do have the three
      // science tags I need." The same two wild tags satisfy the printed requirement.
      playProject(LightningHarvest, 8)
    }
    green.pass()

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
    // automatic workflow is now waiting for that World Government choice.
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

    // "Ellie does World Government ... ocean ... five seven."
    yellow.wgt("OceanTile<Vastitas_5_7>").expect("0 TerraformRating<Yellow>")
    // Mud Slides charges nobody because influence covers every adjacent owned tile. Greens forms
    // the government and Green becomes chairman.
    admin.doTask("SpinOffProducts")
    // "So you're saying this moves to Unity now?" During Changing Times, Venus Infrastructure's
    // neutral delegate raises Unity from three delegates to four, ahead of Reds' three. The engine
    // therefore moves the dominance marker to Unity normally, matching board-12-18-40.jpg.
    admin.assertCounts(
        3 to "PartyDelegate<Reds>",
        4 to "PartyDelegate<Unity>",
        1 to "Dominant<Unity>",
    )
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
        1 to "Current<Class<VenusInfrastructure>>",
        1 to "Coming<Class<SponsoredProjects>>",
        1 to "Distant<Class<SpinOffProducts>>",
    )

    // Generation 3 Research: all three players buy three projects.
    green.buyCards(3)
    yellow.buyCards(3)
    blue.buyCards(3)

    blue.turn {
      // "I'm definitely spending three heat in order to gain four money. And ... that was Blue."
      cardAction1(DirectedHeatUsage) { doTask("4 MC") }.expect("-3 Heat, 4 MC")
    }
    green.turn {
      // "I'm going to use my free action to put a dude into Mars First." The former Green chairman
      // and one non-leader Green delegate returned when Greens formed, so this placement is legal.
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<MarsFirst>") }
    }
    yellow.turn {
      // "Sponsored Academies. I pay nine. I pitch a card and draw three and everyone else gets
      // one."
      playProject(SponsoredAcademies, 9)
      // "I guess I have Local Shading. Remove floater to gain money production."
      cardAction2(LocalShading).expect("-Floater<$LocalShading>, PROD[MC]")
    }
    blue.turn {
      // "Blue ... spending eight money to get the Generalist."
      claimMilestone(cn("Generalist")).expect("-8 MC, Generalist")
    }
    green.turn {
      // "I'm going to use my Septem Tribus action to take 10 money."
      cardAction1(FakeSeptemTribus).expect("10 MC")
    }
    // Materialize Applied Science's wild tag as Earth only for Space Hotels' requirement. Removing
    // it afterward keeps the later Unity policy from treating that wild tag as a printed tag.
    yellow.exMachina("EarthTag<$FakeAppliedScience>")
    yellow.turn {
      // "I spend two titanium and six real on Space Hotels. Increase money production four
      // steps." Sponsored Academies supplies one Earth tag; Applied Science's wild tag is the
      // second Earth tag for the requirement.
      playProject(SpaceHotels, 6, titanium = 2)
    }
    yellow.exMachina("-EarthTag<$FakeAppliedScience>")
    blue.turn {
      // "Blue is spending one steel and seven money ... Natural Preserve ... up here for two
      // steel."
      playProject(NaturalPreserve, 7, steel = 1) { placeTile(4, 1) }
      // "For my second action, I'm going to pay eight money ... Producer."
      claimMilestone(cn("Producer")).expect("-8 MC, Producer")
    }
    green.turn {
      // "Water Splitting Plant. We have the two oceans. I pay the 12 money."
      playProject(WaterSplittingPlant, 12)
    }
    yellow.turn {
      // "Apply Science floater onto Local Shading. And I will pay two steel for Mine to get a
      // steel production."
      cardAction1(FakeAppliedScience) { addCardResources(LocalShading) }
      playProject(Mine, steel = 2)
    }
    blue.turn {
      // "Blue, spending five energy for five cards. And I will keep one."
      cardAction1(TychoMagnetics, x = 5).expect("-5 Energy, ProjectCard")
    }
    green.turn {
      // "Use my Water Splitting Plant, spend three energy, raise oxygen to three, and get a TR."
      cardAction1(WaterSplittingPlant).expect("-3 Energy, OxygenStep, TerraformRating")
    }
    yellow.turn {
      // "I add a delegate in Unity ... for an influence."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
      // "Red Appeasement for zero ... gain two money production and this counts as me passing."
      playProject(RedAppeasement, 0).expect("PROD[2 MC], Pass")
    }
    // Automatic workflow notices Pass after a first action, while Red Appeasement supplies it as
    // Yellow's second. Temporarily defer that Pass until the redundant turn the workflow offers.
    yellow.exMachina("-Pass")
    blue.turn {
      // "Blue is going to send her lobbyist to ... Greens."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    green.turn {
      // "I'm going to use Orbital Cleanup ... and get four money." Green again chooses both wild
      // tags as Science.
      cardAction1(OrbitalCleanup).expect("4 MC")
    }
    green.exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    // This is the Pass already supplied physically by Red Appeasement, deferred solely so the
    // unchanged workflow can recognize it at the point where it checks first-action results.
    yellow.pass(unused = SearchForLife).expect("Pass")
    blue.pass()
    green.pass()

    // board-12-36-50.jpg and the phone histories show these complete physical ledgers after
    // production and before World Government or Turmoil. Yellow's phone still reads 40 M€ because
    // its 6 M€ Pristar reward is entered later with the Solar payouts; the engine has already
    // awarded it and therefore reads 46 M€ here. Automatic workflow is waiting for World
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

    // "Why not Venus? Venus it is. ... Venus is now at two." TR revision remains pending.
    blue.wgt("VenusStep").expect("0 TerraformRating<Blue>")
    // Venus Infrastructure pays Green 6 M€, Yellow 4 M€, and Blue 4 M€ from their recorded
    // Venus tags and influence. Unity then pays 1/3/2 M€ for planetary tags and makes Blue
    // chairman. Diversity—the event headed "Free Academia Treaty"—is revealed with its neutral
    // Scientist delegate.
    admin.doTask("Diversity")

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
        1 to "Current<Class<SponsoredProjects>>",
        1 to "Coming<Class<SpinOffProducts>>",
        1 to "Distant<Class<Diversity>>",
    )

    // Generation 4 Research: Green buys zero, Yellow buys one, and Blue buys three. The complete
    // ledgers disambiguate which voice owns each purchase.
    green.buyCards(0)
    yellow.buyCards(1)
    blue.buyCards(3)

    green.turn {
      // "Cultural Metropolis, for 20. No discounts." The photographed city at Vastitas 6-9
      // supplies the recorded two titanium and two M€ placement bonuses.
      playProject(CulturalMetropolis, 20) {
        placeTile(6, 9)
        doTask("2 PartyDelegate<Reds>")
      }
    }
    yellow.turn {
      // "I'm gonna go place my free delegate in Punity." The board photo shows that the otherwise
      // unlabeled speaker placed Yellow's delegate, not Green's.
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    blue.turn {
      // "Mining Area. I'm going to pay two steel for it."
      playProject(MiningArea, steel = 2) { placeTile(4, 2) }
    }
    green.turn {
      // "I am going to design some microorganisms. Pay 16 ... two plant production."
      playProject(DesignedMicroorganisms, 16)
    }
    yellow.turn {
      // "I'm gonna pay six for Archaebacteria, for a plant production."
      playProject(Archaebacteria, 6)
    }
    blue.turn {
      // "Asteroid Hollowing ... I need one titanium to actually put on the card, so I'll take one
      // back and pay four more money." Final payment was two titanium and eight M€.
      intentionalUnderpay()
      playProject(AsteroidHollowing, 8, titanium = 2)
    }
    green.turn {
      // "I will use Orbital Cleanup to get five money."
      exMachina(fakeWildTags("ScienceTag", 2))
      cardAction1(OrbitalCleanup).expect("5 MC")
    }
    yellow.turn {
      // "Security Fleet for 12." "I'm gonna spend a titanium to put a fighter on it."
      intentionalUnderpay()
      playProject(SecurityFleet, 12)
      cardAction1(SecurityFleet)
    }
    blue.turn {
      cardAction1(AsteroidHollowing)
    }
    green.turn {
      // "Free lobby into Kelvinists"; then "Sponsored Mohole for five ... two heat production."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Kelvinists>") }
      playProject(SponsoredMohole, 5)
    }
    yellow.turn {
      // "Research Coordination for four. And then Interplanetary Trade." Its nine-step increase
      // brings Yellow's M€ production from nine to the photographed 18.
      playProject(FakeResearchCoordination, 4)
      exMachina(fakeWildTags("JovianTag", "PlantTag"))
      playProject(InterplanetaryTrade, 27).expect("PROD[9 MC]")
    }
    blue.turn {
      // "Bioprinting Facility ... one steel and five money." "Spend two energy, gain two plants."
      playProject(BioPrintingFacility, 5, steel = 1)
      cardAction1(BioPrintingFacility) { doTask("2 Plant") }
    }
    green.turn { cardAction1(FakeSeptemTribus).expect("8 MC") }
    yellow.turn {
      cardAction1(SearchForLife) { declineTask() }.expect("-MC, 0 Science")
    }
    blue.turn {
      // Blue's ledger separately records two energy for Bioprinting and one here for Tycho.
      cardAction1(TychoMagnetics, x = 1)
    }
    green.turn {
      cardAction1(WaterSplittingPlant).expect("-3 Energy, OxygenStep, TerraformRating")
    }
    yellow.turn {
      cardAction2(LocalShading).expect("-Floater<$LocalShading>, PROD[MC]")
    }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") } }
    green.pass()
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    yellow.pass()
    blue.turn {
      // "Business Contacts for seven"; the two retained projects are not named in the record.
      playProject(BusinessContacts, 7)
      playProject(RadChemFactory, 8)
    }
    blue.turn { sellPatents(2) }
    blue.pass()

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
    green.wgt("OxygenStep").expect("OxygenStep")
    // At 6:38:48 pm, the table notes that Search for Life has no science to receive a bonus.
    yellow.assertCardResources(0 to SearchForLife)
    admin.doTask("ImprovedEnergyTemplates")

    assertSidebar(gen = 5, temp = -24, oxygen = 5, oceans = 2, venus = 2)
    admin.assertCounts(
        1 to "Ruling<Reds>",
        1 to "Dominant<Scientists>",
        1 to "Current<Class<SpinOffProducts>>",
        1 to "Coming<Class<Diversity>>",
        1 to "Distant<Class<ImprovedEnergyTemplates>>",
    )
    // "Yellow, six. Blue has ten. Green has fourteen."
    yellow.assertCounts(6 to "ProjectCard")
    blue.assertCounts(10 to "ProjectCard")
    green.assertCounts(14 to "ProjectCard")

    // Generation 5 Research after the dinner break: Yellow and Green buy three; Blue buys one.
    yellow.buyCards(3)
    blue.buyCards(1)
    // Blue says she buys one, but Blue's continuous ledger has no 3 M€ purchase and every later
    // payment reaches the photographed balance only if that charge was missed physically.
    blue.exMachina("3 MC")
    green.buyCards(3)

    yellow.turn { fundAward(cn("Collector"), 8) }
    blue.turn {
      // "Blue is going to pay most of her money, 22, to build Stratopolis."
      playProject(Stratopolis, 22)
      // The physical game omitted Stratopolis's two M€ production; Blue's ledger and the
      // board-21-13-22.jpg production track both retain eight.
      exMachina("PROD[-2 MC]")
    }
    green.turn {
      // "I'm going to put my free delegate in reds."
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Reds>") }
    }
    yellow.turn {
      // Dusk Laser Mining: one titanium and five M€. Satellites: three titanium and one M€.
      playProject(DuskLaserMining, 5, titanium = 1)
      exMachina(fakeWildTags("SpaceTag", 2))
      playProject(Satellites, 1, titanium = 3).expect("PROD[7 MC]")
    }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn { playProject(EarthCatapult, 23) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { sellPatents(2) }
    green.turn { playProject(MercurianAlloys, 1) }
    yellow.turn { cardAction2(LocalShading) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<MarsFirst>") } }
    green.turn { cardAction1(FakeSeptemTribus).expect("8 MC") }
    yellow.turn {
      // Symbiotic Fungus is revealed, so Search for Life succeeds.
      cardAction1(SearchForLife) { doTask("Science<$SearchForLife>") }
    }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.turn {
      // Earth Catapult reduces Deuterium Export to nine; Mercurian Alloys makes each titanium
      // worth four, so the recorded payment is two titanium and one M€.
      playProject(DeuteriumExport, 1, titanium = 2)
    }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn {
      playProject(AsteroidDeflectionSystem, 8, steel = 1, titanium = 1)
      // The physical game also omitted Asteroid Deflection System's energy-production decrease.
      exMachina("PROD[Energy]")
    }
    green.turn { cardAction1(DeuteriumExport) }
    yellow.turn { playProject(ImportOfAdvancedGhg, 9) }
    blue.turn { playProject(DustSeals, 2) }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup).expect("5 MC")
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    }
    yellow.turn { playProject(RedShips, 2) }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    green.turn {
      playProject(HeatTrappers, 4) { doTask("PROD[-2 Heat<Yellow>]") }
    }
    yellow.turn { playProject(GhgFactories, 3, steel = 4) }
    blue.turn {
      // Venus Shuttles has no Space tag, so the reveal adds no asteroid.
      cardAction1(AsteroidDeflectionSystem) { declineTask() }
    }
    green.turn { playProject(CarbonateProcessing, 4) }
    yellow.turn { cardAction1(RedShips).expect("MC") }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") } }
    green.turn { playProject(Supercapacitors, 2) }
    yellow.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") } }
    blue.turn { cardAction1(TychoMagnetics, x = 2) }
    green.pass(unused = WaterSplittingPlant)
    yellow.pass()
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    blue.pass()
    // Green's photographed post-production energy and heat show that Supercapacitors was declined.
    green.declineTask()
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
    yellow.wgt("OceanTile<Vastitas_4_3>").expect("0 TerraformRating<Yellow>")
    // Spin-Off Products and the Scientists ruling bonus pay for science tags. Diversity becomes
    // current, Improved Energy Templates becomes coming, and Revolution is revealed.
    admin.doTask("Revolution")
    // Blue has two printed Science tags, so Spin-Off Products and the Scientists ruling bonus pay
    // 4 M€ and 2 M€. The transcript instead says, consecutively, "you get four money and you get
    // six money" for Blue's and Yellow's event awards; Blue's lone +10 row is exactly 4 + 6. The
    // likeliest entry mistake is therefore recording both announced event awards for Blue while
    // omitting Blue's own 2 M€ ruling bonus, a net overpayment of four.
    blue.exMachina("4 MC")

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

    // Generation 6 Research: Blue buys two, Green three, and Yellow four.
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(4)

    blue.turn {
      cardAction1(AsteroidHollowing)
      // Asteroid Hollowing should raise M€ production from eight to nine. The phone next records
      // Food Factory raising it directly from eight to twelve, so Blue added the asteroid and spent
      // the titanium but omitted this production step.
      exMachina("PROD[-MC]")
    }
    green.turn {
      // "Which cost me five. And then I'm going to raise Venus to four percent."
      playProject(OptimalAerobraking, 5)
      // Green's phone omitted this spoken five-M€ payment; preserve its continuous balance.
      exMachina("5 MC")
      playProject(CometForVenus, 9) { declineTask() }
    }
    yellow.turn {
      convertHeat()
      convertHeat()
    }
    blue.turn { convertHeat() }

    // The transcript resumes with Yellow although Green had not passed. To preserve the legal
    // workflow, Green's next narrated action is advanced into that skipped physical turn; the
    // remaining Green actions stay in their sourced order through the otherwise silent Toll
    // Station play.
    green.turn {
      // "I'm gonna plant forest. Obviously here on 6-8 for one plant and four money."
      convertPlants { placeTile(6, 8) }
    }
    yellow.turn {
      // "I'm going to envoys from Venus ... put two delegates in one party ... unity."
      exMachina(fakeWildTags("VenusTag", 2))
      playProject(EnvoysFromVenus, 1) {
        doTask("2 PartyDelegate<Unity>")
      }
      // board-21-13-22.jpg shows Yellow leading an otherwise empty player delegation in Unity
      // before Generation 6. Yellow then announces placing two Yellow Envoys delegates there, but
      // board-21-53-02.jpg instead shows Green as leader with another Green non-leader, while the
      // displaced original Yellow leader remains as Yellow's only Unity delegate. The exact color
      // counts show that the two Envoys cubes were physically taken from Green's supply.
      exMachina("-2 PartyDelegate<Unity>")
      green.exMachina("2 PartyDelegate<Unity>")
      // The table called Envoys free, and Yellow's ledger contains no payment.
      exMachina("MC")
      playProject(EcologicalZone, 12) {
        placeTile(4, 8)
        doTask("PartyDelegate<Greens>")
      }
    }
    blue.turn {
      // "One steel as two money. And then 10 monies ... to play Food Factory."
      playProject(FoodFactory, 10, steel = 1)
    }
    green.turn {
      playProject(MartianLumberCorp, 4)
    }
    yellow.turn {
      // Scientists' ruling policy: "I suppose I'll spend ten to draw three cards."
      stdAction("UseTurmoilPolicyAction").expect("-10 MC, 3 ProjectCard")
    }
    blue.turn { playProject(ReleaseOfInertGases, 14) }
    green.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") } }
    yellow.turn {
      // Summit Logistics: two steel, one titanium, three M€, then five planetary tags/colonies.
      exMachina(fakeWildTags("EarthTag", 2))
      intentionalUnderpay()
      playProject(SummitLogistics, 3, steel = 2, titanium = 1).expect("2 MC")
    }
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    green.turn {
      exMachina(fakeWildTags("AnimalTag"))
      playProject(AdvancedEcosystems, 9)
    }
    yellow.turn { playProject(DiversitySupport, 1) }
    blue.turn {
      // Stanford Tours has a Space tag, so the reveal adds an asteroid.
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn {
      // "Remove a floater ... and gain an energy production." This is Deuterium Export's second
      // action; it supplies Green ledger entry 151 without any direct state repair.
      cardAction2(DeuteriumExport).expect("-Floater<$DeuteriumExport>, PROD[Energy]")
    }
    yellow.turn {
      intentionalUnderpay()
      // "Moholy Lake, 31"; Yellow explicitly kept the two available steel.
      playProject(MoholeLake, 31) { placeTile(6, 7) }
    }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    // The wrong-color Envoys placement above supplies Green's photographed Unity representation
    // and the fifth party counted by this Septem Tribus action.
    green.turn { cardAction1(FakeSeptemTribus).expect("10 MC") }
    yellow.turn {
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(yellow) {
          doTask("ProjectCard FROM PlayedEvent<Class<$DiversitySupport>>")
          doTask("ProjectCard FROM PlayedEvent<Class<$EnvoysFromVenus>>")
        }
      }
      playProject(DiversitySupport, 1)
    }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    green.turn { playProject(TransNeptuneProbe, 4) }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") } }
    green.turn {
      intentionalUnderpay()
      // "I'm going to spend sixteen on aquifer pumping"; Green kept its plants.
      playProject(AquiferPumping, 16)
    }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") } }
    green.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 6)
      }
    }
    yellow.turn { cardAction2(LocalShading) }
    blue.pass(unused = DirectedHeatUsage)
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup).expect("6 MC")
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      // Toll Station is visible in board-21-53-02.jpg and its cost/production are the remaining
      // Green ledger changes, although the transcript does not name the play.
      playProject(TollStation, 10).expect("PROD[8 MC]")
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    green.turn { convertHeat() }
    // The otherwise unnamed +3 M€ row is exactly Red Ships' Generation 6 count: its sourced
    // actions pay one in Generation 5 and four in Generation 7, while the intervening map has three
    // eligible wet cities/special tiles. Yellow has one extra loose heat by production. Repeating
    // the already-awarded -20°C threshold heat is the strongest candidate, but the surviving
    // records cannot exclude an earlier missed one-heat gain or manual correction in this interval.
    yellow.turn { cardAction1(RedShips).expect("3 MC") }
    yellow.exMachina("Heat")
    green.pass(unused = WaterSplittingPlant)
    yellow.pass(unused = SearchForLife)

    // Blue's TR loss for Diversity was entered before production at the table, reducing the
    // physical M€ payout by one; the engine correctly applies the TR loss later in Solar.
    blue.exMachina("-MC")
    // Neither Green's spoken intent correction nor its ledger preserved energy this production.
    green.declineTask()

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

    blue.wgt("VenusStep").expect("0 TerraformRating<Blue>")
    // Diversity resolves, Greens takes government, and Snow Cover is revealed.
    admin.doTask("SnowCover")
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

    // Generation 7 Research: Green buys three, Yellow four, and Blue two.
    green.buyCards(3)
    yellow.buyCards(4)
    blue.buyCards(2)

    green.turn {
      cardAction1(WaterSplittingPlant)
      convertPlants { placeTile(7, 9) }
    }
    yellow.turn {
      playProject(WgProject, 9) { playPrelude(CorporateArchives) }
    }
    blue.turn {
      // The phone call obscures this play in the transcript. Blue's continuous ledger records its
      // exact cost and production, and the next photograph isolates its city at 7-8.
      playProject(CorporateStronghold, 9, steel = 1) { placeTile(7, 8) }
      convertPlants { placeTile(7, 7) }
      // Unlike Blue's later greeneries, the ledger omits this 4 M€ Greens ruling bonus.
      exMachina("-4 MC")
    }
    green.turn { playProject(OrbitalReflectors, 24) }
    yellow.turn {
      exMachina(fakeWildTags("ScienceTag", 2))
      playProject(AntiGravityTechnology, 14)
    }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    green.turn {
      cardAction1(DeuteriumExport)
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(6, 6)
      }
    }
    yellow.turn {
      intentionalUnderpay()
      playProject(SpaceStation, 5, titanium = 1)
    }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn { cardAction1(FakeSeptemTribus).expect("10 MC") }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn { playProject(MartianMediaCenter, 7) }
    green.turn { playProject(Grass, 9) }
    yellow.turn { playProject(Virus, 0) { doTask("-4 Plant<Green>") } }
    blue.turn { playProject(ProtectedValley, 23) { placeTile(3, 2) } }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup).expect("6 MC")
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      // The name is lost under "brain says" in the transcript; Green's ledger records its exact
      // seven-M€ cost and production, and the next photograph shows Adapted Lichen in the tableau.
      playProject(AdaptedLichen, 7)
    }
    yellow.turn { playProject(ImportOfAdvancedGhg, 5) }
    blue.turn {
      cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) }
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
    }
    green.turn { convertHeat() }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Unity>") } }
    green.turn { convertHeat() }
    yellow.turn { playProject(SnowAlgae, 10) }
    blue.turn { convertPlants { placeTile(8, 9) } }
    green.turn { sellPatents(1) }
    yellow.turn { playProject(Moss, 2) }
    blue.turn { cardAction1(AsteroidDeflectionSystem) { declineTask() } }
    green.turn {
      playProject(TopsoilContract, 6)
      // Topsoil costs six after Earth Catapult and its own Microbe tag refunds one, for a legal net
      // cost of five. The phone records both a five-M€ debit and a later one-M€ credit, so the
      // narrow mistake is double-crediting that self-trigger; no later Green Microbe tag exists in
      // the generation.
      exMachina("MC")
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { stdProject("AirScrappingProject") }
    green.pass()
    yellow.turn { cardAction1(LocalShading) }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    yellow.turn { playProject(EnergyMarket, 1) }
    blue.turn { cardAction1(BioPrintingFacility) { doTask("2 Plant") } }
    yellow.turn {
      playProject(Greenhouses, steel = 2)
      convertPlants { placeTile(4, 7) }
    }
    blue.turn { playProject(OutdoorSports, 8) }
    yellow.turn {
      convertHeat()
      cardAction1(RedShips).expect("4 MC")
    }
    blue.pass()
    yellow.turn {
      stdProject("AsteroidProject")
      playProject(Trees, 11)
    }
    yellow.pass(unused = SearchForLife, EnergyMarket)

    // Green converted its remaining energy normally in the photographed production.
    green.declineTask()

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

    green.wgt("OxygenStep")
    admin.doTask("EcoSabotage")
    // The two wrong-color Envoys cubes make Green Unity leader with a Green non-leader still in the
    // party. Those supply two rules-correct influence; Event Analysts supplies the third. Five
    // Power tags plus three influence produce four Improved Energy Templates steps, after which
    // the same physical Unity leader becomes chairman normally. Nothing new goes wrong here.
    admin.assertCounts(
        1 to "Ruling<Unity>",
        1 to "Dominant<Kelvinists>",
        1 to "Chairman<Green>",
    )

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

    // Generation 8 Research: Yellow buys two projects, Blue one, and Green one.
    yellow.buyCards(2)
    blue.buyCards(1)
    green.buyCards(1)

    yellow.turn {
      convertPlants { placeTile(4, 6) }
      intentionalUnderpay()
      playProject(GiantSolarShade, 19, titanium = 1)
    }
    blue.turn {
      playProject(MagneticFieldGeneratorsPromo, 20, steel = 1) { placeTile(5, 4) }
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") }
      // Green explicitly says "pay five" for this Mars First delegate. The app remains at 57 M€
      // until Frontier Town's nine-M€ payment, so the player placed the delegate but failed to
      // decrease the phone balance.
      exMachina("5 MC")
      // Frontier Town repeats the temperature/-4 M€ bonus printed at 5-5 and crosses the
      // zero-degree ocean threshold; the table placed that ocean at 3-3.
      intentionalUnderpay()
      playProject(FrontierTown, 9) {
        placeTile(5, 5)
        placeTile(3, 3)
      }
    }
    yellow.turn {
      playProject(OpenCity, 17, steel = 2) { placeTile(3, 6) }
      convertHeat()
    }
    blue.turn {
      cardAction1(AsteroidHollowing)
      playProject(StratosphericBirds, 12)
    }
    green.turn {
      convertHeat()
      stdProject("CityProject") { placeTile(3, 4) }
    }
    yellow.turn { playProject(Research, 9) }
    blue.turn { playProject(VenusianAnimals, 15) }
    green.turn {
      cardAction1(AquiferPumping) {
        pay(8)
        placeTile(5, 3)
      }
      convertPlants { placeTile(4, 4) }
    }
    yellow.turn { playProject(SolarWindPower, 3, titanium = 1) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.turn { cardAction1(FakeSeptemTribus).expect("8 MC") }
    yellow.turn { cardAction1(SecurityFleet) }
    blue.turn {
      // The revealed project has a Space tag, so Asteroid Deflection System succeeds.
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn {
      playProject(TundraFarming, 14)
      convertPlants { placeTile(4, 5) }
    }
    yellow.turn {
      playProject(DawnCity, 7, titanium = 1)
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") }
    }
    blue.turn {
      // The transcript skips Blue in this rotation and narrates this action after Yellow's next
      // turn. Advance that next Blue action here to preserve the legal turn sequence. It considers
      // Greens and Scientists without saying which was chosen; the next photograph shows Blue
      // leading Greens.
      cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Greens>") }
    }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup).expect("6 MC")
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { cardAction1(StratosphericBirds) }
    green.turn { cardAction2(DeuteriumExport) }
    yellow.turn { cardAction1(FakeAppliedScience) { addCardResources(SecurityFleet) } }
    blue.turn { cardAction1(TychoMagnetics, x = 1) }
    green.turn { playProject(ParliamentHall, 6) }
    yellow.turn { cardAction1(RedShips).expect("7 MC") }
    blue.turn { sellPatents(4) }
    green.turn {
      exMachina(fakeWildTags("EarthTag", "JovianTag", "VenusTag"))
      playProject(LuxuryFoods, 6)
    }
    yellow.turn { playProject(Farming, 14) }
    blue.turn {
      cardAction1(BioPrintingFacility) { addCardResources(VenusianAnimals) }
    }
    green.pass(unused = WaterSplittingPlant)
    yellow.turn { playProject(Heather, 4) }
    blue.turn { cardAction1(DirectedHeatUsage) { doTask("4 MC") } }
    yellow.turn { cardAction2(LocalShading) }
    blue.pass()
    yellow.pass(unused = SearchForLife, EnergyMarket)

    // Green again retained Supercapacitors' energy instead of converting it to heat.
    green.declineTask()

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

    yellow.wgt("TemperatureStep")
    // Revolution resolves, Mars First takes government, and the final photograph identifies
    // Interplanetary Trade as the newly revealed distant event.
    admin.doTask("InterplanetaryTradeGlobalEvent")
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

    // Generation 9 Research: Blue buys one project; Green and Yellow buy two each.
    blue.buyCards(1)
    green.buyCards(2)
    yellow.buyCards(2)

    blue.turn { stdProject("AquiferProject") { placeTile(7, 6) } }
    green.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Scientists>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") }
    }
    yellow.turn {
      stdProject("CityProject") { placeTile(8, 7) }
      convertPlants { placeTile(9, 7) }
    }
    blue.turn {
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }
      convertPlants { placeTile(2, 2) }
    }
    green.turn {
      stdProject("GreeneryProject") { placeTile(3, 5) }
      // The transcript records one-ocean adjacency and no printed resource bonus; 5-2 is the
      // photographed legal space adjacent to Green's 6-2 greenery.
      convertPlants { placeTile(5, 2) }
    }
    yellow.turn {
      stdProject("CityProject") { placeTile(2, 1) }
      convertPlants { placeTile(9, 8) }
      // The final photograph puts this greenery on 9-8. The table and phone treated that printed
      // steel bonus as titanium, so retain the physical resource adjustment.
      exMachina("-Steel, Titanium")
    }
    blue.turn {
      stdProject("CityProject") { placeTile(6, 4) }
      // Blue's phone and final production omit the standard city's production increase.
      exMachina("PROD[-MC]")
    }
    green.turn { playProject(LagrangeObservatory, 7) }
    // The transcript compresses several final-generation rotations. Keep each player's sourced
    // action order while advancing the next available action into the otherwise silent turns. The
    // final tableau and Yellow's combined 11 M€ debit assign Decomposers and Bactoviral Research
    // to Yellow despite the transcript's missing speaker labels.
    yellow.turn { playProject(Decomposers, 3) }
    blue.turn { cardAction1(AsteroidHollowing) }
    green.turn {
      exMachina("ScienceTag<$FakeSeptemTribus>, ScienceTag<$FakeNobelPrize>")
      cardAction1(OrbitalCleanup).expect("7 MC")
      exMachina("-ScienceTag<$FakeSeptemTribus>, -ScienceTag<$FakeNobelPrize>")
      fundAward(cn("Landscaper"), 14)
    }
    yellow.turn {
      // The replay has eleven Science tags here, while the spoken physical count is twelve. This
      // proves a one-tag discrepancy by Bactoviral Research, not that this count caused it: an
      // earlier Science-tag play could be absent from the incomplete record, or the table could
      // have overcounted. Add the consequential microbe here while leaving the cause unresolved.
      playProject(BactoviralResearch, 8) { addCardResources(Decomposers) }
          .expect("11 Microbe<$Decomposers>")
      exMachina("Microbe<$Decomposers>")
    }
    blue.turn { cardAction1(MartianMediaCenter) { doTask("PartyDelegate<Reds>") } }
    green.turn { playProject(Mangrove, 10) { placeTile(6, 5) } }
    yellow.turn {
      playProject(NoctisFarming, steel = 4)
      cardAction1(SecurityFleet)
    }
    blue.turn {
      playProject(DevelopmentCenter, 3, steel = 4)
      cardAction1(DevelopmentCenter)
    }
    green.turn {
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
      // Both physical wild tags are chosen as Jovian for Diaspora Movement. Together with the
      // card's own Jovian tag, they explain the spoken three-M€ payout directly.
      exMachina(fakeWildTags("JovianTag", 2))
      playProject(DiasporaMovement, 5).expect("-2 MC")
    }
    yellow.turn { cardAction1(LocalShading) }
    blue.turn { stdAction("LobbyAction", 1) { doTask("PartyDelegate<Unity>") } }
    green.turn { cardAction1(FakeSeptemTribus).expect("8 MC") }
    yellow.turn { cardAction2(EnergyMarket).expect("PROD[-Energy], 8 MC") }
    blue.turn {
      cardAction1(AsteroidDeflectionSystem) { addCardResources(AsteroidDeflectionSystem) }
    }
    green.turn { fundAward(cn("Politician"), 20) }
    yellow.turn { playProject(PhobosSpaceHaven, 12, titanium = 3) }
    blue.turn { cardAction1(Stratopolis) { addCardResources(Stratopolis, 2) } }
    green.pass(unused = WaterSplittingPlant, AquiferPumping, DeuteriumExport)
    yellow.turn { sellPatents(1) }
    blue.turn { playProject(UnexpectedApplication, 4) }
    yellow.turn { playProject(ArtificialLake, 7, steel = 3) }
    blue.turn {
      playProject(SubCrustMeasurements, 20)
      // The transcript and printed card both say 20 M€, while Blue's phone falls by 22 in this
      // interval. Every named intervening cost is already represented and none costs two. The
      // extra debit may nevertheless be a delayed correction of an earlier G9 entry; only if it
      // was not does this row represent a 22-M€ Sub-Crust payment. Preserve the bounded difference,
      // not the unproved local cause.
      exMachina("-2 MC")
      cardAction1(SubCrustMeasurements)
    }
    yellow.turn { cardAction1(MoholeLake) { addCardResources(EcologicalZone) } }
    blue.turn { cardAction1(StratosphericBirds) }
    yellow.turn { sellPatents(1) }
    blue.turn {
      // The hand count is exact at the Generation 5 checkpoint and every later named source/sink is
      // accounted for. One extra card therefore entered Blue's hand between that checkpoint and
      // this sale. The strongest candidate is Space Elevator, successfully revealed by Asteroid
      // Deflection System minutes earlier: retaining that reveal instead of discarding it supplies
      // exactly the second patent. Stanford Tours (Generation 6) or the unnamed Generation 8
      // successful reveal are earlier alternatives.
      exMachina("ProjectCard")
      sellPatents(2)
    }
    yellow.turn {
      cardAction1(RedShips).expect("9 MC")
      // The table debates ten candidates, excludes the non-wet tile, and settles on nine. Yellow's
      // phone later groups 11 M€ where Red Ships plus the adjacent one-card patent sale explain
      // only ten. Entering ten despite the verbal correction is the strongest candidate, though a
      // delayed unrecorded one-M€ gain from earlier in the interval cannot be excluded.
      exMachina("MC")
    }
    blue.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") } }
    yellow.turn {
      stdAction("LobbyAction", 1) { doTask("PartyDelegate<Greens>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<Greens>") }
    }
    blue.pass(unused = TychoMagnetics, BioPrintingFacility)
    yellow.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") } }
    yellow.pass(unused = SearchForLife, FakeAppliedScience)

    // Green once more retained Supercapacitors' energy during final production.
    green.declineTask()
    // Once Diaspora Movement receives the two evidenced wild Jovian tags above, Green reaches the
    // photographed final cash without any residual adjustment.
    // The final photograph has one more Decomposers microbe than the action record; the surviving
    // records do not locate this difference. It also has twelve animals although the final Mohole
    // Lake action explicitly adds an "eagle" to Ecological Zone; an animal was missed or removed
    // sometime after Ecological Zone's Generation 6 play, but the surviving records cannot identify
    // the responsible trigger.
    yellow.exMachina("Microbe<$Decomposers>, -Animal<$EcologicalZone>")

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
    blue.declineTask()
    green.declineTask()
    yellow.convertPlants { placeTile(8, 6) }
    yellow.declineTask()

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
        | 9 -              LD    L    [G2]  [G2]   LT
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
