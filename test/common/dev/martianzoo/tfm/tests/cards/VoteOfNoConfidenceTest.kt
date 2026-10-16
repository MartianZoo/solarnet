package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class VoteOfNoConfidenceTest : TfmSandboxTest() {
  @Test
  internal fun `Vote of No Confidence can appoint the final available delegate as chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Greens>, 5 PartyFollower<Greens>")
    kim.inTurn {
          doTasks(
              "PlayProject<Class<VoteOfNoConfidence>>",
              "5 Pay<> FROM MC",
              "VoteOfNoConfidence FROM ProjectCard",
          )
          // Hand the chairmanship transfer to Admin, which updates the political state.
          selectTask("Chairman<Kim FROM Neutral>!")
          doTask("TerraformRating")
        }
        .expect("-LobbyDelegate")
  }

  @Test
  internal fun `Vote of No Confidence cannot be played without an available delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Greens>, 6 PartyFollower<Greens>")

    shouldThrow<DeadEndException> {
      kim.inTurn {
        doTask("PlayProject<Class<VoteOfNoConfidence>>")
        doTasks("5 Pay<> FROM MC", "VoteOfNoConfidence FROM ProjectCard")
        doTask("TerraformRating")
        // Hand the chairmanship transfer to Admin, which updates the political state.
        selectTask("Chairman<Kim FROM Neutral>!")
      }
    }

    admin.count("Chairman<Neutral>") shouldBe 1
    kim.count("Chairman") shouldBe 0
  }

  @Test
  internal fun `Vote of No Confidence cannot replace a non-neutral chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyLeader<Greens>, 1 PartyFollower<Greens>")
    kim.exMachina("Chairman<Stan FROM Neutral>")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("PlayProject<Class<VoteOfNoConfidence>>")
        doTasks("5 Pay<> FROM MC", "VoteOfNoConfidence FROM ProjectCard")
        doTask("TerraformRating")
        // Hand the chairmanship transfer to Admin, which updates the political state.
        selectTask("Chairman<Kim FROM Neutral>!")
      }
    }

    kim.count("MC") shouldBe 42
    kim.count("Chairman") shouldBe 0
    stan.count("Chairman") shouldBe 1
  }
}
