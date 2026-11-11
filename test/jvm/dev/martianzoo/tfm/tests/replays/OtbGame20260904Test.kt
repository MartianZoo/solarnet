package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/**
 * Four-player physical game played across 2026-09-04, 2026-09-05, and 2026-09-09. Comments preserve
 * authenticated gameplay information from the recordings and participant corrections. They need not
 * reproduce the machine transcripts' exact wording, timestamps, or speaker labels; those files are
 * navigation aids and are sometimes less reliable than the human-corrected test record.
 */
internal class OtbGame20260904Test : FullGameTest() {
  override val config =
      GameConfig(
          """
          AmazonisMap, Unsafe
          VenusNextExpansion, PreludeExpansion, prelude2:cards, promos:cards
          fakes:cards

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
      CLASS Rainbow : Player { SetupPhase: 4 TerraformRating<This> }
      CLASS Blue : Player { SetupPhase: 2 TerraformRating<This> }
      CLASS Green : Player
      """
          .trimIndent()

  private val yellow
    get() = player(1)

  private val rainbow
    get() = player(2)

  private val blue
    get() = player(3)

  private val green
    get() = player(4)

  @Test
  internal fun otbGame20260904() {
    startGame()
    rainbow.doTask("4 TerraformRating")
    blue.doTask("2 TerraformRating")
    yellow.doTask("20 TerraformRating")
    rainbow.doTask("20 TerraformRating")
    blue.doTask("20 TerraformRating")
    green.doTask("20 TerraformRating")
    yellow.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    rainbow.doTasks(
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
    green.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
    )
    yellow.doTask("-6 ProjectCard<Selecting>")
    rainbow.doTask("-4 ProjectCard<Selecting>")
    blue.doTask("-5 ProjectCard<Selecting>")
    green.doTask("-6 ProjectCard<Selecting>")
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
    with(yellow) {
      // 2:12:23 PM — Yellow: "Okay, um well all I can do for now is put down My corporation is
      // Ecoline"
      // 2:12:55 PM — Blue: "Alright um well I can show you for now um I will s use this slider to
      // buy four cards, um and I will give myself the um two yes, the three plants. Um"
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Ecoline>>",
          "$Ecoline FROM CorporationCard",
          "36 MC",
          "3 Plant",
          "PROD[2 Plant]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(rainbow) {
      // 2:14:50 PM — Rainbow: "I am Morningstar!"
      // 2:15:04 PM — Green: "You've already given yourself fifty money, and then how many cards are
      // you gonna buy?"
      // 2:15:05 PM — Blue: "Mm-hmm. Oh, um a lot. One two three four five six."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$MorningStarInc>>",
          "$MorningStarInc FROM CorporationCard",
          "50 MC",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(blue) {
      // 2:15:31 PM — Blue: "I am Helion."
      // 2:15:34 PM — Blue: "So I start with three heat production and I have 42 mega credits. And
      // then you can turn heat into money and I can use heat as money."
      // 2:15:48 PM — Blue: "Okay, damn, that's even better. And I am buying five cards."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$FakeHelion>>",
          "$FakeHelion FROM CorporationCard",
          "42 MC",
          "PROD[3 Heat]",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      // 2:15:56 PM — Green: "All right, and I am playing Factotum, which is an awesome corporation.
      // So I take 37 plus money. I'm buying four cards."
      // 2:16:15 PM — Green: "I get a steel production."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Factorum>>",
          "$Factorum FROM CorporationCard",
          "37 MC",
          "PROD[Steel]",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(yellow) {
      // 2:16:32 PM — Blue: "Right, um. I got a Doe farming, so, um, yes, um, one plant
      // production, two money production, and huge ass teroid, yep."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
          "PROD[Plant]",
          "PROD[2 MC]",
      )
      // 2:16:51 PM — Blue: "Raise the temperature three um"
      // 2:17:04 PM — Blue: "Yeah um alright and since I hit the bonus I get heat one more time"
      // 2:17:09 PM — Green: "And did you remember to lose the five money? Gotta watch."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$HugeAsteroid>>",
          "$HugeAsteroid FROM PreludeCard",
          "TemperatureStep",
          "TemperatureStep",
          "TemperatureStep",
          "-5 MC",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
      )
    }
    with(rainbow) {
      // 2:17:27 PM — Rainbow: "Yeah, I got space lanes, some planet tags are too off."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AppliedScience>>",
          "$AppliedScience FROM PreludeCard",
          "6 Science<$AppliedScience>",
      )
      // 2:17:52 PM — Rainbow: "um and this one has an action so super bonus um yeah so i get six
      // science resources"
      // 2:18:13 PM — Green: "You've got a wild tag. So remember, you've always got one more tag
      // while you're performing an action, you have one more tag of whatever kind. It won't help
      // you get awards."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SpaceLanes>>",
          "$SpaceLanes FROM PreludeCard",
          "3 Titanium",
      )
    }
    with(blue) {
      // 2:18:56 PM — Blue: "Okay, I am going to play Self-Sufficient Settlement."
      // 2:19:44 PM — Blue: "Uh -huh. Okay. Incredible. I think I shall place this... here for a
      // plant and a and a card."
      // 2:19:58 PM — Blue: "So that is row four column eight."
      // 2:20:00 PM — Green: "Mm-hmm. I think you just counted off nine."
      // 2:20:07 PM — Blue: "One two three four five six oh yeah nine."
      // 2:20:08 PM — Green: "No? Okay, row four column nine. The rows are measured as you think,
      // the columns are this way, pointing directly to you."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SelfSufficientSettlement>>",
          "$SelfSufficientSettlement FROM PreludeCard",
          "PROD[2 MC]",
          "CityTile<Amazonis_04_09>",
          "Plant",
          "ProjectCard",
      )
      // 2:20:25 PM — Blue: "get two money product."
      // 2:20:29 PM — Blue: "And, uh, my other prelude is terraforming deal."
      // 2:20:35 PM — Blue: "Each step my TR is raised, I gain two mega credits."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>",
          "$TerraformingDeal FROM PreludeCard",
      )
    }
    // Blue forgets these, then realizes 100 lines down from here and corrects.
    blue.exMachina("-Plant, -ProjectCard")
    with(green) {
      // 2:20:43 PM — Green: "I am first going to play Sweetable Infrastructure."
      // 2:20:56 PM — Green: "I'm going to gain five steel right away."
      // 2:21:07 PM — Green: "And every time I gain production... I can get two money once per
      // action. So if I gain multiple fractions as part of the same action, I still only get two."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SuitableInfrastructure>>",
          "$SuitableInfrastructure FROM PreludeCard",
          "5 Steel",
      )
      // 2:21:20 PM — Green: "And I should play another one. Atmospheric Enhancers."
      // 2:21:33 PM — Green: "I raised temp two times."
      // 2:21:36 PM — Green: "So temp is already at minus twenty. I get a heat production. I get two
      // T_R_ Uh no no, this is uh either or. So I'm choosing temperature."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$AtmosphericEnhancers>>",
          "$AtmosphericEnhancers FROM PreludeCard",
      )
      doTask("2 SearchForCard<ReferenceFilter<Class<Floater>>>").expect("2 ProjectCard")
      doTasks(
          "2 TemperatureStep",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
    }
    // I already forgot to get the SuitableInfrastructure bonus!
    green.exMachina("-2 MC")

    with(yellow) {
      // board-14-23-07.jpg: Generation 1 action phase, immediately before Space Mirrors.
      // 2:23:17 PM — Rainbow: "I pay three for space mirrors."
      doTasks(*playCardTasks("$SpaceMirrors"), "3 Pay<> FROM MC", "$SpaceMirrors FROM ProjectCard")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 2:23:34 PM — Rainbow: "Okay. I draw cards until I get three Venus."
      doTask("DoRequiredActions")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>").expect("3 ProjectCard")
      doTask("PassBaton")
    }

    with(blue) {
      // 2:26:16 PM — Green: "you paid sixteen for that full price, you got two heat production."
      doTasks(
          *playCardTasks("$HomeostasisBureau"),
          "16 Pay<> FROM MC",
          "$HomeostasisBureau FROM ProjectCard",
          "PROD[2 Heat]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 2:26:24 PM — Green: "I'm gonna play titanium mine and I pay three steel and one reel."
      // 2:26:42 PM — Green: "two money from suitable infrastructure I"
      doTasks(
          *playCardTasks("$TitaniumMine"),
          "3 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$TitaniumMine FROM ProjectCard",
          "PROD[Titanium]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 2:27:00 PM — Blue: "I've noticed um I mean I have something going for me uh seven to use
      // space mirrors gain energy production then six for building infrastructure"
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      // 2:27:18 PM — Green: "That is building industries Well, I just every time I raise production
      // I get money and then I have the car that lets me raise production"
      doTasks(
          *playCardTasks("$BuildingIndustries"),
          "6 Pay<> FROM MC",
          "$BuildingIndustries FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 Steel]",
      )
    }
    with(rainbow) {
      // 2:28:42 PM — Rainbow: "That's okay, because it's cheaper to do a floating workbinder."
      // 2:28:45 PM — Rainbow: "For seven money. Which should be 25."
      // Morning Star's wild tag supplies the third Venus tag counted aloud for this operation.
      doTasks(
          *playCardTasks("$FloatingRefinery"),
          "5 Pay<> FROM MC",
          "$FloatingRefinery FROM ProjectCard",
          "3 Floater<$FloatingRefinery>",
      )
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
      doTask("PassBaton")
    }

    with(blue) {
      // 2:29:45 PM — Blue: "So that cost me 11 money, which is all the money I have left in the
      // world.
      // And I get two titaniums of energy production."
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "11 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 2:29:59 PM — Green: "What I decided to do is That, I'm sorry, what I decided to do, okay,
      // I'm going to play Olympus Conference. I spend two steel and six real, and I put a science
      // resource on it."
      doTasks(
          *playCardTasks("$OlympusConference"),
          "2 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(rainbow) {
      // 2:31:36 PM — Rainbow: "Oh, this will only be four."
      // 2:31:39 PM — Rainbow: "it's a Planet Tag meeting room."
      // 2:31:41 PM — Green: "Oh, meeting group."
      // 2:31:49 PM — Rainbow: "Yeah, let's leave it there for now."
      doTasks(*playCardTasks("$MediaGroup"), "4 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    with(green) {
      // 2:31:50 PM — Green: "is only four all right you're done with stuff you spent all your money
      // all right it is down to me and it is down to you and i'm gonna play robotic workforce which
      // costs me nine look at all the things it does because it's a science tag which means I lose
      // a
      // science resource to draw a free card and it raises my titanium production and because I
      // raised production it gives me two money can't complain about that [Rainbow]'s turn"
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$TitaniumMine>",
          "PROD[Titanium]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
          "ProjectCard FROM Science<$OlympusConference>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 2:33:22 PM — Yellow: "Lava flows."
      // 2:33:38 PM — Yellow: "Three plus and temperature up."
      // 2:33:44 PM — Green: "Yep, so we can all raise temp to minus sixteen if we wish. Give
      // yourself two TR for that."
      // 2:34:10 PM — Green: "All right, so you're going on Hecate's Tholus for two stairs."
      // 2:34:23 PM — Green: "We announced that it's Hecate's Tholus, good enough. But it's five
      // one."
      // 2:34:23 PM — Blue: "Five one. Yeah, that's okay. Okay."
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "TerraformRating",
          "LavaFlows_SpecialTile<Amazonis_05_01>",
          "2 Steel",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 2:34:38 PM — Green: "Yes. And [Blue] has already passed, so I'm going to use my factotum
      // to"
      // 2:34:50 PM — Green: "gain an energy production, gives me two money."
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 2:35:09 PM — Rainbow: "Right. Okay. Well then I'm going to remove two floaters."
      // 2:35:13 PM — Rainbow: "Yeah. Uh gain titanium and and two money. Burp burp."
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton")
    }
    // 2:35:31 PM — Green: "Yeah, so. Okay, now what I'm gonna do is Nothing. I pass. It's all you,
    // [Rainbow]."
    green.doTask("Pass")
    // 2:35:57 PM — Rainbow: "I believe I also pass."
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("25 MC", "3 Plant", "Heat", "2 Steel")
    rainbow.doTask("26 MC")
    blue.doTasks("24 MC", "5 Heat", "Energy")
    green.doTasks("22 MC", "Steel", "Heat", "2 Titanium", "Energy")
    // 2:36:16 PM — Yellow: "Anushkin on this two-card spot so no one gets that."
    // 2:36:20 PM — Green: "So that looks like it's eight four to me."
    // 2:36:26 PM — Green: "Eight four for two car and nobody gets the cards, of course."
    yellow.doTask("OceanTile<Amazonis_08_04>! BY Admin")
  }

  private fun generation2() {
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
      assertCardResources(1 to FloatingRefinery, 6 to AppliedScience)
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    rainbow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(rainbow) {
      // 2:42:41 PM — Green: "So hit your money 27 there, and then the standard projects come up.
      // Oh,
      // but you don't see our scrapping."
      // 2:42:48 PM — Green: "Okay, so do 15 minus. And then give yourself a T_R_ And now Venus is
      // at two."
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // 2:43:13 PM — Blue: "nuclear zone so that cost me ten money oops it cost me ten money and
      // then I raised the temperature two steps which does give me a plant production and also
      // raising the temperature two steps gives me six money"
      // 2:43:39 PM — Blue: "And two T-R boots, which in turn gives me four more money."
      // 2:44:03 PM — Blue: "You're such a jerk. I'm gonna go ahead and go on row three column
      // three and two cards please. Yeah."
      doTasks(
          *playCardTasks("$NuclearZone"),
          "Ok", // No heat paid.
          "10 Pay<> FROM MC",
          "$NuclearZone FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "TerraformRating",
          "PROD[Plant]",
          "3 MC",
          "2 MC",
          "2 MC",
          "NuclearZone_SpecialTile<Amazonis_03_03>",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 2:45:05 PM — Green: "I'm going to use my factotum. I cannot take energy production because
      // I have energy resources, but I can pay three money to draw a building tag. bio printing
      // facility what a strikeout that was discard"
      doTasks(*cardActionTasks("$Factorum", "Action2"), "3 Pay<> FROM MC")
      doTask("SearchForCard<TagFilter<Class<BuildingTag>>>").expect("ProjectCard")
      // 2:45:34 PM — Green: "that was my well you know let me do one more thing on my turn just in
      // case let's play pets ten minus and I get a pet for that"
      doTasks(
          *playCardTasks("$Pets"),
          "10 Pay<> FROM MC",
          "$Pets FROM ProjectCard",
          "Animal<$Pets>",
      )
    }

    with(yellow) {
      // 2:47:06 PM — Yellow: "I think I will "spa-che" mirrors. Seven money for energy product."
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 2:47:21 PM — Rainbow: "Man, I don't produce anything. Okay, um, however, I'm going to spend
      // four on sulfur-eating bacteria."
      doTasks(
          *playCardTasks("$SulphurEatingBacteria"),
          "4 Pay<> FROM MC",
          "$SulphurEatingBacteria FROM ProjectCard",
      )
      // 2:47:48 PM — Green: "Alright, so you played it and you used the action. Put your rainbow on
      // it if you used the action."
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
    }

    with(blue) {
      // 2:49:00 PM — Blue: "My s little sponsors. They love me and they're gonna give me too many
      // production."
      doTasks(
          *playCardTasks("$Sponsors"),
          "Ok", // No heat paid.
          "6 Pay<> FROM MC",
          "$Sponsors FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }
    // 2:50:12 PM — Green: "I know my pass."
    green.doTask("Pass")
    // 2:50:48 PM — Blue: "Um, I think I will also pass."
    yellow.doTask("Pass")

    with(rainbow) {
      // 2:50:54 PM — Rainbow: "Wow. All right, I'm going to add a floater. Nibble it."
      doTasks(*cardActionTasks("$FloatingRefinery"), "Floater<$FloatingRefinery>")
      doTask("PassBaton")
    }
    with(blue) {
      // 2:51:26 PM — Blue: "And I would like to have played both of them before the next round,
      // but I guess whatever. Okay. I'm going to spend 12 money and two heat. Alright. Some
      // Neptunian power consoles next."
      // 2:51:50 PM — Blue: "So when any ocean is placed, I may spend five money to raise my
      // energy production one step and add one hydroelectric resource there."
      doTasks(
          *playCardTasks("$NeptunianPowerConsultants"),
          "2 Pay<Class<Heat>> FROM Heat",
          "12 Pay<> FROM MC",
          "$NeptunianPowerConsultants FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 2:53:10 PM — Green: "Okay. So you've used applied science and put a microbe on
      // sulfur-eating bacteria."
      // 2:53:12 PM — Rainbow: "So I'm gonna have you use applied science Sulfur-eating bacteria.
      // I'm
      // gonna pass."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    blue.doTask("Pass")
    with(rainbow) {
      // 2:53:33 PM — Rainbow: "I want to have money, but I want to have these things. The
      // conundrum.
      // I'm going to go ahead and play my space station."
      // 2:53:53 PM — Rainbow: "And I'm going to spend one titanium and seven money?"
      // 2:54:05 PM — Rainbow: "You're right. I'm going to spend it all down."
      // 2:54:13 PM — Rainbow: "Three titaniums is nine money."
      // 2:54:15 PM — Rainbow: "So I only have to spend one money."
      doTasks(
          *playCardTasks("$SpaceStation"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$SpaceStation FROM ProjectCard",
      )
    }
    // 2:54:19 PM — Rainbow: "Okay. And that I played Space Station. Um no, no I think that's it. I
    // passed."
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("25 MC", "3 Plant", "Heat", "2 Steel", "Energy")
    rainbow.doTask("27 MC")
    blue.doTasks("28 MC", "5 Heat", "Energy", "Heat FROM Energy", "Plant")
    green.doTasks("22 MC", "Steel", "Heat", "2 Titanium", "Energy", "Heat FROM Energy")
    // 2:54:39 PM — Green: "I mean, [Rainbow] decides which global parameter to increase with no
    // benefit
    // to herself, including you could play some ocean if you want."
    // 2:54:56 PM — Rainbow: "Yeah. Nope, Venus up to four, right?"
    rainbow.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation3() {
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
          5 to AppliedScience,
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
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
    rainbow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      // 3:01:51 PM — Blue: "Fuck my stupid fucking childish life. Okay. Lunar beam cost me
      // thirteen money."
      // 3:02:23 PM — Blue: "I lose two mega credits, I gain two heat production, I mean I lose
      // two mega credit production, I gain two heat production, and two energy production. And
      // that's my turn. And now it's Green's turn."
      doTasks(
          *playCardTasks("$LunarBeam"),
          "Ok", // No heat paid.
          "13 Pay<> FROM MC",
          "$LunarBeam FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[2 Heat]",
          "PROD[2 Energy]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:02:40 PM — Green: "Okay, I'm going to... Play Cloud Tourism for full price. I have only
      // two sets of Venus and Earth tags. That's what makes me sad, but I had to get this down."
      // 3:03:05 PM — Green: "So I get two mega credit production and two mega credits from
      // Sweetable Infrastructure. Um, and is there anything else I'm in a hurry to do? No, I pass.
      // I end my turn."
      doTasks(
          *playCardTasks("$CloudTourism"),
          "11 Pay<> FROM MC",
          "$CloudTourism FROM ProjectCard",
          "PROD[2 MC]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // 3:03:39 PM — Yellow: "a seven for spaghettimi roars, so I gain energy product shun. All
      // right."
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:05:56 PM — Rainbow: "i'm close enough that i can play ishtar mining for three uh-huh not
      // bad you got a titanium production finally I finally learned about titanium production."
      doTasks(
          *playCardTasks("$IshtarMining"),
          "3 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // 3:06:29 PM — Blue: "Finally learned about titanium. Alright, that's it. Alright, alright
      // fuck, what do I have now? I have 12 micro, so I have, yeah, oh okay. I'm gonna play
      // advanced
      // hours."
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "Ok", // No heat paid.
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:07:06 PM — Green: "I'm going to use Factorum to spend three and get a wielding card."
      doTasks(*cardActionTasks("$Factorum", "Action2"), "3 Pay<> FROM MC")
      doTask("SearchForCard<TagFilter<Class<BuildingTag>>>").expect("ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:07:34 PM — Blue: "My turn. Flooding."
      // 3:07:50 PM — Rainbow: "I pay a seven."
      // 3:08:02 PM — Rainbow: "Um, get my two plants, get my ocean and"
      // Blue does not have to spend a heat (couldn't if she wanted to)
      doTasks(*playCardTasks("$Flooding"), "7 Pay<> FROM MC", "$Flooding FROM ProjectCard")
      selectTask("OceanTile<Amazonis_05_10>! THEN -3 MC<Blue>")
      doTasks(
          "OceanTile<Amazonis_05_10>",
          "-3 MC<Blue>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("Ok") // Decline the Neptunian Power Consultants response.
      doTask("2 Plant")
      // 3:08:23 PM — Yellow: "Mm-hmm. Underground city."
      // 3:08:32 PM — Blue: "Yeah. Um my four steel and ten reel. Do do do do do do do do do do."
      // 3:08:51 PM — Green: "So that's 10-10."
      doTasks(
          *playCardTasks("$UndergroundCity"),
          "4 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$UndergroundCity FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 Steel]",
          "CityTile<Amazonis_10_10>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("2 Plant")
    }
    with(rainbow) {
      // 3:10:22 PM — Blue: "Okay, I'm going to do sulfur exports."
      // 3:10:49 PM — Green: "Alright, so it's one titanium and fourteen money. Yep."
      // 3:10:58 PM — Rainbow: "yep Okay, so now I increase Venus one step, gives me TR, and I
      // increase my money production one step for each Venus tag I have, which is one two three
      // four
      // five and six."
      doTasks(
          *playCardTasks("$SulphurExports"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "14 Pay<> FROM MC",
          "$SulphurExports FROM ProjectCard",
          "VenusStep",
          "PROD[6 MC]",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(blue) {
      // 3:11:32 PM — Blue: "I'm gonna convert 80 to a temperature."
      // 3:11:38 PM — Blue: "gives me five money."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:11:52 PM — Yellow: "So really, I play Venus shuttles, I pay full price, nine minus, it
      // tells me to add two floaters to cloud tourism, and for my next trick, I'm going to use it.
      // Oh, I don't have any money to use it."
      doTasks(
          *playCardTasks("$VenusShuttles"),
          "9 Pay<> FROM MC",
          "$VenusShuttles FROM ProjectCard",
          "2 Floater<$CloudTourism>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:12:45 PM — Yellow: "Right. What I do is, um, now things might start, hopefully start
      // getting good, um, plant forest for seven."
      // 3:13:11 PM — Green: "Ten nine."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_10_09>",
          "OxygenStep",
          "3 Plant",
          "TerraformRating",
      )
      // 3:13:36 PM — Yellow: "Yeah Wait, wait, I oh I can actually place it here for two steals
      // Uh-huh."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_10_11>",
          "OxygenStep",
          "2 Steel",
          "TerraformRating",
      )
    }
    // 3:14:09 PM — Rainbow: "All right. Um, I think I'm going to remove two floaters to gain one
    // titanium and two money."
    with(rainbow) {
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      doTask("PassBaton")
    }
    // 3:14:21 PM — Blue: "Okay, that's it for now. I pass."
    blue.doTask("Pass")
    with(green) {
      // 3:15:15 PM — Green: "I sell two patents."
      // 3:15:26 PM — Green: "Now I have nine money. Now I use Venus Rebels to spend nine money."
      // 3:15:33 PM — Green: "So Venus is now on eight. I get a TR for that."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTasks(
          *cardActionTasks("$VenusShuttles"),
          "9 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
    }
    with(yellow) {
      // 3:15:42 PM — Blue: "Anyways, what I can do is play Natural Preserve for 2 steel and 5
      // real. increase money production by one. Oh wow."
      // 3:16:10 PM — Blue: "Place it on next to no other tile. I'll choose one four for a card.
      // Hope it's a good one."
      doTasks(
              *playCardTasks("$NaturalPreserve"),
              "2 Pay<Class<Steel>> FROM Steel",
              "5 Pay<> FROM MC",
              "$NaturalPreserve FROM ProjectCard",
              "PROD[MC]",
              "NaturalPreserve_SpecialTile<Amazonis_01_04>",
              "ProjectCard",
          )
          .expect("0 ProjectCard")
      doTask("PassBaton")
    }

    with(rainbow) {
      // 3:17:01 PM — Rainbow: "Okay. I played Rover Construction."
      // 3:17:22 PM — Rainbow: "I paid four and two steel. Well, at this point I only paid two
      // steel.
      // Alright, keep me honest. That's the end of my turn."
      doTasks(
          *playCardTasks("$RoverConstruction"),
          "2 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$RoverConstruction FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 3:17:35 PM — Green: "Okay, I probably am going to keep losing my fees. So I used Vienna
      // strudels already. I'm going to use cloud tourism to add a floater to cloud tourism."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    // 3:18:05 PM — Rainbow: "I'm going to take a science resource and put it on my self-rating
    // bacteria."
    // Applied Science is legally replayed after Yellow passes without moving this evidence.
    // 3:18:15 PM — Yellow: "I think I'm ready to pass out of here. Pass out."
    yellow.doTask("Pass")
    with(rainbow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    // 3:18:26 PM — Rainbow: "Okay. Then I'm going to add another microbe to self-reating bacteria.
    // And then I too shall pass."
    with(rainbow) {
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
    }
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("29 MC", "3 Plant", "Heat", "4 Steel", "Heat FROM Energy")
    rainbow.doTasks("34 MC", "Titanium")
    blue.doTasks("27 MC", "7 Heat", "3 Energy", "Heat FROM Energy", "Plant")
    green.doTasks("25 MC", "Steel", "Heat", "2 Titanium", "Energy", "Heat FROM Energy")
    // 3:19:00 PM — Blue: "Yeah. ocean. Thank goodness."
    // 3:19:37 PM — Green: "Two six."
    blue.doTask("OceanTile<Amazonis_02_06>! BY Admin")

    with(blue) {
      // 3:19:50 PM — Blue: "but I do get to do my Neptunian power consults so I will
      // pay five monies Yeah, so I get an energy production and I place a hydroelectric resource
      // on Neptunian power result."
      doTasks(
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "Ok", // No heat paid.
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
    }
  }

  private fun generation4() {
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
      assertCardResources(4 to SulphurEatingBacteria, 4 to AppliedScience)
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-1 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // 3:26:34 PM — Green: "Earth office, earth office."
      doTasks(*playCardTasks("$EarthOffice"), "1 Pay<> FROM MC", "$EarthOffice FROM ProjectCard")
      // 3:26:38 PM — Green: "You cost me one, then I can get a type of contract, and it costs me
      // only five, and I get three plantos,"
      doTasks(
          *playCardTasks("$TopsoilContract"),
          "5 Pay<> FROM MC",
          "$TopsoilContract FROM ProjectCard",
          "3 Plant",
      )
    }

    with(yellow) {
      // 3:26:58 PM — Yellow: "what was it um I'm gonna Sabotage you, Green, you lose your three
      // steel.
      // I
      // like attacking that."
      // 3:27:14 PM — Yellow: "Yeah, just thought I could hurt you. I hope I attack the right
      // players.
      // What was my plan here? I had a plan, and I think I was going to turn all my microbes into
      // money."
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-3 Steel<Green>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:27:37 PM — Green: "Yeah, but do you want to use your applied sciences first so you have
      // more microbes to turn into money?"
      // 3:27:49 PM — Green: "Yes, applied sciences to put one more on sulfur or whatever, eating
      // bacteria."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      // 3:27:55 PM — Rainbow: "Sulfur eating bacteria, using sulfur eating bacteria to turn five
      // microbes into 15 money."
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-5 Microbe<$SulphurEatingBacteria>",
          "15 MC",
      )
    }
    with(blue) {
      // Blue paid 18 M€ and 5 heat for Protected Valley at 2-7, receiving the space's two titanium
      // and 2 M€ ocean adjacency. She then paid 4 M€ and 4 heat to claim Landshaper.
      doTasks(
          *playCardTasks("$ProtectedValley"),
          "5 Pay<Class<Heat>> FROM Heat",
          "18 Pay<> FROM MC",
          "$ProtectedValley FROM ProjectCard",
          "PROD[2 MC]",
          "GreeneryTile<Amazonis_02_07>",
          "OxygenStep",
          "2 Titanium",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
      doTask("UseAction<ClaimMilestoneAction, Action1>")
      doTasks("4 Pay<Class<Heat>> FROM Heat", "4 Pay<> FROM MC")
      doTask("Landshaper")
    }
    with(green) {
      // 3:33:24 PM — Green: "Okay. I'm gonna sell this card for one money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // 3:33:38 PM — Green: "Earth, microbe, building, science, animal, Jovian, Venus, Paris. power
      // so so I take Diversifier so now Landshaper and Diversifier are taken and I summed eight on
      // that"
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Diversifier")
    }

    with(yellow) {
      // 3:34:12 PM — Blue: "Okay, it is wait, it is my a tour on suppose I may as well spache mi
      // errores on seven. Energia."
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }

    with(rainbow) {
      // 3:35:04 PM — Rainbow: "Alright well, I can build wave power for Eight full money."
      doTasks(
          *playCardTasks("$WavePower"),
          "8 Pay<> FROM MC",
          "$WavePower FROM ProjectCard",
          "PROD[Energy]",
      )
      doTask("PassBaton")
    }
    blue.doTask("Pass")

    with(green) {
      // 3:35:33 PM — Green: "I... Use my cloud tourism data floater, [Yellow]."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(yellow) {
      // 3:35:41 PM — Blue: "I think I will adapt it like Ken. Bit expensive, but gain plant
      // production and I like having that."
      doTasks(
          *playCardTasks("$AdaptedLichen"),
          "9 Pay<> FROM MC",
          "$AdaptedLichen FROM ProjectCard",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:37:01 PM — Rainbow: "But let me pay 15... money... for eosulfur research. And I get three
      // cards."
      doTasks(
          *playCardTasks("$IoSulphurResearch"),
          "15 Pay<> FROM MC",
          "$IoSulphurResearch FROM ProjectCard",
          "3 ProjectCard",
      )
      doTask("PassBaton")
    }
    // 3:37:26 PM — Green: "I cannot use my Factorum action, so I have nothing left, or any
    // shuttles, so I must pass."
    green.doTask("Pass")
    // 3:37:36 PM — Blue: "I believe I pass as well."
    yellow.doTask("Pass")
    with(rainbow) {
      // 3:39:23 PM — Rainbow: "So that be nice. It's not the worst. OK, but I could be more
      // strategic
      // about this. I can and I will somehow. Alright, gotta pay full price for this one. Ten
      // money.
      // For bacterial virus research."
      // 3:40:00 PM — Rainbow: "I get to uh add one microbe to a played card for each science tag I
      // have, including this, which is one, two, and three."
      doTasks(
          *playCardTasks("$BactoviralResearch"),
          "10 Pay<> FROM MC",
          "$BactoviralResearch FROM ProjectCard",
          "ProjectCard",
          "3 Microbe<$SulphurEatingBacteria>",
      )
      // 3:41:02 PM — Rainbow: "Fine, then I'm gonna pay eight money for a at a Linta planitia lab,
      // which gets me two cards."
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "8 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
      )
    }
    // 3:41:34 PM — Rainbow: "Yep. Yeah, believe me, I tried all kinds of ways to make that happen,
    // and I just wasn't. Um, I'm finally done. I pass."
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("29 MC", "4 Plant", "Heat", "4 Steel", "Energy")
    rainbow.doTasks("34 MC", "Titanium", "Energy")
    blue.doTasks("30 MC", "7 Heat", "4 Energy", "3 Heat FROM Energy", "Plant")
    green.doTasks("25 MC", "Steel", "Heat", "2 Titanium", "Energy", "Heat FROM Energy")
    // 3:42:11 PM — Green: "I don't know. What I really care about I think I'll bump oxygen up to
    // four. Then the start player token moves to Wait wait wait wait before we do all those things
    // I
    // wanna take the picture of your exact boundary of the generation"
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation5() {
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
      assertCardResources(3 to SulphurEatingBacteria, 3 to AppliedScience)
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // 3:49:54 PM — Blue: "Sure, let's start with, um, I'm gonna do something not known to
      // mankind and actually play cutting edge."
      // 3:50:03 PM — Blue: "Um, twelve. And then I will pay seven for marriage and survey. We are
      // thankfully just at four percent. Or what, what? No, no, no. Um seven minus two kinds of
      // soup."
      doTasks(
          *playCardTasks("$CuttingEdgeTechnology"),
          "12 Pay<> FROM MC",
          "$CuttingEdgeTechnology FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$MartianSurvey"),
          "7 Pay<> FROM MC",
          "$MartianSurvey FROM ProjectCard",
          "2 ProjectCard",
      )
    }

    with(rainbow) {
      // 3:50:38 PM — Rainbow: "Spend eight more fact or shuns."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tactician")
      doTask("PassBaton")
    }
    with(blue) {
      // 3:51:32 PM — Blue: "I'm gonna play my big ass toroid, so I will spend four titanium for
      // that and eleven mega credits."
      // 3:51:46 PM — Blue: "And then sixteen money, 'cause I have advanced alloys."
      // 3:51:51 PM — Blue: "Um and so for big asteroid I gain four titanium."
      // 3:51:59 PM — Blue: "And I do two temperature raises. Yep."
      // 3:52:11 PM — Blue: "And I get two TR and ten money. And who has plants?"
      // 3:52:30 PM — Blue: "That was so awesome. I basically just spent one money to get two
      // points and steal four of [Yellow]'s plants."
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No heat paid.
          "11 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "TerraformRating",
          "3 MC",
          "TerraformRating",
          "3 MC",
          "2 MC",
          "2 MC",
          "-4 Plant<Yellow>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:53:00 PM — Green: "I'm no longer under as much urgency. Oh, oh, this is interesting.
      // Tech demo. Cost me full price. Draws me two cards. Hmm. Gives me a science resource on
      // account of which I can play Diversity Support."
      // Green's quoted "Cost me full price" establishes that he preserved his titanium despite the
      // Space tag.
      doTasks(
          *playCardTasks("$TechnologyDemonstration"),
          "Ok", // No titanium paid.
          "5 Pay<> FROM MC",
          "$TechnologyDemonstration FROM ProjectCard",
          "2 ProjectCard",
          "Science<$OlympusConference>",
      )
      // 3:53:38 PM — Green: "Because I have Science, Animal, Floater, and all six standard. And
      // that costs me one money, right? I don't have any discounts on. So that's my turn."
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "$DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
    }
    // Green forgot to take the TR from Diversity Support, and corrects this later.
    green.exMachina("-TerraformRating")

    with(yellow) {
      // 3:54:23 PM — Blue: "Okay, nine for Lagange of self with whole hay."
      // 3:54:27 PM — Blue: "La grande. I draw it hard."
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "9 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:54:59 PM — Rainbow: "Are you fucking kidding me? Okay. Well, I'm taking my applied
      // science
      // action, and I'm applying my science resource to Sulfur Eating Bacteria. Then I'm going to
      // take my Sulfur Eating Bacteria action. And turn these four science resources into 12
      // money."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-4 Microbe<$SulphurEatingBacteria>",
          "12 MC",
      )
    }

    with(blue) {
      // 3:55:26 PM — Blue: "Okay, I'm gonna raise the temperature and convert my heat."
      // 3:55:33 PM — Blue: "[Yellow]'s gonna snatch a bonus. And for that I raise my... Okay, the
      // temperature's to minus four, and it raised my T_R_ automatically, so I don't need to do
      // that
      // and I get five more money."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:56:12 PM — Green: "Well, let us use There's so much I want to do and so little to do it
      // with. Let us assume my priority is to play icy impactors. That cost me five titanium."
      doTasks(
          *playCardTasks("$IcyImpactors"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$IcyImpactors FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    // 3:56:59 PM — Yellow: "I didn't like giving that card up. I just like, I couldn't afford it.
    // Um, I pass."
    yellow.doTask("Pass")
    with(rainbow) {
      // Rainbow's app entry 89 debits 2 M€ where the transcript establishes a 25 M€ cash
      // payment. Supply the omitted 23 M€ only long enough for the real billing to reproduce that
      // captured under-debit.
      exMachina("23 MC")

      // 3:57:07–3:58:42 PM — Rainbow values three titanium at nine M€, pays the remaining 25 M€,
      // raises temperature twice, and places three oceans. Her offer to take up to six plants from
      // Yellow removes Yellow's actual five. Blue uses all three Neptunian responses and reaches
      // four hydroelectric resources.
      doTasks(
          *playCardTasks("$GiantIceAsteroid"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "25 Pay<> FROM MC",
          "$GiantIceAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "3 MC",
          "TerraformRating",
          "TerraformRating",
          // 3:58:46 PM — Green: "Alright, since I have you going on seven, four for two money and
          // two plants."
          "OceanTile<Amazonis_07_04>",
          "TerraformRating",
          // The ocean placer hands each Neptunian step to Blue, who then submits that step.
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks("2 Plant", "2 MC", "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }")
      blue.doTask("Ok") // No heat paid.
      doTask("NOW Blue { 5 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("5 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      doTasks(
          // 3:59:00 PM — Green: "All right this would be nine four."
          // 3:59:01 PM — Blue: "Two money, one steel and one titanium. Where am I doing the
          // next
          // one? Only two money or two plants. Do I need another card? Do I need another card?"
          "OceanTile<Amazonis_09_04>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks(
          "Steel",
          "Titanium",
          "2 MC",
          "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }",
      )
      blue.doTask("Ok") // No heat paid.
      doTask("NOW Blue { 5 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("5 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      doTasks(
          // Crossing 0°C supplies Amazonis's temperature-track ocean bonus.
          // 3:59:30 PM — Green: "All right, going to 611 and you get a card. What a turn of
          // events."
          "OceanTile<Amazonis_06_11>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks("ProjectCard", "2 MC", "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }")
      blue.doTask("Ok") // No heat paid.
      doTask("NOW Blue { 5 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("5 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      doTask("-5 Plant<Yellow>")
      doTask("PassBaton")
    }
    // The printed card operation includes Media Group's 3 M€ event response, but Rainbow's app has
    // no
    // corresponding credit in entries 89–98.
    rainbow.exMachina("-3 MC")
    // 3:59:44 PM — Rainbow: "Did I pay for that?"
    // 3:59:48 PM — Rainbow: "I'll try it again. 25."
    // 4:00:09 PM — Green: "You you. just literally just skipped paying."
    // Rainbow then concludes that she skipped the payment and enters the full narrated 25 M€.
    rainbow.exMachina("-25 MC")

    with(blue) {
      // 4:01:12 PM — Blue: "Giant space mirror."
      // 4:01:19 PM — Blue: "Titanium and one mega credit, and I gain three energy production."
      doTasks(
          *playCardTasks("$GiantSpaceMirror"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No heat paid.
          "1 Pay<> FROM MC",
          "$GiantSpaceMirror FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 4:01:55 PM — Green: "Well Let's probably do the most alright. Hmm. Okay. What is important
      // here? What is important? I have all this stuff to you. Okay, okay. Rad Chem factory, one
      // steel six real. I lose in energy production, I gain two TR. Wow, I'm up to 25."
      doTasks(
          *playCardTasks("$RadChemFactory"),
          "1 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$RadChemFactory FROM ProjectCard",
          "PROD[-Energy]",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }
    // 4:02:33 PM — Blue: "Um, I think I pass."
    // 4:02:48 PM — Rainbow: "I'm going to add a floater to my floating refinery."
    rainbow.assertCardResources(0 to FloatingRefinery)

    with(rainbow) {
      doTasks(*cardActionTasks("$FloatingRefinery"), "Floater<$FloatingRefinery>")
      doTask("PassBaton")
    }
    rainbow.assertCardResources(1 to FloatingRefinery)
    // No recording or app entry supplies another action between this one-cube result and the
    // 16:07:06 photograph, which visibly has two cubes. The extra floater's earliest possible point
    // is the preceding Generation 5 checkpoint and its latest is that photograph.
    rainbow.exMachina("Floater<$FloatingRefinery>")
    rainbow.assertCardResources(2 to FloatingRefinery)
    blue.doTask("Pass")
    // 4:02:56 PM — Green: "I'm going to use Icy Impactors to spend three titanium and one real
    // money to put two roids on the card. [Yellow]'s turn."
    with(green) {
      doTasks(
          *cardActionTasks("$IcyImpactors"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "2 Asteroid<$IcyImpactors>",
      )
      doTask("PassBaton")
    }

    with(rainbow) {
      // 4:03:17 PM — Rainbow: "going to spend four money on moss which requires me to lose a plant
      // but
      // I get plant production"
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 4:03:33 PM — Green: "How come I like never get production even though it's it's my thing
      // thing All right. I think I'm going to prioritize getting production then. So I will play
      // Venus Governator, which costs me four. It gives me two money production and then sweetable
      // infrastructure gives me two money. And [Rainbow]'s turn."
      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "$VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(rainbow) {
      // 4:04:53 PM — Rainbow: "Okay, I'm gonna sell two cards for two money"
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // 4:05:07 PM — Green: "Think? Oh, I spend three to draw a building card, because I totally
      // need more of those. Okay, it's geothermal power."
      doTasks(*cardActionTasks("$Factorum", "Action2"), "3 Pay<> FROM MC")
      doTask("SearchForCard<TagFilter<Class<BuildingTag>>>").expect("ProjectCard")
      // 4:05:27 PM — Green: "I can indeed cube it. Now, I think the only other thing I'm doing is
      // cloud tourism, so let's do that and I'm done."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
    }
    with(rainbow) {
      // 4:05:41 PM — Rainbow: "Okay. I am paying seven for thermafiles. Okay. And then I guess I
      // will
      // take the action."
      // 4:06:02 PM — Rainbow: "I can add one microbe to any Venus card, or I can spend two microbes
      // from this card tree as Venus one step. Hmm, but I think at this point I'd rather have
      // potential money."
      doTasks(*playCardTasks("$Thermophiles"), "7 Pay<> FROM MC", "$Thermophiles FROM ProjectCard")
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
    }
    green.doTask("Pass")
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("29 MC", "4 Plant", "Heat", "4 Steel", "Energy", "Heat FROM Energy")
    rainbow.doTasks("39 MC", "Titanium", "Energy", "Heat FROM Energy", "Plant")
    blue.doTasks("33 MC", "7 Heat", "10 Energy", "4 Heat FROM Energy", "Plant")
    green.doTasks("29 MC", "Steel", "Heat", "2 Titanium", "Heat FROM Energy")
    // 4:06:32 PM — Yellow: "I think I'm gonna boop the temperature."
    // 4:06:35 PM — Green: "Temp to two."
    yellow.doTask("TemperatureStep! BY Admin")
  }

  private fun generation6() {
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
    yellow.assertCounts(8 to "CardBack")
    rainbow.assertCounts(12 to "CardBack")
    blue.assertCounts(6 to "CardBack")

    // board-16-07-06.jpg and all four app histories: Generation 6 before Research.
    with(yellow) {
      assertProduction(m = 3, s = 4, t = 0, p = 4, e = 1, h = 1)
      assertResources(m = 29, s = 12, t = 0, p = 4, e = 1, h = 7)
      assertCounts(26 to "TerraformRating", 8 to "CardBack")
    }
    with(rainbow) {
      assertProduction(m = 6, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 45, s = 1, t = 2, p = 2, e = 1, h = 1)
      assertCounts(33 to "TerraformRating", 1 to "Tactician", 12 to "CardBack")
      assertCardResources(
          2 to FloatingRefinery,
          1 to SulphurEatingBacteria,
          2 to AppliedScience,
      )
    }
    with(blue) {
      assertProduction(m = 4, s = 0, t = 0, p = 1, e = 10, h = 7)
      assertResources(m = 45, s = 0, t = 0, p = 5, e = 10, h = 13)
      assertCounts(29 to "TerraformRating", 1 to "Landshaper", 6 to "CardBack")
      assertCardResources(4 to NeptunianPowerConsultants)
    }
    with(green) {
      assertProduction(m = 4, s = 1, t = 2, p = 0, e = 0, h = 1)
      assertResources(m = 33, s = 1, t = 2, p = 3, e = 0, h = 9)
      assertCounts(25 to "TerraformRating", 1 to "Diversifier", 9 to "CardBack")
      assertCardResources(2 to Pets, 5 to CloudTourism, 2 to IcyImpactors, 1 to OlympusConference)
    }
    assertSidebar(gen = 6, temp = 2, oxygen = 4, oceans = 6, venus = 8)

    // Entry 105 restores the TR Green had omitted from Diversity Support in Generation 5.
    green.exMachina("TerraformRating")

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // Green initially chose four cards, returned one, and bought three; Yellow bought three.
    // The phone histories charge Rainbow for one card and Blue for two despite their quoted
    // two- and three-card statements. Rainbow's later complete card flow confirms that she kept
    // the two cards she names here; preserve the app's observed 3 M€ payment separately.
    rainbow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.exMachina("3 MC")
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
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

    with(rainbow) {
      // 3:05:43 PM — Rainbow: "Alright. Um I'm gonna play Mining Rights."
      // 3:05:47 PM — Rainbow: "I'm gonna spend my steel as two money."
      // 3:05:52 PM — Rainbow: "And that means I have to pay seven."
      // 3:07:24 PM — Rainbow: "is around it, but that won't give me points anyway. So guess we come
      // here
      // for two steels."
      // 3:07:31 PM — Green: "Alright, one two."
      doTasks(
          *playCardTasks("$MiningRights"),
          "1 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Amazonis_01_02>",
          "Production<Class<Metal>(HAS PlacementBonus<LandArea(HAS MiningRights_SpecialTile)>)>",
          "2 Steel",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // 3:08:57 PM — Blue: "Convoy from Europa. Yeah. So I'll place an ocean tile."
      // 3:09:09 PM — Blue: "Yeah. And I put it here, which is three... One two three four five
      // six."
      // 3:09:29 PM — Green: "Three six for two money."
      doTasks(
          *playCardTasks("$ConvoyFromEuropa"),
          "Ok", // No heat paid.
          "15 Pay<> FROM MC",
          "$ConvoyFromEuropa FROM ProjectCard",
          "ProjectCard",
          "OceanTile<Amazonis_03_06>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>",
          "Ok", // No heat paid.
          "5 Pay<> FROM MC",
          "PROD[Energy]",
          "Hydroelectric<$NeptunianPowerConsultants>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 3:10:13 PM — Green: "Okay. Now is your turn. You can flip your event code either. Seven
      // oceans, huh? I'm going to play decomposers, which costs me full price. Alright minus, we do
      // have the three percent oxygen, barely. Um it gives me an automatic plant, right?"
      doTasks(
          *playCardTasks("$Decomposers"),
          "5 Pay<> FROM MC",
          "$Decomposers FROM ProjectCard",
          "Microbe<$Decomposers>",
          "MC",
      )
      // 3:11:07 PM — Green: "And as my second for my second trick, I'm just going to go ahead and
      // fund the eccentric"
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Excentric")
    }
    with(yellow) {
      // 3:11:18 PM — Yellow: "Um? What was it? Um? Um yes, I'm going to spend seven Master of
      // Mechanica. Choose two project cards from my event pile. Take them into my hand."
      doTasks(
          *playCardTasks("$AstraMechanica"),
          "7 Pay<> FROM MC",
          "$AstraMechanica FROM ProjectCard",
          "ProjectCard FROM PlayedEvent<Class<$Flooding>>",
          "ProjectCard FROM PlayedEvent<Class<$Sabotage>>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:12:52 PM — Rainbow: "Twenty six plus eighteen is a lot more than forty five."
      // 3:12:54 PM — Yellow: "Forty four."
      // 3:12:54 PM — Rainbow: "Damn it. Alright. Um. So I'm gonna pay nine."
      // 3:13:10 PM — Rainbow: "And then I'm going to take my ansaction."
      // 3:13:32 PM — Green: "Do I have a nine plus card in my hand that has a microbe tag on it and
      // is called ants? All right."
      doTasks(*playCardTasks("$Ants"), "9 Pay<> FROM MC", "$Ants FROM ProjectCard")
      doTasks(*cardActionTasks("$Ants"), "-Microbe<Green, $Decomposers<Green>>", "Microbe<$Ants>")
      assertCounts(26 to "MC")
    }

    with(blue) {
      // 3:13:39 PM — Blue: "Okay, I'm gonna convert my heat."
      // 3:13:42 PM — Blue: "Raise temperature."
      // 3:13:45 PM — Blue: "Oh, and I get five money."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 3:14:06 PM — Green: "I'm going to and take my factotum action to get an energy production
      // because I have no energy resources, and then I get two money at each turn."
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // 3:14:17 PM — Yellow: "I'm gonna take my spacciamerones action. Um, spend seven, gain energy
      // apparatus. Rainbow. Mm-hmm."
      doTasks(*cardActionTasks("$SpaceMirrors"), "7 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:14:27 PM — Rainbow: "Um. I'm gonna go ahead and remove a science resource from applied
      // science and put it on sulfur-eating bacteria."
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      doTask("PassBaton")
    }
    with(blue) {
      // 3:14:42 PM — Blue: "Okay. I'm going to play indentured workers, my bad guys, which costs
      // zero money, and it means my next card costs eight less money, and then I'm playing nitrogen
      // rich asteroid."
      doTasks(
          *playCardTasks("$IndenturedWorkers"),
          "$IndenturedWorkers FROM ProjectCard",
          "IndenturedWorkers_NextCardEffect",
      )
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "Ok", // No heat paid.
          "23 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "PROD[Plant]",
          "4 MC",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
    }

    with(green) {
      // 3:15:54 PM — Green: "Okay. Now what I'm gonna do is I'm going to play Power Plant. I'm
      // going
      // to play Which, tragically, in addition to my steel, makes me pay two money."
      // 3:16:13 PM — Green: "It gives me an energy production and gives me back the two money."
      doTasks(
          *playCardTasks("$PowerPlant"),
          "1 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$PowerPlant FROM ProjectCard",
          "PROD[Energy]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:16:16 PM — Blue: "oh thank god um okay it's already working like um capital is By which
      // I mean I pay all twelve of my steel, because I got my cutting edge. They just got um
      // decrease energy production by two, increase money production by five. Wow, finally eight
      // thingies. Um place the tile."
      doTasks(
          *playCardTasks("$Capital"),
          "12 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$Capital FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[5 MC]",
          "CapitalTile<Amazonis_07_03>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("NOW Rainbow { 2! MC<Rainbow> }")
      rainbow.doTask("2 MC")
      doTasks("2 Plant", "2 MC", "2 MC")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:17:57 PM — Rainbow: "To build fish and And then I'll take the action and add a animal to
      // the card"
      // 3:17:58 PM — Green: "Oh fish. Oh, you got to remove somebody's plant production. That
      // could be [Blue]'s or [Yellow]'s. [Yellow] has four. [Blue]'s two."
      // 3:18:12 PM — Rainbow: "It's gonna be [Yellow]'s I was gonna say oh I haven't I haven't
      // gotten
      // enough plants I had to s build my own greenery this whole entire day, at least I'll take
      // mine."
      doTasks(
          *playCardTasks("$Fish"),
          "9 Pay<> FROM MC",
          "$Fish FROM ProjectCard",
          "PROD[-Plant<Yellow>]",
      )
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      assertCounts(19 to "MC")
    }
    with(blue) {
      // 3:18:48 PM — Blue: "Uh-huh. I guess I will play water splitting plant."
      // 3:19:10 PM — Blue: "Um Mm-hmm. So yeah and that cost me twelve money."
      doTasks(
          *playCardTasks("$WaterSplittingPlant"),
          "Ok", // No heat paid.
          "12 Pay<> FROM MC",
          "$WaterSplittingPlant FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 3:19:13 PM — Green: "Yep. Yep. And what I'm uh gonna do Whole business network which only
      // cost me one and so I lower my mega credit production once more"
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "1 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:19:42 PM — Rainbow: "Yeah. Okay, um... I'm gonna play flooding for seven. an ocean?"
      // 3:20:22 PM — Blue: "Yes. Two money, three heat."
      // 3:20:42 PM — Blue: "However, I actually can't do that."
      // No opposing tile bordered the selected ocean.
      doTasks(
          *playCardTasks("$Flooding"),
          "7 Pay<> FROM MC",
          "$Flooding FROM ProjectCard",
          "OceanTile<Amazonis_06_06>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks("2 Plant", "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }")
      blue.doTask("3 Pay<Class<Heat>> FROM Heat")
      doTask("NOW Blue { 2 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("2 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:21:41 PM — Rainbow: "I'm taking my thermophiles action to add a microbe to sulphur eating
      // bacteria. I'm going to take my floater and find a reaction to remove two floaters and gain
      // one titanium and two money. And that's two actions."
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      assertCounts(21 to "MC")
    }
    // 3:22:14 PM — Blue: "Okay, um, I pass."
    // 3:22:18 PM — Green: "I'm probably going to sell a card. [Yellow]'s turn."
    // 3:22:27 PM — Blue: "Oh, hmm. I forgot about my water splitting plan actually."
    // 3:22:33 PM — Blue: "Yeah. Oh yeah, are you okay? I'll spend three energy,"
    // 3:22:35 PM — Green: "She's retroactively using three energy to raise the oxygen to five."
    // 3:22:39 PM — Blue: "raise the oxygen to five, which gives me two money and a TR."
    with(blue) {
      // Blue announced the pass first, then immediately remembered and took this still-legal action
      // before ending the turn.
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 3:22:51 PM — Green: "And I still sell a patent."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // 3:22:53 PM — Yellow: "Okay, um... You know what? Green, I'm gonna sabotage you to lose
      // seven
      // money."
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-7 MC<Green>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:23:34 PM — Rainbow: "I'm spending eleven money on cloud seating. I decrease my money
      // production one step. [Blue]'s heat production one step."
      // 3:23:48 PM — Rainbow: "I decrease your heat production one step."
      // 3:23:52 PM — Rainbow: "I'm so sad. I decrease my plant production two steps."
      doTasks(
          *playCardTasks("$CloudSeeding"),
          "11 Pay<> FROM MC",
          "$CloudSeeding FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
          "PROD[-Heat<Blue>]",
      )
      assertCounts(10 to "MC")
      doTask("PassBaton")
    }
    // 3:24:09 PM — Yellow: "My pass."
    // 3:24:09 PM — Green: "No, you passed. That's right. Alright, um now I'm going to sell a card.
    // At least turn."
    // Blue's announced pass takes effect on this next legal Blue turn because the retroactive Water
    // Splitting action above occupied the turn in which she first announced it.
    blue.doTask("Pass")

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(rainbow) {
      // 3:25:18 PM — Rainbow: "So I'm using, I'm doing my sulfur eating bacteria action, turning
      // three bacteria into nine money."
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-3 Microbe<$SulphurEatingBacteria>",
          "9 MC",
      )
      assertCounts(19 to "MC")
      doTask("PassBaton")
    }

    with(green) {
      // 3:25:33 PM — Green: "Am gonna play nitrophilic moss Meets the requirement, I lose two
      // plants, gain two plant production which gives me two money, and then I get a decompositor
      // which gives me one more money. Did I pay for that? I did not. I did not pay for that, which
      // is really weird. Because I exactly meant to pay for it."
      doTasks(
          *playCardTasks("$NitrophilicMoss"),
          "8 Pay<> FROM MC",
          "$NitrophilicMoss FROM ProjectCard",
          "PROD[2 Plant]",
          "-2 Plant",
          "Microbe<$Decomposers>",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
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

    with(rainbow) {
      // 3:27:50 PM — Rainbow: "Um, I am. I'm gonna pay two steel acting as four money plus eleven
      // money. For a lava tube settlement, I decrease my energy production, I increase my money
      // production by two, and I place a city tile on a volcanic area."
      // The 11 M€ cash payment and the city's 4 M€ of ocean-adjacency income resolve in one task.
      doTasks(
          *playCardTasks("$LavaTubeSettlement"),
          "2 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$LavaTubeSettlement FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "CityTile<Amazonis_07_11>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTasks("2 MC", "2 MC")
      assertCounts(12 to "MC")
      doTask("PassBaton")
    }
    with(green) {
      // 3:28:52 PM — Green: "Um, now I'm going to use my business network to look at a shitty
      // card."
      // 3:29:09 PM — Green: "I have no prospects of being able to meet this requirement. I do not
      // buy it. Back to you, [Rainbow]."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTask("PassBaton")
    }

    with(rainbow) {
      // 3:29:23 PM — Rainbow: "I'm gonna sell two patents."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    with(green) {
      // 3:29:27 PM — Green: "Okay. I'm going to be a cloud tourist. Back to you."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    rainbow.doTask("Pass")
    with(green) {
      // 3:29:37 PM — Green: "I am going to use Icy Impactors, which is a strange card. I lose an
      // asteroid here. I get to place an ocean tile, but Rainbow gets to tell me where I have to
      // put
      // it. Which sounds weird when I put it that way. Because you're the start player. Start
      // player chooses where you must place it. I'll tell you where to place it."
      // 3:30:14 PM — Green: "Yep on one two, but that's still it's two one which still gets me a TR
      // Oceans are at nine [Blue] you have your option"
      doTasks(
          *cardActionTasks("$IcyImpactors", "Action2"),
          "-Asteroid<$IcyImpactors>",
          "EACH Starter@Player(HAS StartToken) { ChooseOceanArea<Starter@Player, Green>! }",
          "NOW Rainbow { OceanTile<WaterArea(HAS MAX 0 Tile)>* BY Green }",
      )
      rainbow.doTask("OceanTile<Amazonis_02_01> BY Green")
      doTasks(
          "OceanTile<Amazonis_02_01>",
          "TerraformRating",
          // Green chooses when Blue receives the optional Neptunian bonus.
          "NOW Blue { UseAction<Blue, NeptunianOption>? }",
      )
      blue.doTask("0 UseAction<NeptunianOption<$NeptunianPowerConsultants>>")
    }
    green.doTask("Pass")

    // Production.
    yellow.doTasks("35 MC", "3 Plant", "Heat", "4 Steel", "Heat FROM Energy")
    rainbow.doTasks("40 MC", "Titanium", "Heat FROM Energy", "3 Plant", "Steel")
    blue.doTasks("39 MC", "6 Heat", "12 Energy", "7 Heat FROM Energy", "2 Plant")
    green.doTasks("32 MC", "Steel", "Heat", "2 Titanium", "2 Energy", "2 Plant")
    // 3:30:39 PM — Green: "Um is there anything else we can do? I don't think so. I think I used
    // all my stuff. Oh, yeah, shuttles I can't afford. Gotta stop buying things I can't afford.
    // Alright, so I pass, so we hit generation plus, and then Rainbow chooses where to what kind of
    // world government operation to do."
    // 3:31:01 PM — Rainbow: "Venus it."
    // 3:31:02 PM — Green: "Venus to 10"
    rainbow.doTask("VenusStep! BY Admin")
  }

  private fun generation7() {
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
          1 to AppliedScience,
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // 3:31:43 PM — Green: "This time we pass to the right."
    // 3:35:00 PM — Rainbow: "The thing is that... Um, I think I'll buy one card."
    // 3:35:10 PM — Green: "I will stupidly buy three cards."
    // 3:35:17 PM — Blue: "I only buy two cards."
    // 3:35:17 PM — Rainbow: "Wait, I only have three cards in my hand. Thank you."
    // 3:36:40 PM — Rainbow: "I want them both, I want everything, but I'm only keeping two."
    blue.doTasks(
        "-2 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(blue) {
      // 3:36:54 PM — Blue: "I mean I guess for my first action I can just go ahead and convert heat
      // to attempt to beat."
      // 3:37:28 PM — Blue: "I get five money."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "3 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 3:37:59 PM — Green: "Do something basic first. I'll use factotum to spend three and get a
      // William card. Noctis city."
      doTasks(*cardActionTasks("$Factorum", "Action2"), "3 Pay<> FROM MC")
      doTask("SearchForCard<TagFilter<Class<BuildingTag>>>").expect("ProjectCard")
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:38:47 PM — Blue: "Yeah Yeah get two TRs Um the oshkin will go um here for two um plantis
      // and two monies."
      // 3:38:47 PM — Green: "Temp's on plus 12 now."
      // 3:39:06 PM — Blue: "I will spend five money to increase my energy production and add a
      // Neptunian power cell scent."
      // 3:39:13 PM — Blue: "Also, [Blue], you lose three plants."
      // 3:39:21 PM — Green: "Sorry, that was on 6.5."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *playCardTasks("$Comet"),
          "21 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "TerraformRating",
          "OceanTile<Amazonis_06_05>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks("2 Plant", "2 MC", "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }")
      blue.doTask("Ok") // No heat paid.
      doTask("NOW Blue { 5 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("5 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      doTask("-3 Plant<Blue>")
      // Yellow did not physically pay Comet's 21 M€ at this play. Restore the engine-enforced
      // payment until the recorded correction at 3:46:46 PM.
      exMachina("21 MC")
    }
    with(rainbow) {
      // 3:39:37 PM — Rainbow: "Okay, your turn. Oh yeah. All right, so I'm gonna, I'm gonna kind of
      // hurry here to build my Mahole Lake."
      // 3:39:54 PM — Rainbow: "you e I guess um well yes, but minus one steel."
      // 3:39:59 PM — Rainbow: "So twenty nine"
      // 3:40:15 PM — Rainbow: "Yes. I haven't given myself any T_R_ yet. So two goops um I guess.
      // hit the last temp. Okay. There you go, yeah. I guess I'll take there. Yeah, for monies and
      // a
      // card."
      // 3:40:34 PM — Green: "Five five for four money and a card."
      doTasks(
          *playCardTasks("$MoholeLake"),
          "1 Pay<Class<Steel>> FROM Steel",
          "29 Pay<> FROM MC",
          "$MoholeLake FROM ProjectCard",
          "TemperatureStep",
          "3 Plant",
          "TerraformRating",
          "OceanTile<Amazonis_05_05>",
          "TerraformRating",
          "NOW Blue { UseAction<Blue, NeptunianOption<$NeptunianPowerConsultants<Blue>>>? }",
      )
      blue.doTask("UseAction<NeptunianOption<$NeptunianPowerConsultants>, Action1>")
      doTasks(
          "ProjectCard",
          "2 MC",
          "2 MC",
          "NOW Blue { X Pay<Blue, Class<Heat>> FROM Heat<Blue> OR Ok }",
      )
      blue.doTask("Ok") // No heat paid.
      doTask("NOW Blue { 5 Pay<Blue> FROM MC<Blue> }")
      blue.doTask("5 Pay<> FROM MC")
      doTask("NOW Blue { PROD[Energy<Blue>] }")
      blue.doTask("PROD[Energy]")
      doTask("NOW Blue { Hydroelectric<$NeptunianPowerConsultants<Blue>>* }")
      blue.doTask("Hydroelectric<$NeptunianPowerConsultants>")
      // 3:41:04 PM — Rainbow: "Well, my second action, I will greener eyes."
      // 3:41:14 PM — Rainbow: "I'm I'm putting it there so I get another plant and four money."
      // 3:41:24 PM — Green: "That is on 610."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_10>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }
    with(blue) {
      // 3:41:25 PM — Blue: "Yeah, well I better do this while I can. I'm gonna play Domed Crater,
      // which cost me 24 monies."
      // 3:41:36 PM — Blue: "We're domed. And so I gained three plants"
      // 3:41:49 PM — Blue: "I'm going to put it right here, which is 3-7?"
      // 3:41:58 PM — Blue: "Yeah, 3-7. And I get a plant."
      // 3:42:07 PM — Blue: "Oh yeah, and for money. I didn't even think about that. And then, oh
      // yeah, minus an energy production plus three money productions."
      doTasks(
          *playCardTasks("$DomedCrater"),
          "Ok", // No heat paid.
          "24 Pay<> FROM MC",
          "$DomedCrater FROM ProjectCard",
          "3 Plant",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Amazonis_03_07>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("NOW Rainbow { 2! MC<Rainbow> }")
      rainbow.doTask("2 MC")
      doTasks("2 MC", "2 MC")
      // board-14-23-07.jpg shows a plant at 3-7 on the mat used in this game.
      exMachina("Plant")

      // 3:42:19 PM — Blue: "And for my second action I'm going to convert my plants to a greenery."
      // 3:42:58 PM — Blue: "Three eight. Get in there. There we go. Three eight and I get two
      // plants. Okay. Oh, I should probably do... Yeah."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_03_08>",
          "OxygenStep",
          "TerraformRating",
          "ProjectCard",
          "2 MC",
      )
      // The same photograph shows two plants at 3-8.
      exMachina("2 Plant")
    }
    // Blue's app has no 2 M€ Terraforming Deal response to this greenery's oxygen step:
    // entry 178 leaves 10 M€, and the 15:57 dashboard plus entry 188 production both leave
    // 54 M€. Preserve the omitted response at the action that caused it.
    blue.exMachina("-2 MC")
    with(green) {
      // 3:43:13 PM — Green: "I'm going to use my business network. No, better not."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 3:43:48 PM — Blue: "Well, still may as well, um, plant forest."
      // 3:44:15 PM — Rainbow: "Um here for the plant and for money."
      // 3:44:20 PM — Green: "Plant and for money. Is that um eight three?"
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_08_03>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      // 3:44:36 PM — Blue: "For a card hope this one's useful."
      // 3:44:38 PM — Green: "11 11 so"
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_11_11>",
          "OxygenStep",
          "ProjectCard",
          "TerraformRating",
      )
    }
    with(rainbow) {
      // 3:45:19 PM — Rainbow: "Yeah, I'm gonna place my floater."
      // 3:45:23 PM — Green: "Okay, so first action to place on floating refinery."
      doTasks(*cardActionTasks("$FloatingRefinery"), "Floater<$FloatingRefinery>")
      // 3:45:26 PM — Green: "Then second action spends it."
      // 3:45:28 PM — Rainbow: "Spending my ten."
      // 3:45:30 PM — Rainbow: "Yes, striped spherical birds and taking my floater off in some clown
      // finery."
      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "10 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater<$FloatingRefinery>",
      )
    }
    // 3:45:43 PM — Blue: "I'm gonna pass."
    blue.doTask("Pass")

    with(green) {
      // 3:45:50 PM — Green: "Mm 'kay. [Blue] passes. I will use cloud tourism, [Yellow]."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    with(yellow) {
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
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "10 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "EcologicalZone_SpecialTile<Amazonis_04_08>",
          "Plant",
      )
      doTask("PassBaton")
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
    with(rainbow) {
      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Microbe<$SulphurEatingBacteria>",
      )
      assertCardResources(2 to SulphurEatingBacteria)

      exMachina("-Microbe<$SulphurEatingBacteria>")

      assertCardResources(1 to SulphurEatingBacteria)

      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
      assertCardResources(2 to SulphurEatingBacteria)
    }

    with(green) {
      // 3:49:07 PM — Green: "Then I should know what to do. So I'm going to play Venus Magnetizer."
      // 3:49:24 PM — Green: "It costs me seven."
      doTasks(
          *playCardTasks("$VenusMagnetizer"),
          "7 Pay<> FROM MC",
          "$VenusMagnetizer FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // 3:49:37 PM — Yellow: "Well, I now believe I can pave seven cutting edge again for insects
      // since the oxygen's finally gone to six percent. Increase plant production one step for each
      // plant tag I have. One, two, three, four."
      doTasks(
          *playCardTasks("$Insects"),
          "7 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[4 Plant]",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 3:50:05 PM — Rainbow: "Um, you got some tasty microbes there, [Green]?"
      // 3:50:09 PM — Rainbow: "My ants are gonna eat them. Thank you."
      doTasks(*cardActionTasks("$Ants"), "-Microbe<Green, $Decomposers<Green>>", "Microbe<$Ants>")
      doTask("PassBaton")
    }
    with(green) {
      // 3:50:22 PM — Yellow: "I think I'm gonna build Cupola City, which is, it's a city, you get
      // two more."
      // 3:50:30 PM — Green: "And I get a pet, but we should do things in order here. I should pay
      // full price. And I lose an energy production, gain three money production, which gives me
      // two money. I place a shitty tile. My first. I'm not on the board at all."
      // 3:51:10 PM — Yellow: "Six four. For two plants and four money."
      doTasks(
          *playCardTasks("$CupolaCity"),
          "Ok", // No steel paid.
          "16 Pay<> FROM MC",
          "$CupolaCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
          "CityTile<Amazonis_06_04>",
          "Animal<$Pets>",
          "NOW Rainbow { 2! MC<Rainbow> }",
      )
      rainbow.doTask("2 MC")
      doTasks("2 Plant", "2 MC", "2 MC")
      // 3:51:29 PM — Green: "Let's just go ahead and use Venus magnetizer decrease in energy
      // production to raise Venus to 12% and get a TR. All right, who's still in?"
      doTasks(*cardActionTasks("$VenusMagnetizer"), "PROD[-Energy]", "VenusStep", "TerraformRating")
    }
    with(yellow) {
      // 3:51:57 PM — Yellow: "Actually, two. And I will play Smanimals for the four money. Requires
      // 6% oxygen. Yep. Decrease any plant production one step. Man, I'm good."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      // 3:52:17 PM — Green: "Rainbow's got three, I've got two."
      // 3:52:33 PM — Yellow: "Yeah, I'll decrease Rainbow's."
      doTasks(
          *playCardTasks("$SmallAnimals"),
          "4 Pay<> FROM MC",
          "$SmallAnimals FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "PROD[-Plant<Rainbow>]",
      )
    }
    with(rainbow) {
      // 3:52:43 PM — Rainbow: "Okay, turn. Um, well, how many space tags do you collectively have,
      // four
      // or five?"
      // 3:52:59 PM — Rainbow: "Yeah. I'm going to pay ten to build the toll station, which gives me
      // six money production."
      doTasks(
          *playCardTasks("$TollStation"),
          "Ok", // No titanium paid.
          "10 Pay<> FROM MC",
          "$TollStation FROM ProjectCard",
          "PROD[6 MC]",
      )
      doTask("PassBaton")
    }
    // 3:53:47 PM — Green: "All right, I pass. You're still up. [Yellow], just [Yellow]?"
    green.doTask("Pass")
    with(yellow) {
      // 3:53:58 PM — Blue: "I'm still in. Bio, ask combustors for two steel. Wait, no, no, no, one
      // steel. Requires six percent oxygen. Decrease any plant production one step. That'll be
      // [Blue]."
      doTasks(
          *playCardTasks("$BiomassCombustors"),
          "1 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$BiomassCombustors FROM ProjectCard",
          "PROD[2 Energy]",
          "PROD[-Plant<Blue>]",
      )
      doTask("PassBaton")
    }

    with(rainbow) {
      // 3:54:43 PM — Rainbow: "That was hot. Um, I'm going to take my stratospheric's earth action
      // to
      // add an animal to the bird card."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }
    // 3:55:00 PM — Rainbow: "Okay. Um you guys have both passed?"
    // 3:55:03 PM — Yellow: "Yeah."
    // 3:55:03 PM — Blue: "Yeah."
    yellow.doTask("Pass")
    with(rainbow) {
      // 3:55:12 PM — Rainbow: "I can't believe I let Predators go. Held it for so long just in
      // case.
      // Um I'm going to take my fish action and add a fish to the card. And then for Mahomet Lake.
      // gonna add a bird to the stratospheric birds card."
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$StratosphericBirds>")
    }
    // 3:55:35 PM — Rainbow: "Uh, I pass."
    with(rainbow) {
      // Rainbow paid 8 M€ for Miranda Resort without titanium. The table counted her wild tag as
      // Earth while resolving the card, then corrected the resulting production increase to two.
      doTasks(
          *playCardTasks("$MirandaResort"),
          "Ok", // No titanium paid.
          "8 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[2 MC]",
      )
    }
    // 3:57:14 PM — Rainbow: "Alright, I'm also going to pass."
    rainbow.doTask("Pass")

    // Production.
    yellow.doTasks("40 MC", "7 Plant", "Heat", "4 Steel", "2 Energy")
    rainbow.doTasks("51 MC", "Titanium", "2 Plant", "Steel")
    blue.doTasks("44 MC", "6 Heat", "13 Energy", "12 Heat FROM Energy", "Plant")
    green.doTasks("36 MC", "Steel", "Heat", "2 Titanium", "2 Plant", "2 Heat FROM Energy")
    // 3:57:20 PM — Green: "Okay, so we hit plus generation, and then [Blue] chooses about the world
    // governing."
    // 3:57:23 PM — Blue: "Mm-hmm. World government. Um I guess I'm gonna do Venus."
    // 3:57:30 PM — Green: "Venus to fourteen."
    blue.doTask("VenusStep! BY Admin")
  }

  private fun generation8() {
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
    rainbow.assertCounts(54 to "MC", 0 to "Science<$AppliedScience>")

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // 5:31:27 PM — Yellow: "I think I'm gonna buy two cards. My butt hurts. Go up."
    // 5:31:46 PM — Blue: "by one card."
    // 5:32:08 PM — Green: "Okay. Buying three cards."
    // 5:32:36 PM — Rainbow: "Your turn. Oh, wait, I need to I need to actually buy two cards so
    // that A_I_ knows that I need to buy two. Yeah. Um I have bought two cards. It is also stupid
    // of
    // me to buy two, but whatever."
    // Research is submitted below in the game's order; the quotes remain in recording order.
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    // Generation 8 did not follow strict physical rotation: Blue rescinded a pass and Rainbow
    // continued after announcing one. The following legal interleave preserves every player's own
    // action order; neither premature pass remained effective.
    with(green) {
      // 5:33:24 PM — Green: "I'm actually going to play tardiggradees."
      // 5:33:55 PM — Green: "I do not have discounts, so it cost me four entire money.
      // Decomposers gets a microbe from Rainbow and that gives me a money."
      // 5:34:11 PM — Green: "tardigrades? That didn't do what I thought it was going to do. I keep
      // thinking that I had viral enhancers."
      doTasks(
          *playCardTasks("$Tardigrades"),
          "4 Pay<> FROM MC",
          "$Tardigrades FROM ProjectCard",
          "Microbe<$Decomposers>",
          "MC",
      )
      // 5:34:30 PM — Green: "Uh okay, that turned out to be pointless. But now I'm going to use
      // business network to look at this card."
      // 5:34:46 PM — Green: "God, it's so expensive. I think not."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
    }
    with(yellow) {
      // 5:35:06 PM — Yellow: "I'm gonna pay 14 to fund the Highlander. That's the most tiles not
      // next to oceans, which I got. Yes, dry boys."
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Highlander")
      // 5:35:21 PM — Yellow: " And actually, just in case anyone has any ideas, I'm gonna plant
      // forests. Yeah. Um I'll go here for a plant."
      // 5:35:39 PM — Green: "Seven two."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_02>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
    }
    with(rainbow) {
      // 5:35:54 PM — Rainbow: "Got actions to take. I'm going to add one microbe to self-reeding
      // bacteria. Uh"
      doTasks(*cardActionTasks("$SulphurEatingBacteria"), "Microbe<$SulphurEatingBacteria>")
      // 5:36:14 PM — Rainbow: "Oh, you did it at the oxygen's ten. Okay, thank you. Um also I'm
      // going to take my thermophiles action to add one microbe to sulfur eating bacteria."
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
    }
    with(blue) {
      // 5:36:37 PM — Blue: "Okay. I really hate that this is gonna cost me so much money, but I'm
      // gonna spend twenty bucks to fund Thermalist."
      doTasks(
          "UseAction<FundAwardAction, Action1>",
          "Ok", // No heat paid.
          "20 Pay<> FROM MC",
          "Thermalist",
      )
      // 5:36:58 PM — Blue: "And I'm going to um Spend 15 bucks on a plantation."
      // 5:37:10 PM — Blue: "Which I'm gonna put If I go here, I get a resource, right?"
      // 5:37:16 PM — Green: "A standard resource."
      // 5:37:48 PM — Green: "Alright, so you went to five nine."
      // 5:37:58 PM — Blue: "Oxygen is now at eleven, I gain a TR and for gaining a TR I get
      // two money. And for placing next to an ocean I get two money."
      // 5:38:08 PM — Blue: "And I decided that my resource was gonna be a titanium."
      doTasks(
          *playCardTasks("$Plantation"),
          "Ok", // No heat paid.
          "15 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "GreeneryTile<Amazonis_05_09>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "2 MC",
          "Titanium",
      )
    }

    with(green) {
      // 5:38:13 PM — Green: "Okay. Very cool. I will take my factotum action."
      // 5:38:18 PM — Green: "I have no energy resources so I can get an energy production and that
      // gives me two money from sweetable infrastructure."
      doTasks(
          *cardActionTasks("$Factorum"),
          "PROD[Energy]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 5:38:22 PM — Yellow: "Oh, okay. I will sponsor some Academy."
      // 5:38:29 PM — Yellow: "Yeah. Hope it doesn't give you useful stuff. Pay nine. I pitch a
      // card."
      // 5:38:36 PM — Green: "You get three, you get one, you get one, I get one."
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Yellow) { ProjectCard<Other@Player, Hand>! }",
          "3 ProjectCard",
          "ProjectCard<Rainbow, Hand>",
          "ProjectCard<Blue, Hand>",
          "ProjectCard<Green, Hand>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 5:39:42 PM — Rainbow: "I'm gonna do gene repair which costs me 12 monies"
      // 5:40:23 PM — Rainbow: "Uh but gives me too money production"
      doTasks(
          *playCardTasks("$GeneRepair"),
          "12 Pay<> FROM MC",
          "$GeneRepair FROM ProjectCard",
          "PROD[2 MC]",
      )
      // 5:40:40 PM — Rainbow: "Oh, but [Yellow] doesn't have microbes, right. Well, I'm gonna take
      // your tasty microbe then, please. My ants are going to remove a microbe from your whatever."
      doTasks(*cardActionTasks("$Ants"), "-Microbe<$Decomposers<Green>>", "Microbe<$Ants>")
    }
    with(blue) {
      // 5:42:17 PM — Blue: "I am playing Soletta,
      // yeah, so that's gonna cost me So one titanium, twenty mega credits and eleven heat. But I
      // get seven heat production. Noice. Okay."
      doTasks(
          *playCardTasks("$Soletta"),
          "11 Pay<Class<Heat>> FROM Heat",
          "1 Pay<Class<Titanium>> FROM Titanium",
          "20 Pay<> FROM MC",
          "$Soletta FROM ProjectCard",
          "PROD[7 Heat]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 5:43:41 PM — Green: "Um, Venus magnetizer."
      // 5:43:44 PM — Green: "Reduce energy production."
      // 5:43:46 PM — Blue: "It is at sixteen percent."
      // 5:43:47 PM — Green: "Raise Venus to sixteen and get nothing."
      doTasks(*cardActionTasks("$VenusMagnetizer"), "PROD[-Energy]", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }
    with(yellow) {
      // 5:43:49 PM — Yellow: "Go up. All right, um... Business contactos for... Seven. Crap. I got
      // a bunch of junk. Alright, actually these might"
      // The transcript diarizes this as Blue, but Yellow is the player due here; Business Contacts
      // is also the missing fifth Yellow event in the table's 6:00:28 PM Media Archives tally.
      // Yellow's app combines this 7 M€ payment and Sponsored Academies' 9 M€ payment into its
      // single entry 209 debit of 16 M€.
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
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
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      // After buying energy production, Rainbow legally played Maxwell Base for 16 M€, reduced
      // that production, and placed its city; the city also triggered Green's Pets.
      doTasks(
          *playCardTasks("$MaxwellBase"),
          "16 Pay<> FROM MC",
          "$MaxwellBase FROM ProjectCard",
          "PROD[-Energy]",
          "NormalCityTile<MaxwellBase_RemoteArea>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("2 MC")
    }
    with(blue) {
      // 5:47:44 PM — Green: "You don't want to raise the oxygen?"
      // 5:47:47 PM — Blue: "But then the game would end."
      // 5:47:49 PM — Green: "Not for another seven things, but you get a T.R. You don't want a
      // T.R.? Do"
      // 5:48:09 PM — Blue: "I will take my water splitting plant action."
      // 5:48:13 PM — Blue: "I'll take my water splitting plant action, spend three energy to
      // raise oxygen one step."
      // 5:48:19 PM — Blue: "And I gain a TR and I gain two money."
      doTasks(
          *cardActionTasks("$WaterSplittingPlant"),
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 5:48:23 PM — Green: "I play foosion power for four worth of steel and ten real and"
      // 5:48:33 PM — Green: "one two three energy production which gives me two money and a science
      // tag removes my only science resource from Olympus conference and gives me this guy it's
      // actually reasonable That's actually useful in this circumstance, I don't believe it."
      doTasks(
          *playCardTasks("$FusionPower"),
          "2 Pay<Class<Steel>> FROM Steel",
          "10 Pay<> FROM MC",
          "$FusionPower FROM ProjectCard",
          "PROD[3 Energy]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
          "ProjectCard FROM Science<$OlympusConference>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 5:49:01 PM — Yellow: "Wait. Actually, remembering myself, I, um, oh, I shouldn't forget to
      // use smanimals."
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 5:50:11 PM — Rainbow: "So Venus way station should be nine, but I have a
      // minus two for and a minus two for a thing, so it's five."
      doTasks(
          *playCardTasks("$VenusWaystation"),
          "Ok", // No titanium paid.
          "5 Pay<> FROM MC",
          "$VenusWaystation FROM ProjectCard",
      )
      // 5:51:04 PM — Rainbow: "I'm gonna build local shading for free."
      doTasks(*playCardTasks("$LocalShading"), "$LocalShading FROM ProjectCard")
    }
    with(blue) {
      // 5:51:29 PM — Blue: "No, I don't pass. I play project inspection. Use a card action that
      // has already been used this generation. I use my water splitting plant action again. Uh And
      // then I spend three more energy, raise the oxygen one more step, and I gain one more T_R_
      // and
      // two more money."
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$WaterSplittingPlant, Action1>",
          "3 Pay<Class<Energy>> FROM Energy",
          "OxygenStep",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 5:51:51 PM — Green: "What I'm a gonna do is play directed heat usage, which
      // costs me one"
      doTasks(
          *playCardTasks("$DirectedHeatUsage"),
          "1 Pay<> FROM MC",
          "$DirectedHeatUsage FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 5:53:57 PM — Yellow: "Dos patentes."
      // 5:54:03 PM — Yellow: "So, dos patentes. Um, beep."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      // 5:54:07 PM — Yellow: "And play for six, um, Lightning Harvest."
      // 5:54:15 PM — Yellow: "I, um, gain energy product, gain money product. Your turn, Rainbow."
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "6 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
      )
    }
    with(rainbow) {
      // 5:54:29 PM — Rainbow: "Floating refinery action, I'm putting a floater on the card."
      // 5:54:37 PM — Rainbow: "Local shading action, I am putting a floater on that card."
      doTasks(*cardActionTasks("$FloatingRefinery"), "Floater<$FloatingRefinery>")
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
    }
    // Blue takes no further sourced action this generation.
    blue.doTask("Pass")

    with(green) {
      // 5:54:45 PM — Green: "Okay. Um So now what I'm going to do is use directed heat usage to
      // spend three useless heat and get two useful plants and then plant forest which raises
      // oxygen to 14 and it's going to go At five for plant and for money. Those are my two
      // actions."
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "2 Plant")
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_04>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }
    // 5:55:38 PM — Yellow: "Uh-huh. Um, I was gonna pass."
    yellow.doTask("Pass")
    with(rainbow) {
      // 5:55:50 PM — Rainbow: "I got actions. I'm going to take my Maxwell base action, add one
      // resource to another Venus card. And I guess I'm going to do that to the floating refinery."
      doTasks(*cardActionTasks("$MaxwellBase"), "Floater<$FloatingRefinery>")
      // 5:56:07 PM — Rainbow: "mm-hmm and then add a bird to Strato birds"
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
    }

    with(green) {
      // 5:56:18 PM — Green: "I will add a floater to Cloud Tourism."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 5:56:27 PM — Rainbow: "back to me because it passed yeah Okay, and we'll add a fish to fish
      // and I will take the action from mohole lake to add another fish again."
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$Fish>")
    }

    with(green) {
      // 5:56:54 PM — Green: "I will use tardigrades to put a tardigrade on to a ridic dicrid's and
      // I get the money, back to you."
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>", "MC")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 5:57:11 PM — Rainbow: "Fine, I'm going to pay my last six money for greenhouses. Oh wait, I
      // have no."
      // 5:57:25 PM — Blue: "You have a steal if you want to use it."
      // 5:57:28 PM — Rainbow: "Thank you, you want to use it. So giving myself two money Yeah. and
      // taking away one steal. Okay, thank you. One plant for each city tile in play. So I believe
      // that is one, two, three, four, five, six, seven."
      doTasks(
          *playCardTasks("$Greenhouses"),
          "1 Pay<Class<Steel>> FROM Steel",
          "4 Pay<> FROM MC",
          "$Greenhouses FROM ProjectCard",
          "7 Plant",
      )
      // 5:57:51 PM — Rainbow: "That was my first thing this time, right? And so I'm going to do a
      // plant forest action."
      // 5:57:57 PM — Green: "oxygen to 15."
      // 5:57:58 PM — Rainbow: "Yeah. Oops. And where's my forest going to go? I guess right here
      // between my city and my other forest."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_10>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
    }

    // 5:58:11 PM — Green: "So one plant Seven, ten. All right. gonna. How many use Venus shuttles,
    // which I've like never done yet. How many Venus tags do I have? One, two, three, four, five,
    // six. Venus shuttles says I pay only six to raise Venus."
    with(green) {
      // 5:59:05 PM — Blue: "All right. To 18%."
      doTasks(*cardActionTasks("$VenusShuttles"), "6 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }
    // 5:59:10 PM — Green: "Okay, and that gives me a turn. Back to you, [Rainbow]. Let's play water
    // to Venus for three titanium, no discounts. Raise Venus to 20%, get a TR. I'm going to play
    // air
    // scrapping exhibition. For thirteen money. No discounts."
    rainbow.doTask("Pass")

    with(green) {
      doTasks(
          *playCardTasks("$WaterToVenus"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$WaterToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      // 6:00:11 PM — Green: "I get three cloud tours."
      // 6:00:18 PM — Green: "Uh and I raise Venus to twenty two which gives me two TR."
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "13 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "3 Floater<$CloudTourism>",
      )
    }
    with(green) {
      // 6:00:28 PM — Green: "I flip these. I play media archives. It costs me five money. How many
      // event cards has everybody played? Two?"
      // 6:00:37 PM — Yellow: "Uh five."
      // 6:00:38 PM — Blue: "Five."
      // 6:00:39 PM — Green: "And five is seven and five is twelve and four is sixteen."
      // Business Contacts establishes the previously missing fifth Yellow event. The table's
      // spoken 2 + 5 + 5 + 4 tally is therefore exact, and Media Archives pays for sixteen events
      // without an adjustment.
      doTasks(
          *playCardTasks("$MediaArchives"),
          "5 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "16 MC",
      )
      assertCounts(5 to "PlayedEvent<Yellow>")

      doTask("Pass")
    }

    // Production.
    yellow.doTasks("42 MC", "7 Plant", "Heat", "4 Steel", "3 Energy", "2 Heat FROM Energy")
    rainbow.doTasks("54 MC", "Titanium", "2 Plant", "Steel")
    blue.doTasks("47 MC", "13 Heat", "13 Energy", "7 Heat FROM Energy", "Plant")
    green.doTasks("40 MC", "Steel", "Heat", "2 Titanium", "2 Plant", "3 Energy")
    // 6:01:45 PM — Green: "all right I pass it's done yeah I'm sad we hit generation nine I choose
    // what to world government and I'll just um I'm gonna be the last to play and at least echo
    // line I was gonna move the oxygen oxygen at 16 start player marker moves"
    green.doTask("OxygenStep! BY Admin")
  }

  private fun generation9() {
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

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    rainbow.doTask("4 ProjectCard<Selecting>")
    blue.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // 6:06:19 PM — Blue: "Pigeon, I'm I'm buying two cards. I'm by one card."
    // 6:06:32 PM — Green: "I would buy one card."
    // All four app histories supply the otherwise unspoken Yellow and Rainbow counts. Research is
    // submitted in the game's order.
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    rainbow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    blue.doTasks(
        "-3 ProjectCard<Selecting>",
        "Ok", // No heat paid.
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // 6:08:20 PM — Rainbow: "Right what I'm gonna do is plant forest"
      // 6:08:30 PM — Green: "yeah it's just like it means they're about to lay down like eight
      // cities and end the game right just like i gotta get here and do my plan i have all go this
      // shit in my head all right that looks like nine nine you placed a plant you planted a forest
      // on nine nine"
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_09_09>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      // 6:08:34 PM — Blue: "You think I'm gonna, um, boop boop, or, um, You here for, um, two
      // apartments. Yeah, um, and Brooklyn nine nine. Papers, yeah, um, could actually go ahead in
      // a
      // city actually."
      // 6:08:59 PM — Green: "Okay, on six nine."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "NormalCityTile<Amazonis_06_09>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("NOW Rainbow { 2! MC<Rainbow> }")
      rainbow.doTask("2 MC")
    }
    with(rainbow) {
      // 6:09:38 PM — Rainbow: "You need other fish. Okay. Exactly. Um okay, so I am going to take
      // my
      // applied science action. I have one more science resource."
      // 6:10:09 PM — Rainbow: "Put that thing onto flitter and then fire"
      // Applied Science has no reload trigger. Its six sourced uses should have exhausted it at
      // 3:48:27 PM, yet this recording explicitly establishes one remaining science resource. No
      // source identifies where the extra resource arose, so 6:09:38 PM is its latest possible
      // insertion point.
      assertCounts(0 to "Science<$AppliedScience>")

      exMachina("Science<$AppliedScience>")

      assertCounts(1 to "Science<$AppliedScience>")

      doTasks(
          *cardActionTasks("$AppliedScience"),
          "-Science<$AppliedScience>",
          "Floater<$FloatingRefinery>",
      )
      // 6:10:48 PM — Rainbow: "Okay, I'm just gonna do this so that I don't suggest keep doing the
      // math over and over again. I need to pay for an interstellar colony ship."
      // 6:11:00 PM — Rainbow: "It costs 20. Yeah, that's what I'm gonna do. Oops, not eighteen
      // minus
      // six minus titanium and money."
      doTasks(
          *playCardTasks("$InterstellarColonyShip"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$InterstellarColonyShip FROM ProjectCard",
          "3 MC",
      )
      // The app omits Media Group's 3 M€ response to this event.
      rainbow.exMachina("-3 MC")
    }
    with(blue) {
      // 6:11:32 PM — Rainbow: "Flip it. Rip it, rip it down. Okay, so that's two actions that I am
      // finished. Okay. I am going to take the greenery standard project action to place greenery
      // right here at"
      // 6:11:57 PM — Blue: "Seven? At four seven."
      // 6:12:02 PM — Blue: "And I get two plants, two money, I raise the oxygen, it's max now,
      // and
      // I get a TR and I get two money."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "Ok", // No heat paid.
          "23 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_07>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "TerraformRating",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 6:12:28 PM — Green: "I'm going to use my factotum to spend three money and get a building
      // card space"
      // 6:12:47 PM — Green: "elevator. Wow. Not super useful."
      doTasks(*cardActionTasks("$Factorum", "Action2"), "3 Pay<> FROM MC")
      doTask("SearchForCard<TagFilter<Class<BuildingTag>>>").expect("ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // 6:12:54 PM — Blue: "What is it, um, right. I'm gonna pay a seven seals for carbon nano
      // systems."
      doTasks(
          *playCardTasks("$CarbonNanosystems"),
          "7 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$CarbonNanosystems FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 6:13:11 PM — Rainbow: "going to take my floating refinery action to turn these three cubes
      // why two that"
      // 6:13:16 PM — Green: "Two cubes. Says you take two floaters off."
      // 6:13:23 PM — Rainbow: "was dumb of me I could have put my applied science somewhere super
      // better all right fine two off for one titanium and two money D aww, yeah,"
      doTasks(
          *cardActionTasks("$FloatingRefinery", "Action2"),
          "-2 Floater<$FloatingRefinery>",
          "Titanium",
          "2 MC",
      )
      // The app records a 3 M€ gain for the card's 2 M€ action.
      rainbow.exMachina("MC")

      // 6:14:06 PM — Rainbow: "I'm gonna take my thermal viol back, that too out a microbe to any
      // Venus card, and then I will add it to sulfur eating bacteria."
      doTasks(*cardActionTasks("$Thermophiles"), "Microbe<$SulphurEatingBacteria>")
    }
    with(blue) {
      // 9:15:36 PM — Blue: "I'm gonna plant a city. And I'm gonna"
      // 9:15:55 PM — Yellow: "Four six."
      // 9:15:55 PM — Rainbow: "Yeah. Four six. And I get one plant and one two money and my cube
      // goes here."
      doTasks(
          *standardProjectTasks("CityProject"),
          "Ok", // No heat paid.
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_04_06>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("NOW Rainbow { 2! MC<Rainbow> }")
      rainbow.doTask("2 MC")
      doTasks("Plant", "2 MC")
      doTask("PassBaton")
    }
    // Rainbow's app omits this Rover Construction response even though the recording establishes
    // that she took it. The replay keeps the physical 2 M€; consequently Rainbow's M€ states from
    // app entry 234 through final production remain 2 M€ above the app-only balances.
    with(green) {
      // 9:16:45 PM — Green: "Okay. I am going to use the Venus Magnetizer, lose an energy
      // production, raise Venus to 24% and get a TR. [Yellow]'s turn."
      doTasks(*cardActionTasks("$VenusMagnetizer"), "PROD[-Energy]", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }

    with(yellow) {
      // 9:17:07 PM — Yellow: "Muniversity. I pay four steels. I pitch and draw."
      // 9:17:24 PM — Yellow: "Okay. And I add a graphene."
      // The app never logs this four-steel payment. It therefore shows 4 steel both before this
      // play and after production; the replay additionally visits the evidenced intervening zero.
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$MarsUniversity FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 9:17:57 PM — Rainbow: "Okay. So I had a lot of actions to take. Um, fine, I'm gonna pay
      // sixteen, for farming, which increases my money production and plant production,
      // which is not that important now, but it also gives me two more plants, and two points, and
      // um, I guess I will also..."
      doTasks(
          *playCardTasks("$Farming"),
          "16 Pay<> FROM MC",
          "$Farming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[2 Plant]",
          "2 Plant",
      )
      // App entries 233-234 move from 53 to 38 despite recording +3 and -16.
      rainbow.exMachina("MC")

      // 9:18:58 PM — Rainbow: "Green, do you have microbes?"
      // 9:19:12 PM — Green: "I do."
      // 9:19:13 PM — Rainbow: "You got a tardy grade. I'm going to use my ant's action to take one
      // of your microbes. Thank you."
      // 9:19:20 PM — Green: "You took my tardigrade. I have no microbes left."
      doTasks(*cardActionTasks("$Ants"), "-Microbe<$Tardigrades<Green>>", "Microbe<$Ants>")
          .expect("0 MC")
    }
    with(blue) {
      // 9:19:42 PM — Blue: "I'm probably gonna just go on ahead and take another greenery standard
      // project with all my heat that I have to spend."
      // 9:20:12 PM — Blue: "So that I get three five."
      // 9:20:14 PM — Blue: "I get one plant and two money."
      // The sourced transactions since the 51 M€ dashboard leave 6 M€, but Blue's app debits
      // 9 M€ and 14 heat here. Supply the three-M€ overdraft so the real payment can run.
      blue.exMachina("3 MC")

      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "14 Pay<Class<Heat>> FROM Heat",
          "9 Pay<> FROM MC",
          "GreeneryTile<Amazonis_03_05>",
          "OxygenStep",
          "Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // 9:20:25 PM — Green: "Who is this network?"
      // 9:20:27 PM — Blue: "business"
      // 9:20:29 PM — Green: "I'm your business that is pointless at least industrial"
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTask("PassBaton")
    }
    with(yellow) {
      // 9:20:40 PM — Yellow: "Got trans Neptune probe. I'm gonna pay a carbon nano and two monies
      // for it"
      // 9:20:50 PM — Yellow: "I pay for it because it has space tag and then science tag adds
      // carbon nano back oh right and I use University pitch and drop"
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
          "2 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
          "Graphene<$CarbonNanosystems>",
          "-ProjectCard",
          "ProjectCard",
      )
      // Yellow's app posts -3 M€ for this otherwise fully narrated 2 M€ payment.
      exMachina("-MC")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 9:21:01 PM — Rainbow: "Okay. that sucks uh okay so would I rather get three more monies or
      // an entire point I think I would rather get an entire point"
      // 9:21:58 PM — Rainbow: "I don't care about raising my money production then that's gonna be
      // birds"
      // 9:22:18 PM — Rainbow: "I did. That's with Maxwell Bank."
      doTasks(*cardActionTasks("$MaxwellBase"), "Animal<$StratosphericBirds>")
      // 9:22:30 PM — Green: "Okay, you undid Moholy I-Rey Okay."
      // 9:22:33 PM — Rainbow: "unpension. did the Holy Lake action. All right, let's do my sulfur
      // eating bacteria action. I currently have one, two, three, four, five microbes, and I'm
      // going to turn them into fifteen money."
      doTasks(
          *cardActionTasks("$SulphurEatingBacteria", "Action2"),
          "-5 Microbe<$SulphurEatingBacteria>",
          "15 MC",
      )
    }
    with(blue) {
      // 9:23:07 PM — Blue: "I will convert my eight plants into a greenery. Bam. Greenery and it's
      // going here."
      // 9:23:17 PM — Green: "Mm, five, seven."
      // 9:23:18 PM — Blue: "Excuse me. Two plants. At five-seven."
      // Blue first chose 5-7, then moved the greenery to 5-6 before the next operation. The engine
      // cannot relocate a tile, so place it at its sourced final coordinate.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_05_06>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 9:23:28 PM — Green: "All right, I will use tartigrades to put a tartigrade on tartigrades
      // and give a money."
      doTasks(*cardActionTasks("$Tardigrades"), "Microbe<$Tardigrades>", "MC")
      doTask("PassBaton")
    }
    // The table corrected Blue's greenery to 5-6 for 4 M€ ocean adjacency. Blue had not previously
    // taken that money; her app also removed one of the space's two plant bonuses.
    blue.exMachina("-Plant")
    with(yellow) {
      // 9:24:40 PM — Yellow: "Right. I am Ada Smanimal. Your turn."
      doTasks(*cardActionTasks("$SmallAnimals"), "Animal<$SmallAnimals>")
      doTask("PassBaton")
    }
    with(rainbow) {
      // 9:25:56 PM — Rainbow: "All right, I so I'm gonna do a city."
      // 9:26:00 PM — Rainbow: "City, you know, project. Which means that you get a pet."
      // 9:26:17 PM — Rainbow: "No. Okay, my city is at six seven."
      // 9:26:20 PM — Rainbow: "You get to pay, I get two money for the city, and I get two money
      // because
      // it's next to an ocean, and I get two plantos and a steel, and"
      // The 25 M€ project, 2 M€ ocean adjacency, and 2 M€ Rover Construction response are
      // one engine task.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Amazonis_06_07>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTasks("2 MC", "Steel", "2 Plant", "2 MC")
      // 9:26:36 PM — Rainbow: "Yay. Um, hey, yay. And then I'm gonna do a plant, plant forest
      // action."
      // 9:28:13 PM — Green: "Two energies, two money for going on seven."
      // 9:28:17 PM — Rainbow: "Yeah, with a forest."
      // 9:28:20 PM — Rainbow: "Alright, that is two actions, so that is the end of my turn."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_07>",
          "OxygenStep",
          "2 Energy",
          "2 MC",
      )
    }
    with(blue) {
      // 9:28:22 PM — Blue: "Okay. I'm gonna do another greenery standard project."
      // 9:28:27 PM — Green: "that's a lot of heat and And she still gets thermalist guaranteed so
      // that's on four five for two money and and whatever And one plant."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "17 Pay<Class<Heat>> FROM Heat",
          "6 Pay<> FROM MC",
          "GreeneryTile<Amazonis_04_05>",
          "OxygenStep",
          "Plant",
          "2 MC",
      )
      // The app omits the 2 M€ ocean-adjacency income from this final placement.
      blue.exMachina("-2 MC")
      doTask("PassBaton")
    }
    with(green) {
      // 9:29:05 PM — Green: "Let's just play earth catapult first, that costs 20"
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "20 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      // "Then noxious city is only 16 -- two steel and 14 real [sic] I lose an energy production I
      // get three money production. You get two money."
      // Rainbow: "I get two money you get a plant?"
      // 9:29:40 PM — Green: "I place a shitty tile down here at nine eight and I get my gold pet"
      doTasks(
          *playCardTasks("$NoctisCity"),
          "1 Pay<Class<Steel>> FROM Steel",
          "14 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
          "CityTile<Amazonis_09_08>",
          "Animal<$Pets>",
          "NOW Rainbow { 2! MC<Rainbow> }",
      )
      rainbow.doTask("2 MC")
    }
    // The -12 M€ net above is the narrated -14 M€ payment plus Green's real +2 M€ Suitable
    // Infrastructure response to the production increase. Do not erase that income.

    with(yellow) {
      // 9:30:05 PM — Yellow: "I pay 1 for CEO's favorite project and man these are both one away
      // from getting a point. Um, I add to EcoZone."
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "1 Pay<> FROM MC",
          "$CeosFavoriteProject FROM ProjectCard",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }
    with(rainbow) {
      // 9:31:04 PM — Rainbow: "Okay, I'm gonna go ahead and sell three patents."
      // Together with Rover Construction's 2 M€ response to Green's city, this is the app's
      // five-M€ increase. No income is missing.
      doTasks(*standardProjectTasks("SellPatentsProject"), "3 MC FROM ProjectCard")
      doTask("PassBaton")
    }
    // 9:31:07 PM — Blue: "Pass."
    blue.doTask("Pass")

    with(green) {
      // 9:31:07 PM — Green: "I will... Use cloud tourism to add a floater to cloud tourism."
      doTasks(*cardActionTasks("$CloudTourism"), "Floater<$CloudTourism>")
      doTask("PassBaton")
    }

    with(yellow) {
      // 9:31:28 PM — Yellow: "Sell patent."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(rainbow) {
      // 9:31:57 PM — Rainbow: "Adding a fish to my fish card."
      doTasks(*cardActionTasks("$Fish"), "Animal<$Fish>")
      // 9:32:00 PM — Rainbow: "And birds to my stratospheric birds"
      // Yellow: "Birds fall from the window ledge above mine."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
    }
    with(green) {
      // 9:32:09 PM — Green: "I am going to use Venus Shootles and I probably still have six Venus
      // tags so that means I pay six for a TR and Venus is now on 26"
      doTasks(*cardActionTasks("$VenusShuttles"), "6 Pay<> FROM MC", "VenusStep", "TerraformRating")
      doTask("PassBaton")
    }

    with(yellow) {
      // 9:32:30 PM — Yellow: "sell patent."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(rainbow) {
      // 9:32:37 PM — Rainbow: "I'mna use Mohole Lake to put a stratospheric bird."
      doTasks(*cardActionTasks("$MoholeLake"), "Animal<$StratosphericBirds>")
      // 9:32:51 PM — Rainbow: "And I might as well do the same with Maxwell Base. Another bird on
      // the
      // stratospheric birds."
      // 9:32:57 PM — Yellow: "Base bird."
      assertCardResources(6 to StratosphericBirds)

      // Rainbow physically used Maxwell Base at both 9:21:58 and 9:32:51 in the same generation.
      // The first use consumed its action, so record the recorded illegal second bird.
      exMachina("Animal<$StratosphericBirds>")

      assertCardResources(7 to StratosphericBirds)
      doTask("PassBaton")
    }

    with(green) {
      // 9:33:01 PM — Green: "I will direct some heat usage, except I keep grabbing delegates which
      // is very useful. I will spend three heat to gain two planta. Yellow?"
      doTasks(*cardActionTasks("$DirectedHeatUsage"), "3 Pay<Class<Heat>> FROM Heat", "2 Plant")
      doTask("PassBaton")
    }

    with(yellow) {
      // 9:33:10 PM — Yellow: "Oh, right. I now spend um two carbon nanos and eight real on imported
      // hydrogen, um you may be thinking what's the point there's no um what's its face, uh ocean
      // to place but I can gain three plants which lets me place a plant forest. I'll put it on
      // six, umm, whatever this is."
      // 9:34:18 PM — Green: "Exciting. On six two."
      // Carbon Nanosystems accepts both graphene toward this one space project, for 4 M€ each.
      assertCardResources(2 to CarbonNanosystems)

      doTasks(
          *playCardTasks("$ImportedHydrogen"),
          "2 PayFromCard<$CarbonNanosystems> FROM Graphene<$CarbonNanosystems>",
      )
      assertCardResources(0 to CarbonNanosystems)
      doTasks(
          "8 Pay<> FROM MC",
          "$ImportedHydrogen FROM ProjectCard",
          "OceanTile<WaterArea(HAS MAX 0 Tile)>", // All eleven oceans are already placed.
          "3 Plant",
      )
      // Conversely, Yellow's app posts -6 M€ for the narrated 8 M€ cash payment.
      exMachina("2 MC")

      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_06_02>",
          "OxygenStep",
          "Plant",
      )
    }

    with(rainbow) {
      // 9:34:35 PM — Rainbow: "What can I do with my fourteen money? Ah, fine. Um Well, I'm gonna
      // go ahead and add one floater to local shading."
      doTasks(*cardActionTasks("$LocalShading"), "Floater<$LocalShading>")
      doTask("PassBaton")
    }

    with(green) {
      // 9:34:52 PM — Green: "Um I will do something I should have done a long time ago and have a
      // stratospheric expedition which costs me only ten but really I'm going to spend four
      // titanium anyway and that lets me put two floatbois on cloud tourism and draw two Venus
      // cards."
      doTasks(
          *playCardTasks("$StratosphericExpedition"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$StratosphericExpedition FROM ProjectCard",
      )
      doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>").expect("2 ProjectCard")
      doTask("2 Floater<$CloudTourism>")
      doTask("PassBaton")
    }
    // 9:35:59 PM — Yellow: "I pass."
    yellow.doTask("Pass")
    // Yellow's app has no entries for CEO's Favorite or either late patent sale. Those three
    // recorded actions net +1 M€, but the next app balance remains 2 M€ before the
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
    with(rainbow) {
      // 9:36:02–9:36:54 PM — Rainbow prices Asteroid Deflection System at nine, using two steel,
      // one titanium, and 2 M€. When Yellow notices its energy-production cost, Rainbow first buys
      // the 11-M€ Power Plant. Her app records that standard project as a City instead and retains
      // the resulting extra M€ production through the final production credit.
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      exMachina("-14 MC, PROD[MC]")

      assertCounts(16 to "MC")

      doTasks(
          *playCardTasks("$AsteroidDeflectionSystem"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$AsteroidDeflectionSystem FROM ProjectCard",
          "PROD[-Energy]",
      )
      // The app then removes the full 13 M€ cost in addition to the steel and titanium payment.
      exMachina("-11 MC")

      assertCounts(3 to "MC")
    }
    with(green) {
      // 9:37:30 PM — Green: "That changes things. I'm probably not going to bother playing it
      // because it's probably more important that I Yep, it's more important that I play Breathing
      // Filters for nine. And that gives me a cube on Olympics Contest."
      doTasks(
          *playCardTasks("$BreathingFilters"),
          "9 Pay<> FROM MC",
          "$BreathingFilters FROM ProjectCard",
          "Science<$OlympusConference>",
      )
      doTask("PassBaton")
    }

    with(rainbow) {
      // 9:38:07 PM — Rainbow: "Let's reveal the top card of the deck."
      // 9:38:11 PM — Rainbow: "Now no space tag."
      // 9:38:15 PM — Rainbow: "But I still took the asteroid deflection system action, so I put a
      // cube on my card."
      doTasks(
          *cardActionTasks("$AsteroidDeflectionSystem"),
          "ProjectCard<Revealed>",
          "0 ClaimCardReward<TagFilter<Class<SpaceTag>>, $AsteroidDeflectionSystem>",
          "-ProjectCard<Revealed>",
      )
      doTask("PassBaton")
    }
    with(green) {
      // 9:38:27 PM — Green: "Um, well, I mean, actually, I might as well spend the four on ishtar
      // expedition after all, because what else am I going to do now at this point? So, I'll get
      // three titanium, yay, maybe it'll be useful, and let's see if we can actually get two Venus
      // cards in what remains of the deck. If not, I got a lot of shuffling to do. No, I don't
      // think we're going to make it."
      doTasks(
          *playCardTasks("$IshtarExpedition"),
          "4 Pay<> FROM MC",
          "$IshtarExpedition FROM ProjectCard",
          "3 Titanium",
      )
      doTask("2 SearchForCard<TagFilter<Class<VenusTag>>>").expect("2 ProjectCard")
      doTask("PassBaton")
    }
    // 9:39:08 PM — Green: "Leechifrous Corrodies (Corroder Suits), and Jetstream Microscrappers."
    // 9:39:15 PM — Green: "Your turn, Rainbow."
    // 9:39:17 PM — Rainbow: "Um, I pass."
    rainbow.doTask("Pass")
    with(green) {
      // 9:39:42 PM — Green: "Oh, okay. I don't think I can quite get there though. Yeah, I can. One
      // two three four five six patents to get the money I need for corrode her suits."
      // 9:39:59 PM — Green: "That gives me two money production, which gives me two money. and then
      // i put my uh oh wait this is even this is 14 oh is it oh okay got it so now i've got 15 on
      // yeah this whole time i thought it was okay Alright so that was me building coroties."
      doTasks(*standardProjectTasks("SellPatentsProject"), "6 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$CorroderSuits"),
          "6 Pay<> FROM MC",
          "$CorroderSuits FROM ProjectCard",
          "PROD[2 MC]",
          "-SuitableInfrastructureBonus<$SuitableInfrastructure, ActionScope>",
          "2 MC",
          "Floater<$CloudTourism>",
      )
      // Despite the narration, the app omits Suitable Infrastructure's 2 M€ response to the
      // production gain.
      green.exMachina("-2 MC")

      // No point using Icy Impactors anymore.
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("44 MC", "7 Plant", "Heat", "4 Steel", "3 Energy", "3 Heat FROM Energy")
    rainbow.doTasks("57 MC", "Titanium", "4 Plant", "Steel", "2 Heat FROM Energy")
    blue.doTasks("49 MC", "13 Heat", "13 Energy", "13 Heat FROM Energy", "Plant")
    green.doTasks("47 MC", "Steel", "Heat", "2 Titanium", "2 Plant", "Energy", "3 Heat FROM Energy")

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

    with(yellow) {
      // 9:40:56 PM — Green: "And then [Yellow] does not have oh does have enough plants. Yep,
      // [Yellow]
      // places plants first."
      // 9:40:58 PM — Yellow: "Yeah. I do. Yes. Yeah. Ze will um Mm go A hee -haw for one plant."
      // 9:41:17 PM — Green: "Eleven ten to on eleven t eleven ten for one plant. [Rainbow], do you
      // have
      // enough?"
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "7 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_11_10>",
          "Plant",
      )
    }
    yellow.doTask("PassBaton")
    // 9:41:26 PM — Rainbow: "I do not. I only have five."
    rainbow.doTask("PassBaton")
    blue.doTask("PassBaton")

    with(green) {
      // 9:41:51 PM — Green: "I guess I might as well take four money here on seven five."
      // 9:42:04 PM — Green: "And it gives me two energy for whatever that's worth."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Amazonis_07_05>",
          "2 Energy",
          "2 MC",
          "2 MC",
      )
    }
    green.doTask("PassBaton")

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
