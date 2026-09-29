package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.assertCounts
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.IndustrialComplex
import dev.martianzoo.tfm.tests.cards.cardnames.NewPartner
import dev.martianzoo.tfm.tests.cards.cardnames.ValleyTrust
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class NewPartnerTest : CardTest() {

  @Test
  internal fun `New Partner can fizzle an unaffordable Industrial Complex`() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack)
    p1.playCorp(ValleyTrust, 8)
    admin.phase("Prelude")
    shouldThrow<LimitsException> {
      p1.playPrelude(NewPartner) { p1.playPrelude(IndustrialComplex) }
    }

    val checkpoint = game.timeline.checkpoint()
    p1.playPrelude(NewPartner) { doTask("-PreludeCard") }.expect("15 MC, PROD[MC]")
    p1.assertCounts(1 to "$NewPartner", 0 to "$IndustrialComplex", 1 to "PreludeCard")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
