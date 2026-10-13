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
          doTasks("PlayProject<Class<$SpaceElevator>>", "-7 Steel", "-5 Titanium", "Ok")
          doTasks("SpaceElevator FROM ProjectCard", "PROD[Titanium]")
        }
        .expect("0 MC")
  }

  // The resolved late-Merger ruling does not settle first-action timing during Prelude.
  // BGG Valley Trust/Merger first-action discussion:
  // https://boardgamegeek.com/thread/2874012/article/40859020#40859020
  @Test
  internal fun `Valley Trust Merger Tharsis currently places the city in the first required action`() {
    newTestGame(addOptions = "PreludeExpansion, ValleyTrust", kimCorporation = ValleyTrust)
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4! CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>")
      doTask("TharsisRepublic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTasks("40 MC", "-42 MC")
      selectTask("TharsisRepublic_RequiredAction!")
    }
    advanceUntilActionPhase()

    kim.inTurn {
      doTask("RequiredActionsSignal")
      selectTask("3! PreludeCard<Kim, Selecting>")
      doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
      doTask("Donation<Kim> FROM PreludeCard<Kim, Selecting>!")
      doTasks("21 MC", "CityTile<Tharsis_3_3>", "PROD[MC]", "3 MC")
    }
    kim.count("CityTile<Tharsis_3_3>") shouldBe 1
    kim.count("RequiredAction") shouldBe 0
  }

  // BGG Head Start first-action ruling:
  // https://boardgamegeek.com/thread/2993276/article/41447529#41447529
  @Test
  internal fun `Head Start Board Merger Tharsis currently uses the next granted action for its city`() {
    newTestGame(addOptions = "PreludeExpansion, fakes:cards, BoardOfDirectors")
    kim.exMachina("$BoardOfDirectors, Director<$BoardOfDirectors>")
    kim.setToExMachina(54, "MC")

    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<FakeHeadStart>>",
          "FakeHeadStart FROM PreludeCard",
          "2 Steel",
      )
      selectTask("2! MC / ProjectCard<Kim, Hand>")
      doTask("UseCardAction<BoardOfDirectors, Action1>")
      selectTask("PreludeCard<Kim, Hand>!")
      doTask("-12 MC")
      selectTask("-Director<BoardOfDirectors>!")
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4! CorporationCard<Kim, Selecting>")
      doTask("-42 MC")
      doTask("PlayCard<Class<CorporationCard>, Class<TharsisRepublic>>")
      doTask("TharsisRepublic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTask("40 MC")
      selectTask("TharsisRepublic_RequiredAction!")
      selectTask(
          "(MAX 0 RequiredAction: StandardAction!) OR (RequiredAction: RequiredActionsSignal!)"
      )
      doTasks("CityTile<Tharsis_3_3>", "PROD[MC]", "3 MC")
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
      kim.inTurn {
        selectOnlyTask()
        doTask("RequiredActionsSignal")
        selectTask("Colony<Kim, ColonyTile(HAS MAX 0 Colony)>!")
        doTask("Colony<Luna>")
      }
    }
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  // BGG impossible Poseidon first-action discussion:
  // https://boardgamegeek.com/thread/3341272/article/44630207#44630207
  @Test
  internal fun `Merger Poseidon currently remains acquired when its later first colony is impossible`() {
    newTestGame(addOptions = "PreludeExpansion, Luna, Ceres, Triton, Ganymede, Callisto")
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4! CorporationCard<Kim, Selecting>")
      doTask("-42 MC")
      doTask("PlayCard<Class<CorporationCard>, Class<Poseidon>>")
      selectOnlyTask()
      doTask("45 MC")
      selectOnlyTask()
    }
    val moneyAfterMerger = kim.count("MC")
    fillSelectedColonySlots()
    advanceUntilActionPhase()

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("RequiredActionsSignal")
        selectTask("Colony<Kim, ColonyTile(HAS MAX 0 Colony)>!")
        doTask("Colony<Luna>")
      }
    }
    kim.count("$Poseidon") shouldBe 1
    kim.count("$Merger") shouldBe 1
    kim.count("MC") shouldBe moneyAfterMerger
    kim.count("RequiredAction") shouldBe 1
    kim.count("Colony") shouldBe 0
  }

  private fun fillSelectedColonySlots() {
    fillColonySlots("Luna")
    fillColonySlots("Ceres")
    fillColonySlots("Triton")
    fillColonySlots("Ganymede")
    fillColonySlots("Callisto")
  }

  private fun fillColonySlots(track: String) {
    // Keep separate placements: a batched gain of three colonies overshoots the track.
    stan.setToExMachina(1, "Colony<$track>")
    stan.setToExMachina(2, "Colony<$track>")
    stan.setToExMachina(3, "Colony<$track>")
  }
}
