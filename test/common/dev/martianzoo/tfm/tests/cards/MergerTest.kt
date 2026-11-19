package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.OperationBlock
import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cardActionTasks
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class MergerTest : TfmSandboxTest() {
  @Test
  internal fun `Both corporations retain their required first actions`() {
    newTestGame(addOptions = "PreludeExpansion, $ValleyTrust", kimCorporation = ValleyTrust)
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$Merger>>",
          "$Merger FROM PreludeCard",
          "4 CorporationCard<Selecting>",
      )
      doTasks(
          "PlayCard<Class<CorporationCard>, Class<$Celestic>>",
          "$Celestic FROM CorporationCard<Selecting>!",
          "42 MC",
          "-42 MC",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTasks(
              "DoRequiredActions",
              "2 SearchForCard<ReferenceFilter<Class<Floater>>>",
              "3 PreludeCard<Selecting>",
          )
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$SocietySupport>>",
              "$SocietySupport FROM PreludeCard<Selecting>",
              "PROD[-MC]",
              "PROD[Plant]",
              "PROD[Energy]",
              "PROD[Heat]",
          )
        }
        .expect("2 ProjectCard, -2 RequiredAction")
  }

  @Test
  internal fun `New Partner can play Merger while selecting both card families`() {
    newTestGame(addOptions = "PreludeExpansion")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$NewPartner>>",
              "$NewPartner FROM PreludeCard",
              "PROD[MC]",
              "2 PreludeCard<Selecting>",
          )
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$Merger>>",
              "$Merger FROM PreludeCard<Selecting>",
              "4 CorporationCard<Selecting>",
              "-42 MC",
          )
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<$Celestic>>",
              "$Celestic FROM CorporationCard<Selecting>!",
              "42 MC",
          )
        }
        .expect("$Merger, $Celestic, 0 CorporationCard<Selecting>, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `The new corporation can pay for Merger and its own disease losses`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<$Merger>>",
              "$Merger FROM PreludeCard",
              "4 CorporationCard<Selecting>",
          )
          doTasks(
              "PlayCard<Class<CorporationCard>, Class<$PharmacyUnion>>",
              "$PharmacyUnion FROM CorporationCard<Selecting>!",
              "54 MC",
              "-42 MC",
              "SearchForCard<TagFilter<Class<ScienceTag>>>!",
              "Disease<$PharmacyUnion>",
              "-4* MC",
              "Disease<$PharmacyUnion>",
              "-4* MC",
          )
        }
        .expect("4 MC, 2 Disease<$PharmacyUnion>")
  }

  // Unresolved adjudication (question 3, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3067197
  @Test
  internal fun `Pharmacy Union's loss makes Board Merger Recyclon unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion, $BoardOfDirectors")
    kim.exMachina("$PharmacyUnion, 2 Disease<$PharmacyUnion>")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>",
          "$BoardOfDirectors FROM PreludeCard",
          "4 Director<$BoardOfDirectors>",
      )
    }
    advanceUntilActionPhase()
    kim.setToExMachina(17, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard<Hand>",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
        )
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Merger>>",
            "$Merger FROM PreludeCard",
            "4 CorporationCard<Selecting>",
        )

        doTasks(
            "PlayCard<Class<CorporationCard>, Class<$Recyclon>>",
            "$Recyclon FROM CorporationCard<Selecting>",
            "38 MC",
            "Disease<$PharmacyUnion>",
            "-4* MC",
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
    // Adjudication is unresolved; immediate resolution remains the locally adopted target.
    // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
    @Ignore // Merger leaves the mandatory city placement for a later action.
    @Test
    internal fun `Resolves Tharsis first action immediately when acquired after Preludes`() {
      acquireTharsisThroughBoard { doTasks("CityTile<Tharsis_3_3>", "PROD[MC]", "3 MC") }
    }

    // Unresolved adjudication (question 4, 2026-10-14); this records current behavior.
    // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
    @Test
    internal fun `BUG - Defers Tharsis first action when acquired after Preludes`() {
      acquireTharsisThroughBoard().expect("0 CityTile")
      kim.completeOperation {
        doTasks(
            "DoRequiredActions",
            "CityTile<Tharsis_3_3>",
            "PROD[MC]",
            "3 MC",
        )
      }
    }

    private fun acquireTharsisThroughBoard(cityPlacement: OperationBlock = {}): TaskResult {
      newTestGame(addOptions = "$BoardOfDirectors", playerCount = 2)
      // Prepare the played card; acquiring Tharsis and its required action remain real.
      kim.exMachina("$BoardOfDirectors, 4 Director<$BoardOfDirectors>, 21 MC")
      return kim.completeOperation {
        doTasks(
            *cardActionTasks("$BoardOfDirectors"),
            "PreludeCard",
            "-12 MC",
            "-Director<$BoardOfDirectors>",
        )
        doTasks(
            "PlayCard<Class<PreludeCard>, Class<$Merger>>",
            "$Merger FROM PreludeCard",
            "4 CorporationCard<Selecting>",
        )
        doTasks(
            "PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>",
            "$TharsisRepublic FROM CorporationCard<Selecting>",
            "40 MC",
            "-42 MC",
        )
        cityPlacement()
      }
    }
  }
}
