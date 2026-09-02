package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.pets.data.GameConfig
import dev.martianzoo.pets.data.Player
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import dev.martianzoo.tfm.engine.TfmWorkflow
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Game played Sun 2026-08-09 11:19 am. Quotes are transcript-derived and sometimes normalized. */
internal class OtbGame20260809Test : AbstractFullGameTest() {
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
          PromoCardPack

          Coastguard, Landshaper, Mayor, Producer, Sponsor, Hoverlord
          Botanist, Founder, Landlord, Magnate, Metropolist, Venuphile
          Callisto, Luna, Triton, Miranda, Enceladus
          """,
          "Ellie",
          "Dad",
      )

  @Test
  internal fun otbGame20260809() {
    TfmWorkflow.Auto(game).launch()
    val ellie = game.tfm(Player.PLAYER1)
    val dad = game.tfm(Player.PLAYER2)
    // "Miranda and Enceladus are currently out of play."
    engine.assertCounts(3 to "ColonyTile", 5 to "ColonyTileSelection")

    // (11:28 am) "The game is beginning. It is Generation 1."
    engine.assertCounts(1 to "Generation")

    // "It's the corporation phase."
    engine.assertCounts(1 to "CorporationPhase")

    // "I call Mons Insurance again."
    // "Your money production goes up four." "And Dad's money production goes down two."
    // "You're buying six cards, which leaves you with 30 money."
    ellie.playCorp(MonsInsurance, 6).expect("PROD[4 M<Ellie>, -2 M<Dad>], 30 MC")

    // "On my turn, I play Morning Star Inc."
    // "I am purchasing four cards. So I receive 50 money."
    // "And then I'm spending 12 money. So I have 38 money left."
    dad.playCorp(MorningStarInc, 4).expect("38 MC")

    // "It is now the Prelude phase."
    engine.assertCounts(1 to "PreludePhase")

    ellie.turn {
      // "I got Dome Farming."
      // "That is, I gain a plant production and two money production."
      playPrelude(DomeFarming).expect("PROD[P, 2 MC]")

      // "And Society Support: I lose a money production, I gain plant production, gain energy"
      // "production, gain heat production."
      playPrelude(SocietySupport).expect("PROD[-1 MC, P, E, H]")
    }

    dad.turn {
      // "On my turn, I play Nitrogen Shipment."
      // "I get a TR, a plant production, and five money, bringing me to 43 money."
      playPrelude(NitrogenShipment).expect("TR, PROD[P], 5 MC")
      assertCounts(43 to "M") // ledger entry 7

      // (11:30 am) "Then I play Great Aquifer."
      playPrelude(GreatAquifer) {
            // "Place on the one-card spot..."
            doTask("OceanTile<Hellas_5_6>")
            // "... and then on the one-plant spot next to it."
            doTask("OceanTile<Hellas_4_6>")
          }
          // "I get a card, one plant, two money, and two TR."
          .expect("ProjectCard, P, 2 MC, 2 TR")
    }

    // "That concludes the Prelude phase. It is now time for the action phase."
    engine.assertCounts(1 to "ActionPhase")

    ellie.turn {
      // "I play Aquifer Pumping for 18."
      playProject(AquiferPumping, 18)

      // "And then pay eight for an ocean place. So I'm gonna lose 26 money."
      cardAction1(AquiferPumping) {
            pay(8)
            // "You had 30 and now you have four."
            assertCounts(4 to "M") // ledger entry 13
            placeTile(5, 7)
          }
          // "I'm gonna place in this nice little spot where I get four money and three heat."
          .expect("-4 MC, 3 H, TR")
    }

    // "For my first action, I flip cards until I get 3 Venus tags. Let's see how this goes."
    // "I draw Venusian Insects, Air-Scrapping Expedition, and Atalanta Planitia Lab."
    dad.turn { stdAction("HandleMandates").expect("3 ProjectCard") }

    // (11:33 am) "I pitch a card for money, and I spend all my nine money on Robotic Workforce."
    ellie.turn {
      sellPatents(1)
      playProject(RoboticWorkforce, 9) {
            // "It just so happens that Dome Farming is a box-building tag."
            doTask("CopyProductionBox<$DomeFarming>")
          }
          // "So that's a plant production and 2 money production."
          .expect("PROD[P, 2 MC]")
      assertCounts(0 to "M") // ledger entry 17
    }

    // "I can play Moss. I pay four, bringing me down to 41."
    // "I satisfy the three-ocean requirement."
    // "I lose one plant and gain one plant production."
    dad.turn {
      playProject(Moss, 4).expect("-P, PROD[P]")
      assertCounts(41 to "M") // ledger entry 11
    }

    ellie.pass()

    // "I'm passing with 41 money."
    dad.assertCounts(41 to "M") // ledger entry 11
    dad.pass()
    ellie.assertUnusedActionCards()
    dad.assertUnusedActionCards()

    // "We do production. World Government Terraforming is Ellie's choice."
    // "I'm boosting oxygen."
    // "You don't get the TR for that."
    ellie.wgt("OxygenStep").expect("0 TR")

    // (11:36 am)
    // "We produced, we did World Government, now it's time for the colony Solar phase."
    // "We increase the only two—only three colonies that exist so far."
    // "Dad gets the start marker."
    dad.assertCounts(1 to "StartToken<Dad>")

    // -------------------------------------------------------------------------------------------

    // "Generation becomes two. Any cubes come off cards."
    dad.assertCounts(0 to "ActionUsedMarker<Anyone>")
    with(dad) {
      assertProduction(m = -2, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 62, s = 0, t = 0, p = 2, e = 0, h = 0)
    }
    with(ellie) {
      assertProduction(m = 7, s = 0, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 28, s = 0, t = 0, p = 3, e = 1, h = 4)
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 3)

    // See board-11-39-15-corrected.png - verified
    dad.assertCounts(4 to "CardFront")
    dad.assertTags(plt = 1, vet = 1)
    ellie.assertCounts(5 to "CardFront")
    ellie.assertTags(but = 2, sct = 1, plt = 1)
    engine.assertCounts(3 to "Tile")

    // (11:39 am) "Wow, these all suck." "I'll buy two."
    dad.buyCards(2)

    // "I'm going to discard two and buy two."
    ellie.buyCards(2)

    // "I'm going to release my inert gases. For fourteen. That gives me two TR."
    dad.turn { playProject(ReleaseOfInertGases, 14).expect("2 TR") }

    // 11:43 am
    // "I'm gonna pay eight to pump an aquifer."
    ellie.turn {
      cardAction1(AquiferPumping) {
            pay(8)
            assertCounts(14 to "M") // ledger entry 35
            placeTile(4, 7)
          }
          // "I will put it up here for the spot with one plant and four money."
          // "And one TR?"
          .expect("P, -4 MC, TR")
      assertCounts(18 to "M") // ledger entry 37
    }

    // "I'm going to play Terraforming Contract because I can."
    // "So that costs me eight, bringing me down to 34."
    // "It takes me from negative two to positive two money production."
    dad.turn {
      playProject(TerraformingContract, 8).expect("PROD[4 MC]")
      assertCounts(34 to "M", 7 to "PROD[M]") // ledger entry 28; 7 really means 2
    }

    // "I think I'll pass. Requirements."
    ellie.pass()
    // "I pass as well."
    dad.pass()
    ellie.assertUnusedActionCards()
    dad.assertUnusedActionCards()

    // "We do production."
    // "Then World Government Terraforming is my choice."
    // "Let's do Venus."
    // "Venus goes up to 2%."
    dad.wgt("VenusStep")

    // "Then we raise all of our colonies again, of which we still have only three."
    // "I give the start marker to Ellie."
    // "We raise the generation to Generation 3."
    // "We take cubes off cards and draft."
    with(dad) {
      assertProduction(m = 2, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 61, s = 0, t = 0, p = 4, e = 0, h = 0)
    }
    with(ellie) {
      assertProduction(m = 7, s = 0, t = 0, p = 3, e = 1, h = 1)
      assertResources(m = 47, s = 0, t = 0, p = 7, e = 1, h = 6)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 4)

    // (11:46 am) "I'm buying three cards and discarding one."
    dad.buyCards(3)

    // "I buy one card and discard three."
    ellie.buyCards(1)

    ellie.turn {
      // "I feel like I can always start by Aquifer Pumping."
      // "Eight real money to use Aquifer Pumping."
      cardAction1(AquiferPumping) {
            pay(8)
            // "I'm going on the one-one. I get two plants and a TR."
            placeTile(1, 1)
          }
          .expect("2 P, TR")

      // "I spend eight plants to place a greenery."
      convertPlants {
            // "Rightmost of row three."
            placeTile(3, 7)
          }
          // "That's two money, a plant, and a card."
          // "The oxygen goes up to 2%, and Ellie gets a TR."
          .expect("-7 P, 2 MC, ProjectCard, TR")
      engine.assertCounts(2 to "OxygenStep")
    }

    // 11:49 am
    // "Giant Solar Shade. That cost me 27 full real money, bringing me down to 25."
    // "I raise Venus three steps and get three TR and a card."
    dad.turn {
      playProject(GiantSolarShade, 27).expect("3 VenusStep, 3 TR, 0 ProjectCard")
      assertCounts(25 to "M") // ledger entry 41
    }

    // "I think I gave myself minus four because I was thinking of placing next to oceans, but then
    // I"
    // "placed away from oceans, so I had to fully minus eight."
    // "I play seven for Optimal Aerobraking and 21 for Comet."
    ellie.turn {
      playProject(OptimalAerobraking, 7)
      playProject(Comet, 21) {
            // "I raise the temperature one step and place an ocean tile."
            // "I'll go on the rightmost space in the ring for 4 money."
            placeTile(5, 8)
            // "I'll remove yours. And I'll pay you three from Mons Insurance."
            doTask("-3 Plant<Dad>")
          }
          // "Thanks to Optimal Aerobraking, I gain three money and three heat."
          // "I gain one event card in my played-events pile."
          .expect("TemperatureStep, -17 M<Ellie>, 3 M<Dad>, 3 H, PlayedEvent, 2 TR")

      assertCounts(14 to "M") // ledger entry 69
    }
    dad.assertCounts(28 to "M") // ledger entry 46

    // 11:53 am
    // "Because Venus is at 8%, I can play Venusian Insects, which requires Venus at 12%."
    // "That brings Enceladus into play, and the cube goes on the second spot."
    // "I should pay for that card, however. I should pay five for it."
    dad.turn { playProject(VenusianInsects, 5).expect("Enceladus") }

    // "We have more than the five-ocean requirement, so I play Algae."
    // "I pay ten for Algae. I gain one plant and gain two plant production."
    ellie.turn { playProject(Algae, 10).expect("P, PROD[2 P]") }

    // "I'm going to play Topsoil Contract. That costs me eight, leaving me with 15."
    // "I gain three plants."
    dad.turn { playProject(TopsoilContract, 8).expect("3 P") }

    // "I believe I pass."
    ellie.pass()

    // "I'm going to take the action of Venusian Insects to add one microbe to Venusian Insects"
    // "and get one money from Topsoil Contract."
    dad.turn {
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
      assertCounts(16 to "M") // ledger entry 49

      // "I'm going to play Venus Governor because I have three Venus tags and it requires two."
      // "That costs me four, but gets me two money production, bringing me up to four money"
      // "production."
      playProject(VenusGovernor, 4).expect("PROD[2 MC]")

      // 11:55 am
      // "I'm going to play Search for Life, which we're still under the max requirement."
      playProject(SearchForLife, 3)

      // "I pay three and then I'll pay one to use Search for Life."
      // "I flip up Business Contacts, so I do not get a science resource."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")
      assertCounts(8 to "M") // ledger entry 52

      // "I think we're at the point where I've done everything. So, I pass."
      pass()
    }
    ellie.assertUnusedActionCards()
    dad.assertUnusedActionCards()

    // "We hit production. We do World Government Terraforming, and it's your turn."
    // "I increase the oxygen. Oxygen goes to 3%."
    ellie.wgt("OxygenStep")

    // "Then the colonies each go up. This time there are four colony tracks to increase."
    // "The generation goes up to four. The start marker moves to Dad from Ellie."
    // "Cubes come off cards."
    with(dad) {
      assertProduction(m = 4, s = 0, t = 0, p = 2, e = 0, h = 0)
      assertResources(m = 40, s = 0, t = 0, p = 6, e = 0, h = 0)
    }
    with(ellie) {
      assertProduction(m = 7, s = 0, t = 0, p = 5, e = 1, h = 1)
      assertResources(m = 37, s = 0, t = 0, p = 8, e = 1, h = 11)
    }
    assertSidebar(gen = 4, temp = -28, oxygen = 3, oceans = 6)

    // See board-11-59-52-corrected.png - verified
    dad.assertCounts(10 to "CardFront")
    dad.assertTags(spt = 1, sct = 1, eat = 2, vet = 5, plt = 1, mit = 2)
    ellie.assertCounts(7 to "CardFront")
    ellie.assertTags(but = 2, spt = 1, sct = 1, plt = 2)
    engine.assertCounts(7 to "Tile")

    // 12:34 pm
    // "I'm discarding one card and buying three cards."
    dad.buyCards(3)

    // "Same."
    ellie.buyCards(3)
    ellie.assertCounts(28 to "M") // ledger entry 85

    // "I'm going to use Venusian Insects to take a microbe on Venusian Insects and gain one money"
    // "and forgo my second action."
    dad.turn {
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
    }

    // "I'm gonna pay 16 for Imported Hydrogen. I'm gonna gain three plants."
    ellie.turn {
      playProject(ImportedHydrogen, 16) {
            doTask("3 Plant")
            // "Place an ocean tile. On the steel spot, for four money and a steel. What a steal."
            // "And a TR."
            placeTile(6, 8)
          }
          // "Optimal Aerobraking gives me three money and three heat, bringing me up to 14 heat."
          .expect("-9 MC, S, 3 H, PlayedEvent, TR")
      assertCounts(19 to "M") // ledger entry 93

      // "I may as well use Aquifer Pumping."
      cardAction1(AquiferPumping) {
            // "I'm gonna spend that steel and six real to place an ocean in the six-money
            // spot."
            pay(6, steel = 1)
            placeTile(6, 7)
          }
          // "That's another TR and six money."
          .expect("0 M, TR")
    }

    // (12:38 pm) "Peroxide Power costs me seven."
    // "It costs me a money production and gains me two energy production."
    dad.turn {
      playProject(PeroxidePower, 7).expect("PROD[-1 MC, 2 E]")
      assertCounts(25 to "M") // ledger entry 65
    }

    // "I'm gonna buzz buzz. Sponsorize some academies. That's nine to discard a card from hand."
    // "Draw three cards and you get one card."
    ellie.turn {
      playProject(SponsoredAcademies, 9).expect("ProjectCard, ProjectCard<Dad>")
      assertCounts(10 to "M") // ledger entry 100
    }

    // "I'm going to use the Power Plant standard project."
    // "That brings me up to three energy production."
    dad.turn { stdProject("PowerPlantSP").expect("PROD[E]") }
    dad.assertCounts(14 to "M") // ledger entry 69

    // (12:40 pm) "I will convert eight plants to a greenery."
    ellie.turn {
      convertPlants {
            placeTile(3, 6)
          }
          // "And that'll be on the 2-plants, 4-money spot."
          .expect("-6 P, 4 MC, TR")

      // "Oxygen goes to 4%. I'm at 29 TR."
      assertCounts(4 to "OxygenStep", 29 to "TR") // ledger entry 105
      assertCounts(14 to "M") // ledger entry 108

      // "Second action: 11 for Corporate Stronghold."
      // "I lose an energy production and gain three money production."
      playProject(CorporateStronghold, 11) {
            // "I place a city tile at row two, column six, right above my two greeneries, for a"
            // "plant."
            placeTile(2, 6)
          }
          .expect("P, PROD[-E, 3 MC]")
    }

    // "I'm going to play Martian Survey while I still can."
    // "That costs me nine, and it draws me two cards."
    dad.turn {
      playProject(MartianSurvey, 9).expect("ProjectCard")
      assertCounts(5 to "M") // derived from ledger entry 72 before Industrial Center
    }

    // (12:43 pm) "I believe I pass."
    ellie.pass()

    // "I'm going to use Search for Life, and I flip Potatoes, so I do not get a science resource."
    dad.turn {
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")

      // "That appears to be the end of everything I can do except..."
      // HACK: Unlike the other narrated Search for Life actions, I never said that I paid the one
      // money.
      exMachina("1 MC")
      assertCounts(5 to "M") // derived from ledger entry 72 before Industrial Center

      // "Sure. Let us play Industrial Center for four."
      playProject(IndustrialCenter, 4) {
            // "Let's just get in your business over here. So I'm placing row two, column five, and"
            // "getting just one plant."
            placeTile(2, 5)
          }
          .expect("P")
      assertCounts(1 to "M") // ledger entry 72

      // "I don't have enough money to take its action. I pass."
      pass()
    }
    ellie.assertUnusedActionCards()
    dad.assertUnusedActionCards(IndustrialCenter)

    // "Because we've both passed, we hit production."
    // "I will World Gov, and the thing that I will World Gov will be that I will Word Gov."
    // "I will World Gov. Wow. Venus, up to 10%."
    dad.wgt("VenusStep")

    // "Then we move all the colony tracks up one."
    // "Most of them are on the penultimate space on the track, but Enceladus still generates only"
    // "three microbes. Miranda is still out of play."
    engine.assertCounts(
        5 to "ColonyProduction<Callisto>",
        5 to "ColonyProduction<Luna>",
        5 to "ColonyProduction<Triton>",
        3 to "ColonyProduction<Enceladus>",
        0 to "Miranda",
    )

    // "Now it's officially Generation 5. You get the start marker."
    // "We take cubes off cards and draft."
    with(dad) {
      assertProduction(m = 3, s = 0, t = 0, p = 2, e = 3, h = 0)
      assertResources(m = 32, s = 0, t = 0, p = 9, e = 3, h = 0)
    }
    with(ellie) {
      assertProduction(m = 10, s = 0, t = 0, p = 5, e = 0, h = 1)
      assertResources(m = 42, s = 0, t = 0, p = 11, e = 0, h = 16)
    }
    assertSidebar(gen = 5, temp = -28, oxygen = 4, oceans = 8)

    // (12:48 pm) "Shit. It's ruining everything. I need them. I'm buying three cards."
    // "And discarding one."
    ellie.buyCards(3)
    ellie.assertCounts(33 to "M") // ledger entry 124

    // "I believe I did the same."
    dad.buyCards(3)

    // "I will spend eight money to place the last ocean."
    ellie.turn {
      cardAction1(AquiferPumping) {
            pay(8)
            // "I get a TR."
            // "It'll go on the two-one space for two plants and two money."
            placeTile(2, 1)
          }
          .expect("2 P, -6 MC, TR")

      // "And I will play... Energy Market."
      playProject(EnergyMarket, 3)
    }
    // HACK: The ledger says Ellie paid six here, although the recording only names Energy Market.
    ellie.exMachina("-3 MC")

    ellie.assertCounts(21 to "M") // ledger entry 130

    // (12:51 pm) "I'm gonna spend three energy to fly my little ship to Luna."
    // "And I simply take 13 money. The track goes all the way down. And the track zoops."
    dad.turn { stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("13 MC") }

    // "I spend six money to gain three energy."
    ellie.turn {
      cardAction1(EnergyMarket, x = 3)

      // "And then I will use the three energy to trade with Callisto and get ten."
      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }.expect("7 E")
    }

    // "Come to think of it, I don't know why I did that urgently..."
    // HACK: Despite saying "spend six money," she did not record a second six-money payment.
    ellie.exMachina("6 MC")
    ellie.assertCounts(21 to "M") // ledger entry 130

    // "Earth Catapult for 23."
    dad.turn { playProject(EarthCatapult, 23) }

    // (12:54 pm) "I'll spend five on Ishtar Mining."
    // "It requires Venus at 8%, and I get a titanium production."
    ellie.turn { playProject(IshtarMining, 5).expect("PROD[T]") }

    // "Well, I think it's time to play Lunar Mining. That cost me eleven. No, it cost me nine."
    // "It gives me two titanium production."
    dad.turn {
      playProject(LunarMining, 9).expect("PROD[2 T]")
      assertCounts(4 to "M") // ledger entry 85
    }

    // "I raise the temperature twice because I have 16 heat."
    // "The temperature is now −24°C. I get two TR."
    ellie.turn {
      convertHeat().expect("TemperatureStep, TR")
      convertHeat().expect("TemperatureStep, PROD[H], TR")
    }

    // "I'm gonna use Venusian Insects to give myself a microbe."
    // "That gives me one money."
    dad.turn { cardAction1(VenusianInsects).expect("Microbe, 1 MC") }

    // "I'm gonna play... Ironworks for 11. Boop."
    ellie.turn { playProject(Ironworks, 11) }

    // "I use Search for Life to spend one money and flip Forced Precipitation."
    // "That does not get me a science resource."
    dad.turn {
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")
    }

    // "I will spend four energy on Ironworks to gain a steel."
    // "Oxygen goes to 5%. I get a TR."
    ellie.turn { cardAction1(Ironworks).expect("S, TR") }
    engine.assertCounts(5 to "OxygenStep")

    // "I am passing."
    dad.pass()

    // (12:58 pm) "I will spend eight plants to place a greenery. Oxygen is now at six."
    ellie.turn {
      convertPlants {
            // "I get a TR."
            // "I put it at the top right on the one-plant spot."
            placeTile(1, 5)
          }
          .expect("-7 P, TR")
      assertCounts(6 to "OxygenStep")

      // "I spend four money and no steel on Biomass Combustors."
      playProject(BiomassCombustors, 4) {
            // "Decrease any plant production one step." "That's me."
            // "Because I'm Mons, I have to pay you, but I only have one. Sorry, that was
            // unintentional."
            assertCounts(1 to "M") // ledger entry 154
            doTask("PROD[-Plant<Dad>]")
            dad.assertCounts(5 to "M") // ledger entry 92
            // "I increase my energy production two steps."
          }
          .expect("-5 M<Ellie>, 1 M<Dad>, PROD[2 E]")

      pass()
    }
    ellie.assertUnusedActionCards()
    dad.assertUnusedActionCards(IndustrialCenter)

    // "We do production."
    // "I use World Government Terraforming to raise Venus to 12%."
    ellie.wgt("VenusStep")

    // "I take the start player marker. Generation goes to six."
    with(dad) {
      assertProduction(m = 3, s = 0, t = 2, p = 1, e = 3, h = 0)
      assertResources(m = 36, s = 0, t = 2, p = 10, e = 3, h = 0)
    }
    with(ellie) {
      assertProduction(m = 10, s = 0, t = 1, p = 5, e = 2, h = 2)
      assertResources(m = 44, s = 1, t = 1, p = 11, e = 2, h = 8)
    }
    assertSidebar(gen = 6, temp = -24, oxygen = 6, oceans = 9)

    // (1:02 pm) "What a bunch of junk." "I'm keeping two cards."
    ellie.buyCards(2)
    // (1:04 pm) "I'm actually gonna not buy this card either."
    // "I'm gonna give myself my three money back again. I bought no cards this time."
    dad.buyCards(0)

    // See board-13-04-48-corrected.png - verified
    dad.assertCounts(14 to "CardFront")
    dad.assertTags(but = 2, spt = 1, sct = 1, pot = 1, eat = 4, vet = 5, plt = 1, mit = 2)
    ellie.assertCounts(13 to "CardFront")
    ellie.assertTags(but = 5, spt = 1, sct = 2, pot = 2, eat = 1, vet = 1, plt = 2, cit = 1)
    engine.assertCounts(14 to "Tile")

    // "I'm going to fly my ship to Triton by paying three energy."
    // "That lets me take five titanium, bringing me up to seven titanium."
    dad.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }.expect("5 T")
      assertCounts(7 to "Titanium")

      // Dad uses his unusual second action because Cupola City's maximum-oxygen requirement is at
      // risk: "while I still can."
      // "And I'm gonna play Cupola City while I still can, although there's still some time."
      // "That costs me 16."
      playProject(CupolaCity, 14) {
            // "I lose an energy production, and I gain three money production, and I place a shitty
            // tile."
            // "I'm going to place it on the one-steel space at row three, column three."
            placeTile(3, 3)
          }
          // "I take a steel."
          .expect("S, PROD[-E, 3 MC]")
    }
    // "I'm gonna pay 12 for Ecological Zone." "Oh my, does it hold animals?"
    ellie.turn {
      playProject(EcologicalZone, 12) {
            // "It holds animals, so Miranda finally comes into play."
            // "I place the tile adjacent to a greenery on the almost-rightmost space in the fourth"
            // "row. That's four money and a plant."
            placeTile(4, 8)
            assertCounts(30 to "M") // ledger entry 169
          }
          .expect("-8 MC, P, Miranda, 2 Animal")

      // "Landshaper." "I was just gonna get that. I hate you."
      stdAction("ClaimMilestone") { doTask("Landshaper") }

      // (1:08 pm) "That makes me so sad." "I should also add two animals to EcoZone because of
      // its effect."
      assertCounts(2 to "Animal<$EcologicalZone>")
    }

    // "I take back the two extra money I spent because I spent 16 on Cupola City and was only"
    // "supposed to spend 14 on it. Now I'm back up to 22 money."
    dad.assertCounts(22 to "M") // ledger entry 109

    // "I'll spend the eight plants. I'm going to put the greenery on the two-plants, four-money"
    // "spot. I raise oxygen to 7% and get a TR."
    dad.turn {
      convertPlants {
            placeTile(2, 2)
          }
          .expect("-6 P, 4 MC, TR")
      engine.assertCounts(7 to "OxygenStep")
    }

    // "I will do my plant forest."
    ellie.turn {
      convertPlants {
            // "I'm going to put it at row five, column nine."
            placeTile(5, 9)
          }
          // "I get a plant, two money, and an oxygen raise."
          // "I have to manually input the temperature raise and the extra TR."
          .expect("-7 P, 2 MC, OxygenStep, TemperatureStep, 2 TR")

      // "My second action is to raise the temperature with my eight heat."
      // "That gives me an extra TR."
      convertHeat().expect("TemperatureStep, TR")
    }

    // (1:12 pm) "Why do I play badly?" "Temp is at minus 20, and oxygen is at eight."
    // "Oh no, I didn't. I spent the titanium. What am I doing?"
    // "I don't want to spend the six. So I stay at 26 money and five titanium."
    dad.turn { playProject(RotatorImpacts, 1, titanium = 1) }

    // "I play Kelp Farming for 17. We have the six oceans."
    ellie.turn {
      playProject(KelpFarming, 17)
          // "I gain two money production, three plant production, and two plants."
          // "I add an animal to Ecological Zone."
          .expect("PROD[2 MC, 3 P], 2 P, Animal<$EcologicalZone>")
      assertCounts(7 to "M") // ledger entry 186
    }

    // "I'm going to play Nuclear Power, which costs eight. I spend one steel and six money."
    dad.turn {
      playProject(NuclearPower, 6, steel = 1)
          // "I lose two money production and gain three energy production."
          .expect("PROD[-2 MC, 3 E]")
      assertCounts(19 to "M") // ledger entry 122
    }

    // (1:15 pm) "Energy Market: pay four money, gain two energy."
    ellie.turn {
      cardAction1(EnergyMarket, x = 2)

      // "I use Ironworks to pay four energy, gain a steel, and raise oxygen."
      // "Oxygen is at 9%. I get a TR."
      cardAction1(Ironworks).expect("S, TR")
      engine.assertCounts(9 to "OxygenStep")
    }

    // "I... I... I... I am going to spend eight to get Venophile funded."
    dad.turn { stdAction("FundAward") { doTask("Venuphile") } }

    // "I pass."
    ellie.pass()

    // "I am going to spend 11 on Venusian Plants, which is all the money I have."
    dad.turn {
      playProject(VenusianPlants, 11) {
            // (1:17 pm) "I meet the requirement of 16%. I raise Venus one step, which gives me one
            // TR."
            // "I put a microbe on Venusian Insects, which gives me one money."
            addCardResources(VenusianInsects)
          }
          .expect("VenusStep, TR, -10 MC")

      // "I'm going to use... thing. Venusian Insects itself to put another cube on Venusian Insects
      // and take"
      // "another money."
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")

      // "I'm going to use Rotator Impacts to spend two titanium and put an asteroid on Rotator"
      // "Impacts."
      cardAction1(RotatorImpacts) { pay(titanium = 2) }.expect("Asteroid")

      // "I'm going to use Search for Life to pay one money and flip Titan Shuttles, which does not"
      // "get me a thing thing."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")
      assertCounts(1 to "M") // ledger entry 130

      // "I pass. We do production."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping)
    dad.assertUnusedActionCards(IndustrialCenter)

    // "For World Government Terraforming, I'm going to raise the temperature."
    // "The temperature is now −18°C."
    dad.wgt("TemperatureStep")

    // "We raise the colonies one step each. We give you the start player marker."
    // "It becomes Generation 7. We take cubes off cards."
    with(dad) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 35, s = 0, t = 6, p = 5, e = 5, h = 0)
    }
    with(ellie) {
      assertProduction(m = 12, s = 0, t = 1, p = 8, e = 2, h = 3)
      assertResources(m = 53, s = 2, t = 2, p = 15, e = 2, h = 3)
    }
    assertSidebar(gen = 7, temp = -18, oxygen = 9, oceans = 9)

    // See board-13-21-29-corrected.png - verified
    dad.assertCounts(18 to "CardFront")
    dad.assertTags(but = 4, spt = 2, sct = 1, pot = 2, eat = 4, vet = 6, plt = 2, mit = 2, cit = 1)
    ellie.assertCounts(15 to "CardFront")
    ellie.assertTags(5, spt = 1, sct = 2, pot = 2, eat = 1, vet = 1, plt = 4, ant = 1, cit = 1)
    engine.assertCounts(18 to "Tile")

    // (6:21 pm) "We're now playing Generation 7."
    // "I'm buying three cards and discarding one."
    ellie.buyCards(3)
    // "I am... stupidly buying all of my cards. So, 12."
    dad.buyCards(4)

    ellie.turn {
      // "I think I'm just gonna Standard Project City."
      stdProject("CitySP") {
            // "I put it at row one, column two, for two plants and two money."
            placeTile(1, 2)
            assertCounts(21 to "M") // ledger entry 213
          }
          .expect("2 P, -23 MC")

      // "I spend eight plants on a greenery at row one, column three, for two plants."
      // "Oxygen goes up to 10%. I get a TR."
      convertPlants {
            placeTile(1, 3)
          }
          .expect("-6 P, TR")
      engine.assertCounts(10 to "OxygenStep")
    }

    // "Big ass... toroid. Big Asteroid!"
    // "I'm gonna spend all six titanium, plus seven real."
    // "I'm going to get four titanium, two temperature raises, and two TR."
    // "That takes the temperature to −14°C."
    dad.turn {
      playProject(BigAsteroid, 7, titanium = 6) {
            // "You lose four plants."
            doTask("-4 Plant<Ellie>")
          }
          .expect("-2 T, 2 TemperatureStep, 2 TR")
    }

    // (6:25 pm) "I think I'm gonna play some Nitrophilic Moss. It's eight money."
    ellie.turn {
      playProject(NitrophilicMoss, 8)
          // "I lose two plants, gain two plant production, and add an animal to Ecological Zone."
          .expect("-2 P, PROD[2 P], Animal<$EcologicalZone>")
    }

    // "Titan Floating Launch-Pad. That costs all 16 of my money."
    dad.turn {
      playProject(TitanFloatingLaunchPad, 16) {
        // "It tells me to add two floaters to any Jovian card. I wonder which one I will pick."
        // "It'll be itself."
        addCardResources(TitanFloatingLaunchPad)
      }
      assertCounts(0 to "M") // ledger entry 145
    }

    // (6:27 pm) "I will spend four money to gain two energy using Energy Market."
    ellie.turn { cardAction1(EnergyMarket, x = 2) }

    // "I will sell a card."
    dad.turn { sellPatents(1) }

    // "I will spend four energy to gain a steel and raise the oxygen."
    // "Oxygen is at 11%."
    ellie.turn { cardAction1(Ironworks).expect("S, TR") }
    engine.assertCounts(11 to "OxygenStep")

    // "I'm playing a trans card for four money, which I'm going to do as one titanium, one money."
    // "It's Trans-Neptune Probe and that's it. It just—that's it."
    dad.turn { playProject(TransNeptuneProbe, 1, titanium = 1) }

    // "I play Mercurian Alloys for three money."
    ellie.turn {
      playProject(MercurianAlloys, 3)
      assertCounts(6 to "M") // ledger entry 230

      // "I play Solar Wind Power for two titanium and three money."
      // "I increase energy production one step and gain two titanium."
      playProject(SolarWindPower, 3, titanium = 2).expect("PROD[E], 0 T")
    }

    // (6:30 pm) "Oh jeez, I've been sitting on this the whole time. I'll use Rotator Impacts"
    // "to remove an asteroid and do a Venus raise to 16%, which gets me"
    // "two TR."
    dad.turn { cardAction2(RotatorImpacts).expect("-Asteroid, 2 TR") }
    engine.assertCounts(8 to "VenusStep")

    // "I pass."
    ellie.pass()

    // "I use Titan Floating Launch-Pad, lose a floater, and take four microbes from Enceladus."
    // "They go on Venusian Insects, and that gives me four money from Topsoil Contract."
    dad.turn {
      cardAction2(TitanFloatingLaunchPad) {
            doTask("Trade<Enceladus>")
            addCardResources(VenusianInsects)
          }
          .expect("-Floater<$TitanFloatingLaunchPad>, 4 MC")

      // "Then I will use Venusian Insects itself to get one more and one more
      // money."
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
      assertCounts(5 to "M") // ledger entry 152

      // "I use Search for Life, spend one, and flip Research Coordination."
      // "Which, even though it's a wild tag, it does not count."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")

      // (6:33 pm) "I pass. We hit production."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping)
    dad.assertUnusedActionCards(IndustrialCenter)

    // "I use World Government Terraforming to raise the temperature to −12°C."
    ellie.wgt("TemperatureStep")

    // "Then we're gonna give me the start thingy. We're gonna raise the generation to eight."
    // "We're gonna take cubes off cards. Uncube. We're gonna uncube. And then we're gonna draft."
    with(dad) {
      assertProduction(m = 4, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 42, s = 0, t = 5, p = 6, e = 5, h = 5)
    }
    with(ellie) {
      assertProduction(m = 13, s = 0, t = 1, p = 10, e = 3, h = 3)
      assertResources(m = 56, s = 3, t = 3, p = 15, e = 3, h = 6)
    }
    assertSidebar(gen = 8, temp = -12, oxygen = 11, oceans = 9)

    // "I'll buy two cards and discard two cards. I'll do the same."
    dad.buyCards(2)
    ellie.buyCards(2)

    // (6:37 pm) "God damn it. We play Penguins, which costs five."
    dad.turn {
      playProject(Penguins, 5)

      // "And then we just trade three energy to grab two measly little aminals from Miranda."
      // The later Generation 9 ledger and Dad's evidenced eight-heat conversion show that the trade
      // was actually titanium-funded; the transcript's stated payment cannot produce both facts.
      // "Then I can't do it. I already used my two actions."
      // "But I can't use it now because I used two actions to both play Penguins and trade."
      stdAction("TradeAction", 3) {
        doTask("Trade<Miranda>")
        addCardResources(Penguins)
      }
    }

    // (7:51 pm) "I spend three energy to trade with Callisto and get five energy."
    ellie.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Callisto>") }.expect("2 E")

      // "I use Ironworks to spend four energy on steel and oxygen."
      // "Oxygen goes up to 12%. I get a TR."
      cardAction1(Ironworks).expect("S, TR")
      engine.assertCounts(12 to "OxygenStep")
    }

    // (7:54 pm) "Let us finally play Strato Birds. That cost me ten."
    // "I lose a floater from Titan Floating Launch-Pad to do that."
    dad.turn {
      playProject(StratosphericBirds, 10).expect("-Floater<$TitanFloatingLaunchPad>")
      assertCounts(21 to "M") // ledger entry 171
    }

    // "I'm going to use the City standard project."
    ellie.turn {
      stdProject("CitySP") {
            // "I place the city at row three, column five, for two money."
            placeTile(3, 5)
          }
          .expect("-23 MC")
      assertCounts(27 to "M") // ledger entry 257

      // "I convert eight plants to a greenery."
      convertPlants {
            // "I place it at row two, column four, where I get a plant and steel."
            placeTile(2, 4)
          }
          // "Oxygen is now at 13%. I get another TR."
          .expect("-7 P, S, TR")
      engine.assertCounts(13 to "OxygenStep")
    }

    // (7:56 pm) "Boids. I played boids for eight, bringing me down to 13."
    dad.turn {
      playProject(Birds, 8) {
        // "That lowers Ellie's plant production by two."
        doTask("PROD[-2 Plant<Ellie>]")
      }
    }

    // "I'm going to spend eight plants on a greenery."
    ellie.turn {
      convertPlants {
            // "And that will be the four-five for a steel and four money."
            // "You're slaughtering me."
            placeTile(4, 5)
          }
          // "Oh man, you got the last oxygen. I never win this game."
          .expect("S, 4 MC, TR")
      engine.assertCounts(14 to "OxygenStep")

      // "Before I forget, I'm going to claim Mayor for eight."
      stdAction("ClaimMilestone") { doTask("Mayor") }
    }

    // "I'm going to spend one on Extremophiles. I have the two science tags it needs."
    dad.turn { playProject(Extremophiles, 1) }

    // "Before I forget, I'm going to claim Producer for eight. I have all three milestones."
    ellie.turn { stdAction("ClaimMilestone") { doTask("Producer") } }

    // "I'm going to use Venusian Insects and add a microbe to Venusian Insects and take the"
    // "money."
    dad.turn {
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
      assertCounts(13 to "M") // ledger entry 176
    }

    // (7:59 pm) "I'm going to pay 14 for Botanist. Botanist is funded."
    ellie.turn { stdAction("FundAward", which = 2) { doTask("Botanist") } }

    // "I am going to play Satellites."
    // "It would cost eight, but I'm spending six worth of titanium and two money."
    // "I'm getting four money production, bringing me up to eight money production."
    dad.turn { playProject(Satellites, 2, titanium = 2).expect("PROD[4 MC]") }

    // "I pass."
    ellie.pass()

    // "I add a Penguin, a Strato Bird, and a Bird."
    dad.turn {
      cardAction1(Penguins).expect("Animal")
      cardAction1(StratosphericBirds).expect("Animal")
      cardAction1(Birds).expect("Animal")

      // "I use Extremophiles to add a Venusian Insect and take a money."
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }.expect("1 MC")

      // "I use Rotator Impacts to spend six money to add an asteroid."
      cardAction1(RotatorImpacts) { pay(6) }.expect("Asteroid")

      // "I use Titan Floating Launch-Pad to add a floater to Titan Floating Launch-Pad."
      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanFloatingLaunchPad) }

      // "I use Search for Life to pay one and flip Deep Well Heating, which is not a microbe tag."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")
      assertCounts(5 to "M") // ledger entry 181

      // "I pass. We do production."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping, EnergyMarket)
    dad.assertUnusedActionCards(IndustrialCenter)

    // "I use World Government Terraforming to raise the temperature to −10°C."
    dad.wgt("TemperatureStep")

    // "I'm giving Ellie the start marker. It's now Generation 9. Cubes come off cards."
    with(dad) {
      assertProduction(m = 8, s = 0, t = 2, p = 1, e = 5, h = 0)
      assertResources(m = 47, s = 0, t = 2, p = 7, e = 5, h = 10)
    }
    with(ellie) {
      assertProduction(m = 14, s = 0, t = 1, p = 8, e = 3, h = 3)
      assertResources(m = 58, s = 6, t = 4, p = 8, e = 3, h = 10)
    }
    assertSidebar(gen = 9, temp = -10, oxygen = 14, oceans = 9)

    // (8:03 pm) "We draft. I'll buy one card and discard three."
    // The ledger records Ellie buying three cards and Dad buying none.
    ellie.buyCards(3)
    dad.buyCards(0)

    // (8:05 pm) "I believe I start by paying three energy to trade with Luna. That's ten."
    ellie.turn {
      stdAction("TradeAction", 2) { doTask("Trade<Luna>") }.expect("10 MC")

      // "I'm going to use the City standard project on the plant-and-steel space."
      stdProject("CitySP") { placeTile(1, 4) }.expect("P, S")
      assertCounts(34 to "M") // ledger entry 288
    }

    // "I'm going to play Eos Chasma National Park, which costs 14."
    dad.turn {
      playProject(EosChasmaNationalPark, 14) {
            // "It gives me three plants."
            // "It gives me an aminal, which I'm putting on Penguins."
            addCardResources(Penguins)
            // "It gives me two money production. We meet the requirement."
          }
          .expect("3 P, PROD[2 MC]")

      // Dad uses the park's three plants immediately as his second action.
      // "I'm going to do plant boop, which should not give me TR: convert plants to greenery."
      convertPlants {
            // "I'm going to place it at row three, column two, where I get one plant and two
            // money."
            placeTile(3, 2)
          }
          // "It does not give me a TR."
          .expect("-7 P, 2 MC, 0 TR")
    }

    // (8:08 pm) "I will pay ten money for Rego Plastics."
    ellie.turn { playProject(RegoPlastics, 10) }

    // "I use Venusian Insects to add a cube to Venusian Insects and take one money."
    dad.turn { cardAction1(VenusianInsects).expect("Microbe, 1 MC") }

    // "I will pay four steel, because it's worth three now, for Industrial Microbes."
    ellie.turn {
      playProject(IndustrialMicrobes, 0, steel = 4)
          // "I increase energy production and steel production one step each."
          .expect("PROD[S, E]")
      assertCounts(4 to "PROD[E]") // ledger entry 294
    }

    // "I'm going to pay 20 to fund the only award that I have a chance at, which is
    // Magnate."
    dad.turn { stdAction("FundAward", which = 3) { doTask("Magnate") } }

    // "I will pay four titanium for 16 and 12 money for Methane from Titan."
    ellie.turn {
      playProject(MethaneFromTitan, 12, titanium = 4)
          // "I increase heat production two steps and plant production two steps."
          .expect("PROD[2 H, 2 P]")
    }

    // "Maxwell Edison... I'm sorry, Maxwell Base, costs all 16 money that I have."
    // "It gives me a negative energy production, taking me down to four."
    // "And I place a shitty tile on the Maxwell Base space area and put a delegate on it."
    dad.turn { playProject(MaxwellBase, 16).expect("PROD[-E], CityTile") }

    // (8:12 pm) "I raise the temperature to −8°C with eight heat. I get a TR."
    ellie.turn { convertHeat().expect("TR") }
    engine.assertCounts(11 to "TemperatureStep")

    // "I use Maxwell Base to add a Stratospheric Bird."
    dad.turn { cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) } }

    // "I play Nitrite Reducing Bacteria for 11. I get three free microbes on that."
    ellie.turn {
      playProject(NitriteReducingBacteria, 11).expect("3 Microbe")
      assertCounts(1 to "M") // ledger entry 304
    }

    // "I use Rotator Impacts to remove an asteroid and raise Venus to 18%."
    // "That gives me a TR, taking me to 35 TR."
    dad.turn { cardAction2(RotatorImpacts).expect("-Asteroid, TR") }
    engine.assertCounts(9 to "VenusStep")

    // "I use Nitrite Reducing Bacteria to remove three microbes and get a TR."
    ellie.turn { cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR") }

    // "I use Extremophiles to add a Venusian Insect, which gives me one money."
    dad.turn {
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }
      assertCounts(1 to "M") // ledger entry 201
    }

    // "I pass."
    ellie.pass()

    // "I use all three of my animal cards to take one animal each."
    // "That gives me my fifth Penguin."
    dad.turn {
      cardAction1(Penguins).expect("Animal")
      cardAction1(StratosphericBirds).expect("Animal")
      cardAction1(Birds).expect("Animal")

      // "I will use Titan Floating Launch-Pad to remove one floater and trade with, believe it or
      // not, Miranda to take one"
      // "animal. I'll put that on Penguins."
      cardAction2(TitanFloatingLaunchPad) {
            doTask("Trade<Miranda>")
            addCardResources(Penguins)
          }
          .expect("-Floater")

      // (8:15 pm) "I use Search for Life to spend the only money I have and flip AI Central, which"
      // "does not have a microbe tag."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")

      // "I do a heat raise to −6°C. That gives me a TR, so I'm at 36 TR."
      convertHeat().expect("TR")
      engine.assertCounts(12 to "TemperatureStep")

      // "We do production. You get to do World Government Terraforming."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping, EnergyMarket, Ironworks)
    dad.assertUnusedActionCards(IndustrialCenter)
    // "We'll move the temperature. The temperature is now −4°C."
    ellie.wgt("TemperatureStep")

    // "We raise all the colony tracks and take the fleets back."
    // "The start player marker goes to me. The generation is now ten."
    // "Action-used markers come off cards."
    with(dad) {
      assertProduction(m = 10, s = 0, t = 2, p = 1, e = 4, h = 0)
      assertResources(m = 46, s = 0, t = 4, p = 4, e = 4, h = 7)
    }
    with(ellie) {
      assertProduction(m = 15, s = 1, t = 1, p = 10, e = 4, h = 5)
      assertResources(m = 61, s = 4, t = 1, p = 19, e = 4, h = 7)
    }
    assertSidebar(gen = 10, temp = -4, oxygen = 14, oceans = 9)

    // "I'm buying two cards. I'm buying three cards."
    ellie.buyCards(2)
    dad.buyCards(3)

    // (8:18 pm) "I'm going to use three energy to fly to Enceladus and take three microbes."
    // "My three microbes go onto Venusian Insects and give me three money."
    dad.turn {
      stdAction("TradeAction", 2) {
            doTask("Trade<Enceladus>")
            addCardResources(VenusianInsects)
          }
          .expect("3 MC")
    }

    // "I can pay 25 for a City standard project."
    ellie.turn {
      stdProject("CitySP") {
            // "It'll go at row five, column five, for two money."
            placeTile(5, 5)
          }
          .expect("-23 MC")
      assertCounts(32 to "M") // ledger entry 322

      // "Then I'll convert plants to a greenery at row six, column six, for four money."
      convertPlants {
            placeTile(6, 6)
          }
          .expect("4 MC, 0 TR")
    }

    // "What I'm going to do is play Restricted Area for nine."
    dad.turn {
      playProject(RestrictedArea, 9) {
            // (8:21 pm) "Thank you. You know, screw that. I'll give back the two steel."
            // "I'm going to place Restricted Area on the one-card space that is two above the south
            // pole"
            // "so that I can take a card."
            placeTile(7, 6)
          }
          .expect("0 ProjectCard")
    }
    dad.assertCounts(31 to "M") // ledger entry 217

    // "I'm going to play my Sub-Zero Salt Fish." "Oh, I guess I lose a plant production."
    ellie.turn {
      playProject(SubZeroSaltFish, 5) { doTask("PROD[-Plant<Dad>]") }
          .expect("Animal<$EcologicalZone>, -8 M<Ellie>, 3 M<Dad>")
      // "I add an animal to Ecological Zone."
    }
    // "Like that's gonna change much."
    // HACK: That exchange never mentions the Mons Insurance payment, which we omitted physically.
    dad.exMachina("3 M<Ellie> FROM M<Dad>")
    dad.assertCounts(31 to "M") // ledger entry 217
    ellie.assertCounts(31 to "M") // ledger entry 325

    // "I use Restricted Area to draw a card."
    dad.turn { cardAction1(RestrictedArea).expect("ProjectCard") }
    // HACK: I narrated drawing the card but never mentioned Restricted Area's two-money cost.
    dad.exMachina("2 MC")
    dad.assertCounts(31 to "M") // ledger entry 217

    // "I play Medical Lab. I'll spend my four steel as 12 money plus one money."
    // "I increase money production one step for every two building tags."
    // "I have eight building tags, so that's four money production."
    ellie.turn {
      playProject(MedicalLab, 1, steel = 4).expect("PROD[4 MC]")
      assertCounts(25 to "PROD[M]") // ledger entry 328; 25 really means 20
    }

    // (8:24 pm) "I'm gonna play Atalanta Planitia Lab for eight money,"
    // "because I can now finally. Finally."
    // "That gives me two cards."
    dad.turn {
      playProject(AtalantaPlanitiaLab, 8).expect("ProjectCard")
      assertCounts(23 to "M") // ledger entry 221
    }

    // "I'm going to play Venus Soils for 20. I raise Venus one step, which gives me a TR."
    // "I increase plant production one step. I add two microbes to Nitrite Reducing Bacteria."
    // "I also add an animal to Ecological Zone."
    ellie.turn {
      playProject(VenusSoils, 20) { addCardResources(NitriteReducingBacteria) }
          .expect("VenusStep, TR, PROD[P], Animal")
      assertCounts(10 to "M") // ledger entry 329
    }

    // "I play Invention Contest for free and choose this card."
    // Dad's Earth Catapult discount reduces its printed 2-M€ cost to zero.
    dad.turn {
      playProject(InventionContest, 0).expect("0 ProjectCard")

      // "For my second action, I play Lawsuit for free. You lowered my production this generation."
      // "So you lose three money and I gain three money?"
      // "My Mons doesn't activate for that. I would just pay myself."
      playProject(LawSuit, 0) {
            doTask("3 MC<Dad> FROM MC<Ellie>.")
          }
          .expect("3 M<Dad>, -3 M<Ellie>")
    }
    dad.assertCounts(26 to "M") // ledger entry 222
    ellie.assertCounts(7 to "M") // ledger entry 332

    // "I add a Sub-Zero Salt Fish."
    ellie.turn { cardAction1(SubZeroSaltFish).expect("Animal") }

    // "I'm going to spend 23 and put a greenery at the only spot next to my city that isn't next"
    // "to one of yours. I get a steel."
    dad.turn {
      stdProject("GreenerySP") { placeTile(4, 3) }.expect("S, 0 TR")
    }
    ellie.assertCounts(7 to "M") // ledger entry 332

    // (8:28 pm) "I add a Nitrite Reducing Bacterium."
    ellie.turn { cardAction1(NitriteReducingBacteria).expect("Microbe") }

    // "I play Harvest for two and then get 12. Now I have 13 money."
    dad.turn {
      playProject(Harvest, 2).expect("10 MC")
      assertCounts(13 to "M") // ledger entry 226
    }

    // "I'll spend three energy to trade with Miranda for one measly animal, which I'll"
    // "play on Sub-Zero Salt Fish just to even the score or whatever."
    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Miranda>")
        addCardResources(SubZeroSaltFish)
      }
    }

    // "I'm going to play Floating Habs for three. I meet the requirement."
    dad.turn { playProject(FloatingHabs, 3) }

    // "I use Energy Market to pay six money for three energy."
    ellie.turn { cardAction1(EnergyMarket, x = 3) }

    // "I use Titan Floating Launch-Pad to add a floater to Titan Floating Launch-Pad."
    dad.turn {
      cardAction1(TitanFloatingLaunchPad) { addCardResources(TitanFloatingLaunchPad) }
    }

    // (8:30 pm) "I pass."
    ellie.pass()

    // "I'm going to use Venusian Insects to get a microbe and a money."
    dad.turn {
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
      // "I'm going to use Penguins, Stratospheric Birds, and Birds to get one animal on each."
      cardAction1(Penguins).expect("Animal")
      cardAction1(StratosphericBirds).expect("Animal")
      cardAction1(Birds).expect("Animal")

      // "I'm going to pay two to Floating Habs to put a floater on Floating Habs."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      assertCounts(9 to "M") // ledger entry 229

      // "I'm going to use Maxwell Base to add an animal to Penguins."
      // Maxwell Base can only target a Venus card; no later resource assertion establishes that the
      // narrated illegal Penguins placement occurred, so retain the legal Stratospheric Birds
      // target.
      cardAction1(MaxwellBase) { addCardResources(StratosphericBirds) }

      // "I'm going to use Extremophiles to add a microbe to Venusian Insects and give myself"
      // "another money."
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }.expect("1 MC")

      // "I'm going to use Rotator Impacts to pay two titanium to put an asteroid on Rotator
      // Impacts."
      cardAction1(RotatorImpacts) { pay(titanium = 2) }.expect("Asteroid")

      // "I'm going to use Search for Life to spend one and flip this microbe tag right here, which"
      // "is Deimos Down, which is not a microbe tag. So I still do not get a science resource."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")

      // "I pass."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping, Ironworks)
    dad.assertUnusedActionCards(IndustrialCenter)

    // (8:32 pm) "We do production. I will World Gov temperature up to minus two."
    dad.wgt("TemperatureStep")

    // "We will move all the dinguses on the colonies and send the fleets back."
    // "We move the generation to 11 and the start player marker to Ellie."
    with(dad) {
      assertProduction(m = 10, s = 0, t = 2, p = 0, e = 4, h = 0)
      assertResources(m = 55, s = 1, t = 4, p = 4, e = 4, h = 8)
    }
    with(ellie) {
      assertProduction(m = 20, s = 1, t = 1, p = 11, e = 4, h = 5)
      assertResources(m = 67, s = 1, t = 2, p = 22, e = 4, h = 16)
    }
    assertSidebar(gen = 11, temp = -2, oxygen = 14, oceans = 9)

    // See board-20-34-03-corrected.png - verified
    dad.assertCounts(30 to "CardFront")
    dad.assertTags(5, 4, 4, 2, eat = 4, jot = 1, vet = 11, plt = 3, mit = 3, ant = 3, cit = 2)
    ellie.assertCounts(25 to "CardFront")
    ellie.assertTags(8, 4, 4, 3, eat = 1, jot = 1, vet = 2, plt = 6, mit = 2, ant = 2, cit = 1)
    engine.assertCounts(30 to "Tile")

    // HACK: I must have fat-fingered this!?
    dad.exMachina("Plant")
    dad.assertCounts(5 to "Plant") // ledger entry 242

    // (8:35 pm) "I think I'll buy dos cartas."
    // "I really should not buy all of these. 'Tis the struggle. What the fuck? I'll buy them all."
    // "I mean, what the hell? What the hay bale?"
    ellie.buyCards(2)
    dad.buyCards(4)
    dad.assertCounts(43 to "M") // ledger entry 243

    // "I'm gonna heat boop twice. So we're at two temp."
    ellie.turn {
      convertHeat().expect("TR")
      convertHeat().expect("TR")
      engine.assertCounts(16 to "TemperatureStep")
    }

    // (8:38 pm) "I'm going to use Restricted Area to draw a card."
    dad.turn {
      cardAction1(RestrictedArea).expect("ProjectCard")

      // "I'm going to trade with Triton. I spend three energy and take four titanium."
      stdAction("TradeAction", 2) { doTask("Trade<Triton>") }.expect("4 T")
    }
    // HACK: Again, I narrated drawing the card without saying that I paid the two-money action
    // cost.
    dad.exMachina("2 MC")
    dad.assertCounts(43 to "M") // ledger entry 243

    // "I'm going to convert eight plants to a greenery."
    // "I place it at row five, column four, and get two steel."
    ellie.turn {
      convertPlants {
            placeTile(5, 4)
          }
          .expect("2 S, 0 TR")
      // "And... Gyropolis. I pay... Wow. Free steel. What is going on, Ellie? Why do you have six
      // cities?"
      // "I place it in six-three." "Looks like you put it in five-three." "Five-three. Sorry."
      // "I get no placement bonuses, but it's next to two greeneries."
      playProject(Gyropolis, 11, steel = 3) { placeTile(5, 3) }.expect("PROD[3 MC, -2 E]")
    }
    // HACK: Dad distracted Ellie into forgetting to make the production changes
    ellie.exMachina("PROD[-3 MC, 2 E]")

    // (8:41 pm) "I'm going to use Venusian Insects for one microbe and one money."
    dad.turn {
      cardAction1(VenusianInsects).expect("Microbe, 1 MC")
      assertCounts(44 to "M") // ledger entry 247
    }

    // "I remove three Nitrite Reducing Bacteria to gain a TR."
    ellie.turn { cardAction2(NitriteReducingBacteria).expect("-3 Microbe, TR") }

    // "I'm going to use Rotator Impacts to remove an asteroid and raise Venus."
    // "Venus is now at 22%, and my TR is at 37."
    dad.turn { cardAction2(RotatorImpacts).expect("-Asteroid") }
    engine.assertCounts(11 to "VenusStep")
    dad.assertCounts(37 to "TR")

    // "I'm going to pay three energy to trade with Enceladus and get one microbe."
    // "It goes to Nitrite Reducing Bacteria."
    ellie.turn {
      stdAction("TradeAction", 2) {
        doTask("Trade<Enceladus>")
        addCardResources(NitriteReducingBacteria)
      }
    }

    // "I hope this is not a mistake. Jovian Lanterns cost me 18 money."
    // "It gives me a TR and two floaters, which I put on Jovian Lanterns itself."
    dad.turn {
      playProject(JovianLanterns, 18) { addCardResources(JovianLanterns) }.expect("TR")
    }
    // HACK: It was a mistake: I only paid 17.
    dad.exMachina("1 MC")
    dad.assertCounts(27 to "M") // ledger entry 249

    // "I sell a card for one money."
    ellie.turn { sellPatents(1) }

    // (8:45 pm) "In order to Terraform Ganymede, that costs 31, but I spend 24 worth of titanium.
    // "I only have to spend seven mc, taking me down to 20."
    // "That gives me three TR."
    // (later) "Oh shit. Let me take back one titanium and spend three more money."
    dad.turn {
      playProject(TerraformingGanymede, 10, titanium = 7).expect("3 TR")
      assertCounts(17 to "M") // ledger entry 260

      // "Since you haven't done anything, I'mma go ahead and do a heat boop."
      convertHeat().expect("TR")
    }

    // "I will play Molecular Printing for 11. I get 8 money for the 8 cities."
    ellie.turn { playProject(MolecularPrinting, 11).expect("-3 MC") }
    ellie.assertCounts(48 to "M") // ledger entry 365

    // (8:48 pm) "I think I forgot one mc of cost, so I'll correct that."
    // ... but she hadn't ...
    ellie.exMachina("-1 MC")
    ellie.assertCounts(47 to "M") // ledger entry 366

    // "I'm going to use Jovian Lanterns to spend a titanium and put two more floaters on Jovian"
    // "Lanterns."
    dad.turn { cardAction1(JovianLanterns).expect("2 Floater") }

    // (8:50 pm) "Asteroid, Asteroid."
    // "Oh, I'm a dumbass. I forgot to boop the temperature when you did. There we go."
    // "You bought two standard project Asteroids."
    // "And now temp is maxed, so that is last call. The game will end soon."
    ellie.turn {
      stdProject("AsteroidSP").expect("TR")
      stdProject("AsteroidSP").expect("TR")
      engine.assertCounts(19 to "TemperatureStep")
    }

    // "I am going to spend one on Floater Leasing, which gives me two money production."
    dad.turn {
      playProject(FloaterLeasing, 1).expect("PROD[2 MC]")
      assertCounts(16 to "M") // ledger entry 262
    }

    // "I'm not quite sure what I meant by this. Gonna pitch two cards."
    ellie.turn {
      sellPatents(2)
      // "And then I have 21 to play Ecology Research."
      // "I increase plant production one step for each colony, which does not apply to me."
      // "I add one animal to any card, which will be Sub-Zero Salt Fish."
      // "I add two microbes to any card, which will be Nitrite."
      // "Then I get two animals for EcoZone."
      playProject(EcologyResearch, 21) {
            addCardResources(SubZeroSaltFish)
            addCardResources(NitriteReducingBacteria)
          }
          .expect("3 Animal")
      assertCounts(0 to "M") // ledger entry 376
    }

    // "I'm starting to have tough decisions, so I'll just stall more by taking a Penguin."
    dad.turn { cardAction1(Penguins).expect("Animal") }

    // "And, for zero, Project Inspection."
    // "I use Nitrite Reducing Bacteria to turn in three microbes and get another TR."
    ellie.turn {
      playProject(ProjectInspection, 0) { doTask("UseAction<$NitriteReducingBacteria, Action2>") }
          .expect("-3 Microbe, TR")
    }

    // (8:52 pm) "I knew there had to be a plan. I'll take a Strato Bird."
    dad.turn { cardAction1(StratosphericBirds).expect("Animal") }

    // "There is one thing I can do. Not that I really need to. Do a plant forest."
    // "In other words, convert plants to greenery. To this four-two for a plant."
    ellie.turn {
      convertPlants {
            placeTile(4, 2)
          }
          .expect("-7 P, 0 TR")
    }

    // "I'm going to use Birds to take a Bird."
    dad.turn {
      cardAction1(Birds).expect("Animal")
      cardAction1(Extremophiles) { addCardResources(VenusianInsects) }.expect("1 MC")
    }

    // "Not that it would have helped me much, but I can use Energy Market to decrease"
    // "an energy production." "I pitched one card. For the energy production."
    ellie.turn {
      cardAction2(EnergyMarket).expect("PROD[-E], 8 MC")
      assertCounts(8 to "M") // ledger entry 382
    }

    // (8:55 pm) "Sixteen. I think I can do it. Let's find out."
    // "Be brave. Try playing Viral Enhancers first. That cost me seven. And it gives me a plant,
    // right?"
    dad.turn {
      playProject(ViralEnhancers, 7).expect("P")

      // "I play Advanced Ecosystems for nine. It gives me three plants."
      playProject(AdvancedEcosystems, 9).expect("3 P")
    }
    dad.assertCounts(1 to "M") // ledger entry 266

    // Ellie has no further actions after retracting her earlier pass.
    ellie.pass()

    // "Convert plants to greenery." "There's nowhere I can go next to my city that doesn't also"
    // "go next to your city. Whatever. I did it."
    // "I might need the steel, so I'm going on four-four to get two steel."
    dad.turn {
      convertPlants {
            placeTile(4, 4)
          }
          .expect("2 S, 0 TR")

      // (8:58 pm) "Mining Quota cost three, so I'll spend two steel on it."
      // "It gives me two steel production. Yay, my very first steel production."
      // "I believe I now have 17 green. Whew."
      playProject(MiningQuota, steel = 2).expect("PROD[2 S]")

      // "I'm going to sell Stanford Torus to get a money."
      sellPatents(1)
      // "I'm going to pay two money to use Floating Habs to put a floater on Floating Habs."
      cardAction1(FloatingHabs) { addCardResources(FloatingHabs) }
      assertCounts(0 to "M") // ledger entry 274

      // "I'm gonna use Maxwell Base to put a microbe on Venusian Insects and give me a money back."
      // "Give me my money back."
      cardAction1(MaxwellBase) { addCardResources(VenusianInsects) }.expect("1 MC")

      // "I will try Search for Life. Wouldn't that be funny if this made all the difference"
      // "right here? Search for Life, Vesta Shipyard, no life. I got no life."
      cardAction1(SearchForLife) { /* Decline the science resource. */
            declineTask()
          }
          .expect("0 Science")

      // I never narrated paying the action's one money; the ledger suggests I took one instead.
      exMachina("2 MC")
      assertCounts(2 to "M") // ledger entry 275

      // "So now I must pass. And so we hit production."
      pass()
    }
    ellie.assertUnusedActionCards(AquiferPumping, Ironworks, SubZeroSaltFish)
    dad.assertUnusedActionCards(IndustrialCenter, TitanFloatingLaunchPad)

    with(dad) {
      assertProduction(m = 12, s = 2, t = 2, p = 0, e = 4, h = 0)
      assertResources(m = 56, s = 3, t = 2, p = 1, e = 4, h = 1)
    }
    with(ellie) {
      assertProduction(m = 20, s = 1, t = 1, p = 11, e = 3, h = 5)
      assertResources(m = 80, s = 1, t = 3, p = 18, e = 3, h = 6)
    }
    // The app labels final production as Generation 12; the engine keeps the completed action
    // generation at 11 once the game enters Victory.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9)

    // (8:58 pm) "My thing allows me to plant two forestes." "Two? God, Ellie, you're fucking
    // killing me."
    // "I don't understand why every game goes so well for you."
    ellie.convertPlants {
      // The final ledger's steel gain and the narrated city scores establish this placement.
      placeTile(6, 4)
    }
    ellie.convertPlants { placeTile(5, 2) }
    // Decline another final greenery placement.
    ellie.declineTask()
    // Decline the final greenery placement.
    dad.declineTask()

    // (9:02 pm) "Final scoring."
    val score = Summarizer(game)

    dad.assertCounts(42 to "TR")

    // "I think my TR was 52."
    ellie.assertCounts(52 to "TR")

    // "You get 15 points for milestones, which puts you at 67."
    score.net("Milestone", "VP<Ellie>") shouldBe 15

    // "I get Venophile and Magnate, so green gets ten, putting me on 52."
    score.net("FirstPlace", "VP<Dad>") shouldBe 10

    // "You get another five, putting you on 72."
    score.net("FirstPlace", "VP<Ellie>") shouldBe 5

    // "I think what I'm going to do is count my greeneries first. One, two, three, four."
    // "Four points from greeneries."
    score.net("GreeneryTile", "VP<Dad>") shouldBe 4

    // "How many points from greeneries do you get? Twelve. 84."
    score.net("GreeneryTile", "VP<Ellie>") shouldBe 12

    // "My one city is worth four points, putting me on 60."
    score.net("CityTile", "VP<Dad>") shouldBe 4

    // "Go ahead and do your cities. Twenty for your cities, putting you at 104."
    score.net("CityTile", "VP<Ellie>") shouldBe 20

    // "And four is 21, and 11 is 32. So I saved some face here. And then for these, 17 more."
    score.net("Card", "VP<Dad>") shouldBe 49

    // "Four, two, two, one. Ten." Law Suit lowers Ellie's net card score to nine.
    score.net("Card", "VP<Ellie>") shouldBe 9

    // "You only won by five points. 114[sic] to 109."
    // "I'm sorry for all the fucking whining I was doing."
    ellie.assertCounts(113 to "VP", 1 to "Victory")
    dad.assertCounts(109 to "VP", 0 to "Victory")
  }
}
