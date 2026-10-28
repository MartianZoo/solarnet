package dev.martianzoo.tfm.tests.replays

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.catalog.GameConfig
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class HellasPromoGameTest : AbstractFullGameTest() {
  override val config =
      GameConfig(
          """
          HellasMap
          promos:cards, PreludeExpansion
          """,
          "Player1",
          "Player2",
      )

  @Test
  internal fun hellasPromoGame() {
    p1.autoExecPolicy = NONE
    p2.autoExecPolicy = NONE
    admin.startGame()
    p1.doTask("20 TerraformRating")
    p2.doTask("20 TerraformRating")
    p1.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-3 ProjectCard<Selecting>",
    )
    p2.doTasks(
        "2 CorporationCard",
        "-CorporationCard",
        "10 ProjectCard<Selecting>",
        "4 PreludeCard",
        "-2 PreludeCard",
        "-5 ProjectCard<Selecting>",
    )

    p1.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$InterplanetaryCinematics>>",
          "$InterplanetaryCinematics FROM CorporationCard",
          "30 MC",
          "20 Steel",
          "21 Pay<> FROM MC",
          "7 ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
          "$PharmacyUnion FROM CorporationCard",
          "SearchForCard<TagFilter<Class<ScienceTag>>>",
          "54 MC",
          "15 Pay<> FROM MC",
          "5 ProjectCard<Hand FROM Selecting>",
          "Disease<$PharmacyUnion>",
          "Disease<$PharmacyUnion>",
          "-8 MC",
      )
    }

    p1.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$UnmiContractor>>",
          "$UnmiContractor FROM PreludeCard",
          "3 TerraformRating",
          "ProjectCard",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$CorporateArchives>>",
          "$CorporateArchives FROM PreludeCard",
          "13 MC",
          "7 ProjectCard<Selecting>",
          "2 ProjectCard<Hand FROM Selecting>",
      )
    }
    p2.turn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BiosphereSupport>>",
          "$BiosphereSupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[2 Plant]",
      )
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
          "$SocietySupport FROM PreludeCard",
          "PROD[-MC]",
          "PROD[Plant]",
          "PROD[Energy]",
          "PROD[Heat]",
      )
    }

    // Generation 1 (Player1 first)

    p1.turn {
      doTasks(*playCardTasks("$MediaGroup"), "6 Pay<> FROM MC", "$MediaGroup FROM ProjectCard")
      doTasks(
          *playCardTasks("$Sabotage"),
          "1 Pay<> FROM MC",
          "$Sabotage FROM ProjectCard",
          "2 MC",
          "3 MC",
          "-7 MC<Player2>",
      )
    }
    p2.turn {
      doTasks(
          *playCardTasks("$Research"),
          "11 Pay<> FROM MC",
          "$Research FROM ProjectCard",
          "2 ProjectCard",
          "TerraformRating FROM Disease<$PharmacyUnion>",
          "TerraformRating FROM Disease<$PharmacyUnion>",
      )
      doTasks(
          *playCardTasks("$MartianSurvey"),
          "9 Pay<> FROM MC",
          "$MartianSurvey FROM ProjectCard",
          "2 ProjectCard",
          "Ok", // decline Pharmacy Union's optional flip
      )
    }
    p1.doTask("Pass")
    p2.turn {
      doTasks(
          *playCardTasks("$SearchForLife"),
          "3 Pay<> FROM MC",
          "$SearchForLife FROM ProjectCard",
          "PlayedEvent<Class<$PharmacyUnion>> FROM $PharmacyUnion THEN 3 TerraformRating",
          "3 TerraformRating",
      )
      doTasks(
          *cardActionTasks("$SearchForLife"),
          "1 Pay<> FROM MC",
          "ProjectCard<Revealed>",
          "Ok", // decline reward
      )
      doTask("-ProjectCard<Revealed>")
    }
    p2.doTask("Pass")

    p1.doTasks("-5 MC", "5 MC", "23 MC")
    p2.doTasks("3 MC", "25 MC", "-5 MC", "3 Plant", "Energy", "Heat")

    // Generation 2 (Player2 first)
    p1.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )

    p2.turn {
      doTasks(*standardProjectTasks("SellPatentsProject"), "MC FROM ProjectCard")
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "15 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    p1.turn {
      doTasks(
          *playCardTasks("$EarthCatapult"),
          "23 Pay<> FROM MC",
          "$EarthCatapult FROM ProjectCard",
      )
      doTasks(
          *playCardTasks("$OlympusConference"),
          "4 Pay<Class<Steel>> FROM Steel",
          "Ok", // no MC paid
          "$OlympusConference FROM ProjectCard",
          "Science<$OlympusConference>",
      )
    }
    p2.doTask("Pass")

    p1.turn {
      doTasks(
          *playCardTasks("$DevelopmentCenter"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$DevelopmentCenter FROM ProjectCard",
          "ProjectCard FROM Science",
      )
      doTasks(
          *playCardTasks("$GeothermalPower"),
          "4 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$GeothermalPower FROM ProjectCard",
          "PROD[2 Energy]",
      )
      doTasks(
          *playCardTasks("$MirandaResort"),
          "10 Pay<> FROM MC",
          "$MirandaResort FROM ProjectCard",
          "PROD[4 MC]",
      )
      doTasks(
          *playCardTasks("$Hackers"),
          "1 Pay<> FROM MC",
          "$Hackers FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[2 MC]",
          "PROD[-2 MC<Player2>]",
      )
      doTasks(
          *playCardTasks("$MicroMills"),
          "1 Pay<> FROM MC",
          "$MicroMills FROM ProjectCard",
          "PROD[Heat]",
      )
    }
    p1.doTask("Pass")

    p1.doTasks("11 MC", "-5 MC", "23 MC", "Energy", "Heat")
    p2.doTasks("MC", "25 MC", "-5 MC", "3 Plant", "Energy", "Heat", "Heat FROM Energy", "Titanium")

    // Generation 3 (Player1 first)
    p1.doTasks(
        "4 ProjectCard<Selecting>",
        "-ProjectCard<Selecting>",
        "9 Pay<> FROM MC",
        "3 ProjectCard<Hand FROM Selecting>",
    )
    p2.doTasks(
        "4 ProjectCard<Selecting>",
        "-3 ProjectCard<Selecting>",
        "3 Pay<> FROM MC",
        "ProjectCard<Hand FROM Selecting>",
    )

    p1.turn {
      doTasks(
          *cardActionTasks("$DevelopmentCenter"),
          "1 Pay<Class<Energy>> FROM Energy",
          "ProjectCard",
      )
      doTasks(
          *playCardTasks("$ImmigrantCity"),
          "5 Pay<Class<Steel>> FROM Steel",
          "1 Pay<> FROM MC",
          "$ImmigrantCity FROM ProjectCard",
          "PROD[-Energy]",
          "PROD[-2 MC]",
          "CityTile<Hellas_9_7>",
          "PROD[MC]",
          "-6 MC",
          "OceanTile<Hellas_5_6>",
          "TerraformRating",
          "ProjectCard",
      )
    }

    assertSidebar(gen = 3, temp = -30, oxygen = 0, oceans = 1)

    with(p1) {
      assertCounts(24 to "TerraformRating")
      assertProduction(m = 5, s = 0, t = 0, p = 0, e = 0, h = 1)
      assertResources(m = 16, s = 3, t = 0, p = 0, e = 0, h = 1)
      assertCounts(7 to "ProjectCard", 12 to "CardFront")
      assertCounts(5 to "ActiveCard", 4 to "AutomatedCard", 1 to "PlayedEvent")
      assertTags(but = 5, spt = 1, sct = 3, pot = 1, eat = 4, jot = 1, cit = 1)
      assertCounts(1 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
    }

    with(p2) {
      assertCounts(25 to "TerraformRating")
      assertProduction(m = -4, s = 0, t = 1, p = 3, e = 1, h = 1)
      assertResources(m = 18, s = 0, t = 1, p = 6, e = 1, h = 3)
      assertCounts(9 to "ProjectCard", 5 to "CardFront")
      assertCounts(1 to "ActiveCard", 2 to "AutomatedCard", 2 to "PlayedEvent")
      assertTags(spt = 1, sct = 3, jot = 1, plt = 1)
      assertCounts(0 to "CityTile", 0 to "GreeneryTile", 0 to "SpecialTile")
    }

    Summarizer(game).net("$PharmacyUnion", "TerraformRating<Player2>") shouldBe 5
    expectPendingTasksAtEnd(1) // Player2's next action offer
  }
}
