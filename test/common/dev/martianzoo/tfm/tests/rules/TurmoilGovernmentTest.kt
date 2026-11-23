package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.correct
import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.testsupport.count
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class TurmoilGovernmentTest : TfmGameplayTest() {
  @Test
  internal fun `Government replaces the chairman returns its delegates and refills both lobbies`() {
    newTestGame(
        "TurmoilExpansion, -QuickStartVariant",
        2,
        firstGlobalEvents =
            listOf("$DemocraticReform", "$MinimalImpactPolicy", "$ExploreFirstDirective"),
    )
    // Prepare the outgoing chairman and delegates; the election itself uses the real Solar phase.
    kim.exMachina(
        "-LobbyDelegate, PartyLeader<MarsFirst> FROM PartyLeader<MarsFirst, Neutral>, 2 PartyFollower<MarsFirst>"
    )
    stan.exMachina(
        "-LobbyDelegate, Chairman<Stan> FROM Chairman<Neutral>, TerraformRating, PartyLeader<Scientists>, PartyFollower<MarsFirst>"
    )
    agents.correct("PartyFollower<MarsFirst, Neutral>")
    kim.doTask("Pass")
    stan.doTask("Pass")
    kim.doTask("20 MC")
    stan.doTask("21 MC")
    kim.doTask("VenusStep! BY Admin")

    kim.count("Chairman") shouldBe 1
    stan.count("Chairman") shouldBe 0
    kim.count("TerraformRating") shouldBe 20
    stan.count("TerraformRating") shouldBe 20
    kim.count("PartyDelegate<MarsFirst>") shouldBe 0
    stan.count("PartyDelegate<MarsFirst>") shouldBe 0
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 0
    stan.count("PartyDelegate<Scientists>") shouldBe 1
    admin.count("Ruling<MarsFirst>") shouldBe 1
    admin.count("Dominant<Reds>") shouldBe 1
    admin.count("Distant<$ExploreFirstDirective>") shouldBe 1
    (admin.count("PartyFollower<Reds, Neutral>") > 0) shouldBe true
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    stan.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Unity> FROM LobbyDelegate")
          .expect("0 MC, 0 Delegate")
    }
    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "PartyFollower<Unity> FROM LobbyDelegate")
          .expect("0 MC, 0 Delegate")
    }
  }

  @Test
  internal fun `After an election a dominance tie favors the next party clockwise`() {
    prepareElection("MarsFirst", "PartyFollower", "PartyLeader")
    kim.exMachina("PartyLeader<Kelvinists>")
    finishElection()
    admin.count("Dominant<Kelvinists>") shouldBe 1
  }

  @Test
  internal fun `Dominance skips a smaller party before breaking the next tie`() {
    prepareElection("MarsFirst", "PartyFollower", "PartyLeader")
    kim.exMachina("PartyFollower<Reds>, PartyLeader<Unity>, PartyFollower<Unity>")
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
    prepareElection("MarsFirst", "PartyFollower", "PartyLeader")
    kim.exMachina("$Mine, $PowerPlant")
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Scientists counts both science tags on Research`() {
    prepareElection("Scientists")
    kim.exMachina("$Research")
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Unity combines Earth and Jovian tags`() {
    prepareElection("Unity")
    kim.exMachina("$EarthOffice, $VestaShipyard")
    assertRulingBonus(2, 0)
  }

  @Test
  internal fun `Greens combines plant microbe and animal tags`() {
    prepareElection("Greens")
    kim.exMachina("$AdaptedLichen, $Tardigrades, $Pets")
    assertRulingBonus(3, 0)
  }

  @Test
  internal fun `Kelvinists pays both players according to their heat production`() {
    prepareElection("Kelvinists")
    kim.exMachina("PROD[2 Heat]")
    stan.exMachina("PROD[Heat]")
    kim.doTask("Pass")
    kim.doTasks("20 MC", "2 Heat")
    stan.doTasks("20 MC", "Heat")
    expectChanges("2 MC<Kim>, MC<Stan>") { kim.doTask("VenusStep! BY Admin") }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  @Test
  internal fun `Reds raises all tied lowest players before awarding the chairman TR`() {
    prepareElection("Reds", "PartyFollower", "PartyLeader")
    finishElection()
    // Both lose a TR in revision and regain it from Reds; Kim also becomes chairman.
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test
  internal fun `Reds raises only the lowest player`() {
    prepareElection("Reds", "PartyFollower", "PartyLeader")
    kim.exMachina("TerraformRating")
    kim.doTask("Pass")
    kim.doTask("21 MC")
    stan.doTask("20 MC")
    kim.doTask("VenusStep! BY Admin")
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    kim.count("TerraformRating") shouldBe 21
    stan.count("TerraformRating") shouldBe 20
  }

  @Test internal fun `Solo Reds bonus applies at twenty TR after revision`() = soloReds(false)

  @Test
  internal fun `Solo Reds bonus does not apply above twenty TR after revision`() = soloReds(true)

  private fun soloReds(aboveThreshold: Boolean) {
    newTestGame("TurmoilExpansion, -QuickStartVariant, -ColoniesExpansion", 1)
    // Prepare 21 or 22 TR before revision, without manufacturing a history of terraforming.
    kim.exMachina("${if (aboveThreshold) 8 else 7} TerraformRating")
    kim.exMachina("PartyLeader<Reds> FROM PartyLeader<Reds, Neutral>, 2 PartyFollower<Reds>")
    kim.doTask("Pass")
    kim.doTasks(
        "${if (aboveThreshold) 22 else 21} MC",
        "VenusStep! BY Admin",
    )
    // At 20 after revision Reds adds a TR; at 21 it does not. Both gain the chairman TR.
    kim.count("TerraformRating") shouldBe 22
    kim.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>")
    admin.count("ActionPhase") shouldBe 1
  }

  private fun prepareElection(
      party: String,
      firstRole: String = "PartyLeader",
      secondRole: String = "PartyFollower",
  ) {
    newTestGame("TurmoilExpansion, -QuickStartVariant", 2)
    kim.turn {
      doTasks("UseAction<LobbyAction, Action1>", "$firstRole<$party> FROM LobbyDelegate")
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "$secondRole<$party>")
    }
    stan.doTask("Pass")
    kim.turn {
      doTasks("UseAction<PaidLobbyAction, Action1>", "5 Pay<> FROM MC", "PartyFollower<$party>")
    }
  }

  private fun finishElection() {
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    kim.doTask("VenusStep! BY Admin")
    admin.count("PartyLeader<Greens, Neutral>") shouldBe 1
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  private fun assertRulingBonus(kimBonus: Int, stanBonus: Int) {
    kim.doTask("Pass")
    players.forEach { it.doTask("20 MC") }
    expectChanges("$kimBonus MC<Kim>, $stanBonus MC<Stan>") {
      kim.doTask("VenusStep! BY Admin")
    }
    players.forEach { it.doTasks("4 ProjectCard<Selecting>", "-4 ProjectCard<Selecting>") }
    admin.count("ActionPhase") shouldBe 1
  }

  internal class Sandbox : dev.martianzoo.tfm.tests.TfmSandboxTest() {
    @Test
    internal fun `A party requirement accepts its government or two of the player's delegates`() {
      newTestGame(addOptions = "TurmoilExpansion")
      kim.inTurn {
        doTasks(
            *playCardTasks("$GmoContract"),
            "3 Pay<> FROM MC",
            "$GmoContract FROM ProjectCard",
            "2 MC",
        )
      }
      shouldThrow<RequirementException> {
        kim.inTurn {
          doTasks(*playCardTasks("$SupportedResearch"), "3 Pay<> FROM MC")
          // Select the guarded transfer to check the party requirement before the card enters play.
          selectTask("MAX 0 Barrier: $SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn {
        doTasks("UseAction<LobbyAction, Action1>", "PartyLeader<Scientists> FROM LobbyDelegate")
      }
      shouldThrow<RequirementException> {
        kim.inTurn {
          doTasks(*playCardTasks("$SupportedResearch"), "3 Pay<> FROM MC")
          // Select the guarded transfer to check the party requirement before the card enters play.
          selectTask("MAX 0 Barrier: $SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn {
        doTasks(
            "UseAction<PaidLobbyAction, Action1>",
            "5 Pay<> FROM MC",
            "PartyFollower<Scientists>",
        )
      }
      shouldThrow<RequirementException> {
        stan.inTurn {
          doTasks(*playCardTasks("$SupportedResearch"), "3 Pay<> FROM MC")
          // Select the guarded transfer to check the party requirement before the card enters play.
          selectTask("MAX 0 Barrier: $SupportedResearch FROM ProjectCard!")
        }
      }
      kim.inTurn {
        doTasks(
            *playCardTasks("$SupportedResearch"),
            "3 Pay<> FROM MC",
            "$SupportedResearch FROM ProjectCard",
            "2 ProjectCard",
        )
      }
    }
  }
}
