package dev.martianzoo.tfm.tests.cards

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
    newTestGame(addOptions = "PreludeExpansion, ValleyTrust", kimCorporation = ValleyTrust)
    kim.inTurn {
      doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
      selectTask("4 CorporationCard<Kim, Selecting>!")
      selectTask("-3 CorporationCard<Kim, Selecting>!")
      doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>")
      selectTask("MAX 0 Barrier: Celestic FROM CorporationCard<Kim, Selecting>!")
      doTasks("42 MC", "-42 MC")
      selectOnlyTask()
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectTask("2 SearchForCard<Kim, ReferenceFilter<Class<Floater>>>!")
          selectTask("3 PreludeCard<Kim, Selecting>!")
          selectTask("-2 PreludeCard<Kim, Selecting>!")
          doTask("PlayCard<Class<PreludeCard>, Class<SocietySupport>>")
          selectOnlyTask()
          doTasks("PROD[-MC]", "PROD[Plant]", "PROD[Energy]", "PROD[Heat]")
        }
        .expect("2 ProjectCard, -2 RequiredAction")
  }

  @Test
  internal fun `New Partner can play Merger while selecting both card families`() {
    newTestGame(addOptions = "PreludeExpansion")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<NewPartner>>",
              "NewPartner FROM PreludeCard",
              "PROD[MC]",
          )
          selectOnlyTask()
          selectOnlyTask()
          doTask("PlayCard<Class<PreludeCard>, Class<Merger>>")
          selectOnlyTask()
          selectTask("4 CorporationCard<Kim, Selecting>!")
          doTask("-42 MC")
          selectTask("-3 CorporationCard<Kim, Selecting>!")
          doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>")
          selectTask("MAX 0 Barrier: Celestic FROM CorporationCard<Kim, Selecting>!")
          doTask("42 MC")
          selectOnlyTask()
        }
        .expect("$Merger, $Celestic, 0 CorporationCard<Selecting>, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `The new corporation can pay for Merger and its own disease losses`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          selectTask("4 CorporationCard<Kim, Selecting>!")
          selectTask("-3 CorporationCard<Kim, Selecting>!")
          doTask("PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>")
          selectTask("MAX 0 Barrier: PharmacyUnion FROM CorporationCard<Kim, Selecting>!")
          doTask("-42 MC")
          selectTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
          selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
          selectTask("-4 MC.")
          selectTask("Disease<PharmacyUnion>! OR (MAX 0 PharmacyUnion: Ok)")
          selectTask("-4 MC.")
        }
        .expect("4 MC, 2 Disease<$PharmacyUnion>")
  }

  @Test
  internal fun `Pharmacy Union's loss makes Board Merger Recyclon unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    kim.exMachina("$PharmacyUnion, 2 Disease<$PharmacyUnion>")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      selectOnlyTask()
    }
    advanceUntilActionPhase()
    kim.setToExMachina(17, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        selectTask("PreludeCard<Kim, Hand>!")
        selectOnlyTask()
        doTask("-12 MC")
        selectTask("-Director<BoardOfDirectors>!")
        selectOnlyTask()
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
        selectTask("4 CorporationCard<Kim, Selecting>!")
        selectTask("-3 CorporationCard<Kim, Selecting>!")
        selectTask("PlayCard<Kim, Class<CorporationCard>, Class<CardFront>, Selecting>!")
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
            "Recyclon FROM CorporationCard<Selecting>",
            "38 MC",
            "-4 MC.",
            "-42 MC",
        )
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
      kim.stdAction("RequiredActionsSignal") { placeTile(3, 3) }.expect("CityTile<Tharsis_3_3>")
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
