package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilPoliciesTest : TfmGameplayTest() {
  @Test
  internal fun `Greens rewards greenery during action play and leaves before solar`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdProject("GreeneryProject") { placeTile(3, 3) }
          .expect("-19 MC, GreeneryTile, TerraformRating")
    }
    stan.pass()
    kim.pass()

    admin.count("GreensPolicy") shouldBe 0
  }

  @Test
  internal fun `Mars First rewards a city placed on Mars`() {
    electGovernment("MarsFirst")
    stan.pass()

    kim.stdProject("CityProject") { placeTile(3, 3) }.expect("CityTile, Steel, PROD[MC]")
  }

  @Test
  internal fun `Scientists draws three cards only once per generation`() {
    electGovernment("Scientists")
    stan.pass()
    kim.stdAction("UseTurmoilPolicyAction").expect("-10 MC, 3 ProjectCard")
    shouldThrow<NotNowException> { kim.stdAction("UseTurmoilPolicyAction") }
    kim.pass()

    admin.count("ScientistsPolicy") shouldBe 0
  }

  @Test
  internal fun `Unity increases both players titanium payment value`() {
    electGovernment("Unity")
    stan.turn { playProject(SolarWindPower, mc = 7, titanium = 1).expect("Titanium, -7 MC") }
    kim.turn { playProject(VestaShipyard, mc = 11, titanium = 1).expect("-Titanium, -11 MC") }
    stan.pass()
    kim.pass()

    kim.count("ResourceValue<Class<Titanium>>") shouldBe 3
    stan.count("ResourceValue<Class<Titanium>>") shouldBe 3
  }

  @Test
  internal fun `Reds charges for both TR steps gained by Bribed Committee`() {
    electGovernment("Reds")
    stan.pass()

    kim.playProject(BribedCommittee, 7).expect("-13 MC, 2 TerraformRating")
  }

  @Test
  internal fun `Reds rejects an unaffordable TR gain without charging or terraforming`() {
    electGovernment("Reds")
    stan.pass()
    kim.stdProject("AsteroidProject").expect("-17 MC, TemperatureStep, TerraformRating")
    kim.stdProject("AsteroidProject").expect("-17 MC, TemperatureStep, TerraformRating")
    // The remaining 14 MC covers the project but not Reds' additional 3 MC.
    kim.count("MC") shouldBe 14
    val ratingBefore = kim.count("TerraformRating")
    val temperatureBefore = admin.count("TemperatureStep")
    shouldThrow<NotNowException> { kim.stdProject("AsteroidProject") }

    kim.count("MC") shouldBe 14
    kim.count("TerraformRating") shouldBe ratingBefore
    admin.count("TemperatureStep") shouldBe temperatureBefore
  }

  @Test
  internal fun `Kelvinists raises heat and energy production for ten MC`() {
    electGovernment("Kelvinists")
    stan.pass()

    kim.stdAction("UseTurmoilPolicyAction<Action2>").expect("-10 MC, PROD[Heat, Energy]")
    kim.pass()
    admin.count("KelvinistsPolicy") shouldBe 0
  }

  private fun electGovernment(party: String) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<$party>") }
      stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<$party>") }
    }
    stan.pass()
    kim.stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<$party>") }
    kim.stdAction("LobbyAction<Action2>") { doTask("PartyDelegate<$party>") }
    kim.pass()
    kim.wgt("VenusStep")
    admin.doTask("ExploreFirstDirective")
    players.forEach { it.buyCards(0) }
    admin.count("Ruling<$party>") shouldBe 1
  }
}
