package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.Donation
import dev.martianzoo.tfm.tests.cards.cardnames.IndustrialComplex
import dev.martianzoo.tfm.tests.cards.cardnames.NewPartner
import dev.martianzoo.tfm.tests.cards.cardnames.PowerGeneration
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class NewPartnerTest : TfmSandboxTest() {

  @Test
  internal fun `New Partner plays the offered Prelude and leaves the other hand card alone`() {
    newTestGame(addOptions = "PreludeExpansion")

    shouldThrow<NarrowingException> {
      kim.playPrelude(NewPartner) {
        kim.assertCounts(1 to "PreludeCard<Hand>")
        doTask("PlayCard<Class<PreludeCard>, Class<$Donation>, Hand>")
      }
    }

    kim.playPrelude(NewPartner) { kim.playPrelude(Donation) }.expect("21 MC, PROD[MC]")
    kim.assertCounts(1 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>", 1 to "$Donation")

    kim.playPrelude(PowerGeneration).expect("PROD[3 Energy]")
    kim.assertCounts(0 to "PreludeCard<Hand>", 0 to "PreludeCard<Selecting>")
  }

  @Test
  internal fun `New Partner can fizzle an unaffordable Industrial Complex`() {
    newTestGame(addOptions = "PreludeExpansion, Prelude2CardPack")
    kim.setToExMachina(13, "MC")
    shouldThrow<LimitsException> {
      kim.playPrelude(NewPartner) {
        kim.playPrelude(IndustrialComplex)
      }
    }

    kim.playPrelude(NewPartner) { doTask("-PreludeCard<Selecting>") }.expect("15 MC, PROD[MC]")
    kim.assertCounts(0 to "PreludeCard<Selecting>")
    kim.assertCounts(1 to "$NewPartner", 0 to "$IndustrialComplex", 1 to "PreludeCard")
  }
}
