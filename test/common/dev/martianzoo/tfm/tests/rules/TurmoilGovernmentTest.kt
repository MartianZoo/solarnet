package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilGovernmentTest : TfmGameplayTest() {
  @Test
  internal fun `Government replaces the chairman returns its delegates and refills both lobbies`() {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyDelegate<MarsFirst>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<MarsFirst>")
    }
    stan.completeOperation {
      doTask("LobbyAction")
      doTask("PartyDelegate<Scientists>")
    }
    stan.completeOperation {
      doTasks("PlayProject<Class<$VoteOfNoConfidence>>")
      autoExecNow(EAGER)
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<MarsFirst>")
    }
    kim.doTask("0 StandardAction") // Decline the second action.
    stan.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<MarsFirst>")
    }
    stan.doTask("0 StandardAction") // Decline the second action.
    kim.doTask("Pass")
    stan.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

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
    players.forEach { it.doTask("-4 ProjectCard<Selecting>") }
    stan
        .completeOperation {
          doTask("LobbyAction")
          doTask("PartyDelegate<Unity>")
        }
        .expect("0 MC, Delegate")

    stan.doTask("0 StandardAction") // Decline the second action.
    kim.completeOperation {
          doTask("LobbyAction")
          doTask("PartyDelegate<Unity>")
        }
        .expect("0 MC, Delegate")
  }

  @Test
  internal fun `Becoming chairman triggers Terraforming Deal`() {
    newTestGame(
        addOptions = "TurmoilExpansion, PreludeExpansion, TerraformingDeal",
        playerCount = 2,
    )
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$TerraformingDeal>>")
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
    stan.doTask("PlayCard<Class<PreludeCard>, Class<$AlliedBank>>")
    stan.autoExecNow(EAGER)

    stan.doTask("PlayCard<Class<PreludeCard>, Class<$PowerGeneration>>")
    lobbyForElection("Scientists")
    assertRulingBonus(2, 0)
    kim.count("Chairman") shouldBe 1
    kim.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `After an election a dominance tie favors the next party clockwise`() {
    prepareElection("MarsFirst")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Kelvinists>")
    }
    finishElection()
    admin.count("Dominant<Kelvinists>") shouldBe 1
  }

  @Test
  internal fun `Dominance skips a smaller party before breaking the next tie`() {
    prepareElection("MarsFirst")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Reds>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Unity>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Unity>")
    }
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
    kim.doTasks("PlayProject<Class<$Mine>>")
    kim.doTasks("PlayProject<Class<$PowerPlant>>")
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Scientists counts both science tags on Research`() {
    prepareElection("Scientists")
    kim.completeOperation {
      doTasks("PlayProject<Class<$Research>>")
      autoExecNow(EAGER)
    }
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Unity combines Earth and Jovian tags`() {
    prepareElection("Unity")
    kim.doTasks("PlayProject<Class<$EarthOffice>>")
    kim.doTasks("PlayProject<Class<$VestaShipyard>>")
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Greens combines plant microbe and animal tags`() {
    prepareElection("Greens")
    kim.doTasks("PlayProject<Class<$AdaptedLichen>>")
    kim.doTasks("PlayProject<Class<$Tardigrades>>")
    kim.doTasks("PlayProject<Class<$Pets>>")
    assertRulingBonus(3, 0)
  }

  @Test
  internal fun `Kelvinists pays both players according to their heat production`() {
    prepareElection("Kelvinists")
    kim.doTasks("PlayProject<Class<$MicroMills>>")
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
    kim.completeOperation {
      doTasks("UseStandardProject<AsteroidProject>")
      autoExecNow(EAGER)
    }
    finishElection()
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test internal fun `Solo Reds bonus applies at twenty TR after revision`() = soloReds(false)

  @Test
  internal fun `Solo Reds bonus does not apply above twenty TR after revision`() = soloReds(true)

  private fun soloReds(aboveThreshold: Boolean) {
    newTestGame(addOptions = "TurmoilExpansion, PreludeExpansion", playerCount = 1)
    kim.doTask("PlayCard<Class<PreludeCard>, Class<$HugeAsteroid>>")
    kim.autoExecNow(EAGER)

    kim.doTask("PlayCard<Class<PreludeCard>, Class<$SmeltingPlant>>")
    kim.autoExecNow(EAGER)

    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyDelegate<Reds>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Reds>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<Reds>")
    }
    kim.doTasks("PlayProject<Class<$BribedCommittee>>")
    if (aboveThreshold)
        kim.completeOperation {
          doTasks("UseStandardProject<AsteroidProject>")
          autoExecNow(EAGER)
        }
    kim.count("TerraformRating") shouldBe if (aboveThreshold) 22 else 21
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }

    // At 20 after revision Reds adds a TR; at 21 it does not. Both gain the chairman TR.
    kim.count("TerraformRating") shouldBe 22
  }

  private fun prepareElection(party: String) {
    newTestGame(addOptions = "TurmoilExpansion", playerCount = 2)
    lobbyForElection(party)
  }

  private fun lobbyForElection(party: String) {
    kim.completeOperation {
      doTask("LobbyAction")
      doTask("PartyDelegate<$party>")
    }
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<$party>")
    }
    stan.doTask("Pass")
    kim.completeOperation {
      doTasks("LobbyAction<Action2>")
      doTask("PartyDelegate<$party>")
    }
  }

  private fun finishElection() {
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.autoExecNow(EAGER) }
  }

  private fun assertRulingBonus(kimBonus: Int, stanBonus: Int) {
    kim.doTask("Pass")
    players.forEach { it.autoExecNow(EAGER) }
    // Government forms in follow-up work after the World Government choice.
    expectChanges("$kimBonus MC<Kim>, $stanBonus MC<Stan>") {
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }
    }
  }

  internal class Sandbox : dev.martianzoo.tfm.tests.TfmSandboxTest() {
    @Test
    internal fun `A party requirement accepts its government or two of the player's delegates`() {
      newTestGame(addOptions = "TurmoilExpansion")
      kim.inTurn {
        doTasks(
            "PlayProject<Class<GmoContract>>",
            "3 Pay<> FROM MC",
            "GmoContract FROM ProjectCard",
            "2 MC",
        )
      }
      shouldThrow<RequirementException> {
        kim.inTurn {
          doTasks("PlayProject<Class<SupportedResearch>>", "3 Pay<> FROM MC")
          selectTask("MAX 0 Barrier: SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn { doTasks("LobbyAction<Action1>", "PartyDelegate<Scientists>") }
      shouldThrow<RequirementException> {
        kim.inTurn {
          doTasks("PlayProject<Class<SupportedResearch>>", "3 Pay<> FROM MC")
          selectTask("MAX 0 Barrier: SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn {
        doTasks("LobbyAction<Action2>", "5 Pay<> FROM MC", "PartyDelegate<Scientists>")
      }
      shouldThrow<RequirementException> {
        stan.inTurn {
          doTasks("PlayProject<Class<SupportedResearch>>", "3 Pay<> FROM MC")
          selectTask("MAX 0 Barrier: SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn {
        doTasks(
            "PlayProject<Class<SupportedResearch>>",
            "3 Pay<> FROM MC",
            "SupportedResearch FROM ProjectCard",
            "2 ProjectCard",
        )
      }
    }
  }
}
