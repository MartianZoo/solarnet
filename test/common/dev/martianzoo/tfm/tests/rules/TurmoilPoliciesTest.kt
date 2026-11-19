package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.DependencyException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import dev.martianzoo.tfm.tests.standardProjectTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilPoliciesTest : TfmGameplayTest() {
  @Test
  internal fun `Greens rewards greenery during action play and leaves before solar`() {
    newTestGame("TurmoilExpansion, -QuickStartVariant", 2)
    kim.turn {
      doTasks(
          *standardProjectTasks("GreeneryProject"),
          "23 Pay<> FROM MC",
          "GreeneryTile<Tharsis_3_3>",
          "OxygenStep",
          "TerraformRating",
          "4 MC",
      )
    }
    stan.doTask("Pass")
    kim.doTask("Pass")
    kim.doTask("21 MC")
    stan.doTask("20 MC")

    admin.count("GreensPolicy") shouldBe 0
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Mars First rewards a city placed on Mars`() {
    electGovernment("MarsFirst", "PartyFollower", "PartyLeader")
    stan.doTask("Pass")

    kim.turn {
      doTasks(
          *standardProjectTasks("CityProject"),
          "25 Pay<> FROM MC",
          "CityTile<Tharsis_3_3>",
          "PROD[MC]",
          "Steel",
      )
    }
  }

  @Test
  internal fun `Scientists draws three cards only once per generation`() {
    electGovernment("Scientists")
    stan.doTask("Pass")
    shouldThrow<DependencyException> { kim.doTask("UseAction<KelvinistsPolicy, Action1>") }
    kim.turn {
      doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
    }
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks("UseAction<ScientistsPolicy, Action1>", "10 Pay<> FROM MC", "3 ProjectCard")
      }
    }
    kim.doTask("Pass")
    kim.doTask("20 MC")
    stan.doTask("19 MC")

    admin.count("ScientistsPolicy") shouldBe 0
    stan.doTasks(
        "VenusStep! BY Admin",
        "OceanTile<Tharsis_1_5> BY Admin",
    )
    kim.doTasks("Plant", "Steel")
    stan.doTasks("0 Plant", "0 Steel")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `Unity increases both players titanium payment value`() {
    electGovernment("Unity")
    // Each player has a titanium to spend on the actual card purchase.
    kim.exMachina("Titanium")
    stan.exMachina("Titanium")
    stan.turn {
      doTasks(
          *playCardTasks("$SolarWindPower"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "7 Pay<> FROM MC",
          "$SolarWindPower FROM ProjectCard",
          "PROD[Energy]",
          "2 Titanium",
      )
    }
    kim.turn {
      doTasks(
          *playCardTasks("$VestaShipyard"),
          "1 Pay<Class<Titanium>> FROM Titanium",
          "11 Pay<> FROM MC",
          "$VestaShipyard FROM ProjectCard",
          "PROD[Titanium]",
      )
    }
    stan.doTask("Pass")
    kim.doTask("Pass")

    kim.count("ResourceValue<Class<Titanium>>") shouldBe 3
    stan.count("ResourceValue<Class<Titanium>>") shouldBe 3
    kim.doTasks("20 MC", "Titanium")
    stan.doTasks(
        "19 MC",
        "Energy",
        "VenusStep! BY Admin",
        "OceanTile<Tharsis_1_5> BY Admin",
    )
    kim.doTasks("Plant", "Steel")
    stan.doTasks("0 Plant", "0 Steel")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  @Test
  internal fun `Reds charges for both TR steps gained by Bribed Committee`() {
    electGovernment("Reds", "PartyFollower", "PartyLeader", "PartyLeader")
    stan.doTask("Pass")

    kim.turn {
      doTasks(
          *playCardTasks("$BribedCommittee"),
          "7 Pay<> FROM MC",
          "$BribedCommittee FROM ProjectCard",
          "2 TerraformRating",
          "-6 MC",
      )
    }
  }

  @Test
  internal fun `Reds rejects an unaffordable TR gain without charging or terraforming`() {
    electGovernment("Reds", "PartyFollower", "PartyLeader", "PartyLeader")
    stan.doTask("Pass")
    kim.exMachina("MC") // Leave exactly 14 MC after two projects with their Reds charges.
    kim.turn {
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "-3 MC",
      )
      doTasks(
          *standardProjectTasks("AsteroidProject"),
          "14 Pay<> FROM MC",
          "TemperatureStep",
          "TerraformRating",
          "-3 MC",
      )
    }
    kim.count("MC") shouldBe 14
    shouldThrow<LimitsException> {
      kim.completeOperation {
        doTasks(
            *standardProjectTasks("AsteroidProject"),
            "14 Pay<> FROM MC",
            "TemperatureStep",
            "TerraformRating",
            "-3 MC",
        )
      }
    }

    kim.count("MC") shouldBe 14
    kim.count("TerraformRating") shouldBe 23
    admin.count("TemperatureStep") shouldBe 2
  }

  @Test
  internal fun `Kelvinists raises heat and energy production for ten MC`() {
    electGovernment("Kelvinists")
    stan.doTask("Pass")

    kim.turn {
      doTasks(
          "UseAction<KelvinistsPolicy, Action1>",
          "10 Pay<> FROM MC",
          "PROD[Heat]",
          "PROD[Energy]",
      )
    }
    kim.doTask("Pass")
    kim.doTasks("20 MC", "Heat", "Energy")
    stan.doTask("19 MC")
    admin.count("KelvinistsPolicy") shouldBe 0
    stan.doTasks(
        "VenusStep! BY Admin",
        "OceanTile<Tharsis_1_5> BY Admin",
    )
    kim.doTasks("Plant", "Steel")
    stan.doTasks("0 Plant", "0 Steel")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
  }

  private fun electGovernment(
      party: String,
      firstRole: String = "PartyLeader",
      secondRole: String = "PartyFollower",
      neutralRole: String = "PartyFollower",
  ) {
    newTestGame(
        addOptions = "TurmoilExpansion, -QuickStartVariant",
        playerCount = 2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
    )
    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "$firstRole<$party> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "$secondRole<$party>")
    }
    stan.doTask("Pass")
    kim.turn {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<$party>")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<$party>")
    }
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")

    admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
    admin.count("Distant<$ExploreFirstDirective>") shouldBe 1
    (admin.count("$neutralRole<Reds, Neutral>") > 0) shouldBe true
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("Ruling<$party>") shouldBe 1
  }
}
