package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Game played Sun 2026-08-09 11:19 am. Quotes are transcript-derived and sometimes normalized. */
internal class OtbGame20260809Test : FullGameTest() {
  // "This is a two-player game on the Hellas board."
  // "Our colonies are Callisto, Luna, Triton, Miranda, and Enceladus."
  // "We're using the Venus expansion. We're using promo cards. We're using the Prelude
  // expansion."
  // "Our milestones are Coast Guard, Landshaper, Mayor, Producer, Sponsor."
  // "Our awards are Botanist, Founder, Landlord, Magnate, and Metropolist."
  // "And also, we have the Hoverlord milestone and the Venophile award."
  override val config =
      GameConfig(
          """
          HellasMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion
          promos:cards

          Coastguard, Landshaper, Mayor, Producer, Sponsor, Hoverlord
          Botanist, Founder, Landlord, Magnate, Metropolist, Venuphile
          Callisto, Luna, Triton, Miranda, Enceladus
          """,
          "Yellow",
          "Green",
      )

  private val yellow
    get() = player(1)

  private val green
    get() = player(2)

  @Test
  internal fun otbGame20260809() {
    startGame()
    yellow.doTask("20 TerraformRating")
    green.doTask("20 TerraformRating")
    yellow.doTasks(
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
    yellow.doTask("-4 ProjectCard<Selecting>")
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
    generation10()
    generation11()
  }

  private fun generation1() {
    // "Miranda and Enceladus are currently out of play."
    admin.assertCounts(3 to "ColonyTile", 5 to "ColonyTileSelection")

    // (11:28 am) "The game is beginning. It is Generation 1."
    admin.assertCounts(1 to "Generation")

    // "It's the corporation phase."
    admin.assertCounts(1 to "CorporationPhase")

    with(yellow) {
      // "I call Mons Insurance again."
      // "Your money production goes up four." "And [Green]'s money production goes down two."
      // "You're buying six cards, which leaves you with 30 money."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$MonsInsurance>>",
          "$MonsInsurance FROM CorporationCard",
          "48 MC",
          "PROD[4 MC]",
          "18 Pay<> FROM MC",
          "6 ProjectCard<Hand FROM Selecting>",
          "EACH Other@Player(NOT Yellow) { -2! Production<Other@Player, Class<MC>> BY Other@Player }",
          "PROD[-2 MC<Green>] BY Green",
      )
      green.doTask("PROD[-2 MC]")
    }

    with(green) {
      // "On my turn, I play Morning Star Inc."
      // "I am purchasing four cards. So I receive 50 money."
      // "And then I'm spending 12 money. So I have 38 money left."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$MorningStarInc>>",
          "$MorningStarInc FROM CorporationCard",
          "50 MC",
          "12 Pay<> FROM MC",
          "4 ProjectCard<Hand FROM Selecting>",
      )
    }

    // "It is now the Prelude phase."
    admin.assertCounts(1 to "PreludePhase")

    with(yellow) {
      // "I got Dome Farming."
      // "That is, I gain a plant production and two money production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DomeFarming>>",
          "$DomeFarming FROM PreludeCard",
          "PROD[Plant]",
          "PROD[2 MC]",
      )
      // "And Society Support: I lose a money production, I gain plant production, gain energy"
      // "production, gain heat production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
          "$SocietySupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
    }

    with(green) {
      // "On my turn, I play Nitrogen Shipment."
      // "I get a TR, a plant production, and five money, bringing me to 43 money."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$NitrogenShipment>>",
          "$NitrogenShipment FROM PreludeCard",
          "TerraformRating",
          "PROD[Plant]",
          "5 MC",
      )
      assertCounts(43 to "MC") // ledger entry 7

      // (11:30 am) "Then I play Great Aquifer."
      // "Place on the one-card spot..."
      // "... and then on the one-plant spot next to it."
      // "I get a card, one plant, two money, and two TR."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$GreatAquifer>>",
          "$GreatAquifer FROM PreludeCard",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
          "OceanTile<Hellas_4_6>",
          "TerraformRating",
          "Plant",
          "2 MC",
      )
    }

    // "That concludes the Prelude phase. It is now time for the action phase."
    admin.assertCounts(1 to "ActionPhase")

    with(yellow) {
      // "I play Aquifer Pumping for 18."
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "18 Pay<> FROM MC",
          "$AquiferPumping FROM ProjectCard",
      )
      // "And then pay eight for an ocean place. So I'm gonna lose 26 money."
      // "You had 30 and now you have four."
      // "I'm gonna place in this nice little spot where I get four money and three heat."
      doTasks(*cardActionTasks("$AquiferPumping"), "8 Pay<> FROM MC")
      assertCounts(4 to "MC") // ledger entry 13
      doTasks("OceanTile<Hellas_5_7>", "TerraformRating", "3 Heat", "2 MC", "2 MC")
    }

    with(green) {
      // "For my first action, I flip cards until I get 3 Venus tags. Let's see how this goes."
      // "I draw Venusian Insects, Air-Scrapping Expedition, and Atalanta Planitia Lab."
      doTask("DoRequiredActions")
      doTask("3 SearchForCard<TagFilter<Class<VenusTag>>>").expect("3 ProjectCard")
      doTask("PassBaton")
    }

    // (11:33 am) "I pitch a card for money, and I spend all my nine money on Robotic Workforce."
    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "It just so happens that Dome Farming is a box-building tag."
      // "So that's a plant production and 2 money production."
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
          "CopyProductionBox<$DomeFarming>",
          "PROD[Plant]",
          "PROD[2 MC]",
      )
      assertCounts(0 to "MC") // ledger entry 17
    }

    // "I can play Moss. I pay four, bringing me down to 41."
    // "I satisfy the three-ocean requirement."
    // "I lose one plant and gain one plant production."
    with(green) {
      doTasks(
          *playCardTasks("$Moss"),
          "4 Pay<> FROM MC",
          "$Moss FROM ProjectCard",
          "PROD[Plant]",
          "-Plant",
      )
      assertCounts(41 to "MC") // ledger entry 11
      doTask("PassBaton")
    }
    yellow.doTask("Pass")

    // "I'm passing with 41 money."
    green.assertCounts(41 to "MC") // ledger entry 11
    green.doTask("Pass")

    // Production.
    yellow.doTasks("28 MC", "3 Plant", "Energy", "Heat")
    green.doTasks("21 MC", "2 Plant")
    // "We do production. World Government Terraforming is [Yellow]'s choice."
    // "I'm boosting oxygen."
    // "You don't get the TR for that."
    yellow.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")

    // (11:36 am)
    // "We produced, we did World Government, now it's time for the colony Solar phase."
    // "We increase the only two—only three colonies that exist so far."
    // "[Green] gets the start marker."
    green.assertCounts(1 to "StartToken<Green>")
  }

  private fun generation2() {
    // "Generation becomes two. Any cubes come off cards."
    green.assertCounts(0 to "ActionUsedMarker<Anyone>")
    with(green) {
      assertProduction(m = -2, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 62, s = 0, t = 0, p = 2, e = 0, h = 0)
    }
    with(yellow) {
      assertProduction(m = 7, s = 0, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 28, s = 0, t = 0, p = 3, e = 1, h = 4)
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3)

    // See board-11-39-15-corrected.png - verified
    green.assertCounts(4 to "CardFront")
    green.assertTags(plt = 1, vet = 1)
    yellow.assertCounts(5 to "CardFront")
    yellow.assertTags(but = 2, sct = 1, plt = 1)
    admin.assertCounts(3 to "Tile")

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (11:39 am) "Wow, these all suck." "I'll buy two."
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // "I'm going to discard two and buy two."
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "I'm going to release my inert gases. For fourteen. That gives me two TR."
      doTasks(
          *playCardTasks("$ReleaseOfInertGases"),
          "14 Pay<> FROM MC",
          "$ReleaseOfInertGases FROM ProjectCard",
          "2 TerraformRating",
      )
      doTask("PassBaton")
    }

    // 11:43 am
    // "I'm gonna pay eight to pump an aquifer."
    with(yellow) {
      // "I will put it up here for the spot with one plant and four money."
      // "And one TR?"
      doTasks(*cardActionTasks("$AquiferPumping"), "8 Pay<> FROM MC")
      assertCounts(14 to "MC") // ledger entry 35
      doTasks("OceanTile<Hellas_4_7>", "TerraformRating", "Plant", "2 MC", "2 MC")
      assertCounts(18 to "MC") // ledger entry 37
      doTask("PassBaton")
    }

    // "I'm going to play Terraforming Contract because I can."
    // "So that costs me eight, bringing me down to 34."
    // "It takes me from negative two to positive two money production."
    with(green) {
      doTasks(
          *playCardTasks("$TerraformingContract"),
          "8 Pay<> FROM MC",
          "$TerraformingContract FROM ProjectCard",
          "PROD[4 MC]",
      )
      assertCounts(34 to "MC", 7 to "PROD[MC]") // ledger entry 28; 7 really means 2
      doTask("PassBaton")
    }
    // "I think I'll pass. Requirements."
    yellow.doTask("Pass")
    // "I pass as well."
    green.doTask("Pass")

    // Production.
    yellow.doTasks("29 MC", "3 Plant", "Energy", "Heat", "Heat FROM Energy")
    green.doTasks("27 MC", "2 Plant")
    // "We do production."
    // "Then World Government Terraforming is my choice."
    // "Let's do Venus."
    // "Venus goes up to 2%."
    green.doTask("VenusStep! BY Admin")
  }

  private fun generation3() {

    // "Then we raise all of our colonies again, of which we still have only three."
    // "I give the start marker to [Yellow]."
    // "We raise the generation to Generation 3."
    // "We take cubes off cards and draft."
    with(green) {
      assertProduction(m = 2, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 61, s = 0, t = 0, p = 4, e = 0, h = 0)
    }
    with(yellow) {
      assertProduction(m = 7, s = 0, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 47, s = 0, t = 0, p = 7, e = 1, h = 6)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 4)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (11:46 am) "I'm buying three cards and discarding one."
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // "I buy one card and discard three."
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I feel like I can always start by Aquifer Pumping."
      // "Eight real money to use Aquifer Pumping."
      // "I'm going on the one-one. I get two plants and a TR."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Hellas_1_1>",
          "TerraformRating",
          "2 Plant",
      )
      // "I spend eight plants to place a greenery."
      // "Rightmost of row three."
      // "That's two money, a plant, and a card."
      // "The oxygen goes up to 2%, and [Yellow] gets a TR."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_7>",
          "OxygenStep",
          "Plant",
          "ProjectCard",
          "2 MC",
          "TerraformRating",
      )
      admin.assertCounts(2 to "OxygenStep")
    }

    // 11:49 am
    // "Giant Solar Shade. That cost me 27 full real money, bringing me down to 25."
    // "I raise Venus three steps and get three TR and a card."
    with(green) {
      doTasks(
          *playCardTasks("$GiantSolarShade"),
          "27 Pay<> FROM MC",
          "$GiantSolarShade FROM ProjectCard",
          "VenusStep",
          "VenusStep",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
          "TerraformRating",
          "ProjectCard",
      )
      assertCounts(25 to "MC") // ledger entry 41
      doTask("PassBaton")
    }

    // "I think I gave myself minus four because I was thinking of placing next to oceans, but then
    // I"
    // "placed away from oceans, so I had to fully minus eight."
    // "I play seven for Optimal Aerobraking and 21 for Comet."
    with(yellow) {
      doTasks(
          *playCardTasks("$OptimalAerobraking"),
          "7 Pay<> FROM MC",
          "$OptimalAerobraking FROM ProjectCard",
      )
      // "I raise the temperature one step and place an ocean tile."
      // "I'll go on the rightmost space in the ring for 4 money."
      // "I'll remove yours. And I'll pay you three from Mons Insurance."
      // "Thanks to Optimal Aerobraking, I gain three money and three heat."
      // "I gain one event card in my played-events pile."
      doTasks(
          *playCardTasks("$Comet"),
          "21 Pay<> FROM MC",
          "$Comet FROM ProjectCard",
          "TemperatureStep",
          "3 MC",
          "3 Heat",
          "TerraformRating",
          "OceanTile<Hellas_5_8>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "-3 Plant<Green>",
          "3 MC<Green> FROM MC",
      )

      assertCounts(14 to "MC") // ledger entry 69
    }
    green.assertCounts(28 to "MC") // ledger entry 46

    with(green) {
      // 11:53 am
      // "Because Venus is at 8%, I can play Venusian Insects, which requires Venus at 12%."
      // "That brings Enceladus into play, and the cube goes on the second spot."
      // "I should pay for that card, however. I should pay five for it."
      doTasks(
              *playCardTasks("$VenusianInsects"),
              "5 Pay<> FROM MC",
              "$VenusianInsects FROM ProjectCard",
          )
          .expect("Enceladus")
      doTask("PassBaton")
    }

    with(yellow) {
      // "We have more than the five-ocean requirement, so I play Algae."
      // "I pay ten for Algae. I gain one plant and gain two plant production."
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to play Topsoil Contract. That costs me eight, leaving me with 15."
      // "I gain three plants."
      doTasks(
          *playCardTasks("$TopsoilContract"),
          "8 Pay<> FROM MC",
          "$TopsoilContract FROM ProjectCard",
          "3 Plant",
      )
      doTask("PassBaton")
    }
    // "I believe I pass."
    yellow.doTask("Pass")

    // "I'm going to take the action of Venusian Insects to add one microbe to Venusian Insects"
    // "and get one money from Topsoil Contract."
    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      assertCounts(16 to "MC") // ledger entry 49

      // "I'm going to play Venus Governor because I have three Venus tags and it requires two."
      // "That costs me four, but gets me two money production, bringing me up to four money"
      // "production."
      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "$VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
      // 11:55 am
      // "I'm going to play Search for Life, which we're still under the max requirement."
      doTasks(
          *playCardTasks("$SearchForLife"),
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
      )
      // "I pay three and then I'll pay one to use Search for Life."
      // "I flip up Business Contacts, so I do not get a science resource."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      assertCounts(8 to "MC") // ledger entry 52

      // "I think we're at the point where I've done everything. So, I pass."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("33 MC", "5 Plant", "Energy", "Heat", "Heat FROM Energy")
    green.doTasks("32 MC", "2 Plant")
    // "We hit production. We do World Government Terraforming, and it's your turn."
    // "I increase the oxygen. Oxygen goes to 3%."
    yellow.doTask("OxygenStep! BY Admin")
  }

  private fun generation4() {

    // "Then the colonies each go up. This time there are four colony tracks to increase."
    // "The generation goes up to four. The start marker moves to [Green] from [Yellow]."
    // "Cubes come off cards."
    with(green) {
      assertProduction(m = 4, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 40, s = 0, t = 0, p = 6, e = 0, h = 0)
    }
    with(yellow) {
      assertProduction(m = 7, s = 0, t = 0, p = 5, e = 1, h = 1)
      assertResources(m = 37, s = 0, t = 0, p = 8, e = 1, h = 11)
    }
    assertSidebar(gen = 4, temp = -28, oxygen = 3, oceans = 6)

    // See board-11-59-52-corrected.png - verified
    green.assertCounts(10 to "CardFront")
    green.assertTags(spt = 1, sct = 1, eat = 2, vet = 5, plt = 1, mit = 2)
    yellow.assertCounts(7 to "CardFront")
    yellow.assertTags(but = 2, spt = 1, sct = 1, plt = 2)
    admin.assertCounts(7 to "Tile")

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // 12:34 pm
    // "I'm discarding one card and buying three cards."
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // "Same."
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.assertCounts(28 to "MC") // ledger entry 85

    // "I'm going to use Venusian Insects to take a microbe on Venusian Insects and gain one money"
    // "and forgo my second action."
    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      doTask("PassBaton")
    }

    // "I'm gonna pay 16 for Imported Hydrogen. I'm gonna gain three plants."
    with(yellow) {
      // "Place an ocean tile. On the steel spot, for four money and a steel. What a steal."
      // "And a TR."
      // "Optimal Aerobraking gives me three money and three heat, bringing me up to 14 heat."
      doTasks(
          *playCardTasks("$ImportedHydrogen"),
          "16 Pay<> FROM MC",
          "$ImportedHydrogen FROM ProjectCard",
          "3 MC",
          "3 Heat",
          "3 Plant",
          "OceanTile<Hellas_6_8>",
          "TerraformRating",
          "Steel",
          "2 MC",
          "2 MC",
      )
      assertCounts(19 to "MC") // ledger entry 93

      // "I may as well use Aquifer Pumping."
      // "I'm gonna spend that steel and six real to place an ocean in the six-money
      // spot."
      // "That's another TR and six money."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "1 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "OceanTile<Hellas_6_7>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "2 MC",
      )
    }

    // (12:38 pm) "Peroxide Power costs me seven."
    // "It costs me a money production and gains me two energy production."
    with(green) {
      doTasks(
          *playCardTasks("$PeroxidePower"),
          "7 Pay<> FROM MC",
          "$PeroxidePower FROM ProjectCard",
          "PROD[-MC]",
          "PROD[2 Energy]",
      )
      assertCounts(25 to "MC") // ledger entry 65
      doTask("PassBaton")
    }

    // "I'm gonna buzz buzz. Sponsorize some academies. That's nine to discard a card from hand."
    // "Draw three cards and you get one card."
    with(yellow) {
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "9 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Yellow) { ProjectCard<Other@Player, Hand>! }",
          "3 ProjectCard",
          "ProjectCard<Green, Hand>",
      )
      assertCounts(10 to "MC") // ledger entry 100
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to use the Power Plant standard project."
      // "That brings me up to three energy production."
      doTasks(*standardProjectTasks("PowerPlantProject"), "11 Pay<> FROM MC", "PROD[Energy]")
      doTask("PassBaton")
    }
    green.assertCounts(14 to "MC") // ledger entry 69

    // (12:40 pm) "I will convert eight plants to a greenery."
    with(yellow) {
      // "And that'll be on the 2-plants, 4-money spot."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_3_6>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )

      // "Oxygen goes to 4%. I'm at 29 TR."
      assertCounts(4 to "OxygenStep", 29 to "TerraformRating") // ledger entry 105

      assertCounts(14 to "MC") // ledger entry 108

      // "Second action: 11 for Corporate Stronghold."
      // "I lose an energy production and gain three money production."
      // "I place a city tile at row two, column six, right above my two greeneries, for a"
      // "plant."
      doTasks(
          *playCardTasks("$CorporateStronghold"),
          "11 Pay<> FROM MC",
          "$CorporateStronghold FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_2_6>",
          "Plant",
      )
    }

    // "I'm going to play Martian Survey while I still can."
    // "That costs me nine, and it draws me two cards."
    with(green) {
      doTasks(
          *playCardTasks("$MartianSurvey"),
          "9 Pay<> FROM MC",
          "$MartianSurvey FROM ProjectCard",
          "2 ProjectCard",
      )
      assertCounts(5 to "MC") // derived from ledger entry 72 before Industrial Center
      doTask("PassBaton")
    }
    // (12:43 pm) "I believe I pass."
    yellow.doTask("Pass")

    // "I'm going to use Search for Life, and I flip Potatoes, so I do not get a science resource."
    with(green) {
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")

      // "That appears to be the end of everything I can do except..."
      // HACK: Unlike the other narrated Search for Life actions, I never said that I paid the one
      // money.
      exMachina("1 MC")

      assertCounts(5 to "MC") // derived from ledger entry 72 before Industrial Center

      // "Sure. Let us play Industrial Center for four."
      // "Let's just get in your business over here. So I'm placing row two, column five, and"
      // "getting just one plant."
      doTasks(
          *playCardTasks("$IndustrialCenter"),
          "4 Pay<> FROM MC",
          "$IndustrialCenter FROM ProjectCard",
          "IndustrialCenter_SpecialTile<Hellas_2_5>",
          "Plant",
      )
      assertCounts(1 to "MC") // ledger entry 72

      // "I don't have enough money to take its action. I pass."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("39 MC", "5 Plant", "Heat", "Heat FROM Energy")
    green.doTasks("31 MC", "2 Plant", "3 Energy")
    // "Because we've both passed, we hit production."
    // "I will World Gov, and the thing that I will World Gov will be that I will Word Gov."
    // "I will World Gov. Wow. Venus, up to 10%."
    green.doTask("VenusStep! BY Admin")

    // "Then we move all the colony tracks up one."
    // "Most of them are on the penultimate space on the track, but Enceladus still generates only"
    // "three microbes. Miranda is still out of play."
    admin.assertCounts(
        5 to "ColonyProduction<Callisto>",
        5 to "ColonyProduction<Luna>",
        5 to "ColonyProduction<Triton>",
        3 to "ColonyProduction<Enceladus>",
        0 to "Miranda",
    )
  }

  private fun generation5() {

    // "Now it's officially Generation 5. You get the start marker."
    // "We take cubes off cards and draft."
    with(green) {
      assertProduction(m = 3, s = 0, t = 0, p = 2, e = 3, h = 0)
      assertResources(m = 32, s = 0, t = 0, p = 9, e = 3, h = 0)
    }
    with(yellow) {
      assertProduction(m = 10, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 42, s = 0, t = 0, p = 11, e = 0, h = 16)
    }
    assertSidebar(gen = 5, temp = -28, oxygen = 4, oceans = 8)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (12:48 pm) "Shit. It's ruining everything. I need them. I'm buying three cards."
    // "And discarding one."
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    yellow.assertCounts(33 to "MC") // ledger entry 124
    // "I believe I did the same."
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    // "I will spend eight money to place the last ocean."
    with(yellow) {
      // "I get a TR."
      // "It'll go on the two-one space for two plants and two money."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Hellas_2_1>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      // "And I will play... Energy Market."
      doTasks(*playCardTasks("$EnergyMarket"), "3 Pay<> FROM MC", "$EnergyMarket FROM ProjectCard")
    }
    // HACK: The ledger says Yellow paid six here, although the recording only names Energy Market.
    yellow.exMachina("-3 MC")

    yellow.assertCounts(21 to "MC") // ledger entry 130

    // (12:51 pm) "I'm gonna spend three energy to fly my little ship to Luna."
    // "And I simply take 13 money. The track goes all the way down. And the track zoops."
    with(green) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "13 MC",
      )
      doTask("PassBaton")
    }

    // "I spend six money to gain three energy."
    with(yellow) {
      doTasks(*cardActionTasks("$EnergyMarket"), "-6 MC", "3 Energy")
      // "And then I will use the three energy to trade with Callisto and get ten."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Callisto>",
          "10 Energy",
      )
    }

    // "Come to think of it, I don't know why I did that urgently..."
    // HACK: Despite saying "spend six money," she did not record a second six-money payment.
    yellow.exMachina("6 MC")
    yellow.assertCounts(21 to "MC") // ledger entry 130

    with(green) {
      // "Earth Catapult for 23."
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // (12:54 pm) "I'll spend five on Ishtar Mining."
      // "It requires Venus at 8%, and I get a titanium production."
      doTasks(
          *playCardTasks("$IshtarMining"),
          "5 Pay<> FROM MC",
          "$IshtarMining FROM ProjectCard",
          "PROD[Titanium]",
      )
      doTask("PassBaton")
    }

    // "Well, I think it's time to play Lunar Mining. That cost me eleven. No, it cost me nine."
    // "It gives me two titanium production."
    with(green) {
      doTasks(
          *playCardTasks("$LunarMining"),
          "9 Pay<> FROM MC",
          "$LunarMining FROM ProjectCard",
          "PROD[2 Titanium]",
      )
      assertCounts(4 to "MC") // ledger entry 85
      doTask("PassBaton")
    }

    // "I raise the temperature twice because I have 16 heat."
    // "The temperature is now −24°C. I get two TR."
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

    with(green) {
      // "I'm gonna use Venusian Insects to give myself a microbe."
      // "That gives me one money."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm gonna play... Ironworks for 11. Boop."
      doTasks(*playCardTasks("$Ironworks"), "11 Pay<> FROM MC", "$Ironworks FROM ProjectCard")
      doTask("PassBaton")
    }

    // "I use Search for Life to spend one money and flip Forced Precipitation."
    // "That does not get me a science resource."
    with(green) {
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I will spend four energy on Ironworks to gain a steel."
      // "Oxygen goes to 5%. I get a TR."
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(5 to "OxygenStep")
    // "I am passing."
    green.doTask("Pass")

    // (12:58 pm) "I will spend eight plants to place a greenery. Oxygen is now at six."
    with(yellow) {
      // "I get a TR."
      // "I put it at the top right on the one-plant spot."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_5>",
          "OxygenStep",
          "Plant",
          "TerraformRating",
      )
      assertCounts(6 to "OxygenStep")

      // "I spend four money and no steel on Biomass Combustors."
      // "Decrease any plant production one step." "That's me."
      // "Because I'm Mons, I have to pay you, but I only have one. Sorry, that was
      // unintentional."
      // "I increase my energy production two steps."
      doTasks(
          *playCardTasks("$BiomassCombustors"),
          "Ok", // No steel paid.
          "4 Pay<> FROM MC",
          "$BiomassCombustors FROM ProjectCard",
          "PROD[2 Energy]",
      )
      assertCounts(1 to "MC") // ledger entry 154
      doTasks("PROD[-Plant<Green>]", "MC<Green> FROM MC")
      green.assertCounts(5 to "MC") // ledger entry 92
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("44 MC", "5 Plant", "2 Heat", "6 Heat FROM Energy", "Titanium", "2 Energy")
    green.doTasks("31 MC", "Plant", "3 Energy", "2 Titanium")
    // "We do production."
    // "I use World Government Terraforming to raise Venus to 12%."
    yellow.doTask("VenusStep! BY Admin")
  }

  private fun generation6() {

    // "I take the start player marker. Generation goes to six."
    with(green) {
      assertProduction(m = 3, s = 0, t = 2, p = 1, e = 3, h = 0)
      assertResources(m = 36, s = 0, t = 2, p = 10, e = 3, h = 0)
    }
    with(yellow) {
      assertProduction(m = 10, s = 0, t = 1, p = 5, e = 2, h = 2)
      assertResources(m = 44, s = 1, t = 1, p = 11, e = 2, h = 8)
    }
    assertSidebar(gen = 6, temp = -24, oxygen = 6, oceans = 9)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (1:02 pm) "What a bunch of junk." "I'm keeping two cards."
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    // (1:04 pm) "I'm actually gonna not buy this card either."
    // "I'm gonna give myself my three money back again. I bought no cards this time."
    green.doTask("-4 ProjectCard<Selecting>")

    // See board-13-04-48-corrected.png - verified
    green.assertCounts(14 to "CardFront")
    green.assertTags(but = 2, spt = 1, sct = 1, pot = 1, eat = 4, vet = 5, plt = 1, mit = 2)
    yellow.assertCounts(13 to "CardFront")
    yellow.assertTags(but = 5, spt = 1, sct = 2, pot = 2, eat = 1, vet = 1, plt = 2, cit = 1)
    admin.assertCounts(14 to "Tile")

    // "I'm going to fly my ship to Triton by paying three energy."
    // "That lets me take five titanium, bringing me up to seven titanium."
    with(green) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "5 Titanium",
      )
      assertCounts(7 to "Titanium")

      // Green uses his unusual second action because Cupola City's maximum-oxygen requirement is at
      // risk: "while I still can."
      // "And I'm gonna play Cupola City while I still can, although there's still some time."
      // "That costs me 16."
      // "I lose an energy production, and I gain three money production, and I place a shitty
      // tile."
      // "I'm going to place it on the one-steel space at row three, column three."
      // "I take a steel."
      doTasks(
          *playCardTasks("$CupolaCity"),
          "14 Pay<> FROM MC",
          "$CupolaCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Hellas_3_3>",
          "Steel",
      )
    }
    // "I'm gonna pay 12 for Ecological Zone." "Oh my, does it hold animals?"
    with(yellow) {
      // "It holds animals, so Miranda finally comes into play."
      // "I place the tile adjacent to a greenery on the almost-rightmost space in the fourth"
      // "row. That's four money and a plant."
      doTasks(
              *playCardTasks("$EcologicalZone"),
              "12 Pay<> FROM MC",
              "$EcologicalZone FROM ProjectCard",
              "Animal<$EcologicalZone>",
              "Animal<$EcologicalZone>",
              "EcologicalZone_SpecialTile<Hellas_4_8>",
              "Plant",
              "2 MC",
              "2 MC",
          )
          .expect("Miranda")
      assertCounts(30 to "MC") // ledger entry 169
      // "Landshaper." "I was just gonna get that. I hate you."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Landshaper")

      // (1:08 pm) "That makes me so sad." "I should also add two animals to EcoZone because of
      // its effect."
      assertCounts(2 to "Animal<$EcologicalZone>")
    }

    // "I take back the two extra money I spent because I spent 16 on Cupola City and was only"
    // "supposed to spend 14 on it. Now I'm back up to 22 money."
    green.assertCounts(22 to "MC") // ledger entry 109

    // "I'll spend the eight plants. I'm going to put the greenery on the two-plants, four-money"
    // "spot. I raise oxygen to 7% and get a TR."
    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_2>",
          "OxygenStep",
          "2 Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      admin.assertCounts(7 to "OxygenStep")
      doTask("PassBaton")
    }

    // "I will do my plant forest."
    with(yellow) {
      // "I'm going to put it at row five, column nine."
      // "I get a plant, two money, and an oxygen raise."
      // "I have to manually input the temperature raise and the extra TR."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_9>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
      )
      // "My second action is to raise the temperature with my eight heat."
      // "That gives me an extra TR."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "PROD[Heat]",
      )
    }

    with(green) {
      // (1:12 pm) "Why do I play badly?" "Temp is at minus 20, and oxygen is at eight."
      // "Oh no, I didn't. I spent the titanium. What am I doing?"
      // "I don't want to spend the six. So I stay at 26 money and five titanium."
      doTasks(
          *playCardTasks("$RotatorImpacts"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$RotatorImpacts FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    // "I play Kelp Farming for 17. We have the six oceans."
    with(yellow) {
      // "I gain two money production, three plant production, and two plants."
      // "I add an animal to Ecological Zone."
      doTasks(
          *playCardTasks("$KelpFarming"),
          "17 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
          "Animal<$EcologicalZone>",
      )
      assertCounts(7 to "MC") // ledger entry 186
      doTask("PassBaton")
    }

    // "I'm going to play Nuclear Power, which costs eight. I spend one steel and six money."
    with(green) {
      // "I lose two money production and gain three energy production."
      doTasks(
          *playCardTasks("$NuclearPower"),
          "1 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
      assertCounts(19 to "MC") // ledger entry 122
      doTask("PassBaton")
    }

    // (1:15 pm) "Energy Market: pay four money, gain two energy."
    with(yellow) {
      doTasks(*cardActionTasks("$EnergyMarket"), "-4 MC", "2 Energy")
      // "I use Ironworks to pay four energy, gain a steel, and raise oxygen."
      // "Oxygen is at 9%. I get a TR."
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      admin.assertCounts(9 to "OxygenStep")
    }

    with(green) {
      // "I... I... I... I am going to spend eight to get Venophile funded."
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Venuphile")
      doTask("PassBaton")
    }
    // "I pass."
    yellow.doTask("Pass")

    // "I am going to spend 11 on Venusian Plants, which is all the money I have."
    with(green) {
      // (1:17 pm) "I meet the requirement of 16%. I raise Venus one step, which gives me one
      // TR."
      // "I put a microbe on Venusian Insects, which gives me one money."
      doTasks(
          *playCardTasks("$VenusianPlants"),
          "11 Pay<> FROM MC",
          "$VenusianPlants FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "Microbe<$VenusianInsects>",
          "MC",
      )
      // "I'm going to use... thing. Venusian Insects itself to put another cube on Venusian Insects
      // and take"
      // "another money."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      // "I'm going to use Rotator Impacts to spend two titanium and put an asteroid on Rotator"
      // "Impacts."
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "Asteroid<$RotatorImpacts>",
      )
      // "I'm going to use Search for Life to pay one money and flip Titan Shuttles, which does not"
      // "get me a thing thing."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      assertCounts(1 to "MC") // ledger entry 130

      // "I pass. We do production."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("50 MC", "8 Plant", "3 Heat", "Titanium", "2 Energy")
    green.doTasks("34 MC", "Plant", "5 Energy", "2 Titanium")
    // "For World Government Terraforming, I'm going to raise the temperature."
    // "The temperature is now −18°C."
    green.doTask("TemperatureStep! BY Admin")
  }

  private fun generation7() {

    // "We raise the colonies one step each. We give you the start player marker."
    // "It becomes Generation 7. We take cubes off cards."
    with(green) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 35, s = 0, t = 6, p = 5, e = 5, h = 0)
    }
    with(yellow) {
      assertProduction(m = 12, s = 0, t = 1, p = 8, e = 2, h = 3)
      assertResources(m = 53, s = 2, t = 2, p = 15, e = 2, h = 3)
    }
    assertSidebar(gen = 7, temp = -18, oxygen = 9, oceans = 9)

    // See board-13-21-29-corrected.png - verified
    green.assertCounts(18 to "CardFront")
    green.assertTags(
        but = 4,
        spt = 2,
        sct = 1,
        pot = 2,
        eat = 4,
        vet = 6,
        plt = 2,
        mit = 2,
        cit = 1,
    )
    yellow.assertCounts(15 to "CardFront")
    yellow.assertTags(5, spt = 1, sct = 2, pot = 2, eat = 1, vet = 1, plt = 4, ant = 1, cit = 1)
    admin.assertCounts(18 to "Tile")

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (6:21 pm) "We're now playing Generation 7."
    // "I'm buying three cards and discarding one."
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    // "I am... stupidly buying all of my cards. So, 12."
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I think I'm just gonna Standard Project City."
      // "I put it at row one, column two, for two plants and two money."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_1_2>",
          "2 Plant",
          "2 MC",
      )
      assertCounts(21 to "MC") // ledger entry 213
      // "I spend eight plants on a greenery at row one, column three, for two plants."
      // "Oxygen goes up to 10%. I get a TR."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_1_3>",
          "OxygenStep",
          "2 Plant",
          "TerraformRating",
      )
      admin.assertCounts(10 to "OxygenStep")
    }

    // "Big ass... toroid. Big Asteroid!"
    // "I'm gonna spend all six titanium, plus seven real."
    // "I'm going to get four titanium, two temperature raises, and two TR."
    // "That takes the temperature to −14°C."
    with(green) {
      // "You lose four plants."
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "TerraformRating",
          "TerraformRating",
          "-4 Plant<Yellow>",
      )
      doTask("PassBaton")
    }

    // (6:25 pm) "I think I'm gonna play some Nitrophilic Moss. It's eight money."
    with(yellow) {
      // "I lose two plants, gain two plant production, and add an animal to Ecological Zone."
      doTasks(
          *playCardTasks("$NitrophilicMoss"),
          "8 Pay<> FROM MC",
          "$NitrophilicMoss FROM ProjectCard",
          "PROD[2 Plant]",
          "-2 Plant",
          "Animal<$EcologicalZone>",
      )
      doTask("PassBaton")
    }

    // "Titan Floating Launch-Pad. That costs all 16 of my money."
    with(green) {
      // "It tells me to add two floaters to any Jovian card. I wonder which one I will pick."
      // "It'll be itself."
      doTasks(
          *playCardTasks("$TitanFloatingLaunchPad"),
          "16 Pay<> FROM MC",
          "$TitanFloatingLaunchPad FROM ProjectCard",
          "2 Floater<$TitanFloatingLaunchPad>",
      )
      assertCounts(0 to "MC") // ledger entry 145
      doTask("PassBaton")
    }

    with(yellow) {
      // (6:27 pm) "I will spend four money to gain two energy using Energy Market."
      doTasks(*cardActionTasks("$EnergyMarket"), "-4 MC", "2 Energy")
      doTask("PassBaton")
    }

    with(green) {
      // "I will sell a card."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I will spend four energy to gain a steel and raise the oxygen."
      // "Oxygen is at 11%."
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(11 to "OxygenStep")

    with(green) {
      // "I'm playing a trans card for four money, which I'm going to do as one titanium, one
      // money."
      // "It's Trans-Neptune Probe and that's it. It just—that's it."
      doTasks(
          *playCardTasks("$TransNeptuneProbe"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$TransNeptuneProbe FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    // "I play Mercurian Alloys for three money."
    with(yellow) {
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // No titanium paid.
          "3 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
      assertCounts(6 to "MC") // ledger entry 230

      // "I play Solar Wind Power for two titanium and three money."
      // "I increase energy production one step and gain two titanium."
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
    }

    with(green) {
      // (6:30 pm) "Oh jeez, I've been sitting on this the whole time. I'll use Rotator Impacts"
      // "to remove an asteroid and do a Venus raise to 16%, which gets me"
      // "two TR."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(8 to "VenusStep")
    // "I pass."
    yellow.doTask("Pass")

    // "I use Titan Floating Launch-Pad, lose a floater, and take four microbes from Enceladus."
    // "They go on Venusian Insects, and that gives me four money from Topsoil Contract."
    with(green) {
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Enceladus>",
          "4 Microbe<$VenusianInsects>",
          "4 MC",
      )
      // "Then I will use Venusian Insects itself to get one more and one more
      // money."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      assertCounts(5 to "MC") // ledger entry 152

      // "I use Search for Life, spend one, and flip Research Coordination."
      // "Which, even though it's a wild tag, it does not count."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      // (6:33 pm) "I pass. We hit production."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("53 MC", "10 Plant", "3 Heat", "Titanium", "3 Energy")
    green.doTasks("38 MC", "Plant", "5 Energy", "2 Titanium", "5 Heat FROM Energy")
    // "I use World Government Terraforming to raise the temperature to −12°C."
    yellow.doTask("TemperatureStep! BY Admin")
  }

  private fun generation8() {

    // "Then we're gonna give me the start thingy. We're gonna raise the generation to eight."
    // "We're gonna take cubes off cards. Uncube. We're gonna uncube. And then we're gonna draft."
    with(green) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 42, s = 0, t = 5, p = 6, e = 5, h = 5)
    }
    with(yellow) {
      assertProduction(m = 13, s = 0, t = 1, p = 10, e = 3, h = 3)
      assertResources(m = 56, s = 3, t = 3, p = 15, e = 3, h = 6)
    }
    assertSidebar(gen = 8, temp = -12, oxygen = 11, oceans = 9)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // "I'll buy two cards and discard two cards. I'll do the same."
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

    // (6:37 pm) "God damn it. We play Penguins, which costs five."
    with(green) {
      doTasks(*playCardTasks("$Penguins"), "5 Pay<> FROM MC", "$Penguins FROM ProjectCard")
      // "And then we just trade three energy to grab two measly little aminals from Miranda."
      // The later Generation 9 ledger and Green's evidenced eight-heat conversion show that the
      // trade
      // was actually titanium-funded; the transcript's stated payment cannot produce both facts.
      // "Then I can't do it. I already used my two actions."
      // "But I can't use it now because I used two actions to both play Penguins and trade."
      doTasks(
          "UseAction<TradeAction, Action3>",
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Trade<Miranda>",
          "2 Animal<$Penguins>",
      )
    }

    // (7:51 pm) "I spend three energy to trade with Callisto and get five energy."
    with(yellow) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Callisto>",
          "5 Energy",
      )
      // "I use Ironworks to spend four energy on steel and oxygen."
      // "Oxygen goes up to 12%. I get a TR."
      doTasks(
          *cardActionTasks("$Ironworks"),
          "4 Pay<Class<Energy>> FROM Energy",
          "Steel",
          "OxygenStep",
          "TerraformRating",
      )
      admin.assertCounts(12 to "OxygenStep")
    }

    // (7:54 pm) "Let us finally play Strato Birds. That cost me ten."
    // "I lose a floater from Titan Floating Launch-Pad to do that."
    with(green) {
      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "10 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater<$TitanFloatingLaunchPad>",
      )
      assertCounts(21 to "MC") // ledger entry 171
      doTask("PassBaton")
    }

    // "I'm going to use the City standard project."
    with(yellow) {
      // "I place the city at row three, column five, for two money."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_3_5>",
          "2 MC",
      )
      assertCounts(27 to "MC") // ledger entry 257

      // "I convert eight plants to a greenery."
      // "I place it at row two, column four, where I get a plant and steel."
      // "Oxygen is now at 13%. I get another TR."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_2_4>",
          "OxygenStep",
          "Plant",
          "Steel",
          "TerraformRating",
      )
      admin.assertCounts(13 to "OxygenStep")
    }

    // (7:56 pm) "Boids. I played boids for eight, bringing me down to 13."
    with(green) {
      // "That lowers [Yellow]'s plant production by two."
      doTasks(
          *playCardTasks("$Birds"),
          "8 Pay<> FROM MC",
          "$Birds FROM ProjectCard",
          "PROD[-2 Plant<Yellow>]",
      )
      doTask("PassBaton")
    }

    // "I'm going to spend eight plants on a greenery."
    with(yellow) {
      // "And that will be the four-five for a steel and four money."
      // "You're slaughtering me."
      // "Oh man, you got the last oxygen. I never win this game."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_4_5>",
          "OxygenStep",
          "Steel",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
      admin.assertCounts(14 to "OxygenStep")

      // "Before I forget, I'm going to claim Mayor for eight."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Mayor")
    }

    with(green) {
      // "I'm going to spend one on Extremophiles. I have the two science tags it needs."
      doTasks(
          *playCardTasks("$Extremophiles"),
          "1 Pay<> FROM MC",
          "$Extremophiles FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "Before I forget, I'm going to claim Producer for eight. I have all three milestones."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Producer")
      doTask("PassBaton")
    }

    // "I'm going to use Venusian Insects and add a microbe to Venusian Insects and take the"
    // "money."
    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      assertCounts(13 to "MC") // ledger entry 176
      doTask("PassBaton")
    }

    with(yellow) {
      // (7:59 pm) "I'm going to pay 14 for Botanist. Botanist is funded."
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Botanist")
      doTask("PassBaton")
    }

    with(green) {
      // "I am going to play Satellites."
      // "It would cost eight, but I'm spending six worth of titanium and two money."
      // "I'm getting four money production, bringing me up to eight money production."
      doTasks(
          *playCardTasks("$Satellites"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$Satellites FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }
    // "I pass."
    yellow.doTask("Pass")

    // "I add a Penguin, a Strato Bird, and a Bird."
    with(green) {
      doTasks(*cardActionTasks("$Penguins"), "Animal<$Penguins>")
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTasks(*cardActionTasks("$Birds"), "Animal<$Birds>")
      // "I use Extremophiles to add a Venusian Insect and take a money."
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$VenusianInsects>", "MC")
      // "I use Rotator Impacts to spend six money to add an asteroid."
      doTasks(*cardActionTasks("$RotatorImpacts"), "6 Pay<> FROM MC", "Asteroid<$RotatorImpacts>")
      // "I use Titan Floating Launch-Pad to add a floater to Titan Floating Launch-Pad."
      doTasks(*cardActionTasks("$TitanFloatingLaunchPad"), "Floater<$TitanFloatingLaunchPad>")
      // "I use Search for Life to pay one and flip Deep Well Heating, which is not a microbe tag."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      assertCounts(5 to "MC") // ledger entry 181

      // "I pass. We do production."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("57 MC", "8 Plant", "3 Heat", "Titanium", "3 Energy", "Heat FROM Energy")
    green.doTasks("42 MC", "Plant", "5 Energy", "2 Titanium", "5 Heat FROM Energy")
    // "I use World Government Terraforming to raise the temperature to −10°C."
    green.doTask("TemperatureStep! BY Admin")
  }

  private fun generation9() {

    // "I'm giving [Yellow] the start marker. It's now Generation 9. Cubes come off cards."
    with(green) {
      assertProduction(m = 8, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 47, s = 0, t = 2, p = 7, e = 5, h = 10)
    }
    with(yellow) {
      assertProduction(m = 14, s = 0, t = 1, p = 8, e = 3, h = 3)
      assertResources(m = 58, s = 6, t = 4, p = 8, e = 3, h = 10)
    }
    assertSidebar(gen = 9, temp = -10, oxygen = 14, oceans = 9)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (8:03 pm) "We draft. I'll buy one card and discard three."
    // The ledger records Yellow buying three cards and Green buying none.
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    green.doTask("-4 ProjectCard<Selecting>")

    // (8:05 pm) "I believe I start by paying three energy to trade with Luna. That's ten."
    with(yellow) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Luna>",
          "10 MC",
      )
      // "I'm going to use the City standard project on the plant-and-steel space."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_1_4>",
          "Plant",
          "Steel",
      )
      assertCounts(34 to "MC") // ledger entry 288
    }

    // "I'm going to play Eos Chasma National Park, which costs 14."
    with(green) {
      // "It gives me three plants."
      // "It gives me an aminal, which I'm putting on Penguins."
      // "It gives me two money production. We meet the requirement."
      doTasks(
          *playCardTasks("$EosChasmaNationalPark"),
          "14 Pay<> FROM MC",
          "$EosChasmaNationalPark FROM ProjectCard",
          "3 Plant",
          "PROD[2 MC]",
          "Animal<$Penguins>",
      )
      // Green uses the park's three plants immediately as his second action.
      // "I'm going to do plant boop, which should not give me TR: convert plants to greenery."
      // "I'm going to place it at row three, column two, where I get one plant and two
      // money."
      // "It does not give me a TR."
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_3_2>",
              "OxygenStep",
              "Plant",
              "2 MC",
          )
          .expect("0 TerraformRating")
    }

    with(yellow) {
      // (8:08 pm) "I will pay ten money for Rego Plastics."
      doTasks(
          *playCardTasks("$RegoPlastics"),
          "Ok", // No steel paid.
          "10 Pay<> FROM MC",
          "$RegoPlastics FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I use Venusian Insects to add a cube to Venusian Insects and take one money."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      doTask("PassBaton")
    }

    // "I will pay four steel, because it's worth three now, for Industrial Microbes."
    with(yellow) {
      // "I increase energy production and steel production one step each."
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
      assertCounts(4 to "PROD[Energy]") // ledger entry 294
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to pay 20 to fund the only award that I have a chance at, which is
      // Magnate."
      doTasks("UseAction<FundAwardAction, Action1>", "20 Pay<> FROM MC", "Magnate")
      doTask("PassBaton")
    }

    // "I will pay four titanium for 16 and 12 money for Methane from Titan."
    with(yellow) {
      // "I increase heat production two steps and plant production two steps."
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "12 Pay<> FROM MC",
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Maxwell Edison... I'm sorry, Maxwell Base, costs all 16 money that I have."
      // "It gives me a negative energy production, taking me down to four."
      // "And I place a shitty tile on the Maxwell Base space area and put a delegate on it."
      doTasks(
          *playCardTasks("$MaxwellBase"),
          "16 Pay<> FROM MC",
          "$MaxwellBase FROM ProjectCard",
          "PROD[-Energy]",
          "NormalCityTile<MaxwellBase_RemoteArea>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // (8:12 pm) "I raise the temperature to −8°C with eight heat. I get a TR."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(11 to "TemperatureStep")

    with(green) {
      // "I use Maxwell Base to add a Stratospheric Bird."
      doTasks(*cardActionTasks("$MaxwellBase"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    // "I play Nitrite Reducing Bacteria for 11. I get three free microbes on that."
    with(yellow) {
      doTasks(
          *playCardTasks("$NitriteReducingBacteria"),
          "11 Pay<> FROM MC",
          "$NitriteReducingBacteria FROM ProjectCard",
          "3 Microbe<$NitriteReducingBacteria>",
      )
      assertCounts(1 to "MC") // ledger entry 304
      doTask("PassBaton")
    }

    with(green) {
      // "I use Rotator Impacts to remove an asteroid and raise Venus to 18%."
      // "That gives me a TR, taking me to 35 TR."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(9 to "VenusStep")

    with(yellow) {
      // "I use Nitrite Reducing Bacteria to remove three microbes and get a TR."
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    // "I use Extremophiles to add a Venusian Insect, which gives me one money."
    with(green) {
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$VenusianInsects>", "MC")
      assertCounts(1 to "MC") // ledger entry 201
      doTask("PassBaton")
    }
    // "I pass."
    yellow.doTask("Pass")

    // "I use all three of my animal cards to take one animal each."
    // "That gives me my fifth Penguin."
    with(green) {
      doTasks(*cardActionTasks("$Penguins"), "Animal<$Penguins>")
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTasks(*cardActionTasks("$Birds"), "Animal<$Birds>")
      // "I will use Titan Floating Launch-Pad to remove one floater and trade with, believe it or
      // not, Miranda to take one"
      // "animal. I'll put that on Penguins."
      doTasks(
          *cardActionTasks("$TitanFloatingLaunchPad", "Action2"),
          "-Floater<$TitanFloatingLaunchPad>",
          "Trade<Miranda>",
          "Animal<$Penguins>",
      )
      // (8:15 pm) "I use Search for Life to spend the only money I have and flip AI Central, which"
      // "does not have a microbe tag."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      // "I do a heat raise to −6°C. That gives me a TR, so I'm at 36 TR."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      admin.assertCounts(12 to "TemperatureStep")

      // "We do production. You get to do World Government Terraforming."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks("60 MC", "10 Plant", "5 Heat", "Titanium", "4 Energy", "Steel")
    green.doTasks("46 MC", "Plant", "4 Energy", "2 Titanium", "5 Heat FROM Energy")
    // "We'll move the temperature. The temperature is now −4°C."
    yellow.doTask("TemperatureStep! BY Admin")
  }

  private fun generation10() {

    // "We raise all the colony tracks and take the fleets back."
    // "The start player marker goes to me. The generation is now ten."
    // "Action-used markers come off cards."
    with(green) {
      assertProduction(m = 10, s = 0, t = 2, p = 1, e = 4, h = 0)
      assertResources(m = 46, s = 0, t = 4, p = 4, e = 4, h = 7)
    }
    with(yellow) {
      assertProduction(m = 15, s = 1, t = 1, p = 10, e = 4, h = 5)
      assertResources(m = 61, s = 4, t = 1, p = 19, e = 4, h = 7)
    }
    assertSidebar(gen = 10, temp = -4, oxygen = 14, oceans = 9)

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // "I'm buying two cards. I'm buying three cards."
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    // (8:18 pm) "I'm going to use three energy to fly to Enceladus and take three microbes."
    // "My three microbes go onto Venusian Insects and give me three money."
    with(green) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
          "3 Microbe<$VenusianInsects>",
          "3 MC",
      )
      doTask("PassBaton")
    }

    // "I can pay 25 for a City standard project."
    with(yellow) {
      // "It'll go at row five, column five, for two money."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Hellas_5_5>",
          "2 MC",
      )
      assertCounts(32 to "MC") // ledger entry 322

      // "Then I'll convert plants to a greenery at row six, column six, for four money."
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_6_6>",
              "OxygenStep",
              "2 MC",
              "2 MC",
          )
          .expect("0 TerraformRating")
    }

    // "What I'm going to do is play Restricted Area for nine."
    with(green) {
      // (8:21 pm) "Thank you. You know, screw that. I'll give back the two steel."
      // "I'm going to place Restricted Area on the one-card space that is two above the south
      // pole"
      // "so that I can take a card."
      doTasks(
          *playCardTasks("$RestrictedArea"),
          "9 Pay<> FROM MC",
          "$RestrictedArea FROM ProjectCard",
          "RestrictedArea_SpecialTile<Hellas_7_6>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }
    green.assertCounts(31 to "MC") // ledger entry 217

    // "I'm going to play my Sub-Zero Salt Fish." "Oh, I guess I lose a plant production."
    with(yellow) {
      doTasks(
          *playCardTasks("$SubZeroSaltFish"),
          "5 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "PROD[-Plant<Green>]",
          "3 MC<Green> FROM MC",
      )
      doTask("PassBaton")
    }
    // "Like that's gonna change much."
    // HACK: That exchange never mentions the Mons Insurance payment, which we omitted physically.
    green.exMachina("3 MC<Yellow> FROM MC<Green>")
    green.assertCounts(31 to "MC") // ledger entry 217
    yellow.assertCounts(31 to "MC") // ledger entry 325

    with(green) {
      // "I use Restricted Area to draw a card."
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      doTask("PassBaton")
    }
    // HACK: I narrated drawing the card but never mentioned Restricted Area's two-money cost.
    green.exMachina("2 MC")
    green.assertCounts(31 to "MC") // ledger entry 217

    // "I play Medical Lab. I'll spend my four steel as 12 money plus one money."
    // "I increase money production one step for every two building tags."
    // "I have eight building tags, so that's four money production."
    with(yellow) {
      doTasks(
          *playCardTasks("$MedicalLab"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$MedicalLab FROM ProjectCard",
          "PROD[4 MC]",
      )
      assertCounts(25 to "PROD[MC]") // ledger entry 328; 25 really means 20
      doTask("PassBaton")
    }

    // (8:24 pm) "I'm gonna play Atalanta Planitia Lab for eight money,"
    // "because I can now finally. Finally."
    // "That gives me two cards."
    with(green) {
      doTasks(
          *playCardTasks("$AtalantaPlanitiaLab"),
          "8 Pay<> FROM MC",
          "$AtalantaPlanitiaLab FROM ProjectCard",
          "2 ProjectCard",
      )
      assertCounts(23 to "MC") // ledger entry 221
      doTask("PassBaton")
    }

    // "I'm going to play Venus Soils for 20. I raise Venus one step, which gives me a TR."
    // "I increase plant production one step. I add two microbes to Nitrite Reducing Bacteria."
    // "I also add an animal to Ecological Zone."
    with(yellow) {
      doTasks(
          *playCardTasks("$VenusSoils"),
          "20 Pay<> FROM MC",
          "$VenusSoils FROM ProjectCard",
          "VenusStep",
          "PROD[Plant]",
          "Animal<$EcologicalZone>",
          "TerraformRating",
          "2 Microbe<$NitriteReducingBacteria>",
      )
      assertCounts(10 to "MC") // ledger entry 329
      doTask("PassBaton")
    }

    // "I play Invention Contest for free and choose this card."
    // Green's Earth Catapult discount reduces its printed 2-M€ cost to zero.
    with(green) {
      doTasks(
          *playCardTasks("$InventionContest"),
          "$InventionContest FROM ProjectCard",
          "3 ProjectCard<Selecting>",
          "ProjectCard<Hand FROM Selecting>",
      )
      // "For my second action, I play Lawsuit for free. You lowered my production this generation."
      // "So you lose three money and I gain three money?"
      // "My Mons doesn't activate for that. I would just pay myself."
      doTasks(*playCardTasks("$LawSuit"), "$LawSuit FROM ProjectCard", "3 MC FROM MC<Yellow>")
    }
    green.assertCounts(26 to "MC") // ledger entry 222
    yellow.assertCounts(7 to "MC") // ledger entry 332

    with(yellow) {
      // "I add a Sub-Zero Salt Fish."
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTask("PassBaton")
    }

    // "I'm going to spend 23 and put a greenery at the only spot next to my city that isn't next"
    // "to one of yours. I get a steel."
    with(green) {
      doTasks(
              *standardProjectTasks("GreeneryProject"),
              "23 Pay<> FROM MC",
              "GreeneryTile<Hellas_4_3>",
              "OxygenStep",
              "Steel",
          )
          .expect("0 TerraformRating")
      doTask("PassBaton")
    }
    yellow.assertCounts(7 to "MC") // ledger entry 332

    with(yellow) {
      // (8:28 pm) "I add a Nitrite Reducing Bacterium."
      doTasks(*cardActionTasks("$NitriteReducingBacteria"), "Microbe<$NitriteReducingBacteria>")
      doTask("PassBaton")
    }

    // "I play Harvest for two and then get 12. Now I have 13 money."
    with(green) {
      doTasks(*playCardTasks("$Harvest"), "2 Pay<> FROM MC", "$Harvest FROM ProjectCard", "12 MC")
      assertCounts(13 to "MC") // ledger entry 226
      doTask("PassBaton")
    }

    // "I'll spend three energy to trade with Miranda for one measly animal, which I'll"
    // "play on Sub-Zero Salt Fish just to even the score or whatever."
    with(yellow) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "Animal<$SubZeroSaltFish>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to play Floating Habs for three. I meet the requirement."
      doTasks(*playCardTasks("$FloatingHabs"), "3 Pay<> FROM MC", "$FloatingHabs FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I use Energy Market to pay six money for three energy."
      doTasks(*cardActionTasks("$EnergyMarket"), "-6 MC", "3 Energy")
      doTask("PassBaton")
    }

    // "I use Titan Floating Launch-Pad to add a floater to Titan Floating Launch-Pad."
    with(green) {
      doTasks(*cardActionTasks("$TitanFloatingLaunchPad"), "Floater<$TitanFloatingLaunchPad>")
      doTask("PassBaton")
    }
    // (8:30 pm) "I pass."
    yellow.doTask("Pass")

    // "I'm going to use Venusian Insects to get a microbe and a money."
    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      // "I'm going to use Penguins, Stratospheric Birds, and Birds to get one animal on each."
      doTasks(*cardActionTasks("$Penguins"), "Animal<$Penguins>")
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTasks(*cardActionTasks("$Birds"), "Animal<$Birds>")
      // "I'm going to pay two to Floating Habs to put a floater on Floating Habs."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
      assertCounts(9 to "MC") // ledger entry 229

      // "I'm going to use Maxwell Base to add an animal to Penguins."
      // Maxwell Base can only target a Venus card; no later resource assertion establishes that the
      // narrated illegal Penguins placement occurred, so retain the legal Stratospheric Birds
      // target.
      doTasks(*cardActionTasks("$MaxwellBase"), "Animal<$StratosphericBirds>")
      // "I'm going to use Extremophiles to add a microbe to Venusian Insects and give myself"
      // "another money."
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$VenusianInsects>", "MC")
      // "I'm going to use Rotator Impacts to pay two titanium to put an asteroid on Rotator
      // Impacts."
      doTasks(
          *cardActionTasks("$RotatorImpacts"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "Asteroid<$RotatorImpacts>",
      )
      // "I'm going to use Search for Life to spend one and flip this microbe tag right here, which"
      // "is Deimos Down, which is not a microbe tag. So I still do not get a science resource."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")
      // "I pass."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks(
        "66 MC",
        "11 Plant",
        "5 Heat",
        "Titanium",
        "4 Energy",
        "Steel",
        "4 Heat FROM Energy",
    )
    green.doTasks("46 MC", "4 Energy", "2 Titanium", "Heat FROM Energy")
    // (8:32 pm) "We do production. I will World Gov temperature up to minus two."
    green.doTask("TemperatureStep! BY Admin")
  }

  private fun generation11() {

    // "We will move all the dinguses on the colonies and send the fleets back."
    // "We move the generation to 11 and the start player marker to [Yellow]."
    with(green) {
      assertProduction(m = 10, s = 0, t = 2, p = 0, e = 4, h = 0)
      assertResources(m = 55, s = 1, t = 4, p = 4, e = 4, h = 8)
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 1, p = 11, e = 4, h = 5)
      assertResources(m = 67, s = 1, t = 2, p = 22, e = 4, h = 16)
    }
    assertSidebar(gen = 11, temp = -2, oxygen = 14, oceans = 9)

    // See board-20-34-03-corrected.png - verified
    green.assertCounts(30 to "CardFront")
    green.assertTags(5, 4, 4, 2, eat = 4, jot = 1, vet = 11, plt = 3, mit = 3, ant = 3, cit = 2)
    yellow.assertCounts(25 to "CardFront")
    yellow.assertTags(8, 4, 4, 3, eat = 1, jot = 1, vet = 2, plt = 6, mit = 2, ant = 2, cit = 1)
    admin.assertCounts(30 to "Tile")

    // HACK: I must have fat-fingered this!?
    green.exMachina("Plant")
    green.assertCounts(5 to "Plant") // ledger entry 242

    // Research.
    yellow.doTask("4 ProjectCard<Selecting>")
    green.doTask("4 ProjectCard<Selecting>")

    // (8:35 pm) "I think I'll buy dos cartas."
    // "I really should not buy all of these. 'Tis the struggle. What the fuck? I'll buy them all."
    // "I mean, what the hell? What the hay bale?"
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    green.assertCounts(43 to "MC") // ledger entry 243

    // "I'm gonna heat boop twice. So we're at two temp."
    with(yellow) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
          "Ok", // All nine oceans are already placed.
      )
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      admin.assertCounts(16 to "TemperatureStep")
    }

    // (8:38 pm) "I'm going to use Restricted Area to draw a card."
    with(green) {
      doTasks(*cardActionTasks("$RestrictedArea"), "2 Pay<> FROM MC", "ProjectCard")
      // "I'm going to trade with Triton. I spend three energy and take four titanium."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Triton>",
          "4 Titanium",
      )
    }
    // HACK: Again, I narrated drawing the card without saying that I paid the two-money action
    // cost.
    green.exMachina("2 MC")
    green.assertCounts(43 to "MC") // ledger entry 243

    // "I'm going to convert eight plants to a greenery."
    // "I place it at row five, column four, and get two steel."
    with(yellow) {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_5_4>",
              "OxygenStep",
              "2 Steel",
          )
          .expect("0 TerraformRating")
      // "And... Gyropolis. I pay... Wow. Free steel. What is going on, [Yellow]? Why do you have
      // six
      // cities?"
      // "I place it in six-three." "Looks like you put it in five-three." "Five-three. Sorry."
      // "I get no placement bonuses, but it's next to two greeneries."
      doTasks(
          *playCardTasks("$Gyropolis"),
          "3 Pay<Class<Steel>> FROM Steel",
          "11 Pay<> FROM MC",
          "$Gyropolis FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[2 MC]",
          "PROD[MC]",
          "CityTile<Hellas_5_3>",
      )
    }
    // HACK: Green distracted Yellow into forgetting to make the production changes
    yellow.exMachina("PROD[-3 MC, 2 Energy]")

    // (8:41 pm) "I'm going to use Venusian Insects for one microbe and one money."
    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>", "MC")
      assertCounts(44 to "MC") // ledger entry 247
      doTask("PassBaton")
    }

    with(yellow) {
      // "I remove three Nitrite Reducing Bacteria to gain a TR."
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to use Rotator Impacts to remove an asteroid and raise Venus."
      // "Venus is now at 22%, and my TR is at 37."
      doTasks(
          *cardActionTasks("$RotatorImpacts", "Action2"),
          "-Asteroid<$RotatorImpacts>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    admin.assertCounts(11 to "VenusStep")
    green.assertCounts(37 to "TerraformRating")

    // "I'm going to pay three energy to trade with Enceladus and get one microbe."
    // "It goes to Nitrite Reducing Bacteria."
    with(yellow) {
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
          "Microbe<$NitriteReducingBacteria>",
      )
      doTask("PassBaton")
    }

    // "I hope this is not a mistake. Jovian Lanterns cost me 18 money."
    // "It gives me a TR and two floaters, which I put on Jovian Lanterns itself."
    with(green) {
      doTasks(
          *playCardTasks("$JovianLanterns"),
          "18 Pay<> FROM MC",
          "$JovianLanterns FROM ProjectCard",
          "TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton")
    }
    // HACK: It was a mistake: I only paid 17.
    green.exMachina("1 MC")
    green.assertCounts(27 to "MC") // ledger entry 249

    with(yellow) {
      // "I sell a card for one money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    // (8:45 pm) "In order to Terraform Ganymede, that costs 31, but I spend 24 worth of titanium.
    // "I only have to spend seven mc, taking me down to 20."
    // "That gives me three TR."
    // (later) "Oh shit. Let me take back one titanium and spend three more money."
    with(green) {
      doTasks(
          *playCardTasks("$TerraformingGanymede"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "10 Pay<> FROM MC",
          "$TerraformingGanymede FROM ProjectCard",
          "3 TerraformRating",
      )
      assertCounts(17 to "MC") // ledger entry 260

      // "Since you haven't done anything, I'mma go ahead and do a heat boop."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(yellow) {
      // "I will play Molecular Printing for 11. I get 8 money for the 8 cities."
      doTasks(
          *playCardTasks("$MolecularPrinting"),
          "11 Pay<> FROM MC",
          "$MolecularPrinting FROM ProjectCard",
          "8 MC",
          "0 MC",
      )
      doTask("PassBaton")
    }
    yellow.assertCounts(48 to "MC") // ledger entry 365

    // (8:48 pm) "I think I forgot one mc of cost, so I'll correct that."
    // ... but she hadn't ...
    yellow.exMachina("-1 MC")
    yellow.assertCounts(47 to "MC") // ledger entry 366

    with(green) {
      // "I'm going to use Jovian Lanterns to spend a titanium and put two more floaters on Jovian"
      // "Lanterns."
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton")
    }

    // (8:50 pm) "Asteroid, Asteroid."
    // "Oh, I'm a dumbass. I forgot to boop the temperature when you did. There we go."
    // "You bought two standard project Asteroids."
    // "And now temp is maxed, so that is last call. The game will end soon."
    with(yellow) {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
      )
      admin.assertCounts(19 to "TemperatureStep")
    }

    // "I am going to spend one on Floater Leasing, which gives me two money production."
    with(green) {
      doTasks(
          *playCardTasks("$FloaterLeasing"),
          "1 Pay<> FROM MC",
          "$FloaterLeasing FROM ProjectCard",
          "PROD[2 MC]",
      )
      assertCounts(16 to "MC") // ledger entry 262
      doTask("PassBaton")
    }

    // "I'm not quite sure what I meant by this. Gonna pitch two cards."
    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      // "And then I have 21 to play Ecology Research."
      // "I increase plant production one step for each colony, which does not apply to me."
      // "I add one animal to any card, which will be Sub-Zero Salt Fish."
      // "I add two microbes to any card, which will be Nitrite."
      // "Then I get two animals for EcoZone."
      doTasks(
          *playCardTasks("$EcologyResearch"),
          "21 Pay<> FROM MC",
          "$EcologyResearch FROM ProjectCard",
          "PROD[0 Plant]",
      )
      doTask("Animal<$EcologicalZone>", EcologicalZone)
      doTask("Animal<$EcologicalZone>", EcologicalZone)
      doTasks("Animal<$SubZeroSaltFish>", "2 Microbe<$NitriteReducingBacteria>")
      assertCounts(0 to "MC") // ledger entry 376
    }

    with(green) {
      // "I'm starting to have tough decisions, so I'll just stall more by taking a Penguin."
      doTasks(*cardActionTasks("$Penguins"), "Animal<$Penguins>")
      doTask("PassBaton")
    }

    // "And, for zero, Project Inspection."
    // "I use Nitrite Reducing Bacteria to turn in three microbes and get another TR."
    with(yellow) {
      doTasks(
          *playCardTasks("$ProjectInspection"),
          "$ProjectInspection FROM ProjectCard",
          "UseAction<$NitriteReducingBacteria, Action2>",
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // (8:52 pm) "I knew there had to be a plan. I'll take a Strato Bird."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    // "There is one thing I can do. Not that I really need to. Do a plant forest."
    // "In other words, convert plants to greenery. To this four-two for a plant."
    with(yellow) {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_4_2>",
              "OxygenStep",
              "Plant",
          )
          .expect("0 TerraformRating")
      doTask("PassBaton")
    }

    // "I'm going to use Birds to take a Bird."
    with(green) {
      doTasks(*cardActionTasks("$Birds"), "Animal<$Birds>")
      doTasks(*cardActionTasks("$Extremophiles"), "Microbe<$VenusianInsects>", "MC")
    }

    // "Not that it would have helped me much, but I can use Energy Market to decrease"
    // "an energy production." "I pitched one card. For the energy production."
    with(yellow) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      assertCounts(8 to "MC") // ledger entry 382
      doTask("PassBaton")
    }

    // (8:55 pm) "Sixteen. I think I can do it. Let's find out."
    // "Be brave. Try playing Viral Enhancers first. That cost me seven. And it gives me a plant,
    // right?"
    with(green) {
      doTasks(
          *playCardTasks("$ViralEnhancers"),
          "7 Pay<> FROM MC",
          "$ViralEnhancers FROM ProjectCard",
          "Plant",
      )
      // "I play Advanced Ecosystems for nine. It gives me three plants."
      doTasks(
          *playCardTasks("$AdvancedEcosystems"),
          "9 Pay<> FROM MC",
          "$AdvancedEcosystems FROM ProjectCard",
          "Plant",
          "Plant",
          "Plant",
      )
    }
    green.assertCounts(1 to "MC") // ledger entry 266
    // Yellow has no further actions after retracting her earlier pass.
    yellow.doTask("Pass")

    // "Convert plants to greenery." "There's nowhere I can go next to my city that doesn't also"
    // "go next to your city. Whatever. I did it."
    // "I might need the steel, so I'm going on four-four to get two steel."
    with(green) {
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Hellas_4_4>",
              "OxygenStep",
              "2 Steel",
          )
          .expect("0 TerraformRating")
      // (8:58 pm) "Mining Quota cost three, so I'll spend two steel on it."
      // "It gives me two steel production. Yay, my very first steel production."
      // "I believe I now have 17 green. Whew."
      doTasks(
          *playCardTasks("$MiningQuota"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$MiningQuota FROM ProjectCard",
          "PROD[2 Steel]",
      )
      // "I'm going to sell Stanford Torus to get a money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "I'm going to pay two money to use Floating Habs to put a floater on Floating Habs."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$FloatingHabs>")
      assertCounts(0 to "MC") // ledger entry 274

      // "I'm gonna use Maxwell Base to put a microbe on Venusian Insects and give me a money back."
      // "Give me my money back."
      doTasks(*cardActionTasks("$MaxwellBase"), "Microbe<$VenusianInsects>", "MC")
      // "I will try Search for Life. Wouldn't that be funny if this made all the difference"
      // "right here? Search for Life, Vesta Shipyard, no life. I got no life."
      doTasks(
              *cardActionTasks("$SearchForLife"),
              "Pay<> FROM MC",
              "ProjectCard<Revealed>",
              "Ok", // No microbe tag on the revealed card.
              "-ProjectCard<Revealed>",
          )
          .expect("0 Science")

      // I never narrated paying the action's one money; the ledger suggests I took one instead.
      exMachina("2 MC")

      assertCounts(2 to "MC") // ledger entry 275

      // "So now I must pass. And so we hit production."
      doTask("Pass")
    }

    // Production.
    yellow.doTasks(
        "72 MC",
        "11 Plant",
        "5 Heat",
        "Titanium",
        "3 Energy",
        "Steel",
        "Heat FROM Energy",
    )
    green.doTasks("54 MC", "4 Energy", "2 Titanium", "Heat FROM Energy", "2 Steel")

    with(green) {
      assertProduction(m = 12, s = 2, t = 2, p = 0, e = 4, h = 0)
      assertResources(m = 56, s = 3, t = 2, p = 1, e = 4, h = 1)
    }
    with(yellow) {
      assertProduction(m = 20, s = 1, t = 1, p = 11, e = 3, h = 5)
      assertResources(m = 80, s = 1, t = 3, p = 18, e = 3, h = 6)
    }
    // The app labels final production as Generation 12; the engine keeps the completed action
    // generation at 11 once the game enters Victory.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9)

    with(yellow) {
      // (8:58 pm) "My thing allows me to plant two forestes." "Two? God, [Yellow], you're fucking
      // killing me."
      // "I don't understand why every game goes so well for you."
      // The final ledger's steel gain and the narrated city scores establish this placement.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_6_4>",
          "Steel",
      )
    }

    with(yellow) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Hellas_5_2>",
      )
    }
    // Decline another final greenery placement.
    yellow.doTask("PassBaton")
    // Decline the final greenery placement.
    green.doTask("PassBaton")

    // (9:02 pm) "Final scoring."
    val score = Summarizer(game)

    green.assertCounts(42 to "TerraformRating")

    // "I think my TR was 52."
    yellow.assertCounts(52 to "TerraformRating")

    // "You get 15 points for milestones, which puts you at 67."
    score.net("Milestone", "VictoryPoint<Yellow>") shouldBe 15

    // "I get Venophile and Magnate, so green gets ten, putting me on 52."
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 10

    // "You get another five, putting you on 72."
    score.net("FirstPlace", "VictoryPoint<Yellow>") shouldBe 5

    // "I think what I'm going to do is count my greeneries first. One, two, three, four."
    // "Four points from greeneries."
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 4

    // "How many points from greeneries do you get? Twelve. 84."
    score.net("GreeneryTile", "VictoryPoint<Yellow>") shouldBe 12

    // "My one city is worth four points, putting me on 60."
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 4

    // "Go ahead and do your cities. Twenty for your cities, putting you at 104."
    score.net("CityTile", "VictoryPoint<Yellow>") shouldBe 20

    // "And four is 21, and 11 is 32. So I saved some face here. And then for these, 17 more."
    score.net("Card", "VictoryPoint<Green>") shouldBe 49

    // "Four, two, two, one. Ten." Law Suit lowers Yellow's net card score to nine.
    score.net("Card", "VictoryPoint<Yellow>") shouldBe 9

    // "You only won by five points. 114[sic] to 109."
    // "I'm sorry for all the fucking whining I was doing."
    yellow.assertCounts(113 to "VictoryPoint", 1 to "Victory")
    green.assertCounts(109 to "VictoryPoint", 0 to "Victory")
  }
}
