package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class CorridorsOfPowerTest : TfmSandboxTest() {
  @Test
  internal fun `Banning an opponents follower draws for the automatically promoted leader`() {
    newTestGame("TurmoilExpansion, CorridorsOfPower")
    kim.exMachina("CorridorsOfPower, Chairman<Kim FROM Neutral>")
    stan.exMachina("PartyLeader<Scientists>, PartyFollower<Scientists>")
    kim.exMachina("2 PartyFollower<Scientists>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<BannedDelegate>>",
              "BannedDelegate FROM ProjectCard",
              "-PartyFollower<Scientists, Stan>",
              "ProjectCard",
              "PlayedEvent<Class<BannedDelegate>> FROM BannedDelegate",
          )
        }
        .expect("0 ProjectCard, PartyLeader<Scientists>, -PartyLeader<Scientists, Stan>")
  }

  @Test
  internal fun `Demotion at seven delegates neither consumes the lobby nor draws a card`() {
    newTestGame("TurmoilExpansion, CorridorsOfPower")
    kim.exMachina(
        "CorridorsOfPower, PartyLeader<Scientists>, PartyLeader<Unity>, 4 PartyFollower<Unity>"
    )
    stan.inTurn { doTasks("LobbyAction", "PartyFollower<Scientists> FROM LobbyDelegate") }

    stan
        .inTurn { doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyLeader<Scientists>") }
        .expect(
            "-PartyLeader<Scientists, Kim>, PartyFollower<Scientists, Kim>, 0 LobbyDelegate<Kim>, 0 ProjectCard<Kim>"
        )
    kim.count("Delegate") shouldBe 7
    kim.count("LobbyDelegate") shouldBe 1
  }

  @Test
  internal fun `Draws for gaining leadership rather than for every delegate placed`() {
    newTestGame("PreludeExpansion, CorridorsOfPower, TurmoilExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<CorridorsOfPower>>",
          "CorridorsOfPower FROM PreludeCard",
          "TerraformRating",
          "4 MC",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("LobbyAction")
          doTask("PartyLeader<Scientists> FROM LobbyDelegate")
          doTask("ProjectCard<Kim, Hand>")
        }
        .expect("ProjectCard")
    kim.inTurn {
          doTasks("PaidLobbyAction", "5 Pay<> FROM MC", "PartyFollower<Scientists>")
        }
        .expect("0 ProjectCard")
  }
}
