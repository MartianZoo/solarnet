package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.NONE
import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class MergerTest : TfmSandboxTest() {
  @Test
  internal fun `Both corporations retain their required first actions`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = ValleyTrust)
    kim.playPrelude(Merger) { kim.playCorp(Celestic) }
    startActionPhase()

    kim.stdAction("DoRequiredActionsAction") { kim.playPrelude(SocietySupport) }
        .expect("2 ProjectCard, PROD[-MC, Plant, Energy, Heat], -2 RequiredAction")
  }

  @Test
  internal fun `New Partner can play Merger while selecting both card families`() {
    newTestGame(addOptions = "PreludeExpansion")

    kim.playPrelude(NewPartner) {
          kim.playPrelude(Merger) { kim.playCorp(Celestic) }
        }
        .expect("$Merger, $Celestic, 0 CorporationCard<Selecting>, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `The new corporation can pay for Merger and its own disease losses`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")

    kim.playPrelude(Merger) { kim.playCorp(PharmacyUnion) }
        .expect("4 MC, 2 Disease<$PharmacyUnion>")
  }

  @Test
  internal fun `Pharmacy Union's loss makes Board Merger Recyclon unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    kim.exMachina("$PharmacyUnion, 2 Disease<$PharmacyUnion>")
    kim.playPrelude(BoardOfDirectors)
    startActionPhase()
    kim.setToExMachina(17, "MC")

    shouldThrow<LimitsException> {
      kim.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        kim.playPrelude(Merger) {
          kim.autoExecPolicy = NONE
          kim.playCorp(Recyclon) {
            doTask("$Recyclon FROM CorporationCard<Selecting>")
            doTask("38 MC")
            doTask("-4 MC.")
            doTask("-42 MC")
          }
        }
      }
    }
    kim.count("MC") shouldBe 17
    kim.count("Disease<$PharmacyUnion>") shouldBe 2
    kim.count("Director<$BoardOfDirectors>") shouldBe 4
    kim.count("$Recyclon") shouldBe 0
    kim.count("$Merger") shouldBe 0
  }

  internal class Gameplay : TfmGameplayTest() {
    // Resolved FAQ: a corporation acquired after Preludes takes its first action immediately.
    // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
    @Ignore // Merger leaves the mandatory city placement for a later action.
    @Test
    internal fun `Resolves Tharsis first action immediately when acquired after Preludes`() {
      acquireTharsisThroughBoard { doTask("CityTile<Tharsis_3_3>") }.expect("CityTile<Tharsis_3_3>")
    }

    @Test
    internal fun `BUG - Defers Tharsis first action when acquired after Preludes`() {
      acquireTharsisThroughBoard().expect("0 CityTile")
      kim.stdAction("DoRequiredActionsAction") { placeTile(3, 3) }.expect("CityTile<Tharsis_3_3>")
    }

    private fun acquireTharsisThroughBoard(cityPlacement: OperationBlock = {}): TaskResult {
      newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors", playerCount = 2)
      kim.turn {
        playPrelude(BoardOfDirectors)
        playPrelude(Donation)
      }
      stan.turn {
        playPrelude(Supplier)
        playPrelude(MetalsCompany)
      }
      return kim.cardAction1(BoardOfDirectors) {
        doTask("-12 MC")
        kim.playPrelude(Merger) { kim.playCorp(TharsisRepublic, body = cityPlacement) }
      }
    }
  }
}
