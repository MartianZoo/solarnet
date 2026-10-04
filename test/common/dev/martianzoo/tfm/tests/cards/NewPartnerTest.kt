package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.pets.ast.ClassName.Companion.cn
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
    shouldThrow<NarrowingException> {
      p1.playPrelude(NewPartner) {
        p1.assertCounts(1 to "PreludeCard<Hand>")
        p1.playPrelude(IndustrialComplex)
      }
    }
    shouldThrow<LimitsException> {
      p1.playPrelude(NewPartner) {
        p1.playPrelude(IndustrialComplex, location = cn("Selecting"))
      }
    }

    val checkpoint = game.timeline.checkpoint()
    val result = p1.playPrelude(NewPartner) { doTask("-PreludeCard<Selecting>") }
    result.expect("15 MC, PROD[MC]")
    result.changes
        .filter { it.change.gaining?.type == p1.resolve("PreludeCard<Selecting>") }
        .sumOf { it.change.count } shouldBe 2
    p1.assertCounts(0 to "PreludeCard<Selecting>")
    p1.assertCounts(1 to "$NewPartner", 0 to "$IndustrialComplex", 1 to "PreludeCard")
    p1.auditGainsSince(checkpoint) shouldBe 1
  }
}
