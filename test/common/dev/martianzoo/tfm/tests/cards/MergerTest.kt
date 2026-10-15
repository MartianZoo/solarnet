package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
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
      doTask("4 CorporationCard<Kim, Selecting>")
      doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>")
      doTask("Celestic<Kim> FROM CorporationCard<Kim, Selecting>!")
      doTasks("42 MC", "-42 MC")
      doTask("Celestic_RequiredAction")
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("RequiredActionsSignal")
          doTask("2 SearchForCard<ReferenceFilter<Class<Floater>>>")
          doTask("3 PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<SocietySupport>>")
          doTask("SocietySupport FROM PreludeCard<Selecting>")
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
          doTask("2 PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<Merger>>")
          doTask("Merger FROM PreludeCard<Selecting>")
          doTask("4 CorporationCard<Kim, Selecting>")
          doTask("-42 MC")
          doTask("PlayCard<Class<CorporationCard>, Class<Celestic>>")
          doTask("Celestic<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTask("42 MC")
          doTask("Celestic_RequiredAction")
        }
        .expect("$Merger, $Celestic, 0 CorporationCard<Selecting>, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `The new corporation can pay for Merger and its own disease losses`() {
    newTestGame(addOptions = "PreludeExpansion")
    kim.setToExMachina(0, "MC")

    kim.inTurn {
          doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
          doTask("4 CorporationCard<Kim, Selecting>")
          doTask("PlayCard<Class<CorporationCard>, Class<PharmacyUnion>>")
          doTask("PharmacyUnion<Kim> FROM CorporationCard<Kim, Selecting>!")
          doTask("54 MC")
          doTask("-42 MC")
          doTask("SearchForCard<Kim, TagFilter<Class<ScienceTag>>>!")
          doTask("Disease<PharmacyUnion>")
          doTask("-4* MC")
          doTask("Disease<PharmacyUnion>")
          doTask("-4* MC")
        }
        .expect("4 MC, 2 Disease<$PharmacyUnion>")
  }

  // Unresolved adjudication (question 3, 2026-10-14); this records current behavior.
  // https://boardgamegeek.com/thread/3067197
  @Test
  internal fun `Pharmacy Union's loss makes Board Merger Recyclon unaffordable`() {
    newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors")
    kim.exMachina("$PharmacyUnion, 2 Disease<$PharmacyUnion>")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<BoardOfDirectors>>",
          "BoardOfDirectors FROM PreludeCard",
      )
      doTask("4 Director<BoardOfDirectors>")
    }
    advanceUntilActionPhase()
    kim.setToExMachina(17, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        doTask("UseCardAction<BoardOfDirectors, Action1>")
        doTask("PreludeCard<Kim, Hand>")
        doTask("-12 MC")
        doTask("-Director<BoardOfDirectors>")
        doTasks("PlayCard<Class<PreludeCard>, Class<Merger>>", "Merger FROM PreludeCard")
        doTask("4 CorporationCard<Kim, Selecting>")

        doTasks(
            "PlayCard<Class<CorporationCard>, Class<Recyclon>>",
            "Recyclon FROM CorporationCard<Selecting>",
            "38 MC",
            "Disease<PharmacyUnion>",
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
      acquireTharsisThroughBoard { doTask("CityTile<Tharsis_3_3>") }.expect("CityTile<Tharsis_3_3>")
    }

    // Unresolved adjudication (question 4, 2026-10-14); this records current behavior.
    // https://boardgamegeek.com/thread/2886401/article/44823945#44823945
    @Test
    internal fun `BUG - Defers Tharsis first action when acquired after Preludes`() {
      acquireTharsisThroughBoard().expect("0 CityTile")
      kim.completeOperation {
            doTask("RequiredActionsSignal")
            doTask("CityTile<Tharsis_3_3>")
            autoExecNow(EAGER)
          }
          .expect("CityTile<Tharsis_3_3>")
    }

    private fun acquireTharsisThroughBoard(cityPlacement: OperationBlock = {}): TaskResult {
      newTestGame(addOptions = "PreludeExpansion, BoardOfDirectors", playerCount = 2)
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$BoardOfDirectors>>")
      kim.doTask("PlayCard<Class<PreludeCard>, Class<$Donation>>")
      stan.doTask("PlayCard<Class<PreludeCard>, Class<$Supplier>>")
      stan.autoExecNow(EAGER)

      stan.doTask("PlayCard<Class<PreludeCard>, Class<$MetalsCompany>>")
      stan.autoExecNow(EAGER)

      return kim.completeOperation {
        doTask("UseCardAction<$BoardOfDirectors, Action1>")
        doTask("-12 MC")
        autoExecNow(EAGER)
        doTask("PlayCard<Class<PreludeCard>, Class<$Merger>>")
        doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>")
        cityPlacement()
        autoExecNow(EAGER)
      }
    }
  }
}
