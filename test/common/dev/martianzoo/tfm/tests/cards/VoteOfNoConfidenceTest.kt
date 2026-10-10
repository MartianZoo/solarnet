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
    kim.exMachina("6 PartyDelegate<Greens>")
    kim.playProject(VoteOfNoConfidence, 5)
        .expect("Chairman, -Chairman<Neutral>, TerraformRating, -LobbyActionAvailable")
  }

  @Test
  internal fun `Vote of No Confidence cannot be played without an available delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("7 PartyDelegate<Greens>")

    shouldThrow<DeadEndException> { kim.playProject(VoteOfNoConfidence, 5) }

    admin.count("Chairman<Neutral>") shouldBe 1
    kim.count("Chairman") shouldBe 0
  }

  @Test
  internal fun `Vote of No Confidence cannot replace a non-neutral chairman`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<Greens>")
    kim.exMachina("Chairman<Stan> FROM Chairman<Neutral>")

    shouldThrow<LimitsException> { kim.playProject(VoteOfNoConfidence, 5) }

    kim.count("MC") shouldBe 42
    kim.count("Chairman") shouldBe 0
    stan.count("Chairman") shouldBe 1
  }
}
