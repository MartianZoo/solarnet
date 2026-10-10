package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Current behavior for questions whose rule target is not settled. */
internal class UnknownRulesTest : TfmSandboxTest() {
  // BGG overpayment discussion (no designer ruling):
  // https://boardgamegeek.com/thread/3443958/article/45511890#45511890
  @Test
  internal fun `Mixed-metal payment currently accepts seven steel and five titanium for Space Elevator`() {
    newTestGame()
    kim.exMachina("10 Steel, 10 Titanium")
    kim.setToExMachina(1, "ProjectCard")

    kim.inTurn {
          doTask("UseAction<PlayCardFromHandAction, Action1>")
          doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>, Hand>")
          doTask("-7 Steel")
          doTask("-5 Titanium")
          doTask("Ok")
        }
        .expect("-7 Steel, -5 Titanium, -ProjectCard, $SpaceElevator, 0 MC")
  }

  // The resolved late-Merger ruling does not settle first-action timing during Prelude.
  // BGG Valley Trust/Merger first-action discussion:
  // https://boardgamegeek.com/thread/2874012/article/40859020#40859020
  @Test
  internal fun `Valley Trust Merger Tharsis currently places the city in the first required action`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = ValleyTrust)
    kim.playPrelude(Merger) { kim.playCorp(TharsisRepublic) }
    startActionPhase()

    kim.stdAction("DoRequiredActionsAction") {
      kim.playPrelude(Donation) { placeTile(3, 3) }
    }
    kim.count("CityTile<Tharsis_3_3>") shouldBe 1
    kim.count("RequiredAction") shouldBe 0
  }

  // BGG Head Start first-action ruling:
  // https://boardgamegeek.com/thread/2993276/article/41447529#41447529
  @Test
  internal fun `Head Start Board Merger Tharsis currently uses the next granted action for its city`() {
    newTestGame(addOptions = "PreludeExpansion, FakeStuffBundle, BoardOfDirectors")
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>")
    kim.setToExMachina(54, "MC")

    kim.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("UseActionOnCardAction", payment = {}) {
          doTask("UseAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          playPrelude(Merger) { playCorp(TharsisRepublic) }
        }
        useStdAction("DoRequiredActionsAction", payment = {}) {
          placeTile(3, 3)
        }
      }
    }

    kim.count("CityTile<Tharsis_3_3>") shouldBe 1
    kim.count("RequiredAction") shouldBe 0
  }

  // BGG impossible Poseidon first-action discussion:
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Original Poseidon currently keeps an impossible first colony action pending`() {
    newTestGame(addOptions = "Luna, Ceres, Triton, Ganymede, Callisto", kimCorporation = Poseidon)
    fillSelectedColonySlots()

    shouldThrow<LimitsException> {
      kim.stdAction("DoRequiredActionsAction") { doTask("Colony<Luna>") }
    }
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  // BGG impossible Poseidon first-action discussion:
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Merger Poseidon currently remains acquired when its later first colony is impossible`() {
    newTestGame(addOptions = "PreludeExpansion, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.playPrelude(Merger) { kim.playCorp(Poseidon) }
    val moneyAfterMerger = kim.count("MC")
    fillSelectedColonySlots()
    startActionPhase()

    shouldThrow<LimitsException> {
      kim.stdAction("DoRequiredActionsAction") { doTask("Colony<Luna>") }
    }
    kim.count("$Poseidon") shouldBe 1
    kim.count("$Merger") shouldBe 1
    kim.count("MC") shouldBe moneyAfterMerger
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  private fun fillSelectedColonySlots() {
    listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").forEach { track ->
      repeat(3) { stan.exMachina("Colony<$track>") }
    }
  }
}
