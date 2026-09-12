package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.pets.ast.ClassName.Companion.cn
import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Four-player physical game played across 2026-09-04, 2026-09-05, and 2026-09-09. Comments preserve
 * authenticated gameplay information from the recordings and participant corrections. They need not
 * reproduce the machine transcripts' exact wording, timestamps, or speaker labels; those files are
 * navigation aids and are sometimes less reliable than the human-corrected test record.
 */
internal class OtbGame20260904Test : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          AmazonisMap
          VenusNextExpansion, PreludeExpansion, Prelude2Expansion, PromoCardPack
          FakeStuffBundle

          Builder, Diversifier, Generalist, Landshaper, Tactician
          Administrator, Excentric, Highlander, Promoter, Thermalist
          """
      )
  // 2:01:56 PM — Green: "We're playing on Amazonas Planitia, the smooth plain. Our expansions are
  // Prelude 1, Prelude 2, Venus, and all the promo cards that we have. Our milestones are builder,
  // diversifier, generalist, land shaper, tactician, and our awards are administrator, eccentric,
  // Highlander, promoter, thermalist."
  // 2:05:44 PM — Green: "Two? We'll give you two and we'll give her four. And give me six."
  // 2:05:49 PM — Blue: "Handicap. So I just add two to my TR then, right? Mm-hmm. I know."
  // Blue and Rainbow used the two- and four-TR handicaps quoted above; the joking suggestion of six
  // for Green never reached any player record.
  override val playerClassPets =
      """
      CLASS Yellow : Player
      CLASS Rainbow : Player { SetupPhase: 4 TerraformRating }
      CLASS Blue : Player { SetupPhase: 2 TerraformRating }
      CLASS Green : Player
      """
          .trimIndent()

  @Test
  internal fun otbGame20260904() {
    TfmWorkflow.Automatic(agents).launch()
    retainStartingProjects(4, 6, 5, 4)
    val yellow = p1.requireExplicitUnusedActionCards()
    val rainbow = p2.requireExplicitUnusedActionCards()
    val blue = p3.requireExplicitUnusedActionCards()
    val green =
        game
            .testTfm(game.actors.filterIsInstance<Player>()[3])
            .requireExplicitPaymentChoices()
            .requireExplicitUnusedActionCards()

    // 2:12:23 PM — Yellow: "Okay, um well all I can do for now is put down My corporation is
    // Ecoline"
    // 2:12:55 PM — Blue: "Alright um well I can show you for now um I will s use this slider to
    // buy four cards, um and I will give myself the um two yes, the three plants. Um"
    yellow.playCorp(Ecoline, 4)
    // 2:14:50 PM — Rainbow: "I am Morningstar!"
    // 2:15:04 PM — Green: "You've already given yourself fifty money, and then how many cards are
    // you gonna buy?"
    // 2:15:05 PM — Blue: "Mm-hmm. Oh, um a lot. One two three four five six."
    rainbow.playCorp(MorningStarInc, 6)
    // 2:15:31 PM — Blue: "I am Helion."
    // 2:15:34 PM — Blue: "So I start with three heat production and I have 42 mega credits. And
    // then you can turn heat into money and I can use heat as money."
    // 2:15:48 PM — Blue: "Okay, damn, that's even better. And I am buying five cards."
    blue.playCorp(FakeHelion, 5)
    // 2:15:56 PM — Green: "All right, and I am playing Factotum, which is an awesome corporation.
    // So I take 37 plus money. I'm buying four cards."
    // 2:16:15 PM — Green: "I get a steel production."
    green.playCorp(Factorum, 4)

    yellow.turn {
      // 2:16:32 PM — Blue: "Right, um. I got a Doe farming, so, um, yes, um, one plant
      // production, two money production, and huge ass teroid, yep."
      playPrelude(DomeFarming).expect("PROD[Plant], PROD[2 MC]")
      // 2:16:51 PM — Blue: "Raise the temperature three um"
      // 2:17:04 PM — Blue: "Yeah um alright and since I hit the bonus I get heat one more time"
      // 2:17:09 PM — Green: "And did you remember to lose the five money? Gotta watch."
      playPrelude(HugeAsteroid).expect("3 TemperatureStep, 3 TerraformRating, PROD[Heat], -5 MC")
    }
    rainbow.turn {
      // 2:17:27 PM — Rainbow: "Yeah, I got space lanes, some planet tags are too off."
      playPrelude(FakeAppliedScience)
      // 2:17:52 PM — Rainbow: "um and this one has an action so super bonus um yeah so i get six
      // science resources"
      // 2:18:13 PM — Green: "You've got a wild tag. So remember, you've always got one more tag
      // while you're performing an action, you have one more tag of whatever kind. It won't help
      // you get awards."
      playPrelude(SpaceLanes)
    }
    blue.turn {
      // 2:18:56 PM — Blue: "Okay, I am going to play Self-Sufficient Settlement."
      // 2:19:44 PM — Blue: "Uh -huh. Okay. Incredible. I think I shall place this... here for a
      // plant and a and a card."
      // 2:19:58 PM — Blue: "So that is row four column eight."
      // 2:20:00 PM — Green: "Mm-hmm. I think you just counted off nine."
      // 2:20:07 PM — Blue: "One two three four five six oh yeah nine."
      // 2:20:08 PM — Green: "No? Okay, row four column nine. The rows are measured as you think,
      // the columns are this way, pointing directly to you."
      playPrelude(SelfSufficientSettlement) { placeTile(4, 9) }
          .expect("CityTile, Plant, ProjectCard, PROD[2 MC]")
      // 2:20:25 PM — Blue: "get two money product."
      // 2:20:29 PM — Blue: "And, uh, my other prelude is terraforming deal."
      // 2:20:35 PM — Blue: "Each step my TR is raised, I gain two mega credits."
      playPrelude(TerraformingDeal)
    }
    // Blue forgets these, then realizes 100 lines down from here and corrects.
    blue.exMachina("-Plant, -ProjectCard")
    green.turn {
      // 2:20:43 PM — Green: "I am first going to play Sweetable Infrastructure."
      // 2:20:56 PM — Green: "I'm going to gain five steel right away."
      // 2:21:07 PM — Green: "And every time I gain production... I can get two money once per
      // action. So if I gain multiple fractions as part of the same action, I still only get two."
      playPrelude(SuitableInfrastructure).expect("5 Steel")
      // 2:21:20 PM — Green: "And I should play another one. Atmospheric Enhancers."
      // 2:21:33 PM — Green: "I raised temp two times."
      // 2:21:36 PM — Green: "So temp is already at minus twenty. I get a heat production. I get two
      // T_R_ Uh no no, this is uh either or. So I'm choosing temperature."
      playPrelude(AtmosphericEnhancers) { doTask("2 TemperatureStep") }.expect("PROD[Heat], 2 MC")
    }
    // I already forgot to get the SuitableInfrastructure bonus!
    green.exMachina("-2 MC")

    // board-14-23-07.jpg: Generation 1 action phase, immediately before Space Mirrors.
    // 2:23:17 PM — Rainbow: "I pay three for space mirrors."
    yellow.turn { playProject(SpaceMirrors, 3) }
    rainbow.turn {
      // 2:23:34 PM — Rainbow: "Okay. I draw cards until I get three Venus."
      stdAction("DoRequiredActionsAction")
      rainbow.exMachina(fakeWildTags("VenusTag"))
    }
    // 2:26:16 PM — Green: "you paid sixteen for that full price, you got two heat production."
    blue.turn { playProject(HomeostasisBureau, 16).expect("PROD[2 Heat]") }
    // 2:26:24 PM — Green: "I'm gonna play titanium mine and I pay three steel and one reel."
    // 2:26:42 PM — Green: "two money from suitable infrastructure I"
    green.turn { playProject(TitaniumMine, 1, steel = 3).expect("PROD[Titanium], MC") }
    yellow.turn {
      // 2:27:00 PM — Blue: "I've noticed um I mean I have something going for me uh seven to use
      // space mirrors gain energy production then six for building infrastructure"
      cardAction1(SpaceMirrors).expect("PROD[Energy]")
      // 2:27:18 PM — Green: "That is building industries Well, I just every time I raise production
      // I get money and then I have the car that lets me raise production"
      playProject(BuildingIndustries, 6).expect("PROD[2 Steel], PROD[-Energy]")
    }
    rainbow.turn {
      // 2:28:42 PM — Rainbow: "That's okay, because it's cheaper to do a floating workbinder."
      // 2:28:45 PM — Rainbow: "For seven money. Which should be 25."
      // Morning Star's wild tag supplies the third Venus tag counted aloud for this operation.
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(FloatingRefinery, 5).expect("3 Floater<$FloatingRefinery>")
      // Rainbow physically paid seven before remembering Space Lanes' 2 M€ discount. Preserve the
      // recorded overpayment until the equally explicit refund below.
      exMachina("-2 MC")
      // 2:28:54 PM — Rainbow: "Beautiful. Okay. Um, uh and then for my second action oh wait, add
      // one floater here for each Venus tag you have. So that's two?"
      // 2:29:03 PM — Blue: "Actually three, because your wild count."
      // 2:29:03 PM — Rainbow: "Three! Woohoo!"
      assertCardResources(3 to FloatingRefinery)
      // 2:29:05 PM — Green: "Also, did you pay full price for that card?"
      // 2:29:08 PM — Green: "You should take two money back."
      exMachina("2 MC")
    }
    // 2:29:45 PM — Blue: "So that cost me 11 money, which is all the money I have left in the
    // world.
    // And I get two titaniums of energy production."
    blue.turn { playProject(SolarWindPower, 11).expect("2 Titanium, PROD[Energy]") }
    green.turn {
      // 2:29:59 PM — Green: "What I decided to do is That, I'm sorry, what I decided to do, okay,
      // I'm going to play Olympus Conference. I spend two steel and six real, and I put a science
      // resource on it."
      playProject(OlympusConference, 6, steel = 2).expect("Science<$OlympusConference>")
    }
    yellow.pass()
    rainbow.turn {
      // 2:31:36 PM — Rainbow: "Oh, this will only be four."
      // 2:31:39 PM — Rainbow: "it's a Planet Tag meeting room."
      // 2:31:41 PM — Green: "Oh, meeting group."
      // 2:31:49 PM — Rainbow: "Yeah, let's leave it there for now."
      playProject(MediaGroup, 4)
    }
    blue.pass()
    green.turn {
      // 2:31:50 PM — Green: "is only four all right you're done with stuff you spent all your money
      // all right it is down to me and it is down to you and i'm gonna play robotic workforce which
      // costs me nine look at all the things it does because it's a science tag which means I lose
      // a
      // science resource to draw a free card and it raises my titanium production and because I
      // raised production it gives me two money can't complain about that [Rainbow]'s turn"
      playProject(RoboticWorkforce, 9) {
            doTask("CopyProductionBox<$TitaniumMine>")
            doTask("ProjectCard FROM Science<$OlympusConference>")
          }
          .expect("PROD[Titanium], -7 MC, -Science<$OlympusConference>, 0 ProjectCard")
    }
    rainbow.turn {
      // 2:33:22 PM — Yellow: "Lava flows."
      // 2:33:38 PM — Yellow: "Three plus and temperature up."
      // 2:33:44 PM — Green: "Yep, so we can all raise temp to minus sixteen if we wish. Give
      // yourself two TR for that."
      // 2:34:10 PM — Green: "All right, so you're going on Hecate's Tholus for two stairs."
      // 2:34:23 PM — Green: "We announced that it's Hecate's Tholus, good enough. But it's five
      // one."
      // 2:34:23 PM — Blue: "Five one. Yeah, that's okay. Okay."
      playProject(LavaFlows, 18) { placeTile(5, 1) }.expect("2 TemperatureStep, 2 TerraformRating")
    }
    green.turn {
      // 2:34:38 PM — Green: "Yes. And [Blue] has already passed, so I'm going to use my factotum
      // to"
      // 2:34:50 PM — Green: "gain an energy production, gives me two money."
      cardAction1(Factorum).expect("PROD[Energy], 2 MC")
    }
    rainbow.turn {
      // 2:35:09 PM — Rainbow: "Right. Okay. Well then I'm going to remove two floaters."
      // 2:35:13 PM — Rainbow: "Yeah. Uh gain titanium and and two money. Burp burp."
      cardAction2(FloatingRefinery).expect("-2 Floater<$FloatingRefinery>, Titanium, 2 MC")
    }
    // 2:35:31 PM — Green: "Yeah, so. Okay, now what I'm gonna do is Nothing. I pass. It's all you,
    // [Rainbow]."
    green.pass()
    // 2:35:57 PM — Rainbow: "I believe I also pass."
    rainbow.pass(unused = FakeAppliedScience)

    // 2:36:16 PM — Yellow: "Anushkin on this two-card spot so no one gets that."
    // 2:36:20 PM — Green: "So that looks like it's eight four to me."
    // 2:36:26 PM — Green: "Eight four for two car and nobody gets the cards, of course."
    yellow.wgt("OceanTile<Amazonis_08_04>")

    // board-14-37-01.jpg and all four app histories: Generation 2 before Research.
    with(yellow) {
      assertProduction(m = 2, s = 2, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 28, s = 2, t = 0, p = 6, e = 0, h = 1)
      assertCounts(23 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 36, s = 2, t = 4, p = 0, e = 0, h = 0)
      assertCounts(26 to "TerraformRating")
      assertCardResources(1 to FloatingRefinery, 6 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 2, s = 0, t = 0, p = 0, e = 1, h = 5)
      assertResources(m = 24, s = 0, t = 2, p = 0, e = 1, h = 5)
      assertCounts(22 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 0, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 37, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertCounts(22 to "TerraformRating")
    }
    assertSidebar(gen = 2, temp = -16, oxygen = 0, oceans = 1, venus = 0)

    // 2:39:49 PM — Blue: "I will buy two. It occurs to me I maybe could have kept a different,
    // but it's probably too expensive to actually be worth. I'll buy two cards. Did I ever actually
    // draw a card when I put my city on that thing where I would draw a card?"
    // 2:40:15 PM — Blue: "I just realized I don't think I did. All of these cards that I have is
    // because I drafted and bought them."
    // 2:41:59 PM — Blue: "Dude, I never gave myself either of plants."
    // 2:42:01 PM — Yellow: "you never you never gave yourself my plants either that's terrible to
    // have a plant this is crazy"
    // Blue noticed the two settlement bonuses while buying cards and restored them together.
    blue.exMachina("Plant, ProjectCard")

    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(2)

    rainbow.turn {
      // 2:42:41 PM — Green: "So hit your money 27 there, and then the standard projects come up.
      // Oh,
      // but you don't see our scrapping."
      // 2:42:48 PM — Green: "Okay, so do 15 minus. And then give yourself a T_R_ And now Venus is
      // at two."
      stdProject("AirScrappingProject")
    }
    blue.turn {
      // 2:43:13 PM — Blue: "nuclear zone so that cost me ten money oops it cost me ten money and
      // then I raised the temperature two steps which does give me a plant production and also
      // raising the temperature two steps gives me six money"
      // 2:43:39 PM — Blue: "And two T-R boots, which in turn gives me four more money."
      // 2:44:03 PM — Blue: "You're such a jerk. I'm gonna go ahead and go on row three column
      // three and two cards please. Yeah."
      playProject(NuclearZone, 10) { placeTile(3, 3) }
          .expect("2 TemperatureStep, 2 TerraformRating, PROD[Plant]")
    }
    green.turn {
      // 2:45:05 PM — Green: "I'm going to use my factotum. I cannot take energy production because
      // I have energy resources, but I can pay three money to draw a building tag. bio printing
      // facility what a strikeout that was discard"
      cardAction2(Factorum)
      // 2:45:34 PM — Green: "that was my well you know let me do one more thing on my turn just in
      // case let's play pets ten minus and I get a pet for that"
      playProject(Pets, 10).expect("Animal<$Pets>")
    }
    // 2:47:06 PM — Yellow: "I think I will "spa-che" mirrors. Seven money for energy product."
    yellow.turn { cardAction1(SpaceMirrors).expect("PROD[Energy]") }
    rainbow.turn {
      // 2:47:21 PM — Rainbow: "Man, I don't produce anything. Okay, um, however, I'm going to spend
      // four on sulfur-eating bacteria."
      playProject(SulphurEatingBacteria, 4)
      // 2:47:48 PM — Green: "Alright, so you played it and you used the action. Put your rainbow on
      // it if you used the action."
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
    }
    // 2:49:00 PM — Blue: "My s little sponsors. They love me and they're gonna give me too many
    // production."
    blue.turn { playProject(Sponsors, 6).expect("PROD[2 MC]") }
    // 2:50:12 PM — Green: "I know my pass."
    green.pass()
    // 2:50:48 PM — Blue: "Um, I think I will also pass."
    yellow.pass()
    // 2:50:54 PM — Rainbow: "Wow. All right, I'm going to add a floater. Nibble it."
    rainbow.turn { cardAction1(FloatingRefinery).expect("Floater<$FloatingRefinery>") }
    blue.turn {
      // 2:51:26 PM — Blue: "And I would like to have played both of them before the next round,
      // but I guess whatever. Okay. I'm going to spend 12 money and two heat. Alright. Some
      // Neptunian power consoles next."
      // 2:51:50 PM — Blue: "So when any ocean is placed, I may spend five money to raise my
      // energy production one step and add one hydroelectric resource there."
      playProject(NeptunianPowerConsultants, 12, heat = 2)
    }
    rainbow.turn {
      // 2:53:10 PM — Green: "Okay. So you've used applied science and put a microbe on
      // sulfur-eating bacteria."
      // 2:53:12 PM — Rainbow: "So I'm gonna have you use applied science Sulfur-eating bacteria.
      // I'm
      // gonna pass."
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
          .expect("-Science<$FakeAppliedScience>, Microbe<$SulphurEatingBacteria>")
    }
    blue.pass()
    rainbow.turn {
      // 2:53:33 PM — Rainbow: "I want to have money, but I want to have these things. The
      // conundrum.
      // I'm going to go ahead and play my space station."
      // 2:53:53 PM — Rainbow: "And I'm going to spend one titanium and seven money?"
      // 2:54:05 PM — Rainbow: "You're right. I'm going to spend it all down."
      // 2:54:13 PM — Rainbow: "Three titaniums is nine money."
      // 2:54:15 PM — Rainbow: "So I only have to spend one money."
      playProject(SpaceStation, 1, titanium = 3)
    }

    // 2:54:19 PM — Rainbow: "Okay. And that I played Space Station. Um no, no I think that's it. I
    // passed."
    rainbow.pass()
    // 2:54:39 PM — Green: "I mean, [Rainbow] decides which global parameter to increase with no
    // benefit
    // to herself, including you could play some ocean if you want."
    // 2:54:56 PM — Rainbow: "Yeah. Nope, Venus up to four, right?"
    rainbow.wgt("VenusStep").expect("0 TerraformRating")

    // board-14-55-23.jpg and all four app histories: Generation 3 before Research.
    with(yellow) {
      assertProduction(m = 2, s = 2, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 40, s = 4, t = 0, p = 9, e = 1, h = 2)
      assertCounts(23 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 0, s = 0, t = 0, p = 0, e = 0, h = 0)
      assertResources(m = 34, s = 2, t = 1, p = 0, e = 0, h = 0)
      assertCounts(27 to "TerraformRating")
      assertCardResources(
          2 to FloatingRefinery,
          2 to SulphurEatingBacteria,
          5 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 1, h = 5)
      assertResources(m = 28, s = 0, t = 2, p = 2, e = 1, h = 9)
      assertCounts(24 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 0, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 37, s = 2, t = 4, p = 0, e = 1, h = 3)
      assertCounts(22 to "TerraformRating")
      assertCardResources(1 to Pets)
    }
    assertSidebar(gen = 3, temp = -12, oxygen = 0, oceans = 1, venus = 4)

    blue.buyCards(1)
    green.buyCards(3)
    yellow.buyCards(3)
    rainbow.buyCards(4)

    blue.turn {
      // 3:01:51 PM — Blue: "Fuck my stupid fucking childish life. Okay. Lunar beam cost me
      // thirteen money."
      // 3:02:23 PM — Blue: "I lose two mega credits, I gain two heat production, I mean I lose
      // two mega credit production, I gain two heat production, and two energy production. And
      // that's my turn. And now it's Green's turn."
      playProject(LunarBeam, 13).expect("PROD[-2 MC], PROD[2 Heat], PROD[2 Energy]")
    }
    green.turn {
      // 3:02:40 PM — Green: "Okay, I'm going to... Play Cloud Tourism for full price. I have only
      // two sets of Venus and Earth tags. That's what makes me sad, but I had to get this down."
      // 3:03:05 PM — Green: "So I get two mega credit production and two mega credits from
      // Sweetable Infrastructure. Um, and is there anything else I'm in a hurry to do? No, I pass.
      // I end my turn."
      playProject(CloudTourism, 11).expect("PROD[2 MC], -9 MC")
    }
    // 3:03:39 PM — Yellow: "a seven for spaghettimi roars, so I gain energy product shun. All
    // right."
    yellow.turn { cardAction1(SpaceMirrors).expect("PROD[Energy]") }
    rainbow.turn {
      // 3:05:56 PM — Rainbow: "i'm close enough that i can play ishtar mining for three uh-huh not
      // bad you got a titanium production finally I finally learned about titanium production."
      playProject(IshtarMining, 3).expect("PROD[Titanium]")
    }
    blue.turn {
      // 3:06:29 PM — Blue: "Finally learned about titanium. Alright, that's it. Alright, alright
      // fuck, what do I have now? I have 12 micro, so I have, yeah, oh okay. I'm gonna play
      // advanced
      // hours."
      playProject(AdvancedAlloys, 9)
    }
    green.turn {
      // 3:07:06 PM — Green: "I'm going to use Factorum to spend three and get a wielding card."
      cardAction2(Factorum)
    }
    yellow.turn {
      // 3:07:34 PM — Blue: "My turn. Flooding."
      // 3:07:50 PM — Rainbow: "I pay a seven."
      // 3:08:02 PM — Rainbow: "Um, get my two plants, get my ocean and"
      playProject(Flooding, 7) {
            // Blue does not have to spend a heat (couldn't if she wanted to)
            doTask("OceanTile<Amazonis_05_10>! THEN -3 MC<Blue>")
            blue.declineTask()
          }
          .expect("OceanTile, 2 Plant")
      // 3:08:23 PM — Yellow: "Mm-hmm. Underground city."
      // 3:08:32 PM — Blue: "Yeah. Um my four steel and ten reel. Do do do do do do do do do do."
      // 3:08:51 PM — Green: "So that's 10-10."
      playProject(UndergroundCity, 10, steel = 4) { placeTile(10, 10) }
          .expect("CityTile, 2 Plant, PROD[-2 Energy], PROD[2 Steel]")
    }
    rainbow.turn {
      // 3:10:22 PM — Blue: "Okay, I'm going to do sulfur exports."
      // 3:10:49 PM — Green: "Alright, so it's one titanium and fourteen money. Yep."
      // 3:10:58 PM — Rainbow: "yep Okay, so now I increase Venus one step, gives me TR, and I
      // increase my money production one step for each Venus tag I have, which is one two three
      // four
      // five and six."
      rainbow.exMachina(fakeWildTags("VenusTag"))
      playProject(SulphurExports, 14, titanium = 1).expect("VenusStep, TerraformRating, PROD[6 MC]")
    }
    // 3:11:32 PM — Blue: "I'm gonna convert 80 to a temperature."
    // 3:11:38 PM — Blue: "gives me five money."
    blue.turn { convertHeat() }
    green.turn {
      // 3:11:52 PM — Yellow: "So really, I play Venus shuttles, I pay full price, nine minus, it
      // tells me to add two floaters to cloud tourism, and for my next trick, I'm going to use it.
      // Oh, I don't have any money to use it."
      playProject(VenusShuttles, 9) { addCardResources(CloudTourism, 2) }
          .expect("2 Floater<$CloudTourism>")
    }
    yellow.turn {
      // 3:12:45 PM — Yellow: "Right. What I do is, um, now things might start, hopefully start
      // getting good, um, plant forest for seven."
      // 3:13:11 PM — Green: "Ten nine."
      convertPlants { placeTile(10, 9) }
          .expect("-4 Plant, GreeneryTile, OxygenStep, TerraformRating")
      // 3:13:36 PM — Yellow: "Yeah Wait, wait, I oh I can actually place it here for two steals
      // Uh-huh."
      convertPlants { placeTile(10, 11) }
          .expect("-7 Plant, GreeneryTile, OxygenStep, TerraformRating, 2 Steel")
    }
    // 3:14:09 PM — Rainbow: "All right. Um, I think I'm going to remove two floaters to gain one
    // titanium and two money."
    rainbow.turn {
      cardAction2(FloatingRefinery).expect("-2 Floater<$FloatingRefinery>, Titanium, 2 MC")
    }
    // 3:14:21 PM — Blue: "Okay, that's it for now. I pass."
    blue.pass()
    green.turn {
      // 3:15:15 PM — Green: "I sell two patents."
      // 3:15:26 PM — Green: "Now I have nine money. Now I use Venus Rebels to spend nine money."
      // 3:15:33 PM — Green: "So Venus is now on eight. I get a TR for that."
      sellPatents(2)
      cardAction1(VenusShuttles).expect("-9 MC, VenusStep, TerraformRating")
    }
    yellow.turn {
      // 3:15:42 PM — Blue: "Anyways, what I can do is play Natural Preserve for 2 steel and 5
      // real. increase money production by one. Oh wow."
      // 3:16:10 PM — Blue: "Place it on next to no other tile. I'll choose one four for a card.
      // Hope it's a good one."
      playProject(NaturalPreserve, 5, steel = 2) { placeTile(1, 4) }
          .expect("SpecialTile, 0 ProjectCard, PROD[MC]")
    }
    // 3:17:01 PM — Rainbow: "Okay. I played Rover Construction."
    // 3:17:22 PM — Rainbow: "I paid four and two steel. Well, at this point I only paid two steel.
    // Alright, keep me honest. That's the end of my turn."
    rainbow.turn { playProject(RoverConstruction, 4, steel = 2) }
    // 3:17:35 PM — Green: "Okay, I probably am going to keep losing my fees. So I used Vienna
    // strudels already. I'm going to use cloud tourism to add a floater to cloud tourism."
    green.turn { cardAction1(CloudTourism).expect("Floater<$CloudTourism>") }
    // 3:18:05 PM — Rainbow: "I'm going to take a science resource and put it on my self-rating
    // bacteria."
    // Applied Science is legally replayed after Yellow passes without moving this evidence.
    // 3:18:15 PM — Yellow: "I think I'm ready to pass out of here. Pass out."
    yellow.pass()
    rainbow.turn {
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
          .expect("-Science<$FakeAppliedScience>, Microbe<$SulphurEatingBacteria>")
    }
    green.pass()
    // 3:18:26 PM — Rainbow: "Okay. Then I'm going to add another microbe to self-reating bacteria.
    // And then I too shall pass."
    rainbow.turn {
      cardAction1(SulphurEatingBacteria).expect("Microbe<$SulphurEatingBacteria>")
    }
    rainbow.pass()

    // 3:19:00 PM — Blue: "Yeah. ocean. Thank goodness."
    // 3:19:37 PM — Green: "Two six."
    blue.wgt("OceanTile<Amazonis_02_06>")
    // 3:19:50 PM — Blue: "but I do get to do my Neptunian power consults so I will pay five monies
    // Yeah, so I get an energy production and I place a hydroelectric resource on Neptunian power
    // result."
    blue.doTask("UseAction<NeptunianOption, Action1>")
    blue.pay(5)

    // board-15-20-29.jpg and all four app histories: Generation 4 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 31, s = 4, t = 0, p = 5, e = 0, h = 4)
      assertCounts(26 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 0, e = 0, h = 0)
      assertResources(m = 37, s = 0, t = 2, p = 0, e = 0, h = 0)
      assertCounts(28 to "TerraformRating")
      assertCardResources(4 to SulphurEatingBacteria, 4 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 2, s = 0, t = 0, p = 1, e = 4, h = 7)
      assertResources(m = 27, s = 0, t = 2, p = 3, e = 3, h = 9)
      assertCounts(25 to "TerraformRating")
      assertCardResources(1 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 25, s = 3, t = 6, p = 0, e = 1, h = 5)
      assertCounts(23 to "TerraformRating")
      assertCardResources(2 to Pets, 3 to CloudTourism)
    }
    assertSidebar(gen = 4, temp = -10, oxygen = 2, oceans = 3, venus = 8)

    green.buyCards(4)
    yellow.buyCards(3)
    rainbow.buyCards(3)
    blue.buyCards(3)

    green.turn {
      // 3:26:34 PM — Green: "Earth office, earth office."
      playProject(EarthOffice, 1)
      // 3:26:38 PM — Green: "You cost me one, then I can get a type of contract, and it costs me
      // only five, and I get three plantos,"
      playProject(TopsoilContract, 5)
    }
    // 3:26:58 PM — Yellow: "what was it um I'm gonna Sabotage you, Green, you lose your three
    // steel.
    // I
    // like attacking that."
    // 3:27:14 PM — Yellow: "Yeah, just thought I could hurt you. I hope I attack the right players.
    // What was my plan here? I had a plan, and I think I was going to turn all my microbes into
    // money."
    yellow.turn { playProject(Sabotage, 1) { doTask("-3 Steel<Green>") } }
    rainbow.turn {
      // 3:27:37 PM — Green: "Yeah, but do you want to use your applied sciences first so you have
      // more microbes to turn into money?"
      // 3:27:49 PM — Green: "Yes, applied sciences to put one more on sulfur or whatever, eating
      // bacteria."
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      // 3:27:55 PM — Rainbow: "Sulfur eating bacteria, using sulfur eating bacteria to turn five
      // microbes into 15 money."
      cardAction2(SulphurEatingBacteria, x = 5)
    }
    blue.turn {
      // Blue paid 18 M€ and 5 heat for Protected Valley at 2-7, receiving the space's two titanium
      // and 2 M€ ocean adjacency. She then paid 4 M€ and 4 heat to claim Landshaper.
      playProject(ProtectedValley, 18, heat = 5) { placeTile(2, 7) }
      doTask("UseAction<ClaimMilestoneAction, Action1>")
      pay(4, heat = 4)
      doTask("Landshaper")
    }
    green.turn {
      // 3:33:24 PM — Green: "Okay. I'm gonna sell this card for one money."
      sellPatents(1)
      // 3:33:38 PM — Green: "Earth, microbe, building, science, animal, Jovian, Venus, Paris. power
      // so so I take Diversifier so now Landshaper and Diversifier are taken and I summed eight on
      // that"
      claimMilestone(cn("Diversifier"))
    }
    // 3:34:12 PM — Blue: "Okay, it is wait, it is my a tour on suppose I may as well spache mi
    // errores on seven. Energia."
    yellow.turn { cardAction1(SpaceMirrors) }
    // 3:35:04 PM — Rainbow: "Alright well, I can build wave power for Eight full money."
    rainbow.turn { playProject(WavePower, 8) }
    blue.pass()
    // 3:35:33 PM — Green: "I... Use my cloud tourism data floater, [Yellow]."
    green.turn { cardAction1(CloudTourism) }
    // 3:35:41 PM — Blue: "I think I will adapt it like Ken. Bit expensive, but gain plant
    // production and I like having that."
    yellow.turn { playProject(AdaptedLichen, 9) }
    rainbow.turn {
      // 3:37:01 PM — Rainbow: "But let me pay 15... money... for eosulfur research. And I get three
      // cards."
      playProject(IoSulphurResearch, 15) { doTask("3 ProjectCard") }
    }
    // 3:37:26 PM — Green: "I cannot use my Factorum action, so I have nothing left, or any
    // shuttles, so I must pass."
    green.pass(unused = setOf(Factorum, VenusShuttles))
    // 3:37:36 PM — Blue: "I believe I pass as well."
    yellow.pass()
    rainbow.turn {
      // 3:39:23 PM — Rainbow: "So that be nice. It's not the worst. OK, but I could be more
      // strategic
      // about this. I can and I will somehow. Alright, gotta pay full price for this one. Ten
      // money.
      // For bacterial virus research."
      // 3:40:00 PM — Rainbow: "I get to uh add one microbe to a played card for each science tag I
      // have, including this, which is one, two, and three."
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(BactoviralResearch, 10) {
        addCardResources(SulphurEatingBacteria)
      }
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      // 3:41:02 PM — Rainbow: "Fine, then I'm gonna pay eight money for a at a Linta planitia lab,
      // which gets me two cards."
      playProject(AtalantaPlanitiaLab, 8)
    }
    // 3:41:34 PM — Rainbow: "Yep. Yeah, believe me, I tried all kinds of ways to make that happen,
    // and I just wasn't. Um, I'm finally done. I pass."
    rainbow.pass(unused = FloatingRefinery)

    // 3:42:11 PM — Green: "I don't know. What I really care about I think I'll bump oxygen up to
    // four. Then the start player token moves to Wait wait wait wait before we do all those things
    // I
    // wanna take the picture of your exact boundary of the generation"
    green.wgt("OxygenStep")

    // board-15-42-48.jpg and all four app histories: Generation 5 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 34, s = 8, t = 0, p = 9, e = 1, h = 5)
      assertCounts(26 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 0, e = 1, h = 0)
      assertResources(m = 36, s = 0, t = 3, p = 0, e = 1, h = 0)
      assertCounts(28 to "TerraformRating")
      assertCardResources(3 to SulphurEatingBacteria, 3 to FakeAppliedScience)
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 4, h = 7)
      assertResources(m = 30, s = 0, t = 4, p = 4, e = 4, h = 10)
      assertCounts(26 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(1 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 2, s = 1, t = 2, p = 0, e = 1, h = 1)
      assertResources(m = 25, s = 1, t = 8, p = 3, e = 1, h = 7)
      assertCounts(23 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(2 to Pets, 4 to CloudTourism)
    }
    assertSidebar(gen = 5, temp = -10, oxygen = 4, oceans = 3, venus = 8)

    yellow.buyCards(2)
    rainbow.buyCards(3)
    blue.buyCards(2)
    green.buyCards(2)

    yellow.turn {
      // 3:49:54 PM — Blue: "Sure, let's start with, um, I'm gonna do something not known to
      // mankind and actually play cutting edge."
      // 3:50:03 PM — Blue: "Um, twelve. And then I will pay seven for marriage and survey. We are
      // thankfully just at four percent. Or what, what? No, no, no. Um seven minus two kinds of
      // soup."
      playProject(CuttingEdgeTechnology, 12)
      playProject(MartianSurvey, 7)
    }
    // 3:50:38 PM — Rainbow: "Spend eight more fact or shuns."
    rainbow.turn { claimMilestone(cn("Tactician")) }
    blue.turn {
      // 3:51:32 PM — Blue: "I'm gonna play my big ass toroid, so I will spend four titanium for
      // that and eleven mega credits."
      // 3:51:46 PM — Blue: "And then sixteen money, 'cause I have advanced alloys."
      // 3:51:51 PM — Blue: "Um and so for big asteroid I gain four titanium."
      // 3:51:59 PM — Blue: "And I do two temperature raises. Yep."
      // 3:52:11 PM — Blue: "And I get two TR and ten money. And who has plants?"
      // 3:52:30 PM — Blue: "That was so awesome. I basically just spent one money to get two
      // points and steal four of [Yellow]'s plants."
      playProject(BigAsteroid, 11, titanium = 4) { doTask("-4 Plant<Yellow>") }
    }
    green.turn {
      // 3:53:00 PM — Green: "I'm no longer under as much urgency. Oh, oh, this is interesting.
      // Tech demo. Cost me full price. Draws me two cards. Hmm. Gives me a science resource on
      // account of which I can play Diversity Support."
      // Green's quoted "Cost me full price" establishes that he preserved his titanium despite the
      // Space tag.
      intentionalUnderpay()
      playProject(TechnologyDemonstration, 5)
      // 3:53:38 PM — Green: "Because I have Science, Animal, Floater, and all six standard. And
      // that costs me one money, right? I don't have any discounts on. So that's my turn."
      playProject(DiversitySupport, 1)
    }
    // Green forgot to take the TR from Diversity Support, and corrects this later.
    green.exMachina("-TerraformRating")
    // 3:54:23 PM — Blue: "Okay, nine for Lagange of self with whole hay."
    // 3:54:27 PM — Blue: "La grande. I draw it hard."
    yellow.turn { playProject(LagrangeObservatory, 9) }
    rainbow.turn {
      // 3:54:59 PM — Rainbow: "Are you fucking kidding me? Okay. Well, I'm taking my applied
      // science
      // action, and I'm applying my science resource to Sulfur Eating Bacteria. Then I'm going to
      // take my Sulfur Eating Bacteria action. And turn these four science resources into 12
      // money."
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
      cardAction2(SulphurEatingBacteria, x = 4)
    }
    // 3:55:26 PM — Blue: "Okay, I'm gonna raise the temperature and convert my heat."
    // 3:55:33 PM — Blue: "[Yellow]'s gonna snatch a bonus. And for that I raise my... Okay, the
    // temperature's to minus four, and it raised my T_R_ automatically, so I don't need to do that
    // and I get five more money."
    blue.turn { convertHeat() }
    green.turn {
      // 3:56:12 PM — Green: "Well, let us use There's so much I want to do and so little to do it
      // with. Let us assume my priority is to play icy impactors. That cost me five titanium."
      playProject(IcyImpactors, titanium = 5)
    }
    // 3:56:59 PM — Yellow: "I didn't like giving that card up. I just like, I couldn't afford it.
    // Um, I pass."
    yellow.pass(unused = SpaceMirrors)
    rainbow.turn {
      // Rainbow's app entry 89 debits 2 M€ where the transcript establishes a 25 M€ cash
      // payment. Supply the omitted 23 M€ only long enough for the real invoice to reproduce that
      // captured under-debit.
      exMachina("23 MC")
      // 3:57:07–3:58:42 PM — Rainbow values three titanium at nine M€, pays the remaining 25 M€,
      // raises temperature twice, and places three oceans. Her offer to take up to six plants from
      // Yellow removes Yellow's actual five. Blue uses all three Neptunian responses and reaches
      // four hydroelectric resources.
      playProject(GiantIceAsteroid, 25, titanium = 3) {
            // 3:58:46 PM — Green: "Alright, since I have you going on seven, four for two money and
            // two plants."
            placeTile(7, 4)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            // 3:59:00 PM — Green: "All right this would be nine four."
            // 3:59:01 PM — Blue: "Two money, one steel and one titanium. Where am I doing the
            // next
            // one? Only two money or two plants. Do I need another card? Do I need another card?"
            placeTile(9, 4)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            // Crossing 0°C supplies Amazonis's temperature-track ocean bonus.
            // 3:59:30 PM — Green: "All right, going to 611 and you get a card. What a turn of
            // events."
            placeTile(6, 11)
            autoExecNow()
            selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
            doTask("-5 Plant<Yellow>!")
          }
          .expect("-16 MC, -2 Titanium, Steel, 2 Plant, PROD[3 Energy<Blue>]")
    }
    // The real card operation includes Media Group's 3 M€ event response, but Rainbow's app has no
    // corresponding credit in entries 89–98.
    rainbow.exMachina("-3 MC")
    // 3:59:44 PM — Rainbow: "Did I pay for that?"
    // 3:59:48 PM — Rainbow: "I'll try it again. 25."
    // 4:00:09 PM — Green: "You you. just literally just skipped paying."
    // Rainbow then concludes that she skipped the payment and enters the full narrated 25 M€.
    rainbow.exMachina("-25 MC")

    blue.turn {
      // 4:01:12 PM — Blue: "Giant space mirror."
      // 4:01:19 PM — Blue: "Titanium and one mega credit, and I gain three energy production."
      playProject(GiantSpaceMirror, 1, titanium = 4)
    }
    green.turn {
      // 4:01:55 PM — Green: "Well Let's probably do the most alright. Hmm. Okay. What is important
      // here? What is important? I have all this stuff to you. Okay, okay. Rad Chem factory, one
      // steel six real. I lose in energy production, I gain two TR. Wow, I'm up to 25."
      playProject(RadChemFactory, 6, steel = 1)
    }
    // 4:02:33 PM — Blue: "Um, I think I pass."
    // 4:02:48 PM — Rainbow: "I'm going to add a floater to my floating refinery."
    rainbow.assertCardResources(0 to FloatingRefinery)
    rainbow.turn { cardAction1(FloatingRefinery).expect("Floater<$FloatingRefinery>") }
    rainbow.assertCardResources(1 to FloatingRefinery)
    // No recording or app entry supplies another action between this one-cube result and the
    // 16:07:06 photograph, which visibly has two cubes. The extra floater's earliest possible point
    // is the preceding Generation 5 checkpoint and its latest is that photograph.
    rainbow.exMachina("Floater<$FloatingRefinery>")
    rainbow.assertCardResources(2 to FloatingRefinery)
    blue.pass()
    // 4:02:56 PM — Green: "I'm going to use Icy Impactors to spend three titanium and one real
    // money to put two roids on the card. [Yellow]'s turn."
    green.turn { cardAction1(IcyImpactors) { pay(1, titanium = 3) } }
    // 4:03:17 PM — Rainbow: "going to spend four money on moss which requires me to lose a plant
    // but
    // I get plant production"
    rainbow.turn { playProject(Moss, 4) }
    green.turn {
      // 4:03:33 PM — Green: "How come I like never get production even though it's it's my thing
      // thing All right. I think I'm going to prioritize getting production then. So I will play
      // Venus Governator, which costs me four. It gives me two money production and then sweetable
      // infrastructure gives me two money. And [Rainbow]'s turn."
      playProject(VenusGovernor, 4)
    }
    // 4:04:53 PM — Rainbow: "Okay, I'm gonna sell two cards for two money"
    rainbow.turn { sellPatents(2) }
    green.turn {
      // 4:05:07 PM — Green: "Think? Oh, I spend three to draw a building card, because I totally
      // need more of those. Okay, it's geothermal power."
      cardAction2(Factorum)
      // 4:05:27 PM — Green: "I can indeed cube it. Now, I think the only other thing I'm doing is
      // cloud tourism, so let's do that and I'm done."
      cardAction1(CloudTourism)
    }
    rainbow.turn {
      // 4:05:41 PM — Rainbow: "Okay. I am paying seven for thermafiles. Okay. And then I guess I
      // will
      // take the action."
      // 4:06:02 PM — Rainbow: "I can add one microbe to any Venus card, or I can spend two microbes
      // from this card tree as Venus one step. Hmm, but I think at this point I'd rather have
      // potential money."
      playProject(Thermophiles, 7)
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    green.pass(unused = VenusShuttles)
    rainbow.pass()

    // 4:06:32 PM — Yellow: "I think I'm gonna boop the temperature."
    // 4:06:35 PM — Green: "Temp to two."
    yellow.wgt("TemperatureStep")

    // 2:59:17 PM — Green confirms 45 M€ for Rainbow and 33 M€ for Green before Research. Rainbow's
    // entries 103/113 remain at 40 despite entry 113 saying -5, then entry 114 adds 5 to reach 45.
    // Green's entry 104 changes 30 to 33. Neither correction's cause is known, so each is isolated
    // at its own app entry.
    rainbow.assertCounts(40 to "MC")
    green.assertCounts(30 to "MC")
    rainbow.exMachina("5 MC")
    green.exMachina("3 MC")
    rainbow.assertCounts(45 to "MC")
    green.assertCounts(33 to "MC")

    // 2:59:32–2:59:45 PM — Spoken counts are Green 9, then machine-labeled Yellow 6, Blue 8, and
    // Rainbow 11. The later complete card flows establish Yellow 8, Blue 6, and Rainbow 12 here;
    // machine speaker labels and the off-by-one Rainbow count do not override those owned cards.
    yellow.assertCounts(8 to "CardBack<Hand>")
    rainbow.assertCounts(12 to "CardBack<Hand>")
    blue.assertCounts(6 to "CardBack<Hand>")

    // board-16-07-06.jpg and all four app histories: Generation 6 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 29, s = 12, t = 0, p = 4, e = 1, h = 7)
      assertCounts(26 to "TerraformRating", 8 to "CardBack<Hand>")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 45, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertCounts(33 to "TerraformRating", 1 to "Tactician", 12 to "CardBack<Hand>")
      assertCardResources(
          2 to FloatingRefinery,
          1 to SulphurEatingBacteria,
          2 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 10, h = 7)
      assertResources(m = 45, s = 0, t = 0, p = 5, e = 10, h = 13)
      assertCounts(29 to "TerraformRating", 1 to "Landshaper", 6 to "CardBack<Hand>")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 4, s = 1, t = 2, p = 0, e = 0, h = 1)
      assertResources(m = 33, s = 1, t = 2, p = 3, e = 0, h = 9)
      assertCounts(25 to "TerraformRating", 1 to "Diversifier", 9 to "CardBack<Hand>")
      assertCardResources(2 to Pets, 5 to CloudTourism, 2 to IcyImpactors, 1 to OlympusConference)
    }
    assertSidebar(gen = 6, temp = 2, oxygen = 4, oceans = 6, venus = 8)

    // Entry 105 restores the TR Green had omitted from Diversity Support in Generation 5.
    green.exMachina("TerraformRating")

    // Green initially chose four cards, returned one, and bought three; Yellow bought three.
    // The phone histories charge Rainbow for one card and Blue for two despite their quoted
    // two- and three-card statements. Rainbow's later complete card flow confirms that she kept
    // the two cards she names here; preserve the app's observed 3 M€ payment separately.
    rainbow.buyCards(2).expect("-6 MC")
    rainbow.exMachina("3 MC")
    blue.buyCards(2).expect("-6 MC")
    green.buyCards(3).expect("-9 MC")
    yellow.buyCards(3)

    rainbow.turn {
      // 3:05:43 PM — Rainbow: "Alright. Um I'm gonna play Mining Rights."
      // 3:05:47 PM — Rainbow: "I'm gonna spend my steel as two money."
      // 3:05:52 PM — Rainbow: "And that means I have to pay seven."
      // 3:07:24 PM — Rainbow: "is around it, but that won't give me points anyway. So guess we come
      // here
      // for two steels."
      // 3:07:31 PM — Green: "Alright, one two."
      playProject(MiningRights, 7, steel = 1) { placeTile(1, 2) }.expect("-7 MC")
    }
    blue.turn {
      // 3:08:57 PM — Blue: "Convoy from Europa. Yeah. So I'll place an ocean tile."
      // 3:09:09 PM — Blue: "Yeah. And I put it here, which is three... One two three four five
      // six."
      // 3:09:29 PM — Green: "Three six for two money."
      playProject(ConvoyFromEuropa, 15) {
            placeTile(3, 6)
            doTask("UseAction<NeptunianOption, Action1>")
            pay(5)
          }
          .expect("-16 MC")
    }
    green.turn {
      // 3:10:13 PM — Green: "Okay. Now is your turn. You can flip your event code either. Seven
      // oceans, huh? I'm going to play decomposers, which costs me full price. Alright minus, we do
      // have the three percent oxygen, barely. Um it gives me an automatic plant, right?"
      playProject(Decomposers, 5)
      // 3:11:07 PM — Green: "And as my second for my second trick, I'm just going to go ahead and
      // fund the eccentric"
      fundAward(cn("Excentric"), 8)
    }
    yellow.turn {
      // 3:11:18 PM — Yellow: "Um? What was it? Um? Um yes, I'm going to spend seven Master of
      // Mechanica. Choose two project cards from my event pile. Take them into my hand."
      playProject(AstraMechanica, 7) {
        doWithoutAutoExec(yellow) {
          doTask("ProjectCard FROM PlayedEvent<Class<$Flooding>>")
          doTask("ProjectCard FROM PlayedEvent<Class<$Sabotage>>")
        }
      }
    }
    rainbow.turn {
      // 3:12:52 PM — Rainbow: "Twenty six plus eighteen is a lot more than forty five."
      // 3:12:54 PM — Yellow: "Forty four."
      // 3:12:54 PM — Rainbow: "Damn it. Alright. Um. So I'm gonna pay nine."
      // 3:13:10 PM — Rainbow: "And then I'm going to take my ansaction."
      // 3:13:32 PM — Green: "Do I have a nine plus card in my hand that has a microbe tag on it and
      // is called ants? All right."
      playProject(Ants, 9).expect("-9 MC")
      cardAction1(Ants) {
        doTask("-Microbe<Green, $Decomposers<Green>>")
      }
      assertCounts(26 to "MC")
    }
    // 3:13:39 PM — Blue: "Okay, I'm gonna convert my heat."
    // 3:13:42 PM — Blue: "Raise temperature."
    // 3:13:45 PM — Blue: "Oh, and I get five money."
    blue.turn { convertHeat() }
    // 3:14:06 PM — Green: "I'm going to and take my factotum action to get an energy production
    // because I have no energy resources, and then I get two money at each turn."
    green.turn { cardAction1(Factorum) }
    // 3:14:17 PM — Yellow: "I'm gonna take my spacciamerones action. Um, spend seven, gain energy
    // apparatus. Rainbow. Mm-hmm."
    yellow.turn { cardAction1(SpaceMirrors) }
    rainbow.turn {
      // 3:14:27 PM — Rainbow: "Um. I'm gonna go ahead and remove a science resource from applied
      // science and put it on sulfur-eating bacteria."
      cardAction1(FakeAppliedScience) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      // 3:14:42 PM — Blue: "Okay. I'm going to play indentured workers, my bad guys, which costs
      // zero money, and it means my next card costs eight less money, and then I'm playing nitrogen
      // rich asteroid."
      playProject(IndenturedWorkers, 0)
      playProject(NitrogenRichAsteroid, 23)
    }
    // 3:15:54 PM — Green: "Okay. Now what I'm gonna do is I'm going to play Power Plant. I'm going
    // to play Which, tragically, in addition to my steel, makes me pay two money."
    // 3:16:13 PM — Green: "It gives me an energy production and gives me back the two money."
    green.turn { playProject(PowerPlant, 2, steel = 1) }
    yellow.turn {
      // 3:16:16 PM — Blue: "oh thank god um okay it's already working like um capital is By which
      // I mean I pay all twelve of my steel, because I got my cutting edge. They just got um
      // decrease energy production by two, increase money production by five. Wow, finally eight
      // thingies. Um place the tile."
      playProject(Capital, steel = 12) { placeTile(7, 3) }.expect("2 MC<Rainbow>")
    }
    rainbow.turn {
      // 3:17:57 PM — Rainbow: "To build fish and And then I'll take the action and add a animal to
      // the card"
      // 3:17:58 PM — Green: "Oh fish. Oh, you got to remove somebody's plant production. That
      // could be [Blue]'s or [Yellow]'s. [Yellow] has four. [Blue]'s two."
      // 3:18:12 PM — Rainbow: "It's gonna be [Yellow]'s I was gonna say oh I haven't I haven't
      // gotten
      // enough plants I had to s build my own greenery this whole entire day, at least I'll take
      // mine."
      playProject(Fish, 9) { doTask("PROD[-Plant<Yellow>]") }.expect("-9 MC")
      cardAction1(Fish)
      assertCounts(19 to "MC")
    }
    blue.turn {
      // 3:18:48 PM — Blue: "Uh-huh. I guess I will play water splitting plant."
      // 3:19:10 PM — Blue: "Um Mm-hmm. So yeah and that cost me twelve money."
      playProject(WaterSplittingPlant, 12)
    }
    // 3:19:13 PM — Green: "Yep. Yep. And what I'm uh gonna do Whole business network which only
    // cost me one and so I lower my mega credit production once more"
    green.turn { playProject(BusinessNetwork, 1) }
    yellow.turn {
      // 3:19:42 PM — Rainbow: "Yeah. Okay, um... I'm gonna play flooding for seven. an ocean?"
      // 3:20:22 PM — Blue: "Yes. Two money, three heat."
      // 3:20:42 PM — Blue: "However, I actually can't do that."
      // No opposing tile bordered the selected ocean.
      playProject(Flooding, 7) {
        placeTile(6, 6)
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.intentionalUnderpay()
        blue.pay(2, heat = 3)
      }
    }
    rainbow.turn {
      // 3:21:41 PM — Rainbow: "I'm taking my thermophiles action to add a microbe to sulphur eating
      // bacteria. I'm going to take my floater and find a reaction to remove two floaters and gain
      // one titanium and two money. And that's two actions."
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
      rainbow.exMachina(fakeWildTags("VenusTag"))
      stdAction("UseActionOnCardAction") {
            doTask("ActionUsedMarker<$FloatingRefinery>")
            doTask("UseAction<$FloatingRefinery, Action2>")
          }
          .expect("2 MC")
      assertCounts(21 to "MC")
    }
    // 3:22:14 PM — Blue: "Okay, um, I pass."
    // 3:22:18 PM — Green: "I'm probably going to sell a card. [Yellow]'s turn."
    // 3:22:27 PM — Blue: "Oh, hmm. I forgot about my water splitting plan actually."
    // 3:22:33 PM — Blue: "Yeah. Oh yeah, are you okay? I'll spend three energy,"
    // 3:22:35 PM — Green: "She's retroactively using three energy to raise the oxygen to five."
    // 3:22:39 PM — Blue: "raise the oxygen to five, which gives me two money and a TR."
    blue.turn {
      // Blue announced the pass first, then immediately remembered and took this still-legal action
      // before ending the turn.
      cardAction1(WaterSplittingPlant)
      autoExecNow()
    }
    // 3:22:51 PM — Green: "And I still sell a patent."
    green.turn { sellPatents(1) }
    // 3:22:53 PM — Yellow: "Okay, um... You know what? Green, I'm gonna sabotage you to lose seven
    // money."
    yellow.turn { playProject(Sabotage, 1) { doTask("-7 MC<Green>") } }
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("PlantTag"))
      // 3:23:34 PM — Rainbow: "I'm spending eleven money on cloud seating. I decrease my money
      // production one step. [Blue]'s heat production one step."
      // 3:23:48 PM — Rainbow: "I decrease your heat production one step."
      // 3:23:52 PM — Rainbow: "I'm so sad. I decrease my plant production two steps."
      playProject(CloudSeeding, 11) {
            doTask("PROD[-Heat<Blue>]")
          }
          .expect("-11 MC")
      assertCounts(10 to "MC")
    }
    // 3:24:09 PM — Yellow: "My pass."
    // 3:24:09 PM — Green: "No, you passed. That's right. Alright, um now I'm going to sell a card.
    // At least turn."
    // Blue's announced pass takes effect on this next legal Blue turn because the retroactive Water
    // Splitting action above occupied the turn in which she first announced it.
    blue.pass()
    green.turn { sellPatents(1) }
    yellow.pass()
    rainbow.turn {
      rainbow.exMachina(fakeWildTags("MicrobeTag"))
      // 3:25:18 PM — Rainbow: "So I'm using, I'm doing my sulfur eating bacteria action, turning
      // three bacteria into nine money."
      cardAction2(SulphurEatingBacteria, x = 3).expect("9 MC")
      assertCounts(19 to "MC")
    }
    // 3:25:33 PM — Green: "Am gonna play nitrophilic moss Meets the requirement, I lose two
    // plants, gain two plant production which gives me two money, and then I get a decompositor
    // which gives me one more money. Did I pay for that? I did not. I did not pay for that, which
    // is really weird. Because I exactly meant to pay for it."
    green.turn { playProject(NitrophilicMoss, 8).expect("-5 MC") }
    // The engine charged the card immediately; restore that 8 M€ to preserve the spoken missed
    // payment until Green corrects it below.
    green.exMachina("8 MC")
    green.assertCounts(11 to "MC")
    // 3:26:07–3:26:42 PM — Green audits the app: Yellow's sabotage left 7 M€, a patent sale made
    // 8, Nitrophilic Moss spent all 8, then Suitable Infrastructure and Decomposers should leave
    // 3. He enters that correction after initially forgetting the payment.
    green.exMachina("-8 MC")
    green.assertCounts(3 to "MC")
    // I accidentally gave myself 2 production instead of 2 money resources
    // But I knew what the ending money balance was supposed to be, so I got confused and ended
    // up giving the 2 money anyway, but never realized what I had done.
    green.exMachina("PROD[2 MC]")

    rainbow.turn {
      // 3:27:50 PM — Rainbow: "Um, I am. I'm gonna pay two steel acting as four money plus eleven
      // money. For a lava tube settlement, I decrease my energy production, I increase my money
      // production by two, and I place a city tile on a volcanic area."
      // The 11 M€ cash payment and the city's 4 M€ of ocean-adjacency income resolve in one task.
      playProject(LavaTubeSettlement, 11, steel = 2) { placeTile(7, 11) }.expect("-7 MC")
      assertCounts(12 to "MC")
    }
    green.turn {
      // 3:28:52 PM — Green: "Um, now I'm going to use my business network to look at a shitty
      // card."
      // 3:29:09 PM — Green: "I have no prospects of being able to meet this requirement. I do not
      // buy it. Back to you, [Rainbow]."
      cardAction1(BusinessNetwork) { buyCards(0) }.expect("0 ProjectCard")
    }
    // 3:29:23 PM — Rainbow: "I'm gonna sell two patents."
    rainbow.turn { sellPatents(2).expect("2 MC") }
    green.turn {
      // 3:29:27 PM — Green: "Okay. I'm going to be a cloud tourist. Back to you."
      cardAction1(CloudTourism)
    }
    rainbow.pass()
    green.turn {
      // 3:29:37 PM — Green: "I am going to use Icy Impactors, which is a strange card. I lose an
      // asteroid here. I get to place an ocean tile, but Rainbow gets to tell me where I have to
      // put
      // it. Which sounds weird when I put it that way. Because you're the start player. Start
      // player chooses where you must place it. I'll tell you where to place it."
      // 3:30:14 PM — Green: "Yep on one two, but that's still it's two one which still gets me a TR
      // Oceans are at nine [Blue] you have your option"
      cardAction2(IcyImpactors) {
        rainbow.doTask("OceanTile<Amazonis_02_01> BY Green")
        green.doTask("TerraformRating")
        selectTask("UseAction<Blue, NeptunianOption<Blue>>?")
        blue.narrowTask("Ok")
      }
    }
    green.pass(unused = VenusShuttles)

    // 3:30:39 PM — Green: "Um is there anything else we can do? I don't think so. I think I used
    // all my stuff. Oh, yeah, shuttles I can't afford. Gotta stop buying things I can't afford.
    // Alright, so I pass, so we hit generation plus, and then Rainbow chooses where to what kind of
    // world government operation to do."
    // 3:31:01 PM — Rainbow: "Venus it."
    // 3:31:02 PM — Green: "Venus to 10"
    rainbow.wgt("VenusStep")

    // board-15-31-15.jpg and all four app histories: Generation 7 before Research.
    with(yellow) {
      assertProduction(m = 8, s = 4, t = 0, p = 3, e = 0, h = 1)
      assertResources(m = 37, s = 4, t = 0, p = 11, e = 0, h = 9)
      assertCounts(27 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 7, s = 1, t = 1, p = 3, e = 0, h = 0)
      assertResources(m = 54, s = 1, t = 4, p = 5, e = 0, h = 2)
      assertCounts(33 to "TerraformRating", 1 to "Tactician")
      assertCardResources(
          1 to Ants,
          1 to Fish,
          1 to FakeAppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 2, e = 12, h = 6)
      assertResources(m = 41, s = 0, t = 0, p = 7, e = 12, h = 15)
      assertCounts(35 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(6 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 5, s = 1, t = 2, p = 2, e = 2, h = 1)
      assertResources(m = 35, s = 1, t = 4, p = 3, e = 2, h = 10)
      assertCounts(27 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(
          4 to Pets,
          6 to CloudTourism,
          1 to IcyImpactors,
          1 to OlympusConference,
      )
    }
    assertSidebar(gen = 7, temp = 6, oxygen = 5, oceans = 9, venus = 10)

    // 3:31:43 PM — Green: "This time we pass to the right."
    // 3:35:00 PM — Rainbow: "The thing is that... Um, I think I'll buy one card."
    // 3:35:10 PM — Green: "I will stupidly buy three cards."
    // 3:35:17 PM — Blue: "I only buy two cards."
    // 3:35:17 PM — Rainbow: "Wait, I only have three cards in my hand. Thank you."
    // 3:36:40 PM — Rainbow: "I want them both, I want everything, but I'm only keeping two."
    blue.buyCards(2)
    green.buyCards(3)
    yellow.buyCards(1)
    rainbow.buyCards(2)

    // 3:36:54 PM — Blue: "I mean I guess for my first action I can just go ahead and convert heat
    // to attempt to beat."
    // 3:37:28 PM — Blue: "I get five money."
    blue.turn { convertHeat() }
    // 3:37:59 PM — Green: "Do something basic first. I'll use factotum to spend three and get a
    // William card. Noctis city."
    green.turn { cardAction2(Factorum) }
    yellow.turn {
      // 3:38:47 PM — Blue: "Yeah Yeah get two TRs Um the oshkin will go um here for two um plantis
      // and two monies."
      // 3:38:47 PM — Green: "Temp's on plus 12 now."
      // 3:39:06 PM — Blue: "I will spend five money to increase my energy production and add a
      // Neptunian power cell scent."
      // 3:39:13 PM — Blue: "Also, [Blue], you lose three plants."
      // 3:39:21 PM — Green: "Sorry, that was on 6.5."
      convertHeat()
      playProject(Comet, 21) {
            placeTile(6, 5)
            doTask("-3 Plant<Blue>")
            blue.doTask("UseAction<NeptunianOption, Action1>")
            blue.pay(5)
          }
          .expect("-19 MC")
      // Yellow did not physically pay Comet's 21 M€ at this play. Restore the engine-enforced
      // payment until the recorded correction at 3:46:46 PM.
      exMachina("21 MC")
    }
    rainbow.turn {
      // 3:39:37 PM — Rainbow: "Okay, your turn. Oh yeah. All right, so I'm gonna, I'm gonna kind of
      // hurry here to build my Mahole Lake."
      // 3:39:54 PM — Rainbow: "you e I guess um well yes, but minus one steel."
      // 3:39:59 PM — Rainbow: "So twenty nine"
      // 3:40:15 PM — Rainbow: "Yes. I haven't given myself any T_R_ yet. So two goops um I guess.
      // hit the last temp. Okay. There you go, yeah. I guess I'll take there. Yeah, for monies and
      // a
      // card."
      // 3:40:34 PM — Green: "Five five for four money and a card."
      playProject(MoholeLake, 29, steel = 1) {
        placeTile(5, 5)
        autoExecNow()
        blue.doTask("UseAction<NeptunianOption, Action1>")
        blue.pay(5)
      }
      // 3:41:04 PM — Rainbow: "Well, my second action, I will greener eyes."
      // 3:41:14 PM — Rainbow: "I'm I'm putting it there so I get another plant and four money."
      // 3:41:24 PM — Green: "That is on 610."
      convertPlants { placeTile(6, 10) }
    }
    blue.turn {
      // 3:41:25 PM — Blue: "Yeah, well I better do this while I can. I'm gonna play Domed Crater,
      // which cost me 24 monies."
      // 3:41:36 PM — Blue: "We're domed. And so I gained three plants"
      // 3:41:49 PM — Blue: "I'm going to put it right here, which is 3-7?"
      // 3:41:58 PM — Blue: "Yeah, 3-7. And I get a plant."
      // 3:42:07 PM — Blue: "Oh yeah, and for money. I didn't even think about that. And then, oh
      // yeah, minus an energy production plus three money productions."
      playProject(DomedCrater, 24) { placeTile(3, 7) }
      // 3:42:19 PM — Blue: "And for my second action I'm going to convert my plants to a greenery."
      // 3:42:58 PM — Blue: "Three eight. Get in there. There we go. Three eight and I get two
      // plants. Okay. Oh, I should probably do... Yeah."
      convertPlants { placeTile(3, 8) }
    }
    // Blue's app has no 2 M€ Terraforming Deal response to this greenery's oxygen step:
    // entry 178 leaves 10 M€, and the 15:57 dashboard plus entry 188 production both leave
    // 54 M€. Preserve the omitted response at the action that caused it.
    blue.exMachina("-2 MC")
    green.turn {
      // 3:43:13 PM — Green: "I'm going to use my business network. No, better not."
      cardAction1(BusinessNetwork) { buyCards(0) }.expect("0 ProjectCard")
    }
    yellow.turn {
      // 3:43:48 PM — Blue: "Well, still may as well, um, plant forest."
      // 3:44:15 PM — Rainbow: "Um here for the plant and for money."
      // 3:44:20 PM — Green: "Plant and for money. Is that um eight three?"
      convertPlants { placeTile(8, 3) }
      // 3:44:36 PM — Blue: "For a card hope this one's useful."
      // 3:44:38 PM — Green: "11 11 so"
      convertPlants { placeTile(11, 11) }
    }
    rainbow.turn {
      // 3:45:19 PM — Rainbow: "Yeah, I'm gonna place my floater."
      // 3:45:23 PM — Green: "Okay, so first action to place on floating refinery."
      cardAction1(FloatingRefinery)
      rainbow.exMachina(fakeWildTags("VenusTag"))
      // 3:45:26 PM — Green: "Then second action spends it."
      // 3:45:28 PM — Rainbow: "Spending my ten."
      // 3:45:30 PM — Rainbow: "Yes, striped spherical birds and taking my floater off in some clown
      // finery."
      playProject(StratosphericBirds, 10)
    }
    // 3:45:43 PM — Blue: "I'm gonna pass."
    blue.pass(unused = WaterSplittingPlant)
    // 3:45:50 PM — Green: "Mm 'kay. [Blue] passes. I will use cloud tourism, [Yellow]."
    green.turn { cardAction1(CloudTourism) }
    yellow.turn {
      // 3:46:27 PM — Yellow: "I freaking, um, I did the comet this generation, right?"
      // 3:46:31 PM — Rainbow: "Yes, because that's why you switched one of the oceans."
      // 3:46:36 PM — Yellow: "Yeah. I forgot to pay for it."
      // 3:46:46 PM — Yellow: "So I'm doing that now."
      exMachina("-21 MC")
      // 3:47:19 PM — Yellow: "I'm regretting a little bit, yeah. But, um, oh, well, I can still,
      // um,
      // pay ten because of this for Ecological Zone."
      // 3:47:34 PM — Yellow: "Ten. Spend. I place, requires I have a greenery, which, um..."
      // 3:47:53 PM — Green: "That looks like four eight."
      // 3:48:00 PM — Green: "Four, eight, and then you get two aminals?"
      playProject(EcologicalZone, 10) { placeTile(4, 8) }
    }
    // Applied Science only accepts a card that already has a resource; the table put its science
    // onto the now-empty Sulphur-Eating Bacteria anyway. Seed and remove a witness resource so the
    // sourced illegal target can pass through the card's real action.
    // 3:48:27 PM — Rainbow: "I gotta like add some sulfur eating bacteria. Add one microbe to
    // sulfur eating bacteria."
    // 3:48:55 PM — Rainbow: "Yes. And I may need to use my thermophile section."
    // 3:49:00 PM — Rainbow: "It says I can add one microbe to any Venus card."
    // 3:49:06 PM — Rainbow: "Add it to some burning bacteria."
    rainbow.assertCardResources(0 to SulphurEatingBacteria)
    rainbow.exMachina("Microbe<$SulphurEatingBacteria>")
    rainbow.assertCardResources(1 to SulphurEatingBacteria)
    rainbow.turn {
      cardAction1(FakeAppliedScience) {
        addCardResources(SulphurEatingBacteria)
      }
      assertCardResources(2 to SulphurEatingBacteria)
      exMachina("-Microbe<$SulphurEatingBacteria>")
      assertCardResources(1 to SulphurEatingBacteria)
      cardAction1(Thermophiles) {
        addCardResources(SulphurEatingBacteria)
      }
      assertCardResources(2 to SulphurEatingBacteria)
    }
    // 3:49:07 PM — Green: "Then I should know what to do. So I'm going to play Venus Magnetizer."
    // 3:49:24 PM — Green: "It costs me seven."
    green.turn { playProject(VenusMagnetizer, 7) }
    // 3:49:37 PM — Yellow: "Well, I now believe I can pave seven cutting edge again for insects
    // since the oxygen's finally gone to six percent. Increase plant production one step for each
    // plant tag I have. One, two, three, four."
    yellow.turn { playProject(Insects, 7) }
    rainbow.turn {
      // 3:50:05 PM — Rainbow: "Um, you got some tasty microbes there, [Green]?"
      // 3:50:09 PM — Rainbow: "My ants are gonna eat them. Thank you."
      cardAction1(Ants) { doTask("-Microbe<Green, $Decomposers<Green>>") }
    }
    green.turn {
      // 3:50:22 PM — Yellow: "I think I'm gonna build Cupola City, which is, it's a city, you get
      // two more."
      // 3:50:30 PM — Green: "And I get a pet, but we should do things in order here. I should pay
      // full price. And I lose an energy production, gain three money production, which gives me
      // two money. I place a shitty tile. My first. I'm not on the board at all."
      // 3:51:10 PM — Yellow: "Six four. For two plants and four money."
      intentionalUnderpay()
      playProject(CupolaCity, 16) { placeTile(6, 4) }
      // 3:51:29 PM — Green: "Let's just go ahead and use Venus magnetizer decrease in energy
      // production to raise Venus to 12% and get a TR. All right, who's still in?"
      cardAction1(VenusMagnetizer)
    }
    yellow.turn {
      // 3:51:57 PM — Yellow: "Actually, two. And I will play Smanimals for the four money. Requires
      // 6% oxygen. Yep. Decrease any plant production one step. Man, I'm good."
      sellPatents(2)
      playProject(SmallAnimals, 4) {
        // 3:52:17 PM — Green: "Rainbow's got three, I've got two."
        // 3:52:33 PM — Yellow: "Yeah, I'll decrease Rainbow's."
        doTask("PROD[-Plant<Rainbow>]")
      }
    }
    rainbow.turn {
      // 3:52:43 PM — Rainbow: "Okay, turn. Um, well, how many space tags do you collectively have,
      // four
      // or five?"
      // 3:52:59 PM — Rainbow: "Yeah. I'm going to pay ten to build the toll station, which gives me
      // six money production."
      intentionalUnderpay()
      playProject(TollStation, 10)
    }
    // 3:53:47 PM — Green: "All right, I pass. You're still up. [Yellow], just [Yellow]?"
    green.pass(unused = setOf(VenusShuttles, IcyImpactors))
    yellow.turn {
      // 3:53:58 PM — Blue: "I'm still in. Bio, ask combustors for two steel. Wait, no, no, no, one
      // steel. Requires six percent oxygen. Decrease any plant production one step. That'll be
      // [Blue]."
      playProject(BiomassCombustors, steel = 1) { doTask("PROD[-Plant<Blue>]") }
    }
    // 3:54:43 PM — Rainbow: "That was hot. Um, I'm going to take my stratospheric's earth action to
    // add an animal to the bird card."
    rainbow.turn { cardAction1(StratosphericBirds) }
    // 3:55:00 PM — Rainbow: "Okay. Um you guys have both passed?"
    // 3:55:03 PM — Yellow: "Yeah."
    // 3:55:03 PM — Blue: "Yeah."
    yellow.pass(unused = setOf(SmallAnimals, SpaceMirrors))
    rainbow.turn {
      // 3:55:12 PM — Rainbow: "I can't believe I let Predators go. Held it for so long just in
      // case.
      // Um I'm going to take my fish action and add a fish to the card. And then for Mahomet Lake.
      // gonna add a bird to the stratospheric birds card."
      cardAction1(Fish)
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
    }
    // 3:55:35 PM — Rainbow: "Uh, I pass."
    rainbow.turn {
      // Rainbow paid 8 M€ for Miranda Resort without titanium. The table counted her wild tag as
      // Earth while resolving the card, then corrected the resulting production increase to two.
      intentionalUnderpay()
      rainbow.exMachina(fakeWildTags("EarthTag"))
      playProject(MirandaResort, 8)
    }
    // 3:57:14 PM — Rainbow: "Alright, I'm also going to pass."
    rainbow.pass(unused = SulphurEatingBacteria)

    // 3:57:20 PM — Green: "Okay, so we hit plus generation, and then [Blue] chooses about the world
    // governing."
    // 3:57:23 PM — Blue: "Mm-hmm. World government. Um I guess I'm gonna do Venus."
    // 3:57:30 PM — Green: "Venus to fourteen."
    blue.wgt("VenusStep")

    // board-15-57-46.jpg and all four app histories: Generation 8 before Research.
    with(yellow) {
      assertProduction(m = 8, s = 4, t = 0, p = 7, e = 2, h = 1)
      assertResources(m = 40, s = 7, t = 0, p = 8, e = 2, h = 2)
      assertCounts(32 to "TerraformRating")
      assertCardResources(3 to EcologicalZone)
    }
    with(rainbow) {
      assertProduction(m = 15, s = 1, t = 1, p = 2, e = 0, h = 0)
      assertResources(m = 54, s = 1, t = 5, p = 3, e = 0, h = 2)
      assertCounts(36 to "TerraformRating", 1 to "Tactician")
      assertCardResources(
          2 to Ants,
          2 to Fish,
          2 to StratosphericBirds,
          2 to SulphurEatingBacteria,
      )
    }
    with(blue) {
      assertProduction(m = 7, s = 0, t = 0, p = 1, e = 13, h = 6)
      assertResources(m = 54, s = 0, t = 0, p = 3, e = 13, h = 25)
      assertCounts(37 to "TerraformRating", 1 to "Landshaper")
      assertCardResources(8 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 8, s = 1, t = 2, p = 2, e = 0, h = 1)
      assertResources(m = 42, s = 2, t = 6, p = 7, e = 0, h = 13)
      assertCounts(28 to "TerraformRating", 1 to "Diversifier")
      assertCardResources(6 to Pets, 7 to CloudTourism, 1 to IcyImpactors)
    }
    assertSidebar(gen = 8, temp = 14, oxygen = 9, oceans = 11, venus = 14)

    // The table broke here on 2026-09-05 and resumed on 2026-09-09 at 5:25 PM, opening with two
    // corrections Green had worked out in between.
    // 5:26:07 PM — Green: "Are you questioning the god of game corrections? Okay, so I have taken
    // away two of my money correction and four of my money. Hopefully that was the right thing to
    // do."
    green.exMachina("PROD[-2 MC], -4 MC")
    // 5:26:20 PM — Rainbow: "So, um, I certainly used the... Applied science thing for three money.
    // Should I take away three money? I mean it might have been okay"
    // 5:26:30 PM — Green: "Let's just let it stand."
    // The table explicitly chose not to correct either state here.
    rainbow.assertCounts(54 to "MC", 0 to "Science<$FakeAppliedScience>")

    // 5:31:27 PM — Yellow: "I think I'm gonna buy two cards. My butt hurts. Go up."
    // 5:31:46 PM — Blue: "by one card."
    // 5:32:08 PM — Green: "Okay. Buying three cards."
    // 5:32:36 PM — Rainbow: "Your turn. Oh, wait, I need to I need to actually buy two cards so
    // that A_I_ knows that I need to buy two. Yeah. Um I have bought two cards. It is also stupid
    // of
    // me to buy two, but whatever."
    // Research is submitted below in workflow order; the quotes remain in recording order.
    green.buyCards(3)
    yellow.buyCards(2)
    rainbow.buyCards(2)
    blue.buyCards(1)

    // Generation 8 did not follow strict physical rotation: Blue rescinded a pass and Rainbow
    // continued after announcing one. The following legal interleave preserves every player's own
    // action order; neither premature pass remained effective.
    green.turn {
      // 5:33:24 PM — Green: "I'm actually going to play tardiggradees."
      // 5:33:55 PM — Green: "I do not have discounts, so it cost me four entire money.
      // Decomposers gets a microbe from Rainbow and that gives me a money."
      // 5:34:11 PM — Green: "tardigrades? That didn't do what I thought it was going to do. I keep
      // thinking that I had viral enhancers."
      playProject(Tardigrades, 4).expect("Microbe, -3 MC")
      // 5:34:30 PM — Green: "Uh okay, that turned out to be pointless. But now I'm going to use
      // business network to look at this card."
      // 5:34:46 PM — Green: "God, it's so expensive. I think not."
      cardAction1(BusinessNetwork) { buyCards(0) }.expect("0 ProjectCard")
    }
    yellow.turn {
      // 5:35:06 PM — Yellow: "I'm gonna pay 14 to fund the Highlander. That's the most tiles not
      // next to oceans, which I got. Yes, dry boys."
      fundAward(cn("Highlander"), 14)
      // 5:35:21 PM — Yellow: " And actually, just in case anyone has any ideas, I'm gonna plant
      // forests. Yeah. Um I'll go here for a plant."
      // 5:35:39 PM — Green: "Seven two."
      convertPlants { placeTile(7, 2) }
    }
    rainbow.turn {
      // 5:35:54 PM — Rainbow: "Got actions to take. I'm going to add one microbe to self-reeding
      // bacteria. Uh"
      cardAction1(SulphurEatingBacteria)
      // 5:36:14 PM — Rainbow: "Oh, you did it at the oxygen's ten. Okay, thank you. Um also I'm
      // going to take my thermophiles action to add one microbe to sulfur eating bacteria."
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      // 5:36:37 PM — Blue: "Okay. I really hate that this is gonna cost me so much money, but I'm
      // gonna spend twenty bucks to fund Thermalist."
      fundAward(cn("Thermalist"), 20)

      // 5:36:58 PM — Blue: "And I'm going to um Spend 15 bucks on a plantation."
      // 5:37:10 PM — Blue: "Which I'm gonna put If I go here, I get a resource, right?"
      // 5:37:16 PM — Green: "A standard resource."
      // 5:37:48 PM — Green: "Alright, so you went to five nine."
      // 5:37:58 PM — Blue: "Oxygen is now at eleven, I gain a TR and for gaining a TR I get
      // two money. And for placing next to an ocean I get two money."
      // 5:38:08 PM — Blue: "And I decided that my resource was gonna be a titanium."
      playProject(Plantation, 15) {
        placeTile(5, 9)
        doTask("Titanium")
      }
    }

    green.turn {
      // 5:38:13 PM — Green: "Okay. Very cool. I will take my factotum action."
      // 5:38:18 PM — Green: "I have no energy resources so I can get an energy production and that
      // gives me two money from sweetable infrastructure."
      cardAction1(Factorum).expect("PROD[Energy], 2 MC")
    }
    yellow.turn {
      // 5:38:22 PM — Yellow: "Oh, okay. I will sponsor some Academy."
      // 5:38:29 PM — Yellow: "Yeah. Hope it doesn't give you useful stuff. Pay nine. I pitch a
      // card."
      // 5:38:36 PM — Green: "You get three, you get one, you get one, I get one."
      playProject(SponsoredAcademies, 9)
          .expect(
              "-9 MC, ProjectCard<Yellow>, ProjectCard<Blue>, ProjectCard<Green>, " +
                  "ProjectCard<Rainbow>"
          )
    }
    rainbow.turn {
      // 5:39:42 PM — Rainbow: "I'm gonna do gene repair which costs me 12 monies"
      // 5:40:23 PM — Rainbow: "Uh but gives me too money production"
      playProject(GeneRepair, 12).expect("-12 MC, PROD[2 MC]")
      // 5:40:40 PM — Rainbow: "Oh, but [Yellow] doesn't have microbes, right. Well, I'm gonna take
      // your tasty microbe then, please. My ants are going to remove a microbe from your whatever."
      cardAction1(Ants) { doTask("-Microbe<$Decomposers<Green>>") }
    }
    blue.turn {
      // 5:42:17 PM — Blue: "I am playing Soletta,
      // yeah, so that's gonna cost me So one titanium, twenty mega credits and eleven heat. But I
      // get seven heat production. Noice. Okay."
      playProject(Soletta, 20, titanium = 1, heat = 11)
    }

    green.turn {
      // 5:43:41 PM — Green: "Um, Venus magnetizer."
      // 5:43:44 PM — Green: "Reduce energy production."
      // 5:43:46 PM — Blue: "It is at sixteen percent."
      // 5:43:47 PM — Green: "Raise Venus to sixteen and get nothing."
      cardAction1(VenusMagnetizer)
    }
    yellow.turn {
      // 5:43:49 PM — Yellow: "Go up. All right, um... Business contactos for... Seven. Crap. I got
      // a bunch of junk. Alright, actually these might"
      // The transcript diarizes this as Blue, but Yellow is the player due here; Business Contacts
      // is also the missing fifth Yellow event in the table's 6:00:28 PM Media Archives tally.
      // Yellow's app combines this 7 M€ payment and Sponsored Academies' 9 M€ payment into its
      // single entry 209 debit of 16 M€.
      playProject(BusinessContacts, 7).expect("-7 MC, ProjectCard, PlayedEvent")
    }
    rainbow.turn {
      // Rainbow paid 7 M€ for Venus Waystation and 14 M€ for Maxwell Base. Both debits reached her
      // physical/app balance before she found Maxwell's missing energy-production prerequisite.
      assertCounts(36 to "MC")
      exMachina("-21 MC")
      assertCounts(15 to "MC")
      // She canceled both plays and restored the full 21 M€, having spent no titanium.
      exMachina("21 MC")
      assertCounts(36 to "MC")
      // 5:45:53 PM — Rainbow: "So first I'm going to take the, it's 11 standard action."
      // 5:45:58 PM — Yellow: "If you want a standard project, tap your money, your 36 money. You
      // don't tap the production. Okay, now there you go. Okay, okay, you did it."
      stdProject("PowerPlantProject")
      // After buying energy production, Rainbow legally played Maxwell Base for 16 M€, reduced
      // that production, and placed its city; the city also triggered Green's Pets.
      playProject(MaxwellBase, 16)
          .expect("-14 MC, PROD[-Energy], CityTile, Animal<Green, $Pets<Green>>")
    }
    blue.turn {
      // 5:47:44 PM — Green: "You don't want to raise the oxygen?"
      // 5:47:47 PM — Blue: "But then the game would end."
      // 5:47:49 PM — Green: "Not for another seven things, but you get a T.R. You don't want a
      // T.R.? Do"
      // 5:48:09 PM — Blue: "I will take my water splitting plant action."
      // 5:48:13 PM — Blue: "I'll take my water splitting plant action, spend three energy to
      // raise oxygen one step."
      // 5:48:19 PM — Blue: "And I gain a TR and I gain two money."
      cardAction1(WaterSplittingPlant)
    }

    green.turn {
      // 5:48:23 PM — Green: "I play foosion power for four worth of steel and ten real and"
      // 5:48:33 PM — Green: "one two three energy production which gives me two money and a science
      // tag removes my only science resource from Olympus conference and gives me this guy it's
      // actually reasonable That's actually useful in this circumstance, I don't believe it."
      playProject(FusionPower, 10, steel = 2) {
        doTask("ProjectCard FROM Science<$OlympusConference>")
      }
    }
    yellow.turn {
      // 5:49:01 PM — Yellow: "Wait. Actually, remembering myself, I, um, oh, I shouldn't forget to
      // use smanimals."
      cardAction1(SmallAnimals)
    }
    rainbow.turn {
      // 5:50:11 PM — Rainbow: "So Venus way station should be nine, but I have a
      // minus two for and a minus two for a thing, so it's five."
      intentionalUnderpay()
      playProject(VenusWaystation, 5)
      // 5:51:04 PM — Rainbow: "I'm gonna build local shading for free."
      playProject(LocalShading, 0)
    }
    blue.turn {
      // 5:51:29 PM — Blue: "No, I don't pass. I play project inspection. Use a card action that
      // has already been used this generation. I use my water splitting plant action again. Uh And
      // then I spend three more energy, raise the oxygen one more step, and I gain one more T_R_
      // and
      // two more money."
      playProject(ProjectInspection, 0) {
        doTask("UseAction<$WaterSplittingPlant, Action1>")
        pay(energy = 3)
      }
    }

    green.turn {
      // 5:51:51 PM — Green: "What I'm a gonna do is play directed heat usage, which
      // costs me one"
      playProject(DirectedHeatUsage, 1)
    }
    yellow.turn {
      // 5:53:57 PM — Yellow: "Dos patentes."
      // 5:54:03 PM — Yellow: "So, dos patentes. Um, beep."
      sellPatents(2).expect("-2 ProjectCard, 2 MC")
      // 5:54:07 PM — Yellow: "And play for six, um, Lightning Harvest."
      // 5:54:15 PM — Yellow: "I, um, gain energy product, gain money product. Your turn, Rainbow."
      playProject(LightningHarvest, 6).expect("-6 MC, PROD[Energy], PROD[MC]")
    }
    rainbow.turn {
      // 5:54:29 PM — Rainbow: "Floating refinery action, I'm putting a floater on the card."
      // 5:54:37 PM — Rainbow: "Local shading action, I am putting a floater on that card."
      cardAction1(FloatingRefinery)
      cardAction1(LocalShading)
    }
    // Blue takes no further sourced action this generation.
    blue.pass()

    green.turn {
      // 5:54:45 PM — Green: "Okay. Um So now what I'm going to do is use directed heat usage to
      // spend three useless heat and get two useful plants and then plant forest which raises
      // oxygen to 14 and it's going to go At five for plant and for money. Those are my two
      // actions."
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }
      convertPlants { placeTile(5, 4) }
    }
    // 5:55:38 PM — Yellow: "Uh-huh. Um, I was gonna pass."
    yellow.pass(unused = setOf(SpaceMirrors))
    rainbow.turn {
      // 5:55:50 PM — Rainbow: "I got actions. I'm going to take my Maxwell base action, add one
      // resource to another Venus card. And I guess I'm going to do that to the floating refinery."
      cardAction1(MaxwellBase) { addCardResources(FloatingRefinery) }
          .expect("Floater<$FloatingRefinery>")
      // 5:56:07 PM — Rainbow: "mm-hmm and then add a bird to Strato birds"
      cardAction1(StratosphericBirds)
    }

    green.turn {
      // 5:56:18 PM — Green: "I will add a floater to Cloud Tourism."
      cardAction1(CloudTourism)
    }
    rainbow.turn {
      // 5:56:27 PM — Rainbow: "back to me because it passed yeah Okay, and we'll add a fish to fish
      // and I will take the action from mohole lake to add another fish again."
      cardAction1(Fish)
      cardAction1(MoholeLake) { addCardResources(Fish) }
    }

    green.turn {
      // 5:56:54 PM — Green: "I will use tardigrades to put a tardigrade on to a ridic dicrid's and
      // I get the money, back to you."
      cardAction1(Tardigrades).expect("MC")
    }
    rainbow.turn {
      // 5:57:11 PM — Rainbow: "Fine, I'm going to pay my last six money for greenhouses. Oh wait, I
      // have no."
      // 5:57:25 PM — Blue: "You have a steal if you want to use it."
      // 5:57:28 PM — Rainbow: "Thank you, you want to use it. So giving myself two money Yeah. and
      // taking away one steal. Okay, thank you. One plant for each city tile in play. So I believe
      // that is one, two, three, four, five, six, seven."
      playProject(Greenhouses, 4, steel = 1).expect("7 Plant")
      // 5:57:51 PM — Rainbow: "That was my first thing this time, right? And so I'm going to do a
      // plant forest action."
      // 5:57:57 PM — Green: "oxygen to 15."
      // 5:57:58 PM — Rainbow: "Yeah. Oops. And where's my forest going to go? I guess right here
      // between my city and my other forest."
      convertPlants { placeTile(7, 10) }
    }

    // 5:58:11 PM — Green: "So one plant Seven, ten. All right. gonna. How many use Venus shuttles,
    // which I've like never done yet. How many Venus tags do I have? One, two, three, four, five,
    // six. Venus shuttles says I pay only six to raise Venus."
    green.turn {
      // 5:59:05 PM — Blue: "All right. To 18%."
      cardAction1(VenusShuttles).expect("TerraformRating")
    }
    // 5:59:10 PM — Green: "Okay, and that gives me a turn. Back to you, [Rainbow]. Let's play water
    // to Venus for three titanium, no discounts. Raise Venus to 20%, get a TR. I'm going to play
    // air
    // scrapping exhibition. For thirteen money. No discounts."
    rainbow.pass(unused = FakeAppliedScience)

    green.turn {
      playProject(WaterToVenus, titanium = 3)
      // 6:00:11 PM — Green: "I get three cloud tours."
      // 6:00:18 PM — Green: "Uh and I raise Venus to twenty two which gives me two TR."
      playProject(AirScrappingExpedition, 13) { addCardResources(CloudTourism, 3) }
    }
    green.turn {
      // 6:00:28 PM — Green: "I flip these. I play media archives. It costs me five money. How many
      // event cards has everybody played? Two?"
      // 6:00:37 PM — Yellow: "Uh five."
      // 6:00:38 PM — Blue: "Five."
      // 6:00:39 PM — Green: "And five is seven and five is twelve and four is sixteen."
      // Business Contacts establishes the previously missing fifth Yellow event. The table's
      // spoken 2 + 5 + 5 + 4 tally is therefore exact, and Media Archives pays for sixteen events
      // without an adjustment.
      playProject(MediaArchives, 5).expect("11 MC, -ProjectCard")
      assertCounts(5 to "PlayedEvent<Yellow>")

      pass(unused = IcyImpactors)
    }

    // 6:01:45 PM — Green: "all right I pass it's done yeah I'm sad we hit generation nine I choose
    // what to world government and I'll just um I'm gonna be the last to play and at least echo
    // line I was gonna move the oxygen oxygen at 16 start player marker moves"
    green.wgt("OxygenStep")

    // board-0909-18-03-31.jpg and all four app histories: Generation 9 before Research.
    with(yellow) {
      assertProduction(m = 9, s = 4, t = 0, p = 7, e = 3, h = 1)
      assertResources(m = 42, s = 11, t = 0, p = 9, e = 3, h = 5)
      assertCounts(33 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 17, s = 1, t = 1, p = 2, e = 0, h = 0)
      assertResources(m = 56, s = 1, t = 6, p = 5, e = 0, h = 2)
      assertCounts(37 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 7, s = 0, t = 0, p = 1, e = 13, h = 13)
      assertResources(m = 51, s = 0, t = 0, p = 4, e = 13, h = 34)
      assertCounts(40 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 6, s = 1, t = 2, p = 2, e = 3, h = 1)
      assertResources(m = 56, s = 1, t = 5, p = 4, e = 3, h = 11)
      assertCounts(34 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 14, oxygen = 16, oceans = 11, venus = 22)

    // Rainbow's app entry 223 removes one TR immediately before Research. The original recording
    // does not explain the correction, but every later app balance retains it; entry 223 is both
    // the earliest and latest source-compatible position, and a deliberate app correction is the
    // likeliest cause.
    rainbow.exMachina("-TerraformRating")
    rainbow.assertCounts(36 to "TerraformRating")

    // 6:06:19 PM — Blue: "Pigeon, I'm I'm buying two cards. I'm by one card."
    // 6:06:32 PM — Green: "I would buy one card."
    // All four app histories supply the otherwise unspoken Yellow and Rainbow counts. Research is
    // submitted in workflow order.
    yellow.buyCards(2)
    rainbow.buyCards(2)
    blue.buyCards(1)
    green.buyCards(1)

    yellow.turn {
      // 6:08:20 PM — Rainbow: "Right what I'm gonna do is plant forest"
      // 6:08:30 PM — Green: "yeah it's just like it means they're about to lay down like eight
      // cities and end the game right just like i gotta get here and do my plan i have all go this
      // shit in my head all right that looks like nine nine you placed a plant you planted a forest
      // on nine nine"
      convertPlants { placeTile(9, 9) }
      // 6:08:34 PM — Blue: "You think I'm gonna, um, boop boop, or, um, You here for, um, two
      // apartments. Yeah, um, and Brooklyn nine nine. Papers, yeah, um, could actually go ahead in
      // a
      // city actually."
      // 6:08:59 PM — Green: "Okay, on six nine."
      stdProject("CityProject") { placeTile(6, 9) }
    }
    rainbow.turn {
      // 6:09:38 PM — Rainbow: "You need other fish. Okay. Exactly. Um okay, so I am going to take
      // my
      // applied science action. I have one more science resource."
      // 6:10:09 PM — Rainbow: "Put that thing onto flitter and then fire"
      // Applied Science has no reload trigger. Its six sourced uses should have exhausted it at
      // 3:48:27 PM, yet this recording explicitly establishes one remaining science resource. No
      // source identifies where the extra resource arose, so 6:09:38 PM is its latest possible
      // insertion point.
      assertCounts(0 to "Science<$FakeAppliedScience>")
      exMachina("Science<$FakeAppliedScience>")
      assertCounts(1 to "Science<$FakeAppliedScience>")
      cardAction1(FakeAppliedScience) { addCardResources(FloatingRefinery) }
      // 6:10:48 PM — Rainbow: "Okay, I'm just gonna do this so that I don't suggest keep doing the
      // math over and over again. I need to pay for an interstellar colony ship."
      // 6:11:00 PM — Rainbow: "It costs 20. Yeah, that's what I'm gonna do. Oops, not eighteen
      // minus
      // six minus titanium and money."
      rainbow.exMachina(fakeWildTags("ScienceTag"))
      playProject(InterstellarColonyShip, 2, titanium = 6).expect("MC")
      // The app omits Media Group's 3 M€ response to this event.
      rainbow.exMachina("-3 MC")
    }
    blue.turn {
      // 6:11:32 PM — Rainbow: "Flip it. Rip it, rip it down. Okay, so that's two actions that I am
      // finished. Okay. I am going to take the greenery standard project action to place greenery
      // right here at"
      // 6:11:57 PM — Blue: "Seven? At four seven."
      // 6:12:02 PM — Blue: "And I get two plants, two money, I raise the oxygen, it's max now,
      // and
      // I get a TR and I get two money."
      stdProject("GreeneryProject") { placeTile(4, 7) }
    }
    green.turn {
      // 6:12:28 PM — Green: "I'm going to use my factotum to spend three money and get a building
      // card space"
      // 6:12:47 PM — Green: "elevator. Wow. Not super useful."
      cardAction2(Factorum)
    }

    yellow.turn {
      // 6:12:54 PM — Blue: "What is it, um, right. I'm gonna pay a seven seals for carbon nano
      // systems."
      playProject(CarbonNanosystems, steel = 7)
    }
    rainbow.turn {
      // 6:13:11 PM — Rainbow: "going to take my floating refinery action to turn these three cubes
      // why two that"
      // 6:13:16 PM — Green: "Two cubes. Says you take two floaters off."
      // 6:13:23 PM — Rainbow: "was dumb of me I could have put my applied science somewhere super
      // better all right fine two off for one titanium and two money D aww, yeah,"
      cardAction2(FloatingRefinery) { doTask("-2 Floater<$FloatingRefinery>!") }
      // The app records a 3 M€ gain for the card's 2 M€ action.
      rainbow.exMachina("MC")
      // 6:14:06 PM — Rainbow: "I'm gonna take my thermal viol back, that too out a microbe to any
      // Venus card, and then I will add it to sulfur eating bacteria."
      cardAction1(Thermophiles) { addCardResources(SulphurEatingBacteria) }
    }
    blue.turn {
      // 9:15:36 PM — Blue: "I'm gonna plant a city. And I'm gonna"
      // 9:15:55 PM — Yellow: "Four six."
      // 9:15:55 PM — Rainbow: "Yeah. Four six. And I get one plant and one two money and my cube
      // goes here."
      stdProject("CityProject") { placeTile(4, 6) }.expect("-23 MC, Plant, CityTile, 2 MC<Rainbow>")
    }
    // Rainbow's app omits this Rover Construction response even though the recording establishes
    // that she took it. The replay keeps the physical 2 M€; consequently Rainbow's M€ states from
    // app entry 234 through final production remain 2 M€ above the app-only balances.
    green.turn {
      // 9:16:45 PM — Green: "Okay. I am going to use the Venus Magnetizer, lose an energy
      // production, raise Venus to 24% and get a TR. [Yellow]'s turn."
      cardAction1(VenusMagnetizer)
    }

    yellow.turn {
      // 9:17:07 PM — Yellow: "Muniversity. I pay four steels. I pitch and draw."
      // 9:17:24 PM — Yellow: "Okay. And I add a graphene."
      // The app never logs this four-steel payment. It therefore shows 4 steel both before this
      // play and after production; the replay additionally visits the evidenced intervening zero.
      playProject(MarsUniversity, steel = 4) { doTask("-ProjectCard") }
          .expect("-4 Steel, Graphene<$CarbonNanosystems>")
    }
    rainbow.turn {
      // 9:17:57 PM — Rainbow: "Okay. So I had a lot of actions to take. Um, fine, I'm gonna pay
      // sixteen, for farming, which increases my money production and plant production,
      // which is not that important now, but it also gives me two more plants, and two points, and
      // um, I guess I will also..."
      playProject(Farming, 16).expect("-16 MC")
      // App entries 233-234 move from 53 to 38 despite recording +3 and -16.
      rainbow.exMachina("MC")
      // 9:18:58 PM — Rainbow: "Green, do you have microbes?"
      // 9:19:12 PM — Green: "I do."
      // 9:19:13 PM — Rainbow: "You got a tardy grade. I'm going to use my ant's action to take one
      // of your microbes. Thank you."
      // 9:19:20 PM — Green: "You took my tardigrade. I have no microbes left."
      cardAction1(Ants) { doTask("-Microbe<$Tardigrades<Green>>") }.expect("0 MC")
    }
    blue.turn {
      // 9:19:42 PM — Blue: "I'm probably gonna just go on ahead and take another greenery standard
      // project with all my heat that I have to spend."
      // 9:20:12 PM — Blue: "So that I get three five."
      // 9:20:14 PM — Blue: "I get one plant and two money."
      // The sourced transactions since the 51 M€ dashboard leave 6 M€, but Blue's app debits
      // 9 M€ and 14 heat here. Supply the three-M€ overdraft so the real payment can run.
      blue.exMachina("3 MC")
      stdProject("GreeneryProject", payment = { pay(9, heat = 14) }) { placeTile(3, 5) }
    }

    green.turn {
      // 9:20:25 PM — Green: "Who is this network?"
      // 9:20:27 PM — Blue: "business"
      // 9:20:29 PM — Green: "I'm your business that is pointless at least industrial"
      cardAction1(BusinessNetwork) { buyCards(0) }.expect("0 ProjectCard")
    }
    yellow.turn {
      // 9:20:40 PM — Yellow: "Got trans Neptune probe. I'm gonna pay a carbon nano and two monies
      // for it"
      // 9:20:50 PM — Yellow: "I pay for it because it has space tag and then science tag adds
      // carbon nano back oh right and I use University pitch and drop"
      playProject(TransNeptuneProbe, 2) {
            doTask("PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
            doTask("-ProjectCard")
          }
          .expect("-2 MC")
      // Yellow's app posts -3 M€ for this otherwise fully narrated 2 M€ payment.
      exMachina("-MC")
    }
    rainbow.turn {
      // 9:21:01 PM — Rainbow: "Okay. that sucks uh okay so would I rather get three more monies or
      // an entire point I think I would rather get an entire point"
      // 9:21:58 PM — Rainbow: "I don't care about raising my money production then that's gonna be
      // birds"
      // 9:22:18 PM — Rainbow: "I did. That's with Maxwell Bank."
      cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) }
          .expect("Animal<$StratosphericBirds>")
      // 9:22:30 PM — Green: "Okay, you undid Moholy I-Rey Okay."
      // 9:22:33 PM — Rainbow: "unpension. did the Holy Lake action. All right, let's do my sulfur
      // eating bacteria action. I currently have one, two, three, four, five microbes, and I'm
      // going to turn them into fifteen money."
      cardAction2(SulphurEatingBacteria, x = 5).expect("15 MC")
    }
    blue.turn {
      // 9:23:07 PM — Blue: "I will convert my eight plants into a greenery. Bam. Greenery and it's
      // going here."
      // 9:23:17 PM — Green: "Mm, five, seven."
      // 9:23:18 PM — Blue: "Excuse me. Two plants. At five-seven."
      // Blue first chose 5-7, then moved the greenery to 5-6 before the next operation. The engine
      // cannot relocate a tile, so place it at its sourced final coordinate.
      convertPlants { placeTile(5, 6) }
    }
    green.turn {
      // 9:23:28 PM — Green: "All right, I will use tartigrades to put a tartigrade on tartigrades
      // and give a money."
      cardAction1(Tardigrades).expect("MC, Microbe<$Tardigrades>")
    }
    // The table corrected Blue's greenery to 5-6 for 4 M€ ocean adjacency. Blue had not previously
    // taken that money; her app also removed one of the space's two plant bonuses.
    blue.exMachina("-Plant")
    yellow.turn {
      // 9:24:40 PM — Yellow: "Right. I am Ada Smanimal. Your turn."
      cardAction1(SmallAnimals).expect("Animal<$SmallAnimals>")
    }
    rainbow.turn {
      // 9:25:56 PM — Rainbow: "All right, I so I'm gonna do a city."
      // 9:26:00 PM — Rainbow: "City, you know, project. Which means that you get a pet."
      // 9:26:17 PM — Rainbow: "No. Okay, my city is at six seven."
      // 9:26:20 PM — Rainbow: "You get to pay, I get two money for the city, and I get two money
      // because
      // it's next to an ocean, and I get two plantos and a steel, and"
      // The 25 M€ project, 2 M€ ocean adjacency, and 2 M€ Rover Construction response are
      // one engine task.
      stdProject("CityProject") { placeTile(6, 7) }.expect("-21 MC")
      // 9:26:36 PM — Rainbow: "Yay. Um, hey, yay. And then I'm gonna do a plant, plant forest
      // action."
      // 9:28:13 PM — Green: "Two energies, two money for going on seven."
      // 9:28:17 PM — Rainbow: "Yeah, with a forest."
      // 9:28:20 PM — Rainbow: "Alright, that is two actions, so that is the end of my turn."
      convertPlants { placeTile(7, 7) }
    }
    blue.turn {
      // 9:28:22 PM — Blue: "Okay. I'm gonna do another greenery standard project."
      // 9:28:27 PM — Green: "that's a lot of heat and And she still gets thermalist guaranteed so
      // that's on four five for two money and and whatever And one plant."
      stdProject("GreeneryProject", payment = { pay(6, heat = 17) }) {
        placeTile(4, 5)
      }
      // The app omits the 2 M€ ocean-adjacency income from this final placement.
      blue.exMachina("-2 MC")
    }
    green.turn {
      // 9:29:05 PM — Green: "Let's just play earth catapult first, that costs 20"
      playProject(EarthCatapult, 20)
      // "Then noxious city is only 16 -- two steel and 14 real [sic] I lose an energy production I
      // get three money production. You get two money."
      // Rainbow: "I get two money you get a plant?"
      // 9:29:40 PM — Green: "I place a shitty tile down here at nine eight and I get my gold pet"
      playProject(NoctisCity, 14, steel = 1) { placeTile(9, 8) }
          .expect("-12 MC, PROD[3 MC], Animal<$Pets>, 2 MC<Rainbow>")
    }
    // The -12 M€ net above is the narrated -14 M€ payment plus Green's real +2 M€ Suitable
    // Infrastructure response to the production increase. Do not erase that income.

    yellow.turn {
      // 9:30:05 PM — Yellow: "I pay 1 for CEO's favorite project and man these are both one away
      // from getting a point. Um, I add to EcoZone."
      playProject(CeosFavoriteProject, 1) { addCardResources(EcologicalZone) }
          .expect("Animal<$EcologicalZone>")
    }
    rainbow.turn {
      // 9:31:04 PM — Rainbow: "Okay, I'm gonna go ahead and sell three patents."
      // Together with Rover Construction's 2 M€ response to Green's city, this is the app's
      // five-M€ increase. No income is missing.
      sellPatents(3).expect("-3 ProjectCard, 3 MC")
    }

    // 9:31:07 PM — Blue: "Pass."
    blue.pass(unused = WaterSplittingPlant)

    green.turn {
      // 9:31:07 PM — Green: "I will... Use cloud tourism to add a floater to cloud tourism."
      cardAction1(CloudTourism).expect("Floater<$CloudTourism>")
    }

    yellow.turn {
      // 9:31:28 PM — Yellow: "Sell patent."
      sellPatents(1).expect("-ProjectCard, MC")
    }

    rainbow.turn {
      // 9:31:57 PM — Rainbow: "Adding a fish to my fish card."
      cardAction1(Fish).expect("Animal<$Fish>")
      // 9:32:00 PM — Rainbow: "And birds to my stratospheric birds"
      // Yellow: "Birds fall from the window ledge above mine."
      cardAction1(StratosphericBirds).expect("Animal<$StratosphericBirds>")
    }
    green.turn {
      // 9:32:09 PM — Green: "I am going to use Venus Shootles and I probably still have six Venus
      // tags so that means I pay six for a TR and Venus is now on 26"
      cardAction1(VenusShuttles).expect("-6 MC, VenusStep, TerraformRating")
    }

    yellow.turn {
      // 9:32:30 PM — Yellow: "sell patent."
      sellPatents(1).expect("-ProjectCard, MC")
    }

    rainbow.turn {
      // 9:32:37 PM — Rainbow: "I'mna use Mohole Lake to put a stratospheric bird."
      cardAction1(MoholeLake) { addCardResources(StratosphericBirds) }
          .expect("Animal<$StratosphericBirds>")
      // 9:32:51 PM — Rainbow: "And I might as well do the same with Maxwell Base. Another bird on
      // the
      // stratospheric birds."
      // 9:32:57 PM — Yellow: "Base bird."
      assertCardResources(6 to StratosphericBirds)
      // Rainbow physically used Maxwell Base at both 9:21:58 and 9:32:51 in the same generation.
      // The first use consumed its action, so record the directly evidenced illegal second bird.
      exMachina("Animal<$StratosphericBirds>")
      assertCardResources(7 to StratosphericBirds)
    }

    green.turn {
      // 9:33:01 PM — Green: "I will direct some heat usage, except I keep grabbing delegates which
      // is very useful. I will spend three heat to gain two planta. Yellow?"
      cardAction1(DirectedHeatUsage) { doTask("2 Plant") }.expect("-3 Heat, 2 Plant")
    }

    yellow.turn {
      // 9:33:10 PM — Yellow: "Oh, right. I now spend um two carbon nanos and eight real on imported
      // hydrogen, um you may be thinking what's the point there's no um what's its face, uh ocean
      // to place but I can gain three plants which lets me place a plant forest. I'll put it on
      // six, umm, whatever this is."
      // 9:34:18 PM — Green: "Exciting. On six two."
      // Carbon Nanosystems accepts both graphene toward this one space project, for 4 M€ each.
      assertCardResources(2 to CarbonNanosystems)
      playProject(
              ImportedHydrogen,
              payment = {
                doTask("2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>")
                assertCardResources(0 to CarbonNanosystems)
                pay(8)
              },
          ) {
            doTask("3 Plant")
          }
          .expect("-2 Graphene<$CarbonNanosystems>, -8 MC, 3 Plant")
      // Conversely, Yellow's app posts -6 M€ for the narrated 8 M€ cash payment.
      exMachina("2 MC")
      convertPlants { placeTile(6, 2) }
    }

    rainbow.turn {
      // 9:34:35 PM — Rainbow: "What can I do with my fourteen money? Ah, fine. Um Well, I'm gonna
      // go ahead and add one floater to local shading."
      cardAction1(LocalShading).expect("Floater<$LocalShading>")
    }

    green.turn {
      // 9:34:52 PM — Green: "Um I will do something I should have done a long time ago and have a
      // stratospheric expedition which costs me only ten but really I'm going to spend four
      // titanium anyway and that lets me put two floatbois on cloud tourism and draw two Venus
      // cards."
      playProject(StratosphericExpedition, titanium = 4) {
        addCardResources(CloudTourism, 2)
      }
    }

    // 9:35:59 PM — Yellow: "I pass."
    yellow.pass(unused = SpaceMirrors)
    // Yellow's app has no entries for CEO's Favorite or either late patent sale. Those three
    // directly evidenced actions net +1 M€, but the next app balance remains 2 M€ before the
    // 44 M€ production credit. Reconcile that recorded one-M€ omission at Yellow's final action.
    yellow.assertCounts(3 to "MC")
    yellow.exMachina("-MC")
    yellow.assertCounts(2 to "MC")

    // Rainbow app entry 249 is 39 M€ immediately before the mistakenly logged second City
    // purchase. The replay is at 41 M€ because it retains the recorded Rover Construction income
    // that the app omitted after Blue's city.
    with(rainbow) {
      assertProduction(m = 20, s = 1, t = 1, p = 4, e = 0, h = 0)
      assertResources(m = 41, s = 2, t = 1, p = 1, e = 2, h = 2)
      assertCounts(36 to "TerraformRating")
    }
    rainbow.turn {
      // 9:36:02–9:36:54 PM — Rainbow prices Asteroid Deflection System at nine, using two steel,
      // one titanium, and 2 M€. When Yellow notices its energy-production cost, Rainbow first buys
      // the 11-M€ Power Plant. Her app records that standard project as a City instead and retains
      // the resulting extra M€ production through the final production credit.
      stdProject("PowerPlantProject").expect("-11 MC")
      exMachina("-14 MC, PROD[MC]")
      assertCounts(16 to "MC")
      playProject(AsteroidDeflectionSystem, 2, steel = 2, titanium = 1)
          .expect("-ProjectCard, -2 MC")
      // The app then removes the full 13 M€ cost in addition to the steel and titanium payment.
      exMachina("-11 MC")
      assertCounts(3 to "MC")
    }
    green.turn {
      // 9:37:30 PM — Green: "That changes things. I'm probably not going to bother playing it
      // because it's probably more important that I Yep, it's more important that I play Breathing
      // Filters for nine. And that gives me a cube on Olympics Contest."
      playProject(BreathingFilters, 9)
    }

    rainbow.turn {
      // 9:38:07 PM — Rainbow: "Let's reveal the top card of the deck."
      // 9:38:11 PM — Rainbow: "Now no space tag."
      // 9:38:15 PM — Rainbow: "But I still took the asteroid deflection system action, so I put a
      // cube on my card."
      cardAction1(AsteroidDeflectionSystem) { doTask("Ok") }
    }
    green.turn {
      // 9:38:27 PM — Green: "Um, well, I mean, actually, I might as well spend the four on ishtar
      // expedition after all, because what else am I going to do now at this point? So, I'll get
      // three titanium, yay, maybe it'll be useful, and let's see if we can actually get two Venus
      // cards in what remains of the deck. If not, I got a lot of shuffling to do. No, I don't
      // think we're going to make it."
      playProject(IshtarExpedition, 4)
    }

    // 9:39:08 PM — Green: "Leechifrous Corrodies (Corroder Suits), and Jetstream Microscrappers."
    // 9:39:15 PM — Green: "Your turn, Rainbow."
    // 9:39:17 PM — Rainbow: "Um, I pass."
    rainbow.pass()
    green.turn {
      // 9:39:42 PM — Green: "Oh, okay. I don't think I can quite get there though. Yeah, I can. One
      // two three four five six patents to get the money I need for corrode her suits."
      // 9:39:59 PM — Green: "That gives me two money production, which gives me two money. and then
      // i put my uh oh wait this is even this is 14 oh is it oh okay got it so now i've got 15 on
      // yeah this whole time i thought it was okay Alright so that was me building coroties."
      sellPatents(6)
      playProject(CorroderSuits, 6) { addCardResources(CloudTourism) }
      // Despite the narration, the app omits Suitable Infrastructure's 2 M€ response to the
      // production gain.
      green.exMachina("-2 MC")
      pass(unused = IcyImpactors) // no point anymore
    }

    // Green's app credits 50 M€ at production despite 36 TR and 11 M€ production totaling 47.
    green.assertCounts(47 to "MC")
    green.exMachina("3 MC")
    green.assertCounts(50 to "MC")
    // Post-production states from all four histories, before final greenery. Rainbow's 60 M€
    // retains the physical Rover income missing from her displayed 58; Blue's history exposes only
    // deltas, so its absolute state follows the independently anchored replay path.
    with(yellow) {
      assertProduction(m = 10, s = 4, t = 0, p = 7, e = 3, h = 1)
      assertResources(m = 46, s = 4, t = 0, p = 8, e = 3, h = 9)
      assertCounts(34 to "TerraformRating")
    }
    with(rainbow) {
      assertProduction(m = 21, s = 1, t = 1, p = 4, e = 0, h = 0)
      assertResources(m = 60, s = 1, t = 1, p = 5, e = 0, h = 4)
      assertCounts(36 to "TerraformRating")
    }
    with(blue) {
      assertProduction(m = 8, s = 0, t = 0, p = 1, e = 13, h = 13)
      assertResources(m = 49, s = 0, t = 0, p = 2, e = 13, h = 29)
      assertCounts(41 to "TerraformRating")
    }
    with(green) {
      assertProduction(m = 11, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertResources(m = 50, s = 1, t = 6, p = 8, e = 1, h = 12)
      assertCounts(36 to "TerraformRating")
    }
    assertSidebar(gen = 9, temp = 14, oxygen = 18, oceans = 11, venus = 26)

    // 9:40:56 PM — Green: "And then [Yellow] does not have oh does have enough plants. Yep,
    // [Yellow]
    // places plants first."
    // 9:40:58 PM — Yellow: "Yeah. I do. Yes. Yeah. Ze will um Mm go A hee -haw for one plant."
    // 9:41:17 PM — Green: "Eleven ten to on eleven t eleven ten for one plant. [Rainbow], do you
    // have
    // enough?"
    yellow.convertPlants { placeTile(11, 10) }
    yellow.declineTask()
    // 9:41:26 PM — Rainbow: "I do not. I only have five."
    rainbow.declineTask()
    blue.declineTask()
    // 9:41:51 PM — Green: "I guess I might as well take four money here on seven five."
    // 9:42:04 PM — Green: "And it gives me two energy for whatever that's worth."
    green.convertPlants { placeTile(7, 5) }
    green.declineTask()

    yellow.assertResources(m = 46, s = 4, t = 0, p = 2, e = 3, h = 9)
    rainbow.assertResources(m = 60, s = 1, t = 1, p = 5, e = 0, h = 4)
    blue.assertResources(m = 49, s = 0, t = 0, p = 2, e = 13, h = 29)
    green.assertResources(m = 54, s = 1, t = 6, p = 0, e = 3, h = 12)

    // board-0909-21-45-22.jpg: final tile layout and controlling tableau-ownership census.
    // 9:42:17–9:43:44 PM — Green wins Excentric with 28 card resources; Rainbow is second. Yellow
    // wins Highlander, Rainbow is second, and Blue/Green have one dry tile each. Blue wins
    // Thermalist 29–12 over Green. Everyone except Yellow has a milestone.
    // 9:47:14 PM — Spoken totals are Green 70, Yellow 70, and Blue 73. Rainbow retracts her 85
    // because she had double-counted card-resource points, so her spoken total is not asserted.
    val score = Summarizer(game)
    score.net("Milestone", "VictoryPoint<Yellow>") shouldBe 0
    score.net("Milestone", "VictoryPoint<Rainbow>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Blue>") shouldBe 5
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Yellow>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Rainbow>") shouldBe 0
    score.net("FirstPlace", "VictoryPoint<Blue>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 5
    score.net("SecondPlace", "VictoryPoint<Yellow>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Rainbow>") shouldBe 4
    score.net("SecondPlace", "VictoryPoint<Blue>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 2
    score.net("GreeneryTile", "VictoryPoint<Yellow>") shouldBe 8
    score.net("GreeneryTile", "VictoryPoint<Rainbow>") shouldBe 3
    score.net("GreeneryTile", "VictoryPoint<Blue>") shouldBe 7
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 2
    score.net("CityTile", "VictoryPoint<Yellow>") shouldBe 11
    score.net("CityTile", "VictoryPoint<Rainbow>") shouldBe 4
    score.net("CityTile", "VictoryPoint<Blue>") shouldBe 9
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 4
    score.net("Card", "VictoryPoint<Yellow>") shouldBe 12
    score.net("Card", "VictoryPoint<Rainbow>") shouldBe 34
    score.net("Card", "VictoryPoint<Blue>") shouldBe 6
    score.net("Card", "VictoryPoint<Green>") shouldBe 16

    // The complete engine categories reproduce the spoken totals for Yellow, Blue, and Green.
    // Rainbow's categories total 86; her spoken 85 is the explicitly corrected, unreliable tally.
    yellow.assertCounts(70 to "VictoryPoint", 0 to "Victory")
    rainbow.assertCounts(86 to "VictoryPoint", 1 to "Victory")
    blue.assertCounts(73 to "VictoryPoint", 0 to "Victory")
    green.assertCounts(70 to "VictoryPoint", 0 to "Victory")
  }
}
