package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.CardTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

/** Current behavior for questions whose rule target is not settled. */
internal class UnknownRulesTest : CardTest() {
  // BGG overpayment discussion (no designer ruling):
  // https://boardgamegeek.com/thread/3443958/article/45511890#45511890
  @Test
  internal fun `Mixed-metal payment currently accepts seven steel and five titanium for Space Elevator`() {
    newGame()
    admin.phase("Action")
    p1.runOperation("10 Steel, 10 Titanium, ProjectCard")

    p1.inTurn {
      doTask("UseAction<PlayCardFromHandAction, Action1>")
      doTask("PlayCard<Class<ProjectCard>, Class<$SpaceElevator>, Hand>")
      doTask("7 Pay<Class<Steel>> FROM Steel")
      doTask("5 Pay<Class<Titanium>> FROM Titanium")
      doTask("Ok")
    }

    p1.assertCounts(3 to "Steel", 5 to "Titanium", 0 to "ProjectCard", 1 to "$SpaceElevator")
  }

  // The resolved late-Merger ruling does not settle first-action timing during Prelude.
  // BGG Valley Trust/Merger first-action discussion:
  // https://boardgamegeek.com/thread/2874012/article/40859020#40859020
  @Test
  internal fun `Valley Trust Merger Tharsis currently places the city in the first required action`() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(ValleyTrust, 5)
    admin.phase("Prelude")
    p1.playPrelude(Merger) { p1.playCorp(TharsisRepublic) }
    admin.phase("Action")

    p1.stdAction("DoRequiredActionsAction") {
      p1.playPrelude(Donation) { placeTile(3, 3) }
    }
    p1.count("CityTile<Tharsis_3_3>") shouldBe 1
    p1.count("RequiredAction") shouldBe 0
  }

  // BGG Head Start first-action ruling:
  // https://boardgamegeek.com/thread/2993276/article/41447529#41447529
  @Test
  internal fun `Head Start Board Merger Tharsis currently uses the next granted action for its city`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle)
    p1.runOperation("$BoardOfDirectors, 54 MC")
    admin.phase("Prelude")
    p1.runOperation("2 PreludeCard")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("UseActionOnCardAction", payment = {}) {
          doTask("ActionUsedMarker<$BoardOfDirectors>")
          doTask("UseAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          playPrelude(Merger) { playCorp(TharsisRepublic) }
        }
        useStdAction("DoRequiredActionsAction", payment = {}) {
          placeTile(3, 3)
        }
      }
    }

    p1.count("CityTile<Tharsis_3_3>") shouldBe 1
    p1.count("RequiredAction") shouldBe 0
  }

  // BGG impossible Poseidon first-action discussion:
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Original Poseidon currently keeps an impossible first colony action pending`() {
    newGame(ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, Poseidon)
    fillSelectedColonySlots()
    admin.phase("Action")

    shouldThrow<LimitsException> { p1.stdAction("DoRequiredActionsAction") }
    p1.count("RequiredAction") shouldBe 1
    p1.count("Colony") shouldBe 0
  }

  // BGG impossible Poseidon first-action discussion:
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Merger Poseidon currently remains acquired when its later first colony is impossible`() {
    newGame(PreludeExpansion, PromoCardPack, ColoniesExpansion, colonyTiles = testColonyTiles(2))
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")
    p1.playPrelude(Merger) { p1.playCorp(Poseidon) }
    val moneyAfterMerger = p1.count("MC")
    fillSelectedColonySlots()
    admin.phase("Action")

    shouldThrow<LimitsException> { p1.stdAction("DoRequiredActionsAction") }
    p1.count("$Poseidon") shouldBe 1
    p1.count("$Merger") shouldBe 1
    p1.count("MC") shouldBe moneyAfterMerger
    p1.count("RequiredAction") shouldBe 1
    p1.count("Colony") shouldBe 0
  }

  private fun fillSelectedColonySlots() {
    val p2 = requireP2()
    listOf("Luna", "Ceres", "Triton", "Ganymede", "Callisto").forEach { track ->
      repeat(3) { p2.runOperation("Colony<$track>") }
    }
  }
}
