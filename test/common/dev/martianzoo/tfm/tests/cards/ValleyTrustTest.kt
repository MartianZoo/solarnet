package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class ValleyTrustTest : TfmSandboxTest() {
  @Test
  internal fun `First action draws a Prelude without Prelude rules or an explicitly selected pool`() {
    newTestGame(kimCorporation = ValleyTrust)

    kim.stdAction("RequiredActionsSignal") { kim.playPrelude(Donation) }
        .expect("21 MC, -RequiredAction, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `First action plays a Prelude after the Prelude phase has ended`() {
    newTestGame(addOptions = "PreludeExpansion", kimCorporation = ValleyTrust)
    startActionPhase()

    kim.stdAction("RequiredActionsSignal") { kim.playPrelude(MartianIndustries) }
        .expect("PROD[Steel, Energy], -RequiredAction, 0 PreludeCard<Selecting>")
  }

  @Test
  internal fun `First action can draw from Prelude 2 without Prelude 1`() {
    newTestGame(
        addOptions = "PreludeExpansion, Prelude2CardPack, -Prelude1CardPack",
        kimCorporation = ValleyTrust,
    )
    startActionPhase()

    kim.stdAction("RequiredActionsSignal") { kim.playPrelude(SpaceLanes) }
        .expect("-RequiredAction, 0 PreludeCard<Selecting>, $SpaceLanes")
  }

  @Test
  internal fun `First action can fizzle an unaffordable Industrial Complex`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack", kimCorporation = ValleyTrust)
    startActionPhase()
    kim.setToExMachina(13, "MC")

    shouldThrow<LimitsException> {
      kim.stdAction("RequiredActionsSignal") { kim.playPrelude(IndustrialComplex) }
    }
    kim.stdAction("RequiredActionsSignal") { doTask("-PreludeCard<Selecting>") }
        .expect("15 MC, -RequiredAction, 0 $IndustrialComplex, 0 PreludeCard<Selecting>")
  }
}
