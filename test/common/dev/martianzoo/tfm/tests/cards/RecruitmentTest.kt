package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.DeadEndException
import dev.martianzoo.pets.api.Exceptions.NotNowException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class RecruitmentTest : TfmSandboxTest() {
  @Test
  internal fun `Recruitment cannot be played without a neutral non-leader delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")

    shouldThrow<NotNowException> {
      kim.playProject(Recruitment, 2) {
        doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
      }
    }
  }

  @Test
  internal fun `Recruitment exchanges a neutral non-leader for an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")

    kim.exMachina(
        "PartyDelegate<MarsFirst, Neutral>, " +
            "PartyDelegate<Unity, Neutral>, PartyDelegate<Unity, Neutral>"
    )
    admin.count("Dominant<MarsFirst>") shouldBe 1
    kim.playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
        }
        .expect(
            "PartyDelegate<MarsFirst>, Delegate, -Delegate<Neutral>, 0 Dominant<MarsFirst>, 0 PartyLeader<MarsFirst, Neutral>"
        )
  }

  @Test
  internal fun `Recruitment makes only the recruiting player leader after a tied challenge`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyDelegate<MarsFirst, Neutral>")
    kim.exMachina("PartyDelegate<MarsFirst>")
    stan.exMachina("PartyDelegate<MarsFirst>, PartyDelegate<MarsFirst>")

    kim.playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
        }
        .expect(
            "PartyLeader<MarsFirst>, -PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Recruitment promotes another player with the sole largest delegation`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst, Neutral>")
    kim.exMachina("PartyDelegate<MarsFirst>")
    stan.exMachina("3 PartyDelegate<MarsFirst>")

    kim.playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
        }
        .expect(
            "PartyLeader<MarsFirst, Stan>, -PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst>"
        )
  }

  @Test
  internal fun `Recruitment preserves a tied player incumbent`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyDelegate<MarsFirst, Neutral>")
    stan.exMachina("3 PartyDelegate<MarsFirst>")
    kim.exMachina("2 PartyDelegate<MarsFirst>")

    kim.playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
        }
        .expect("0 PartyLeader<MarsFirst>, 0 PartyLeader<MarsFirst, Stan>")
  }

  @Test
  internal fun `Recruitment chooses the nearest clockwise challenger when the recruiter trails`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst, Neutral>")
    kim.exMachina("PartyDelegate<MarsFirst>")
    stan.exMachina("3 PartyDelegate<MarsFirst>")
    kim.exMachina("3 PartyDelegate<MarsFirst, Rob>")

    kim.playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
        }
        .expect(
            "PartyLeader<MarsFirst, Stan>, 0 PartyLeader<MarsFirst, Rob>, " +
                "-PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst>"
        )
  }

  @Test
  internal fun `Recruitment measures clockwise order from the recruiting player`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<MarsFirst, Neutral>")
    stan.exMachina("PartyDelegate<MarsFirst>")
    kim.exMachina("3 PartyDelegate<MarsFirst>")
    kim.exMachina("3 PartyDelegate<MarsFirst, Rob>")

    stan
        .playProject(Recruitment, 2) {
          doTask("PartyDelegate<MarsFirst, Stan FROM Neutral>")
        }
        .expect(
            "PartyLeader<MarsFirst, Rob>, 0 PartyLeader<MarsFirst, Kim>, " +
                "-PartyLeader<MarsFirst, Neutral>, 0 PartyLeader<MarsFirst, Stan>"
        )
  }

  @Test
  internal fun `Recruitment cannot be played without an available owned delegate`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("PartyDelegate<MarsFirst, Neutral>")
    kim.exMachina("7 PartyDelegate<Unity>")

    shouldThrow<DeadEndException> {
      kim.playProject(Recruitment, 2) {
        doTask("PartyDelegate<MarsFirst, Kim FROM Neutral>")
      }
    }

    kim.count("Delegate") shouldBe 7
    admin.count("PartyDelegate<MarsFirst, Neutral>") shouldBe 2
  }
}
