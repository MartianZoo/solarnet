package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ValleyTrustTest : TfmSandboxTest() {
  @Test
  internal fun `First action draws a Prelude without Prelude rules or an explicitly selected pool`() {
    newTestGame(addOptions = "ValleyTrust", kimCorporation = ValleyTrust)

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectTask("3! PreludeCard<Kim, Selecting>")
          selectTask("-2! PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<Donation>>")
          doTask("Donation<Kim> FROM PreludeCard<Kim, Selecting>!")
          doTask("21 MC")
        }
        .expect("-RequiredAction, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `First action plays a Prelude after the Prelude phase has ended`() {
    newTestGame(addOptions = "PreludeExpansion, ValleyTrust", kimCorporation = ValleyTrust)
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectTask("3! PreludeCard<Kim, Selecting>")
          selectTask("-2! PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<MartianIndustries>>")
          doTask("MartianIndustries<Kim> FROM PreludeCard<Kim, Selecting>!")
          doTasks("PROD[Energy]", "PROD[Steel]", "6 MC")
        }
        .expect("-RequiredAction, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `First action can draw from Prelude 2 without Prelude 1`() {
    newTestGame(
        addOptions = "PreludeExpansion, prelude2:cards, -prelude:cards, ValleyTrust",
        kimCorporation = ValleyTrust,
    )
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectTask("3! PreludeCard<Kim, Selecting>")
          selectTask("-2! PreludeCard<Kim, Selecting>")
          doTask("PlayCard<Class<PreludeCard>, Class<SpaceLanes>>")
          selectOnlyTask()
          doTask("3 Titanium")
        }
        .expect("-RequiredAction, 0 PreludeCard<Selecting>, $SpaceLanes")
  }

  @Test
  internal fun `First action can fizzle an unaffordable Industrial Complex`() {
    newTestGame(
        addOptions = "PreludeExpansion, prelude2:cards, ValleyTrust",
        kimCorporation = ValleyTrust,
    )
    advanceUntilActionPhase()
    kim.setToExMachina(13, "MC")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("RequiredActionsSignal")
        selectTask("3! PreludeCard<Kim, Selecting>")
        selectTask("-2! PreludeCard<Kim, Selecting>")
        selectOnlyTask()
        doTask("PlayCard<Class<PreludeCard>, Class<IndustrialComplex>>")
        selectOnlyTask()
        selectTask("-18! MC")
      }
    }
    kim.inTurn {
          doTask("RequiredActionsSignal")
          selectTask("3! PreludeCard<Kim, Selecting>")
          selectTask("-2! PreludeCard<Kim, Selecting>")
          doTask("-PreludeCard<Selecting>")
          selectTask("Audit!")
          doTask("15 MC")
        }
        .expect("-RequiredAction, 0 $IndustrialComplex, 0 PreludeCard<Selecting>")
  }
}
