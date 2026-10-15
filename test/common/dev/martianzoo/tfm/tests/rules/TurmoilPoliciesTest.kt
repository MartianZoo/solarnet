package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
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
    kim.completeOperation {
          doTasks("UseStandardProject<GreeneryProject>")
          doTask("GreeneryTile<Tharsis_3_3>")
          autoExecNow(EAGER)
        }
        .expect("-19 MC, GreeneryTile, TerraformRating")

    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }

    admin.count("GreensPolicy") shouldBe 0
  }

  @Test
  internal fun `Mars First rewards a city placed on Mars`() {
    electGovernment("MarsFirst", "PartyFollower", "PartyLeader")
    stan.doTask("Pass")

    kim.completeOperation {
          doTasks("UseStandardProject<CityProject>")
          doTask("CityTile<Tharsis_3_3>")
          autoExecNow(EAGER)
        }
        .expect("CityTile, Steel, PROD[MC]")
  }

  @Test
  internal fun `Scientists draws three cards only once per generation`() {
    electGovernment("Scientists")
    stan.doTask("Pass")
    kim.doTasks("UseTurmoilPolicyAction").expect("-10 MC, 3 ProjectCard")
    shouldThrow<NotNowException> { kim.doTasks("UseTurmoilPolicyAction") }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }

    admin.count("ScientistsPolicy") shouldBe 0
  }

  @Test
  internal fun `Unity increases both players titanium payment value`() {
    electGovernment("Unity")
    stan
        .completeOperation {
          doTasks("PlayProject<Class<$SolarWindPower>>", "1 Pay<Class<Titanium>> FROM Titanium")
          autoExecNow(EAGER)
        }
        .expect("Titanium, -7 MC")
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.doTasks("PlayProject<Class<$VestaShipyard>>", "1 Pay<Class<Titanium>> FROM Titanium")
        .expect("-Titanium, -11 MC")
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.doTask("Pass")
    kim.doTask("Pass")

    kim.count("ResourceValue<Class<Titanium>>") shouldBe 3
    stan.count("ResourceValue<Class<Titanium>>") shouldBe 3
  }

  @Test
  internal fun `Reds charges for both TR steps gained by Bribed Committee`() {
    electGovernment("Reds", "PartyFollower", "PartyLeader", "PartyLeader")
    stan.doTask("Pass")

    kim.doTasks("PlayProject<Class<$BribedCommittee>>").expect("-13 MC, 2 TerraformRating")
  }

  @Test
  internal fun `Reds rejects an unaffordable TR gain without charging or terraforming`() {
    electGovernment("Reds", "PartyFollower", "PartyLeader", "PartyLeader")
    stan.doTask("Pass")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("-17 MC, TemperatureStep, TerraformRating")
    kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
        .expect("-17 MC, TemperatureStep, TerraformRating")
    // The remaining 14 MC covers the project but not Reds' additional 3 MC.
    kim.count("MC") shouldBe 14
    shouldThrow<NotNowException> {
      kim.completeOperation {
        doTasks("UseStandardProject<AsteroidProject>")
        autoExecNow(EAGER)
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

    kim.completeOperation {
          doTasks("UseTurmoilPolicyAction<Action2>")
          autoExecNow(EAGER)
        }
        .expect("-10 MC, PROD[Heat, Energy]")
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    admin.count("KelvinistsPolicy") shouldBe 0
  }

  private fun electGovernment(
      party: String,
      firstRole: String = "PartyLeader",
      secondRole: String = "PartyFollower",
      neutralRole: String = "PartyFollower",
  ) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("$firstRole<$party> FROM LobbyDelegate")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("$secondRole<$party>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<$party>")
    }
    kim.completeOperation {
      doTasks("PaidLobbyAction")
      doTask("PartyFollower<$party>")
    }
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    admin.doTask("PartyLeader<Greens, Neutral>")
    admin.doTasks("ExploreFirstDirective", "$neutralRole<Reds, Neutral>")
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    admin.count("Ruling<$party>") shouldBe 1
  }
}
