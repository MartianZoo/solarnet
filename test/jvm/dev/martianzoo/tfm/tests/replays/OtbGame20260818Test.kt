package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.state.GameFileJson
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

/** Live game begun Tue 2026-08-18. Quoted evidence is verbatim from the supplied transcripts. */
internal class OtbGame20260818Test : FullGameTest() {
  private val colonyTiles = listOf("Enceladus", "Miranda", "Europa", "Io", "Pluto")

  // "We are playing on the Utopia Planitia board."
  // "We have Preludes. We have Venus, Colonies, Promos, Milestones and Awards expansion."
  // The transcript includes Briber, which Solarnet does not implement; it is not claimed in this
  // game, so the executable pool omits it. The transcript says Enceladus twice; the
  // photographed five-tile colony setup has one Enceladus.
  override val config =
      GameConfig(
          """
          UtopiaMap
          VenusNextExpansion, PreludeExpansion, ColoniesExpansion
          promos:cards
          fakes:cards

          Ecologist, Merchant, Metallurgist, Tactician, Hoverlord
          Constructor, Excentric, Highlander, Mogul, Traveller, Venuphile
          ${colonyTiles.joinToString()}
          """,
          "Green",
          "Yellow",
      )

  private val green
    get() = player(1)

  private val yellow
    get() = player(2)

  @Test
  internal fun otbGame20260818() {
    startGame()
    green.doTask("20 TerraformRating")
    yellow.doTask("20 TerraformRating")
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
    green.doTask("-3 ProjectCard<Selecting>")
    yellow.doTask("-5 ProjectCard<Selecting>")
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
    // board-11-00-18.jpg: initial global state, before either corporation is played.
    assertSidebar(gen = 1, temp = -30, oxygen = 0, oceans = 0, venus = 0)
    green.assertCounts(20 to "TerraformRating", 0 to "OwnedTile")
    yellow.assertCounts(20 to "TerraformRating", 0 to "OwnedTile")

    with(green) {
      // "I'm Point Luna... I get a titanium production." "I'm keeping seven cards."
      // "So I pay 21. I have 17 money remaining."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PointLuna>>",
          "$PointLuna FROM CorporationCard",
          "38 MC",
          "PROD[Titanium]",
          "ProjectCard",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(yellow) {
      // "I have Valley Trust. I'm keeping five cards... I have 22 money."
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$ValleyTrust>>",
          "$ValleyTrust FROM CorporationCard",
          "37 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
      )
    }

    with(green) {
      // "I play Biofuels... two plants, a plant production, and an energy production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Biofuels>>",
          "$Biofuels FROM PreludeCard",
          "PROD[Plant]",
          "PROD[Energy]",
          "2 Plant",
      )
      // "And then I play Donation and get 21 money."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Donation>>",
          "$Donation FROM PreludeCard",
          "21 MC",
      )
    }

    with(yellow) {
      // "Supplier... four steel, and two energy production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Supplier>>",
          "$Supplier FROM PreludeCard",
          "PROD[2 Energy]",
          "4 Steel",
      )
      // "Martian Industries... six money, one steel production, and one energy production."
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$MartianIndustries>>",
          "$MartianIndustries FROM PreludeCard",
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
      )
    }

    with(green) {
      // "[Green] pays 10 to play Pets... Miranda comes into play, and I get an animal on Pets."
      doTasks(
              *playCardTasks("$Pets"),
              "10 Pay<> FROM MC",
              "$Pets FROM ProjectCard",
              "Animal<$Pets>",
              "ProjectCard",
          )
          .expect("Miranda")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I use Valley Trust and I get Double Down, which I play... copy Martian Industries."
      doTasks("DoRequiredActions", "3 PreludeCard<Selecting>")
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$DoubleDown>>",
          "$DoubleDown FROM PreludeCard<Selecting>",
          "CopyPrelude<$MartianIndustries>",
          "PROD[Energy]",
          "PROD[Steel]",
          "6 MC",
      )
      // "I spend two money to play Psychrophiles."
      doTasks(
              *playCardTasks("$Psychrophiles"),
              "2 Pay<> FROM MC",
              "$Psychrophiles FROM ProjectCard",
          )
          .expect("Enceladus")
    }

    with(green) {
      // "I pay eleven for Aerial Mappers."
      doTasks(
          *playCardTasks("$AerialMappers"),
          "11 Pay<> FROM MC",
          "$AerialMappers FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I pay eight for Forced Precipitation."
      doTasks(
          *playCardTasks("$ForcedPrecipitation"),
          "8 Pay<> FROM MC",
          "$ForcedPrecipitation FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to add a floater to Aerial Mappers."
      doTasks(*cardActionTasks("$AerialMappers"), "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I spend 21 to play Extractor Balloons. It gets three floaters."
      doTasks(
          *playCardTasks("$ExtractorBalloons"),
          "21 Pay<> FROM MC",
          "$ExtractorBalloons FROM ProjectCard",
          "3 Floater<$ExtractorBalloons>",
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    with(yellow) {
      // "I'm going to add a microbe to Psychrophiles."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      // "Remove two floaters from Extractor Balloons and raise Venus."
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      // "Then pay two money to add a floater to Forced Precipitation."
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("20 MC", "Titanium", "Plant", "Energy")
    yellow.doTasks("21 MC", "4 Energy", "2 Steel")
    // "[Green] uses World Government Terraforming to increase oxygen."
    green.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation2() {
    // board-11-09-02.jpg and both player ledgers: after Generation 1 transition, before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 1, p = 1, e = 1, h = 0)
      assertResources(m = 37, s = 0, t = 1, p = 3, e = 1, h = 0)
      assertCounts(20 to "TerraformRating", 5 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(yellow) {
      assertProduction(m = 0, s = 2, t = 0, p = 0, e = 4, h = 0)
      assertResources(m = 22, s = 6, t = 0, p = 0, e = 4, h = 0)
      assertCounts(21 to "TerraformRating", 7 to "CardFront")
      assertCardResources(1 to Psychrophiles, 1 to ForcedPrecipitation, 1 to ExtractorBalloons)
    }
    assertSidebar(gen = 2, temp = -30, oxygen = 1, oceans = 0, venus = 2)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "I will buy four cards."
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    // Green first said two, then physically corrected the purchase to three before the action
    // phase.
    green.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I'm going to play Mining Rights... row three, column six. I get two cards and a
      // titanium... and increase titanium production."
      doTasks(
          *playCardTasks("$MiningRights"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$MiningRights FROM ProjectCard",
          "MiningRights_SpecialTile<Utopia_3_6>",
          "PROD[Titanium]",
          "2 ProjectCard",
          "Titanium",
      )
      // "I play Energy Tapping... [Green] loses an energy production."
      doTasks(
          *playCardTasks("$EnergyTapping"),
          "3 Pay<> FROM MC",
          "$EnergyTapping FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-Energy<Green>]",
      )
    }

    with(green) {
      // "I play CEO's Favorite Project... put a floater on Aerial Mappers."
      doTasks(
          *playCardTasks("$CeosFavoriteProject"),
          "1 Pay<> FROM MC",
          "$CeosFavoriteProject FROM ProjectCard",
          "Floater<$AerialMappers>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm going to add a floater to Extractor Balloons."
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTask("PassBaton")
    }

    with(green) {
      // "I remove a floater from Aerial Mappers and draw a card."
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm adding a microbe to Psychrophiles."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I pay two and add a floater to Forced Precipitation."
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I pay all 28 money and one titanium to play 16 Psyche."
      doTasks(
          *playCardTasks("$SixteenPsyche"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "28 Pay<> FROM MC",
          "$SixteenPsyche FROM ProjectCard",
          "PROD[2 Titanium]",
          "3 Titanium",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    green.doTask("Pass")

    // Production.
    green.doTasks("20 MC", "3 Titanium", "Plant", "Heat FROM Energy")
    yellow.doTasks("21 MC", "5 Energy", "2 Steel", "4 Heat FROM Energy", "Titanium")
    // "I'm going to use World Government Terraforming to increase Venus."
    yellow.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation3() {
    // board-11-17-20.jpg and both player ledgers: after Generation 2 transition, before Research.
    with(green) {
      assertProduction(m = 0, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 20, s = 0, t = 6, p = 4, e = 0, h = 1)
      assertCounts(20 to "TerraformRating", 6 to "CardFront")
      assertCardResources(1 to Pets, 1 to AerialMappers)
    }
    with(yellow) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 5, h = 0)
      assertResources(m = 25, s = 4, t = 2, p = 0, e = 5, h = 4)
      assertCounts(
          21 to "TerraformRating",
          9 to "CardFront",
          1 to "OwnedTile",
          1 to "SpecialTile<Utopia_3_6>",
      )
      assertCardResources(2 to Psychrophiles, 2 to ForcedPrecipitation, 2 to ExtractorBalloons)
    }
    assertSidebar(gen = 3, temp = -30, oxygen = 1, oceans = 0, venus = 4)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "I'm going to buy one."
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    // Yellow corrected an initial purchase entry: "I'm buying zero cards."
    yellow.doTask("-4 ProjectCard<Selecting>")

    with(green) {
      // "I play Imported Hydrogen... five titanium and one money. Put two animals on Pets."
      // "The ocean goes row four, column one... I get a plant and a card."
      doTasks(
          *playCardTasks("$ImportedHydrogen"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportedHydrogen FROM ProjectCard",
          "ProjectCard",
          "2 Animal<$Pets>",
          "OceanTile<Utopia_4_1>",
          "TerraformRating",
          "Plant",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I remove two floaters from Forced Precipitation and increase Venus."
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      // "I remove two floaters from Extractor Balloons and increase Venus."
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
          "ProjectCard",
      )
    }

    with(green) {
      // "I pay eight for Cartel... three money production."
      doTasks(
          *playCardTasks("$Cartel"),
          "8 Pay<> FROM MC",
          "$Cartel FROM ProjectCard",
          "PROD[3 MC]",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I play Colonizer Training Camp, paying four steel."
      doTasks(
          *playCardTasks("$ColonizerTrainingCamp"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$ColonizerTrainingCamp FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "I play Beam from a Thorium Asteroid... two titanium and 26 money."
      doTasks(
          *playCardTasks("$BeamFromAThoriumAsteroid"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "26 Pay<> FROM MC",
          "$BeamFromAThoriumAsteroid FROM ProjectCard",
          "PROD[3 Heat]",
          "PROD[3 Energy]",
      )
    }

    with(green) {
      // "I pay four for Research Coordination."
      doTasks(
          *playCardTasks("$ResearchCoordination"),
          "4 Pay<> FROM MC",
          "$ResearchCoordination FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm going to add to Psychrophiles."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      // "I pay four for Venus Governor... two money production."
      doTasks(
          *playCardTasks("$VenusGovernor"),
          "4 Pay<> FROM MC",
          "$VenusGovernor FROM ProjectCard",
          "PROD[2 MC]",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("26 MC", "3 Titanium", "Plant")
    yellow.doTasks("23 MC", "8 Energy", "2 Steel", "5 Heat FROM Energy", "Titanium", "3 Heat")
    // "[Green] increases temperature with World Government Terraforming."
    green.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation4() {
    // Both player ledgers: after Generation 3 transition, before Research.
    with(green) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 4, p = 6, e = 0, h = 1)
    }
    with(yellow) {
      assertProduction(m = 0, s = 2, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 23, s = 2, t = 1, p = 0, e = 8, h = 12)
    }
    assertSidebar(gen = 4, temp = -28, oxygen = 1, oceans = 1, venus = 8)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I'm trading with Pluto... paying three energy, and I get three cards."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "3 ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Then I play Mars University for eight... discard one and draw one."
      doTasks(
          *playCardTasks("$MarsUniversity"),
          "8 Pay<> FROM MC",
          "$MarsUniversity FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I pay seven for Flooding... row three, column one."
      doTasks(
          *playCardTasks("$Flooding"),
          "7 Pay<> FROM MC",
          "$Flooding FROM ProjectCard",
          "OceanTile<Utopia_3_1>",
          "TerraformRating",
          "3 Plant",
          "2 MC",
      )
      // "I use one Psychrophiles microbe to play Potatoes... lose two plants and get two money
      // production."
      doTasks(
          *playCardTasks("$Potatoes"),
          "PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "Ok", // No M€ needed after paying with Psychrophiles microbes.
          "$Potatoes FROM ProjectCard",
          "-2 Plant",
          "PROD[2 MC]",
      )
    }

    with(green) {
      // "I pay three for Mercurian Alloys."
      doTasks(
          *playCardTasks("$MercurianAlloys"),
          "Ok", // No titanium paid.
          "3 Pay<> FROM MC",
          "$MercurianAlloys FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm just going to take my turn, and that is add to Psychrophiles."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      // "I pay two money and two titanium for Asteroid Rights... it gets two asteroids."
      doTasks(
          *playCardTasks("$AsteroidRights"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$AsteroidRights FROM ProjectCard",
          "2 Asteroid<$AsteroidRights>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I play Mine, paying two steel."
      doTasks(
          *playCardTasks("$Mine"),
          "2 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$Mine FROM ProjectCard",
          "PROD[Steel]",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I remove an asteroid and get two titanium."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    with(green) {
      // "I pay five for Floating Habs."
      doTasks(*playCardTasks("$FloatingHabs"), "5 Pay<> FROM MC", "$FloatingHabs FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "I spend 11 on Nitrate [sic] Reducing Bacteria."
      doTasks(
          *playCardTasks("$NitriteReducingBacteria"),
          "11 Pay<> FROM MC",
          "$NitriteReducingBacteria FROM ProjectCard",
          "3 Microbe<$NitriteReducingBacteria>",
      )
    }

    with(green) {
      // "I pay two with Floating Habs and put a floater on Aerial Mappers."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I take three microbes off Nitrate Reducing Bacteria and gain a TR."
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    green.doTask("Pass")

    // Production.
    green.doTasks("26 MC", "3 Titanium", "Plant")
    yellow.doTasks("27 MC", "8 Energy", "3 Steel", "5 Heat FROM Energy", "Titanium", "3 Heat")
    // "I'm going to increase Venus" with World Government Terraforming.
    yellow.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation5() {
    // board-13-20-01.jpg and both player ledgers: after Generation 4 transition, before Research.
    with(green) {
      assertProduction(m = 5, s = 0, t = 3, p = 1, e = 0, h = 0)
      assertResources(m = 27, s = 0, t = 7, p = 7, e = 0, h = 1)
      assertCounts(21 to "TerraformRating", 13 to "CardFront")
      assertCardResources(3 to Pets, 0 to AerialMappers, 1 to AsteroidRights)
    }
    with(yellow) {
      assertProduction(m = 2, s = 3, t = 1, p = 0, e = 8, h = 3)
      assertResources(m = 27, s = 3, t = 2, p = 1, e = 8, h = 20)
      assertCounts(25 to "TerraformRating", 14 to "CardFront")
      assertCardResources(
          3 to Psychrophiles,
          0 to NitriteReducingBacteria,
          1 to ForcedPrecipitation,
          1 to ExtractorBalloons,
      )
    }
    admin.assertCounts(1 to "OceanTile<Utopia_4_1>", 1 to "OceanTile<Utopia_3_1>")
    assertSidebar(gen = 5, temp = -28, oxygen = 1, oceans = 2, venus = 10)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // Point Luna tableau in board-13-46-12.jpg: "Energy Market. Cost me three."
      doTasks(*playCardTasks("$EnergyMarket"), "3 Pay<> FROM MC", "$EnergyMarket FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // Valley Trust tableau in board-13-46-12.jpg: "Hydrogen to Venus. I spend two titanium and
      // five real... add two to Forced Precipitation."
      doTasks(
          *playCardTasks("$HydrogenToVenus"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "5 Pay<> FROM MC",
          "$HydrogenToVenus FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "2 Floater<$ForcedPrecipitation>",
      )
      // User clarification: Yellow played Hermetic Order of Mars. Her ledger combines its six-M€
      // gain with the following twelve-M€ Stratospheric Birds payment.
      doTasks(
          *playCardTasks("$HermeticOrderOfMars"),
          "10 Pay<> FROM MC",
          "$HermeticOrderOfMars FROM ProjectCard",
          "PROD[2 MC]",
          "6 MC",
      )
    }

    with(green) {
      // "Use my Energy Market to pay six, which gives me three energy, and then use that three
      // energy to send my little boat to Io and take ten heat."
      doTasks(*cardActionTasks("$EnergyMarket"), "-6 MC", "3 Energy")
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Io>",
          "10 Heat",
      )
    }

    with(yellow) {
      // Valley Trust tableau in board-13-46-12.jpg: Stratospheric Birds. The played card consumes
      // one Forced Precipitation floater.
      doTasks(
          *playCardTasks("$StratosphericBirds"),
          "12 Pay<> FROM MC",
          "$StratosphericBirds FROM ProjectCard",
          "-Floater<$ForcedPrecipitation>",
      )
      // "I spend three energy to trade with Miranda. Three animals on Stratospheric Birds."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "3 Animal<$StratosphericBirds>",
      )
    }

    with(green) {
      // "Big Asteroid... all titanium... overspending one... four titanium back, two temperature
      // boops... remove one plant."
      doTasks(
          *playCardTasks("$BigAsteroid"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$BigAsteroid FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "4 Titanium",
          "TerraformRating",
          "TerraformRating",
          "PROD[Heat]",
          "-Plant<Yellow>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow's ledger records two eight-heat conversions after Big Asteroid.
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
      // "Lunar Mining. It costs me 11... six Earth tags... six titanium production."
      doTasks(
          *playCardTasks("$LunarMining"),
          "11 Pay<> FROM MC",
          "$LunarMining FROM ProjectCard",
          "PROD[3 Titanium]",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation"),
          "2 Pay<> FROM MC",
          "Floater<$ForcedPrecipitation>",
      )
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    with(green) {
      // "Luna Metropolis... five titanium and one real money... seven money production."
      doTasks(
          *playCardTasks("$LunaMetropolis"),
          "5 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$LunaMetropolis FROM ProjectCard",
          "PROD[7 MC]",
          "CityTile<LunaMetropolis_RemoteArea>",
          "ProjectCard",
          "Animal<$Pets>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(*cardActionTasks("$NitriteReducingBacteria"), "Microbe<$NitriteReducingBacteria>")
    }

    with(green) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
    }
    yellow.doTask("Pass")
    with(green) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("36 MC", "6 Titanium", "Plant", "Heat")
    yellow.doTasks("32 MC", "8 Energy", "3 Steel", "5 Heat FROM Energy", "Titanium", "4 Heat")
    // "World Government us an ocean... nine-eight."
    green.doTask("OceanTile<Utopia_9_8>! BY Admin").expect("0 TerraformRating")
  }

  private fun generation6() {
    // Both player ledgers: after Generation 5 transition, before Research.
    with(green) {
      assertProduction(m = 12, s = 0, t = 6, p = 1, e = 0, h = 1)
      assertResources(m = 37, s = 0, t = 7, p = 8, e = 0, h = 4)
    }
    with(yellow) {
      assertProduction(m = 4, s = 3, t = 1, p = 0, e = 8, h = 4)
      assertResources(m = 33, s = 6, t = 1, p = 0, e = 8, h = 13)
    }
    assertSidebar(gen = 6, temp = -18, oxygen = 1, oceans = 3, venus = 12)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    with(yellow) {
      // "I will spend three energy to trade with Enceladus. That is five microbes going to
      // Nitrate Reducing Bacteria."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
          "5 Microbe<$NitriteReducingBacteria>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to play Industrial Microbes for full price. And now I'm going to pay eight to
      // become the Ecologist."
      doTasks(
          *playCardTasks("$IndustrialMicrobes"),
          "12 Pay<> FROM MC",
          "$IndustrialMicrobes FROM ProjectCard",
          "PROD[Energy]",
          "PROD[Steel]",
      )
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Ecologist")
    }

    // Green never narrated or logged Industrial Microbes' steel and energy production; both remain
    // absent from the generation-seven photograph and ledger.
    green.exMachina("PROD[-Steel, -Energy]")

    with(yellow) {
      // "Forced Precipitation and Extractor Balloons. Remove two off both of them to raise Venus
      // by two. Oh, it is at 16, which means I get an extra TR."
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
          "TerraformRating",
      )
    }
    with(green) {
      // "Import some GHG for two titanium, one real money, draw a card, get two heat production."
      doTasks(
          *playCardTasks("$ImportOfAdvancedGhg"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "1 Pay<> FROM MC",
          "$ImportOfAdvancedGhg FROM ProjectCard",
          "PROD[2 Heat]",
          "ProjectCard",
      )
      // "For my second, let's just get this other milestone taken care of. Eight to be the
      // Metallurgist."
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Metallurgist")
    }

    with(yellow) {
      doTasks("UseAction<ClaimMilestoneAction, Action1>", "8 Pay<> FROM MC", "Tactician")
      doTask("PassBaton")
    }
    with(green) {
      // "Use Floating Habs to spend two money to put a floater on Aerial Mappers, and use that to
      // draw a card."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
    }

    with(yellow) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }
    with(green) {
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      // "Hired Raiders, pay one... I'm going to take three money."
      doTasks(
          *playCardTasks("$HiredRaiders"),
          "1 Pay<> FROM MC",
          "$HiredRaiders FROM ProjectCard",
          "3 MC FROM MC<Yellow>",
      )
    }
    with(yellow) {
      // "Nitrate Reducing Bacteria. I will reduce the nitrates. Spend three of them to gain a TR."
      // The transcript places this immediately after Hired Raiders; move it to the preceding legal
      // Yellow turn rather than assigning any of Green's photographed cards to her.
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Use Asteroid Rights to spend one of my three money to put an asteroid on Asteroid Rights."
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      doTask("PassBaton")
    }
    with(yellow) {
      // "Before I forget, I will heat boop."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }
    with(green) {
      // "Use Energy Market to spend my last two money to get one energy resource."
      doTasks(*cardActionTasks("$EnergyMarket"), "-2 MC", "Energy")
      // "I'm going to convert plants and get in this spot where I get a plant and four money."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_4_2>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    // Green accidentally took another TR, not realizing the app gave it to him already
    green.exMachina("TerraformRating")

    with(yellow) {
      // "Noctis City... six steel and six real... place a city tile... on three-two."
      doTasks(
          *playCardTasks("$NoctisCity"),
          "6 Pay<Class<Steel>> FROM Steel",
          "6 Pay<> FROM MC",
          "$NoctisCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[3 MC]",
          "CityTile<Utopia_3_2>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTask("2 MC")
      doTask("PassBaton")
    }
    green.doTask("Pass")
    with(yellow) {
      // "I stratobird. Five. I get silver stratobirds."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
    }
    yellow.doTask("Pass")

    // Production.
    green.doTasks("38 MC", "6 Titanium", "Plant", "3 Heat", "Heat FROM Energy")
    yellow.doTasks("40 MC", "7 Energy", "3 Steel", "5 Heat FROM Energy", "Titanium", "4 Heat")
    // "I will World Government an ocean on six-four."
    yellow.doTask("OceanTile<Utopia_6_4>! BY Admin").expect("0 TerraformRating")
  }

  private fun generation7() {
    // board-13-46-12.jpg and both player ledgers: after Generation 6 transition.
    with(green) {
      assertProduction(m = 12, s = 0, t = 6, p = 1, e = 0, h = 3)
      assertResources(m = 42, s = 0, t = 11, p = 2, e = 0, h = 8)
      assertCounts(
          26 to "TerraformRating",
          17 to "CardFront",
          1 to "Ecologist",
          1 to "Metallurgist",
          1 to "$MarsUniversity",
          2 to "OwnedTile",
          1 to "GreeneryTile<Utopia_4_2>",
          1 to "CityTile<RemoteArea>",
      )
      assertCardResources(5 to Pets, 0 to AerialMappers, 1 to AsteroidRights)
    }
    with(yellow) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 55, s = 3, t = 2, p = 0, e = 7, h = 14)
      assertCounts(
          33 to "TerraformRating",
          17 to "CardFront",
          1 to "Tactician",
          1 to "$HermeticOrderOfMars",
          2 to "OwnedTile",
          1 to "CityTile<Utopia_3_2>",
      )
      assertCardResources(
          5 to StratosphericBirds,
          3 to NitriteReducingBacteria,
          5 to Psychrophiles,
          1 to ForcedPrecipitation,
          0 to ExtractorBalloons,
      )
    }
    admin.assertCounts(
        1 to "OceanTile<Utopia_9_8>",
        1 to "OceanTile<Utopia_6_4>",
    )
    assertSidebar(gen = 7, temp = -16, oxygen = 2, oceans = 4, venus = 16)

    // When we resumed the physical game, we knew about the errors found above, so we made this
    // manual correction to put things right again.
    green.exMachina("-TerraformRating, -1 MC, PROD[Steel, Energy], Steel, Energy")

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    green.exMachina("6 MC") // And then I immediately screwed up and forgot to pay for my cards!
    yellow.doTasks(
        "-1 ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // Green: "Sure. Let's pay eight to fund Traveller. I have funded the Traveller award."
      doTasks("UseAction<FundAwardAction, Action1>", "8 Pay<> FROM MC", "Traveller")
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "I pay one for Market Manipulation. Increase the colony track one step."
      // Green: "So she's increasing Pluto." Yellow: "Yes. Decrease Io."
      doTasks(
          *playCardTasks("$MarketManipulation"),
          "1 Pay<> FROM MC",
          "$MarketManipulation FROM ProjectCard",
          "ColonyProduction<Pluto> FROM ColonyProduction<Io>",
      )
      // Yellow: "Then I will spend three energy to trade with Pluto, which now gives me three
      // cards." Green: "Nice. Three cards free and clear."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "3 ProjectCard",
      )
    }

    with(green) {
      // Green: "I guess I'll play a Martian Zoo. I believe that cost me full price. I want to pay
      // 12."
      doTasks(
          *playCardTasks("$MartianZoo"),
          "Ok", // No steel paid.
          "12 Pay<> FROM MC",
          "$MartianZoo FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "Io Sulphur Research for 17. I have three Venus tags from the two floater cards and
      // Strata Birds, so three cards."
      doTasks(
          *playCardTasks("$IoSulphurResearch"),
          "15 Pay<> FROM MC",
          "$IoSulphurResearch FROM ProjectCard",
          "3 ProjectCard",
      )

      // She forgot her Valley Trust discount
      yellow.exMachina("-2 MC")
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I'm going to play Nuclear Power. That cost me ten. I lose two money production. I
      // gain three energy production."
      doTasks(
          *playCardTasks("$NuclearPower"),
          "Ok", // No steel paid.
          "10 Pay<> FROM MC",
          "$NuclearPower FROM ProjectCard",
          "PROD[-2 MC]",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "Air-Scrapping Expedition for 13. Raise Venus one step, and I get a TR. Add three
      // floaters to a Venus card. That'll be Forced Precipitation."
      doTasks(
          *playCardTasks("$AirScrappingExpedition"),
          "13 Pay<> FROM MC",
          "$AirScrappingExpedition FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
          "3 Floater<$ForcedPrecipitation>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I am going to play Miranda—Miranda Resort. I guess it costs me three titanium. And
      // I
      // get one, two, three, four, five, six, seven money production."
      doTasks(
          *playCardTasks("$MirandaResort"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$MirandaResort FROM ProjectCard",
          "PROD[7 MC]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "I used Forced Precipitation. Remove two floaters to increase Venus."
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I'm going to play Business Contactos, which cost me seven of my nine money. I look
      // at
      // the top four cards and I pick two of them. Then, because I played an Earth tag, I draw a
      // card and I get a little aminal on Martian Zoo."
      doTasks(
          *playCardTasks("$BusinessContacts"),
          "7 Pay<> FROM MC",
          "$BusinessContacts FROM ProjectCard",
          "4 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
          "ProjectCard",
          "Animal<$MartianZoo>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I'm going to import some Nitrogen. I'm going to slightly overspend by spending six
      // titanium. I will draw the card for the Earth tag. I'll get a TR. I'll get four plants. I
      // don't have a microbe card. And I think I'm going to take two animals on Martian Zoo."
      doTasks(
          *playCardTasks("$ImportedNitrogen"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$ImportedNitrogen FROM ProjectCard",
          "TerraformRating",
          "4 Plant",
          "3 Microbe",
          "ProjectCard",
          "Animal<$MartianZoo>",
          "2 Animal<$MartianZoo>",
      )
      doTask("PassBaton")
    }

    // I forgot the extra animal from MZ's effect
    green.exMachina("-Animal<$MartianZoo>")

    with(yellow) {
      // Yellow: "Nitrite Reducing Bacteria. I remove three and get a TR."
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    // It looks like she forgot to take her TR
    yellow.exMachina("-TerraformRating")

    with(green) {
      // Green: "Now I'm going to use Asteroid Rights to remove an asteroid from Asteroid Rights.
      // And
      // honestly, I think I'll take the money production."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "PROD[1 MC]",
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
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I'll take the Martian Zoo action to take three money."
      doTasks(*cardActionTasks("$MartianZoo"), "3 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "Neutralizer Factory. Pay seven. We've definitely met the ten percent Venus
      // requirement. Increase Venus one step."
      doTasks(
          *playCardTasks("$NeutralizerFactory"),
          "7 Pay<> FROM MC",
          "$NeutralizerFactory FROM ProjectCard",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Green: "Venusian Insects is the card that I played and spent five on."
      doTasks(
          *playCardTasks("$VenusianInsects"),
          "5 Pay<> FROM MC",
          "$VenusianInsects FROM ProjectCard",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // Green: "Now I'm taking its action, putting a microbe on it. On it like a bonnet."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      // Green: "I think I will use Energy Market to reduce my energy production by one and get
      // eight
      // money."
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      // Green: "Then I have enough money for Nitrophilic Moss. We do have the three-ocean
      // requirement. And I will lose two plants and gain two plant production. And that costs the
      // eight money."
      doTasks(
          *playCardTasks("$NitrophilicMoss"),
          "8 Pay<> FROM MC",
          "$NitrophilicMoss FROM ProjectCard",
          "PROD[2 Plant]",
          "-2 Plant",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks(
        "44 MC",
        "6 Titanium",
        "3 Plant",
        "3 Heat",
        "Steel",
        "3 Energy",
        "Heat FROM Energy",
    )
    yellow.doTasks("44 MC", "7 Energy", "3 Steel", "4 Heat FROM Energy", "Titanium", "4 Heat")
    // Green uses World Government Terraforming to increase Venus.
    green.doTask("VenusStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation8() {
    // Both player ledgers: after Generation 7 transition, before Research.
    with(green) {
      assertProduction(m = 18, s = 1, t = 6, p = 3, e = 3, h = 3)
      assertResources(m = 44, s = 2, t = 8, p = 7, e = 3, h = 12)
      assertCounts(26 to "TerraformRating")
      assertCardResources(3 to MartianZoo, 1 to VenusianInsects)
    }
    with(yellow) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 7, h = 4)
      assertResources(m = 52, s = 6, t = 3, p = 0, e = 7, h = 14)
      assertCounts(37 to "TerraformRating")
    }
    assertSidebar(gen = 8, temp = -14, oxygen = 2, oceans = 4, venus = 24)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    green.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    // I suddenly realized my mistake last turn when I forgot to pay for my cards!
    // So I pay for them now, and realize I wouldn't have afforded VI last round.
    // I let it stay, but reason that it should lose a microbe that I wouldn't have had the
    // chance to play.
    green.exMachina("-6 MC, -Microbe<$VenusianInsects>")

    with(yellow) {
      // Yellow: "Ice Moon Colony. I will pay two of my three titanium for six money, and then 17
      // real. I will place on Miranda. And it gives me an animal to my Strata Birds. I place an
      // ocean tile." Green: "It's row eight." Yellow: "Four. Eight-four."
      doTasks(
          *playCardTasks("$IceMoonColony"),
          "2 Pay<Class<Titanium>> FROM Titanium",
          "17 Pay<> FROM MC",
          "$IceMoonColony FROM ProjectCard",
          "Colony<Miranda>",
          "Animal<$StratosphericBirds>",
          "OceanTile<Utopia_8_7>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      // Yellow: "And for my second action, three energy to trade with Miranda. I get two aminals
      // and
      // a card."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "ProjectCard",
          "2 Animal<$StratosphericBirds>",
      )
    }

    with(green) {
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "I remembered why I left a titanium, and that is so I can play Diversity Support.
      // I've got all six standard resources and microbes, animals, floaters. So pay one money, get
      // one TR."
      doTasks(
          *playCardTasks("$DiversitySupport"),
          "1 Pay<> FROM MC",
          "$DiversitySupport FROM ProjectCard",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Green: "It'll involve the playing of Plantation, which I can do because I have a science
      // tag
      // and a wild tag. I've gotten so much use out of this wild tag. So that costs me 15 entire
      // money. And then I place a greenery tile, which I'm going to place at five-three."
      doTasks(
          *playCardTasks("$Plantation"),
          "15 Pay<> FROM MC",
          "$Plantation FROM ProjectCard",
          "GreeneryTile<Utopia_5_3>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
      // Green: "Then I'm going to Kaguya its ass. I'm playing Kaguya Tech for ten full money. I get
      // two money production. I get a card. I swap this greenery tile. I flip it, basically."
      doTasks(
          *playCardTasks("$KaguyaTech"),
          "10 Pay<> FROM MC",
          "$KaguyaTech FROM ProjectCard",
          "PROD[2 MC]",
          "ProjectCard",
          "CityTile<Utopia_5_3> FROM GreeneryTile<Utopia_5_3>",
          "Animal<$Pets>",
          "2 MC",
      )
    }

    // The Generation 9 photograph still has five animals on Pets, so Green missed the animal caused
    // by Kaguya Tech's city placement.
    green.exMachina("-Animal<$Pets>")

    with(yellow) {
      // Yellow: "Right. Venusian Animals." Green: "Oh my god." Yellow: "Yep. It immediately adds an
      // animal to itself."
      doTasks(
          *playCardTasks("$VenusianAnimals"),
          "13 Pay<> FROM MC",
          "$VenusianAnimals FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    // Again forgot her Valley Trust discount.
    yellow.exMachina("-2 MC")

    with(green) {
      // Green: "I'm gonna go ahead and use three real and seven titanium. And I'm going to get four
      // plant production, two TR plus another TR for raising temp to minus 12."
      doTasks(
          *playCardTasks("$NitrogenRichAsteroid"),
          "7 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$NitrogenRichAsteroid FROM ProjectCard",
          "2 TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "PROD[4 Plant]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$ForcedPrecipitation", "Action2"),
          "-2 Floater<$ForcedPrecipitation>",
          "VenusStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTask("PassBaton")
    }

    with(green) {
      // Green: "Business Network. Cost me four. I lose a money production. Aw, I only have 19 money
      // production now. And I get an animal on Martian Zoo and a card."
      doTasks(
          *playCardTasks("$BusinessNetwork"),
          "4 Pay<> FROM MC",
          "$BusinessNetwork FROM ProjectCard",
          "PROD[-MC]",
          "ProjectCard",
          "Animal<$MartianZoo>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // Green: "I lose two steel and I get four money back. All right, so in the end what happened
      // is
      // I paid two steel and two real. I gain an energy production and you lose two heat
      // production."
      doTasks(
          *playCardTasks("$HeatTrappers"),
          "2 Pay<Class<Steel>> FROM Steel",
          "2 Pay<> FROM MC",
          "$HeatTrappers FROM ProjectCard",
          "PROD[Energy]",
          "PROD[-2 Heat<Yellow>]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "Luckily I have this power tag so I can play Power Supply Consortium."
      // Green: "I lose an energy production." Yellow: "I pay five and I gain an energy production."
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
      // Green: "I guess that means if I want to trade, I better do it now. I'll trade for three
      // energy and I'll do Europa. So I get a plant production."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Europa>",
          "PROD[Plant]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      // Green: "All right, I am going to import some zhuzh. This time it's just Imported Zhuzh. I'm
      // going to pay seven real for it. I get one heat production, three heat. I get a silver
      // animal on Martian Zoo. I get a card."
      doTasks(
          *playCardTasks("$ImportedGhg"),
          "Ok", // No titanium paid.
          "7 Pay<> FROM MC",
          "$ImportedGhg FROM ProjectCard",
          "PROD[Heat]",
          "3 Heat",
          "ProjectCard",
          "Animal<$MartianZoo>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // Yellow: "Imported Nutrients. I pay a titanium and 11 real, gain four plants, and add four
      // microbes to Nitrite-Reducing Bacteria." Green: "Man, you're just churning that thing."
      doTasks(
          *playCardTasks("$ImportedNutrients"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "11 Pay<> FROM MC",
          "$ImportedNutrients FROM ProjectCard",
          "4 Plant",
          "4 Microbe<$NitriteReducingBacteria>",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // Green: "I will use Martian Zoo to take five money."
      doTasks(*cardActionTasks("$MartianZoo"), "5 MC")
      // Green: "It appears I never used Business Network. So I'm going to use Business Network to
      // look at a card. I think that makes me want to take it more, so I will pay three for it."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "Ok", // Buy the Business Network card.
          "3 Pay<> FROM MC",
          "ProjectCard<Hand FROM Selecting>",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks("49 MC", "6 Titanium", "8 Plant", "4 Heat", "Steel", "2 Energy")
    yellow.doTasks("49 MC", "8 Energy", "3 Steel", "4 Heat FROM Energy", "Titanium", "2 Heat")
    // Yellow uses World Government Terraforming to increase oxygen.
    yellow.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation9() {
    // board-21-13-43.jpg, board-21-14-23.jpg, and both player ledgers: after Generation 8
    // transition.
    with(green) {
      assertProduction(m = 19, s = 1, t = 6, p = 8, e = 2, h = 4)
      assertResources(m = 51, s = 1, t = 7, p = 15, e = 2, h = 19)
      assertCounts(
          30 to "TerraformRating",
          26 to "CardFront",
          3 to "OwnedTile",
          1 to "CityTile<Utopia_5_3>",
          0 to "Colony",
      )
      // Pets scores one VP per two animals; this count does not mean five VP.
      assertCardResources(
          5 to Pets,
          5 to MartianZoo,
          1 to VenusianInsects,
          0 to AerialMappers,
          1 to AsteroidRights,
      )
    }
    with(yellow) {
      assertProduction(m = 7, s = 3, t = 1, p = 0, e = 8, h = 2)
      assertResources(m = 51, s = 9, t = 1, p = 6, e = 8, h = 12)
      assertCounts(
          42 to "TerraformRating",
          22 to "CardFront",
          2 to "OwnedTile",
          1 to "Colony<Miranda>",
      )
      assertCardResources(
          10 to StratosphericBirds,
          1 to VenusianAnimals,
          1 to NitriteReducingBacteria,
          7 to Psychrophiles,
          0 to ForcedPrecipitation,
          2 to ExtractorBalloons,
      )
    }
    admin.assertCounts(
        1 to "Traveller",
        1 to "OceanTile<Utopia_8_7>",
        2 to "TradeFleet",
        0 to "Trade",
    )
    assertColonyProductions(3, 2, 1, 3, 2)
    assertSidebar(gen = 9, temp = -10, oxygen = 4, oceans = 5, venus = 26)

    // Before Generation 9 Research, both players reconciled the physical table against their
    // resource logs. Green took three M€ and an animal on Martian Zoo and discarded a card; Yellow
    // took six M€ and one TR.
    green.exMachina("3 MC, Animal<$MartianZoo>, -ProjectCard")
    yellow.exMachina("6 MC, TerraformRating")

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "You kept all your cards?" "Bada-bing. Yeah. I just hated to give them up."
    green.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "Ok", // Keep all four Research cards.
        "12 Pay<> FROM MC",
        "4 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "I'm planting a forest. I'm eternally hopeful. I'm going to place on six, four to get two
      // steel and two money."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_6_3>",
          "OxygenStep",
          "2 Steel",
          "2 MC",
          "TerraformRating",
      )
      // "Well, let us just go ahead and use floating hubs to put a cube on aerial mappers."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
    }

    with(yellow) {
      // "I'm going to use my extractor balloons, spend the two floaters, increase Venus. To 28."
      doTasks(
          *cardActionTasks("$ExtractorBalloons", "Action2"),
          "-2 Floater<$ExtractorBalloons>",
          "VenusStep",
          "TerraformRating",
      )
      // "Then I'm going to spend three floaters, trade with Aran. This might not be the right call,
      // but enchiladas, actually." "Yeah, gain three microbes."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
          "3 Microbe<$NitriteReducingBacteria>",
      )
    }

    with(green) {
      // "I will use aerial mappers to remove a floater from aerial mappers and get a card."
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "Nine for sponsored academies." "I pitch a card." "And then you get three cards and I also
      // get a card."
      doTasks(
          *playCardTasks("$SponsoredAcademies"),
          "7 Pay<> FROM MC",
          "$SponsoredAcademies FROM ProjectCard",
          "-ProjectCard",
          "EACH Other@Player(NOT Yellow) { ProjectCard<Other@Player, Hand>! }",
          "Animal<$VenusianAnimals>",
          "3 ProjectCard",
          "ProjectCard<Green, Hand>",
      )
      // "La Grange Observatoire." "One titanium and four money." "I believe I get a card."
      doTasks(
          *playCardTasks("$LagrangeObservatory"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "4 Pay<> FROM MC",
          "$LagrangeObservatory FROM ProjectCard",
          "ProjectCard",
          "Animal<$VenusianAnimals>",
      )
    }

    // "I am going to use my business network to look at this card." "I will not buy this card."
    with(green) {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "May as well. Spend all nine of my steel on aquifer pumping."
      doTasks(
          *playCardTasks("$AquiferPumping"),
          "9 Pay<Class<Steel>> FROM Steel",
          "Ok", // No M€ needed after paying with steel.
          "$AquiferPumping FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm gonna play advanced alloys." "It costs me nine." "No discounts."
      doTasks(
          *playCardTasks("$AdvancedAlloys"),
          "9 Pay<> FROM MC",
          "$AdvancedAlloys FROM ProjectCard",
          "-ProjectCard",
          "ProjectCard",
      )
      // "I am gonna go ahead and play Solar Logistics." "But I spend four titanium on that." "I get
      // two titanium from it. I get a minimal on Martian Zoo. I get a card."
      doTasks(
          *playCardTasks("$SolarLogistics"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$SolarLogistics FROM ProjectCard",
          "2 Titanium",
          "ProjectCard",
          "Animal<$MartianZoo>",
      )
    }

    with(yellow) {
      // "I spend eight on aquifer pumping. The action, that is." "Row seven, column six, I
      // believe."
      // "Yes, two plants, two money."
      doTasks(
          *cardActionTasks("$AquiferPumping"),
          "8 Pay<> FROM MC",
          "OceanTile<Utopia_7_6>",
          "TerraformRating",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm gonna play Ice Asteroid. For four titanium and three wheel. It's a space event. So I
      // draw a card from solar logistics. I place two ocean tiles." "So that gets me two TR and 10
      // money."
      doTasks(
          *playCardTasks("$IceAsteroid"),
          "4 Pay<Class<Titanium>> FROM Titanium",
          "3 Pay<> FROM MC",
          "$IceAsteroid FROM ProjectCard",
          "ProjectCard",
          "OceanTile<Utopia_7_5>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "OceanTile<Utopia_8_6>",
          "TerraformRating",
          "2 MC",
          "2 MC",
          "2 MC",
      )
      // "Yeah, what the hell, let's buy a standard project, shall we?" "Aquifer." "I'm just gonna
      // take two plants by placing on four, five."
      doTasks(
          *standardProjectTasks("AquiferProject"),
          "18 Pay<> FROM MC",
          "OceanTile<Utopia_4_5>",
          "TerraformRating",
          "2 Plant",
      )
    }

    // "I spend 26 money. Lose two energy productions. Gain five money productions." "Place." "It'll
    // go right here. That would be row six, column five. Yeah. For six money and two plants."
    // "Actually, any chance I can undo and play conscription first?" "Yeah, sure."
    with(yellow) {
      doTasks(
          *playCardTasks("$Conscription"),
          "5 Pay<> FROM MC",
          "$Conscription FROM ProjectCard",
          "Conscription_NextCardEffect",
      )
      doTasks(
          *playCardTasks("$Capital"),
          "10 Pay<> FROM MC",
          "$Capital FROM ProjectCard",
          "PROD[-2 Energy]",
          "PROD[5 MC]",
          "CapitalTile<Utopia_6_5>",
          "NOW Green { Animal<$Pets<Green>>* }",
      )
      green.doTask("Animal<$Pets>")
      doTasks("2 Plant", "2 MC", "2 MC", "2 MC")
    }

    with(green) {
      // "Three steel, nine MC. That gets me tectonic stress power." "And so I get my
      // three energy production."
      doTasks(
          *playCardTasks("$TectonicStressPower"),
          "3 Pay<Class<Steel>> FROM Steel",
          "9 Pay<> FROM MC",
          "$TectonicStressPower FROM ProjectCard",
          "PROD[3 Energy]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'm just gonna heat boop." "A heat boop has been done. That means converting heat to
      // temperature."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
      // "I will use nitrate-reducing bacteria, remove three microbes, gain a TR."
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
    }

    with(green) {
      // "I will use asteroid rights to take one asteroid off of asteroid rights and give myself
      // two titanium."
      doTasks(
          *cardActionTasks("$AsteroidRights", "Action2"),
          "-Asteroid<$AsteroidRights>",
          "2 Titanium",
      )
      // "I will plant a greenery or plant a forest, as they like to call it on this app." "I'll
      // put it next to my city for two money."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_5_2>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    with(yellow) {
      // "You know, it occurred to me I can probably spend 15 on a final Venus boop."
      doTasks(
          *standardProjectTasks("AirScrappingProject"),
          "15 Pay<> FROM MC",
          "VenusStep",
          "TerraformRating",
      )
      // "I'm going to psychrophile."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
    }

    with(green) {
      // "Well, I'm going to heat boop." "I'm going to heat boop. I didn't move it either time."
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
      )
    }

    with(yellow) {
      // "Anyways, I stratoburb."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      // "Energy market. Reduce energy production to four, gain eight money."
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // "I'm going to play Lunar Exports, which costs me three titanium and four money." "And I
      // get a card from Point Luna." "I'm actually going to take the money production."
      doTasks(
          *playCardTasks("$LunarExports"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "2 Pay<> FROM MC",
          "$LunarExports FROM ProjectCard",
          "ProjectCard",
          "Animal<$MartianZoo>",
          "PROD[5 MC]",
      )
      // "Let's play Solar Net for seven real money. I draw two cards."
      doTasks(
          *playCardTasks("$Solarnet"),
          "7 Pay<> FROM MC",
          "$Solarnet FROM ProjectCard",
          "2 ProjectCard",
      )
      // "Let's play Algae for ten money." "I get two plant production and one plant."
      doTasks(
          *playCardTasks("$Algae"),
          "10 Pay<> FROM MC",
          "$Algae FROM ProjectCard",
          "Plant",
          "PROD[2 Plant]",
      )
      // "I will go ahead and use my Martian Zoo now to take eight money."
      doTasks(*cardActionTasks("$MartianZoo"), "8 MC")
      // "I will use my Venusian Insects to take a Venusian insect, which is apparently a kind of
      // microbe now."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      // "Okay, here goes insects." "And I get one, two, three, four, five, five plant production."
      doTasks(
          *playCardTasks("$Insects"),
          "9 Pay<> FROM MC",
          "$Insects FROM ProjectCard",
          "PROD[5 Plant]",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks(
        "61 MC",
        "6 Titanium",
        "15 Plant",
        "4 Heat",
        "Steel",
        "4 Energy",
        "2 Heat FROM Energy",
    )
    yellow.doTasks("60 MC", "6 Energy", "3 Steel", "5 Heat FROM Energy", "Titanium", "2 Heat")
    // "The world government is me, and well, I'm not going to do oxygen. I do temperature up to
    // minus two."
    green.doTask("TemperatureStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation10() {
    // board-16-19-30.jpg and both player ledgers: after Generation 9 transition.
    with(green) {
      assertProduction(m = 24, s = 1, t = 6, p = 15, e = 4, h = 4)
      assertResources(m = 64, s = 1, t = 6, p = 17, e = 4, h = 9)
      assertCounts(
          37 to "TerraformRating",
          33 to "CardFront",
          5 to "OwnedTile",
      )
      assertCardResources(
          6 to Pets,
          8 to MartianZoo,
          2 to VenusianInsects,
          0 to AerialMappers,
          0 to AsteroidRights,
      )
    }
    with(yellow) {
      assertProduction(m = 12, s = 3, t = 1, p = 0, e = 6, h = 2)
      assertResources(m = 64, s = 3, t = 1, p = 10, e = 6, h = 11)
      assertCounts(
          48 to "TerraformRating",
          26 to "CardFront",
          3 to "OwnedTile",
      )
      assertCardResources(
          11 to StratosphericBirds,
          3 to VenusianAnimals,
          1 to NitriteReducingBacteria,
          8 to Psychrophiles,
          0 to ForcedPrecipitation,
          0 to ExtractorBalloons,
      )
    }
    admin.assertCounts(
        1 to "OceanTile<Utopia_7_6>",
        1 to "OceanTile<Utopia_7_5>",
        1 to "OceanTile<Utopia_8_6>",
        1 to "OceanTile<Utopia_4_5>",
    )
    green.assertCounts(
        1 to "GreeneryTile<Utopia_6_3>",
        1 to "GreeneryTile<Utopia_5_2>",
    )
    yellow.assertCounts(1 to "CityTile<Utopia_6_5>")
    assertSidebar(gen = 10, temp = -2, oxygen = 6, oceans = 9, venus = 30)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "Well, I'm buying three cards." "All right, [Yellow] buys three cards, and I'm going to
    // stupidly
    // buy two cards. You know what? I'm going to buy three cards. Talk about stupid. Three cards."
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

    with(yellow) {
      // "I will plant forest, put my greenery on... Looks like 5-5, right?" "Okay. Two money and a
      // plant."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_5_5>",
          "OxygenStep",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
      // "And I will greenery standard project." "But you've got two TR from one move." "Yeah, and
      // an extra for being the one to get it." "Anyways, the second one goes... 2-1."
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Utopia_2_1>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
          "TemperatureStep",
          "TerraformRating",
          "Ok", // All nine oceans are already placed.
      )
    }

    with(green) {
      // "I am feeling like I had better put a cute little city down while I can. So I paid for
      // standard project." "4-4 for two money and two plants."
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Utopia_4_4>",
          "Animal<$Pets>",
          "2 Plant",
          "2 MC",
      )
      // "Ecological zone. Cost me 12 entire." "Well, for these two, I get two animals right away."
      // "Putting it on 2-2?" "Yes. For two steel."
      doTasks(
          *playCardTasks("$EcologicalZone"),
          "12 Pay<> FROM MC",
          "$EcologicalZone FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "Animal<$EcologicalZone>",
          "EcologicalZone_SpecialTile<Utopia_2_2>",
          "2 Steel",
      )
    }

    with(yellow) {
      // "Eight for cryosleep because I have science tag discount. Pay eight. And I get a new
      // animal."
      doTasks(
          *playCardTasks("$CryoSleep"),
          "8 Pay<> FROM MC",
          "$CryoSleep FROM ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      // "And I spend two energy to trade with Miranda for two animals and a card."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "ProjectCard",
          "2 Animal<$StratosphericBirds>",
      )
    }

    with(green) {
      // "I will use my business network to look at a card. Absolutely not."
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      // "Heat boob." "Now your turn."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(yellow) {
      // "Back to viral research, baby. Cost you eight?" "Yes. Almost forgot. Draw one card."
      // "I will choose Nitrate Reducing Bacteria." "Six microbes."
      doTasks(
              *playCardTasks("$BactoviralResearch"),
              "8 Pay<> FROM MC",
              "$BactoviralResearch FROM ProjectCard",
              "ProjectCard",
              "Animal<$VenusianAnimals>",
              "6 Microbe<$NitriteReducingBacteria>",
          )
          .expect("0 Animal<Green, $EcologicalZone<Green>>")
      doTask("PassBaton")
    }

    with(green) {
      // "Herbivores, again with the full price." "I do add an animal to this card and an animal to
      // Ecozone. And you lose a plant production." "Oh, shit, I don't have any plant production."
      // Yellow has none to lose; Green's ledger records his own decrease to 14 (entry 272).
      doTasks(
              *playCardTasks("$Herbivores"),
              "12 Pay<> FROM MC",
              "$Herbivores FROM ProjectCard",
              "Animal<$Herbivores>",
              "PROD[-Plant]",
              "Animal<$EcologicalZone>",
          )
          .expect("PROD[0 Plant<Yellow>]")
      doTask("PassBaton")
    }

    with(yellow) {
      // "Jovian lanterns for 20." "Increase your TR one step." "Add two floaters to any card. I
      // will
      // add it to itself."
      doTasks(
          *playCardTasks("$JovianLanterns"),
          "20 Pay<> FROM MC",
          "$JovianLanterns FROM ProjectCard",
          "TerraformRating",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Plant boop, plant boop." "So two money and two plants." "And played greenery. So I add a
      // minimal to herbivores. I add two of them."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_4_3>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "Plant",
          "TerraformRating",
      )
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_5_4>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "Plant",
          "2 MC",
          "TerraformRating",
      )
    }

    with(yellow) {
      // "I use Jovian Lantern, spend a titanium to add two floaters here."
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      // "Actually, I'm just going to go like add a thing to extractor balloons."
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
    }

    with(green) {
      // "I think I'm going to use Martian Zoo to take eight money and then spend eighteen money on
      // lava flows." "Two cards and four money."
      doTasks(*cardActionTasks("$MartianZoo"), "8 MC")
      doTasks(
          *playCardTasks("$LavaFlows"),
          "18 Pay<> FROM MC",
          "$LavaFlows FROM ProjectCard",
          "TemperatureStep",
          "TemperatureStep",
          "TerraformRating",
          "TerraformRating",
          "LavaFlows_SpecialTile<Utopia_8_5>",
          "2 ProjectCard",
          "2 MC",
          "2 MC",
      )
    }
    // Green's ledger omitted the two TR from Lava Flows' temperature steps.
    green.exMachina("-2 TerraformRating")

    with(yellow) {
      // "Oh, I add a Strato Bird."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      // "Oh, probably be smart for me to do my own heat boob."
      doTasks(
          "UseAction<ConvertHeatAction, Action1>",
          "8 Pay<Class<Heat>> FROM Heat",
          "TemperatureStep",
          "TerraformRating",
      )
    }

    with(green) {
      // "It's weird, but I'm gonna play a card I've never played before in my life. Food Factory."
      // "And three real gives me four money production. Takes away one of my plant production."
      doTasks(
          *playCardTasks("$FoodFactory"),
          "3 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$FoodFactory FROM ProjectCard",
          "PROD[-Plant]",
          "PROD[4 MC]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I will add Psychrophile and I will remove three nitrites for a TR."
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
    }

    with(green) {
      // "I'm going to sell a patent to get a money and then spend two money on floating habs. To
      // use
      // floating habs to put a floater onto aerial mappers."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
    }

    with(yellow) {
      // "I'll pay three psychrophiles for green houses." "Gain one plant for each city tile in
      // play. That's one, two, three, four, five."
      doTasks(
              *playCardTasks("$Greenhouses"),
              "3 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
              "Ok", // No steel paid.
              "Ok", // No M€ needed after paying with Psychrophiles microbes.
              "$Greenhouses FROM ProjectCard",
              "5 Plant",
          )
          .expect("0 Animal<Green, $EcologicalZone<Green>>")
      // "And I will greenery boop." "It's six, six, sorry." "It's the last possible spot next to my
      // capital for two money."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_6_6>",
          "OxygenStep",
          "2 MC",
          "TerraformRating",
      )
    }

    with(green) {
      // "I'm going to use aerial mappers to take a floater off of aerial mappers and draw a card."
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
      doTask("PassBaton")
    }
    yellow.doTask("Pass")
    with(green) {
      // "And then I'm going to use energy market to reduce energy production and give myself eight
      // money."
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      // "I guess I play Dawn City for three titanium." "I lose an energy production. I gain a
      // titanium production." "And so when I place that, I believe I get a pet."
      // "Oh shit. I don't." "Okay, since I already committed to it, what I will do is I will sell a
      // patent to get one money, spend nine money on robotic workforce just to make an honest card
      // out of it." "I'll copy industrial microbes." "Robotic workforce in Dawn City, we pretended
      // they were in that order."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$RoboticWorkforce"),
          "9 Pay<> FROM MC",
          "$RoboticWorkforce FROM ProjectCard",
      )
      // Name the context because matching EACH against Mars University's pending THEN task
      // otherwise throws a syntax error; see the task-matching TODO.
      doTask(
          "EACH @WildTagElection<Green>(HAS MAX 0 Tag) { Tag<@WildTagElection>? }",
          RoboticWorkforce,
      )
      doTasks(
          "-ProjectCard",
          "ProjectCard",
          "0 BuildingTag<WildTagElection<WildTag<$ResearchCoordination>>>",
          "CopyProductionBox<$IndustrialMicrobes>",
          "PROD[Energy]",
          "PROD[Steel]",
      )
      doTasks(
          *playCardTasks("$DawnCity"),
          "3 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$DawnCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[Titanium]",
          "NormalCityTile<DawnCity_RemoteArea>",
          "Animal<$Pets>",
      )
      // "I'm gonna add a Venusian insect."
      doTasks(*cardActionTasks("$VenusianInsects"), "Microbe<$VenusianInsects>")
      // "I'm going to yet again, sell a patent for one money and spend that one money on asteroid
      // rights to put an asteroid onto that."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTasks(*cardActionTasks("$AsteroidRights"), "Pay<> FROM MC", "Asteroid<$AsteroidRights>")
      // "But I'll take the cards using three energy for the Pluto, take two cards."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Pluto>",
          "2 ProjectCard",
      )
      doTask("Pass")
    }

    // Production.
    green.doTasks(
        "69 MC",
        "7 Titanium",
        "13 Plant",
        "4 Heat",
        "2 Steel",
        "3 Energy",
        "Heat FROM Energy",
    )
    yellow.doTasks("67 MC", "6 Energy", "3 Steel", "4 Heat FROM Energy", "Titanium", "2 Heat")
    // Yellow uses World Government Terraforming to raise oxygen to 12%.
    yellow.doTask("OxygenStep! BY Admin").expect("0 TerraformRating")
  }

  private fun generation11() {
    // board-16-44-30.jpg and both player ledgers: after Generation 10 transition.
    with(green) {
      assertProduction(m = 29, s = 2, t = 7, p = 13, e = 3, h = 4)
      assertResources(m = 69, s = 2, t = 10, p = 18, e = 3, h = 6)
      assertCounts(
          40 to "TerraformRating",
          38 to "CardFront",
          11 to "OwnedTile",
      )
      assertCardResources(
          8 to Pets,
          8 to MartianZoo,
          3 to EcologicalZone,
          3 to Herbivores,
          3 to VenusianInsects,
          0 to AerialMappers,
          1 to AsteroidRights,
      )
    }
    with(yellow) {
      assertProduction(m = 12, s = 3, t = 1, p = 0, e = 6, h = 2)
      assertResources(m = 69, s = 6, t = 1, p = 0, e = 6, h = 9)
      assertCounts(
          55 to "TerraformRating",
          30 to "CardFront",
          6 to "OwnedTile",
      )
      assertCardResources(
          14 to StratosphericBirds,
          5 to VenusianAnimals,
          4 to NitriteReducingBacteria,
          6 to Psychrophiles,
          0 to ForcedPrecipitation,
          1 to ExtractorBalloons,
          4 to JovianLanterns,
      )
    }
    green.assertCounts(
        1 to "CityTile<Utopia_4_4>",
        1 to "SpecialTile<Utopia_2_2>",
        1 to "GreeneryTile<Utopia_4_3>",
        1 to "GreeneryTile<Utopia_5_4>",
        1 to "SpecialTile<Utopia_8_5>",
    )
    yellow.assertCounts(
        1 to "GreeneryTile<Utopia_5_5>",
        1 to "GreeneryTile<Utopia_2_1>",
        1 to "GreeneryTile<Utopia_6_6>",
    )
    assertSidebar(gen = 11, temp = 8, oxygen = 12, oceans = 9, venus = 30)

    // Research.
    green.doTask("4 ProjectCard<Selecting>")
    yellow.doTask("4 ProjectCard<Selecting>")

    // "Man. Yeah. I'm buying two. I'll buy one. I'm gonna actually buy it."
    green.doTasks(
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "1 ProjectCard<Hand FROM Selecting>",
    )
    yellow.doTasks(
        "-2 ProjectCard<Selecting>",
        "6 Pay<> FROM MC",
        "2 ProjectCard<Hand FROM Selecting>",
    )

    with(green) {
      // "Yep. Boop, boop. Indeed. And the game will officially end this round."
      // "One goes here for just two money. That is three, four." "I think I'll just take the four
      // money down here."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_3_4>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "2 MC",
          "TerraformRating",
      )
      // The second placement is gesture-only in the transcript; its four-M€ ocean income locates
      // it at the open land area between the row-six and row-seven oceans.
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_7_4>",
          "OxygenStep",
          "Animal<$Herbivores>",
          "2 MC",
          "2 MC",
          "TerraformRating",
      )
    }

    with(yellow) {
      // "I will... pay two energy to trade with Miranda... for one aminal and a card. This time
      // I'll
      // put the aminal on Venusian."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "2 Pay<Class<Energy>> FROM Energy",
          "Trade<Miranda>",
          "ProjectCard",
          "Animal<$VenusianAnimals>",
      )
      // "Productive outpost for zero... Gain all my colony bonuses, which is literally just draw a
      // card."
      doTasks(
          *playCardTasks("$ProductiveOutpost"),
          "$ProductiveOutpost FROM ProjectCard",
          "ProjectCard",
      )
    }

    with(green) {
      // "I pay fourteen... Mogul." "Yeah. I think I got that one."
      doTasks("UseAction<FundAwardAction, Action1>", "14 Pay<> FROM MC", "Mogul")
      // "Listen, all of y'all. It's sabotage. So... You lose... Seven money, and that's it."
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "-7 MC<Yellow>",
      )
    }

    with(yellow) {
      // "I'm going to spend thirteen money, no titanus." "Lose two money production." "Place a
      // colony on Pluto. To get two cards."
      doTasks(
          *playCardTasks("$PioneerSettlement"),
          "Ok", // No titanium paid.
          "13 Pay<> FROM MC",
          "$PioneerSettlement FROM ProjectCard",
          "PROD[-2 MC]",
          "Colony<Pluto>",
          "2 ProjectCard",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm going to spend two on floating habs. To put a dingus on aerial mappers." "Use aerial
      // mappers to draw a card."
      doTasks(*cardActionTasks("$FloatingHabs"), "2 Pay<> FROM MC", "Floater<$AerialMappers>")
      doTasks(
          *cardActionTasks("$AerialMappers", "Action2"),
          "-Floater<$AerialMappers>",
          "ProjectCard",
      )
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$JovianLanterns"),
          "Pay<Class<Titanium>> FROM Titanium",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Immigrant City, spending six worth of steel and seven rail." "I better decrease my energy
      // production, decrease my money production by two and then back up one." "Five, six, on five,
      // six."
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "2 Pay<Class<Steel>> FROM Steel",
          "7 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Utopia_5_6>",
          "Animal<$Pets>",
          "PROD[MC]",
          "2 Plant",
          "2 MC",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      doTasks(
          *cardActionTasks("$NitriteReducingBacteria", "Action2"),
          "-3 Microbe<$NitriteReducingBacteria>",
          "TerraformRating",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "I'm gonna put it right here, two energy."
      // The City standard project and Immigrant City each increase M€ production for this
      // placement.
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "PROD[MC]",
          "CityTile<Utopia_2_3>",
          "Animal<$Pets>",
          "PROD[MC]",
          "2 Energy",
      )
      // "And for my second trick, commercial district, from sixteen, lose an energy production,
      // gain four money production, place a shitty tile, not a shitty tile."
      doTasks(
          *playCardTasks("$CommercialDistrict"),
          "16 Pay<> FROM MC",
          "$CommercialDistrict FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[4 MC]",
          "CommercialDistrict_SpecialTile<Utopia_3_3>",
          "Steel",
      )
    }
    // Green confirms he forgot Immigrant City's trigger. His ledger records only the standard
    // project's one M€ production step, so remove the omitted Immigrant City step here.
    green.exMachina("PROD[-MC]")

    with(yellow) {
      doTasks(*cardActionTasks("$Psychrophiles"), "Microbe<$Psychrophiles>")
      doTask("PassBaton")
    }

    with(green) {
      // "I'm gonna play robot pollinators for all of my money. It gives me a plant production and
      // one plant per plant tag. One, two, three, four, five. Five plants."
      doTasks(
          *playCardTasks("$RobotPollinators"),
          "9 Pay<> FROM MC",
          "$RobotPollinators FROM ProjectCard",
          "PROD[Plant]",
          "6 Plant",
      )
      // "I'm just gonna do the plant boop now." "Did not give me TR good." "This plant boop will go
      // here for two energy and a card. That is two, four."
      doTasks(
              "UseAction<ConvertPlantsAction, Action1>",
              "8 Pay<Class<Plant>> FROM Plant",
              "GreeneryTile<Utopia_2_4>",
              "OxygenStep",
              "Animal<$Herbivores>",
              "2 Energy",
              "ProjectCard",
          )
          .expect("0 TerraformRating")
    }

    // 16:55:51: Green counted five plants, overlooking Research Coordination's wild tag.
    green.exMachina("-Plant")

    with(yellow) {
      // "I'm gonna add a strato bird." "I'm at 15 strato birds."
      doTasks(*cardActionTasks("$StratosphericBirds"), "Animal<$StratosphericBirds>")
      doTask("PassBaton")
    }

    with(green) {
      // "Methane from Titan." "I'm gonna spend six titanium." "I mostly played it for the two
      // points."
      doTasks(
          *playCardTasks("$MethaneFromTitan"),
          "6 Pay<Class<Titanium>> FROM Titanium",
          "Ok", // No M€ needed after paying with titanium.
          "$MethaneFromTitan FROM ProjectCard",
          "PROD[2 Heat]",
          "PROD[2 Plant]",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I add an extractor balloon."
      doTasks(*cardActionTasks("$ExtractorBalloons"), "Floater<$ExtractorBalloons>")
      doTask("PassBaton")
    }

    with(green) {
      // "I'll go ahead and use Martian Zoo to take eight money."
      doTasks(*cardActionTasks("$MartianZoo"), "8 MC")
      doTask("PassBaton")
    }

    with(yellow) {
      // "I'll use local heat trapping. One money. Spend five heat. And I will add two Venusian
      // animals."
      doTasks(
          *playCardTasks("$LocalHeatTrapping"),
          "1 Pay<> FROM MC",
          "$LocalHeatTrapping FROM ProjectCard",
          "-5 Heat",
          "2 Animal<$VenusianAnimals>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Trading colony for my four titanium." "No, no, no. I'm gonna get three microbes." "Three
      // microbes which go on to Venusian insects."
      doTasks(
              *playCardTasks("$TradingColony"),
              "4 Pay<Class<Titanium>> FROM Titanium",
              "Ok", // No M€ needed after paying with titanium.
              "$TradingColony FROM ProjectCard",
              "Colony<Enceladus>",
              "3 Microbe<$VenusianInsects>",
          )
          .expect("PROD[0 MC]")
      doTask("PassBaton")
    }

    with(yellow) {
      // "Airliners for 11 requires that you have three floaters." "Gain two money production, add
      // two floaters to another card, which will be Jovian lanterns."
      doTasks(
          *playCardTasks("$Airliners"),
          "11 Pay<> FROM MC",
          "$Airliners FROM ProjectCard",
          "PROD[2 MC]",
          "2 Floater<$JovianLanterns>",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Now, I'm going to fly my little boat to Angelatus." "I get three and one." "They all four
      // go on to the New Zealand insects."
      doTasks(
          "UseAction<TradeAction, Action2>",
          "3 Pay<Class<Energy>> FROM Energy",
          "Trade<Enceladus>",
          "ColonyProduction<Enceladus>",
          "3 Microbe<$VenusianInsects>",
          "Microbe<$VenusianInsects>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "My seven psychrophiles and three real." "Increase money production two steps. Increase
      // plant production three steps. Increase... No, gain two plants."
      doTasks(
          *playCardTasks("$KelpFarming"),
          "7 PayFromCard<$Psychrophiles> FROM Microbe<$Psychrophiles>",
          "3 Pay<> FROM MC",
          "$KelpFarming FROM ProjectCard",
          "PROD[2 MC]",
          "PROD[3 Plant]",
          "2 Plant",
      )
      doTask("PassBaton")
    }

    with(green) {
      // "Just to be funny, I'm going to play one for land claim, just so you can go there."
      // The source does not identify the claimed area; Utopia_1_1 is a neutral test inference.
      doTasks(
          *playCardTasks("$LandClaim"),
          "1 Pay<> FROM MC",
          "$LandClaim FROM ProjectCard",
          "Community<Utopia_1_1>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "I sell a card for a money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      // "I'll go ahead and sell five cards for five money. None of them have victory points on
      // them."
      doTasks(*standardProjectTasks("SellPatentsProject"), "5 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(yellow) {
      // "Wait. I sell a card for a money."
      doTasks(*standardProjectTasks("SellPatentsProject"), "1 MC FROM ProjectCard")
      doTask("PassBaton")
    }

    with(green) {
      // "I'll spend eight on lightning harvest. One energy product, one money product, and a
      // point."
      doTasks(
          *playCardTasks("$LightningHarvest"),
          "8 Pay<> FROM MC",
          "$LightningHarvest FROM ProjectCard",
          "PROD[Energy]",
          "PROD[MC]",
      )
      doTask("PassBaton")
    }

    // Yellow's ledger groups Media Archives' net thirteen-M€ gain with the twenty-five-M€ Water
    // Import from Europa payment below as one twelve-M€ debit at entry 325.
    with(yellow) {
      // "Sell three cards for three money. Wait, actually. Hold on. Just in case that can be useful
      // somehow. I'll sell two for two."
      doTasks(*standardProjectTasks("SellPatentsProject"), "2 MC FROM ProjectCard")
      // "I'm going to play Media Archives." "I have eight. I have 13, so that's 21 money for you."
      doTasks(
          *playCardTasks("$MediaArchives"),
          "8 Pay<> FROM MC",
          "$MediaArchives FROM ProjectCard",
          "21 MC",
      )
    }

    // "Oh my god, I forgot to use my business network. Fine, I'll use that then."
    with(green) {
      doTasks(
          *cardActionTasks("$BusinessNetwork"),
          "ProjectCard<Selecting>",
          "-1 ProjectCard<Selecting>",
      )
      doTask("PassBaton")
    }

    with(yellow) {
      // "Finally playing. Water Import from Europa."
      doTasks(
          *playCardTasks("$WaterImportFromEuropa"),
          "25 Pay<> FROM MC",
          "$WaterImportFromEuropa FROM ProjectCard",
      )
      doTask("PassBaton")
    }

    // The transcript identifies Sub-Zero Salt Fish in Green's hand, while the generic reconstructed
    // hand is one card short after the sourced plays and patent sales.
    green.exMachina("ProjectCard")
    with(green) {
      // "Okay, now I can use Energy Market. Get all up to 12 money."
      doTasks(*cardActionTasks("$EnergyMarket", "Action2"), "PROD[-Energy]", "8 MC")
      // "Play Sub-Zero Salt Fish." "Now you lose plant production. I spend five on that." "It's an
      // animal tag." "So I get an Ecomole."
      doTasks(
          *playCardTasks("$SubZeroSaltFish"),
          "5 Pay<> FROM MC",
          "$SubZeroSaltFish FROM ProjectCard",
          "Animal<$EcologicalZone>",
          "PROD[-Plant<Yellow>]",
      )
    }
    // Green took Energy Market's eight M€ but did not record its energy-production decrease.
    green.exMachina("PROD[Energy]")

    with(yellow) {
      // "But, well, still, for the means, I can play Predators."
      // "Well, I guess you're going to take my Ecomal, then."
      doTasks(*playCardTasks("$Predators"), "14 Pay<> FROM MC", "$Predators FROM ProjectCard")
          .expect("0 Animal<Green, $EcologicalZone<Green>>")
      doTasks(
          *cardActionTasks("$Predators"),
          "-Animal<Green, $EcologicalZone<Green>>",
          "Animal<$Predators>",
      )
    }

    with(green) {
      // "And then, you know, for all the good it'll do, I'll just use the action to add another
      // animal."
      doTasks(*cardActionTasks("$SubZeroSaltFish"), "Animal<$SubZeroSaltFish>")
      doTask("PassBaton")
    }

    with(yellow) {
      // "Thanks to my steel, I can spend all six and three real for a point from Artificial Lake."
      doTasks(
          *playCardTasks("$ArtificialLake"),
          "6 Pay<Class<Steel>> FROM Steel",
          "3 Pay<> FROM MC",
          "$ArtificialLake FROM ProjectCard",
          "0 OceanTile<LandArea>", // All nine oceans are already placed.
      )
      doTask("PassBaton")
    }
    green.doTask("Pass")
    yellow.doTask("Pass")

    // Production.
    green.doTasks(
        "76 MC",
        "7 Titanium",
        "16 Plant",
        "6 Heat",
        "2 Steel",
        "2 Energy",
        "4 Heat FROM Energy",
    )
    yellow.doTasks(
        "70 MC",
        "6 Energy",
        "3 Steel",
        "4 Heat FROM Energy",
        "Titanium",
        "2 Heat",
        "2 Plant",
    )

    // Both ledgers: after the final production phase and before final greenery placement.
    with(green) {
      assertProduction(m = 34, s = 2, t = 7, p = 16, e = 2, h = 6)
      assertResources(m = 83, s = 3, t = 7, p = 17, e = 2, h = 16)
      assertCounts(42 to "TerraformRating")
    }
    with(yellow) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
      assertCounts(56 to "TerraformRating")
    }
    // The resource apps incremented their display to 12 during final production; the engine keeps
    // the completed action generation numbered 11.
    assertSidebar(gen = 11, temp = 8, oxygen = 14, oceans = 9, venus = 30)

    // Reconcile the mistakes still present in the physical game so final greenery and scoring use
    // the ordinary-rules state. This net delta is characterized by the separate exMachina-free
    // replay, not by the ledgers: retain Lava Flows' two TR and Kaguya Tech's Pets animal, retain
    // Immigrant City's M€-production trigger, and retain Energy Market's production decrease.
    green.exMachina("4 MC, 2 TerraformRating, PROD[MC, -Energy], -Energy, Animal<$Pets>")
    with(green) {
      assertProduction(m = 35, s = 2, t = 7, p = 16, e = 1, h = 6)
      assertResources(m = 87, s = 3, t = 7, p = 17, e = 1, h = 16)
      assertCounts(44 to "TerraformRating", 11 to "Animal<$Pets>")
    }

    if (exportReplay) {
      runTest {
        val originalEvents = game.events.entriesSince(0)
        val file = checkNotNull(completedGameFileJson())
        val decoded = GameFileJson.parse(file).decode(gamePremise)
        val reconstructed = decoded.open().world
        val decodedEvents = reconstructed.events.entriesSince(0)
        val allComponents = game.classTable.componentClass.baseType

        // The file stores Pets text, which can expand implicit type-variable arguments on parse.
        GameFileJson.encode(decoded, agents.taskLog.inputs()) shouldBe file
        decodedEvents.map { it.toString() } shouldBe originalEvents.map { it.toString() }
        decodedEvents.map { it.notes } shouldBe originalEvents.map { it.notes }
        reconstructed.tasks.extract { it } shouldBe game.tasks.extract { it }
        reconstructed.reader.getComponents(allComponents).entries shouldBe
            game.reader.getComponents(allComponents).entries
      }
    }

    with(green) {
      // "So I'm going to 1-2 and 1-3."
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_1_2>",
          "Animal<$Herbivores>",
      )
    }

    with(green) {
      doTasks(
          "UseAction<ConvertPlantsAction, Action1>",
          "8 Pay<Class<Plant>> FROM Plant",
          "GreeneryTile<Utopia_1_3>",
          "Animal<$Herbivores>",
          "2 Energy",
      )
    }
    // Decline another final greenery placement.
    green.doTask("PassBaton")
    // Decline the final greenery placement.
    yellow.doTask("PassBaton")

    val score = Summarizer(game)
    green.assertCounts(
        44 to "TerraformRating",
    )
    yellow.assertCounts(
        56 to "TerraformRating",
    )
    score.net("Milestone", "VictoryPoint<Green>") shouldBe 10
    score.net("Milestone", "VictoryPoint<Yellow>") shouldBe 5
    score.net("FirstPlace", "VictoryPoint<Green>") shouldBe 10
    score.net("FirstPlace", "VictoryPoint<Yellow>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Green>") shouldBe 0
    score.net("SecondPlace", "VictoryPoint<Yellow>") shouldBe 0
    score.net("GreeneryTile", "VictoryPoint<Green>") shouldBe 10
    score.net("GreeneryTile", "VictoryPoint<Yellow>") shouldBe 3
    score.net("CityTile", "VictoryPoint<Green>") shouldBe 15
    score.net("CityTile", "VictoryPoint<Yellow>") shouldBe 6
    score.net("Card", "VictoryPoint<Green>") shouldBe 35
    score.net("Card", "VictoryPoint<Yellow>") shouldBe 46
    score.net("$Pets", "VictoryPoint<Green>") shouldBe 5
    score.net("$VenusianInsects", "VictoryPoint<Green>") shouldBe 5
    score.net("$EcologicalZone", "VictoryPoint<Green>") shouldBe 1
    score.net("$Herbivores", "VictoryPoint<Green>") shouldBe 4
    // "Resource points on cards. One, four. Holy shit. Yeah. One, four, eight, and fifteen."
    // The earlier explicit count, "I'm at 15 strato birds," identifies the last value.
    score.net("$Predators", "VictoryPoint<Yellow>") shouldBe 1
    score.net("$JovianLanterns", "VictoryPoint<Yellow>") shouldBe 4
    score.net("$VenusianAnimals", "VictoryPoint<Yellow>") shouldBe 8
    score.net("$StratosphericBirds", "VictoryPoint<Yellow>") shouldBe 15

    // The spoken 118-115 tally omitted Green's four Herbivores points and the two Lava Flows TR
    // that the corrected scoring state retains. Yellow's spoken total is one point below the
    // complete
    // replay categories, which sum to 116.
    green.assertCounts(124 to "VictoryPoint", 1 to "Victory")
    yellow.assertCounts(116 to "VictoryPoint", 0 to "Victory")

    with(green) {
      assertProduction(m = 35, s = 2, t = 7, p = 16, e = 1, h = 6)
      assertResources(m = 87, s = 3, t = 7, p = 1, e = 3, h = 16)
    }
    with(yellow) {
      assertProduction(m = 14, s = 3, t = 1, p = 2, e = 6, h = 2)
      assertResources(m = 73, s = 3, t = 1, p = 4, e = 6, h = 10)
    }
  }

  private fun assertColonyProductions(vararg productions: Int) {
    require(productions.size == colonyTiles.size)
    admin.assertCounts(
        *productions
            .zip(colonyTiles) { production, colony -> production to "ColonyProduction<$colony>" }
            .toTypedArray()
    )
  }
}
