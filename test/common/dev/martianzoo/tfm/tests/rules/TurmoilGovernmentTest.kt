package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilGovernmentTest : TfmGameplayTest() {
  @Test
  internal fun `Government replaces the chairman returns its delegates and refills both lobbies`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<MarsFirst>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") }
    }
    stan.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
      playProject(VoteOfNoConfidence, 5)
    }
    kim.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") } }
    stan.turn { stdAction("LobbyAction", 2) { doTask("PartyDelegate<MarsFirst>") } }
    kim.pass()
    stan.pass()
    kim.wgt("VenusStep")
    kim.count("Chairman") shouldBe 1
    stan.count("Chairman") shouldBe 0
    kim.count("TerraformRating") shouldBe 20
    stan.count("TerraformRating") shouldBe 20

    // Kim gains the chairman TR after revision; Stan only receives revision.
    kim.count("PartyDelegate<MarsFirst>") shouldBe 0
    stan.count("PartyDelegate<MarsFirst>") shouldBe 0
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 0
    stan.count("PartyDelegate<Scientists>") shouldBe 1
    admin.count("Ruling<MarsFirst>") shouldBe 1
    admin.count("Dominant<Reds>") shouldBe 1
    admin.doTask("ExploreFirstDirective")
    players.forEach { it.buyCards(0) }
    stan.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }.expect("0 MC, Delegate")
    }
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Unity>") }.expect("0 MC, Delegate")
  }

  @Test
  internal fun `Becoming chairman triggers Terraforming Deal`() {
    newTestGame(
        addOptions = "TurmoilExpansion, PreludeExpansion, TerraformingDeal",
        playerCount = 2,
    )
    kim.turn {
      playPrelude(TerraformingDeal)
      playPrelude(Donation)
    }
    stan.turn {
      playPrelude(AlliedBank)
      playPrelude(PowerGeneration)
    }
    lobbyForElection("Scientists")
    assertRulingBonus(2, 0)
    kim.count("Chairman") shouldBe 1
    kim.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `After an election a dominance tie favors the next party clockwise`() {
    prepareElection("MarsFirst")
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Kelvinists>") }
    finishElection()
    admin.count("Dominant<Kelvinists>") shouldBe 1
  }

  @Test
  internal fun `Dominance skips a smaller party before breaking the next tie`() {
    prepareElection("MarsFirst")
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") }
    repeat(2) { kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Unity>") } }
    finishElection()
    admin.count("Dominant<Reds>") shouldBe 1
  }

  @Test
  internal fun `Dominance wraps clockwise from Scientists to Mars First`() {
    prepareElection("Scientists")
    finishElection()
    admin.count("Dominant<MarsFirst>") shouldBe 1
  }

  @Test
  internal fun `Mars First pays for building tags`() {
    prepareElection("MarsFirst")
    kim.playProject(Mine, 4)
    kim.playProject(PowerPlant, 4)
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Scientists counts both science tags on Research`() {
    prepareElection("Scientists")
    kim.playProject(Research, 11)
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Unity combines Earth and Jovian tags`() {
    prepareElection("Unity")
    kim.playProject(EarthOffice, 1)
    kim.playProject(VestaShipyard, 15)
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Greens combines plant microbe and animal tags`() {
    prepareElection("Greens")
    kim.playProject(AdaptedLichen, 9)
    kim.playProject(Tardigrades, 4)
    kim.playProject(Pets, 10)
    assertRulingBonus(3, 0)
  }

  @Test
  internal fun `Kelvinists pays both players according to their heat production`() {
    prepareElection("Kelvinists")
    kim.playProject(MicroMills, 3)
    assertRulingBonus(2, 1)
  }

  @Test
  internal fun `Reds raises all tied lowest players before awarding the chairman TR`() {
    prepareElection("Reds")
    finishElection()
    // Both lose one in revision and regain one from Reds; Kim also becomes chairman.
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Reds raises only the lowest player`() {
    prepareElection("Reds")
    kim.stdProject("AsteroidProject")
    finishElection()
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test internal fun `Solo Reds bonus applies at twenty TR after revision`() = soloReds(false)

  @Test
  internal fun `Solo Reds bonus does not apply above twenty TR after revision`() = soloReds(true)

  private fun soloReds(aboveThreshold: Boolean) {
    newTestGame(addOptions = "TurmoilExpansion, PreludeExpansion", playerCount = 1)
    kim.turn {
      playPrelude(HugeAsteroid)
      playPrelude(SmeltingPlant)
    }
    kim.stdAction("LobbyAction") { doTask("PartyDelegate<Reds>") }
    repeat(2) { kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Reds>") } }
    kim.playProject(BribedCommittee, 7)
    if (aboveThreshold) kim.stdProject("AsteroidProject")
    kim.count("TerraformRating") shouldBe if (aboveThreshold) 22 else 21
    kim.pass()
    kim.wgt("VenusStep")
    // At 20 after revision Reds adds a TR; at 21 it does not. Both gain the chairman TR.
    kim.count("TerraformRating") shouldBe 22
  }

  private fun prepareElection(party: String) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    lobbyForElection(party)
  }

  private fun lobbyForElection(party: String) {
    kim.turn {
      stdAction("LobbyAction") { doTask("PartyDelegate<$party>") }
      stdAction("LobbyAction", 2) { doTask("PartyDelegate<$party>") }
    }
    stan.pass()
    kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<$party>") }
  }

  private fun finishElection() {
    kim.pass()
    kim.wgt("VenusStep")
  }

  private fun assertRulingBonus(kimBonus: Int, stanBonus: Int) {
    kim.pass()
    val kimMoney = kim.count("MC")
    val stanMoney = stan.count("MC")
    kim.wgt("VenusStep")
    // Government forms in a subsequent workflow operation, outside wgt's TaskResult.
    kim.count("MC") shouldBe kimMoney + kimBonus
    stan.count("MC") shouldBe stanMoney + stanBonus
  }

  internal class Sandbox : dev.martianzoo.tfm.tests.TfmSandboxTest() {
    @Test
    internal fun `A party requirement accepts its government or two of the player's delegates`() {
      newTestGame(addOptions = "TurmoilExpansion")
      kim.playProject(GmoContract, 3).expect("$GmoContract")
      shouldThrow<NotNowException> { kim.playProject(SupportedResearch, 3) }
      kim.stdAction("LobbyAction") { doTask("PartyDelegate<Scientists>") }
      shouldThrow<NotNowException> { kim.playProject(SupportedResearch, 3) }
      kim.stdAction("LobbyAction", 2) { doTask("PartyDelegate<Scientists>") }
      shouldThrow<NotNowException> { stan.playProject(SupportedResearch, 3) }
      kim.playProject(SupportedResearch, 3).expect("ProjectCard, $SupportedResearch")
    }
  }
}
