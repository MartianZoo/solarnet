package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class DoubleDownTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp(PharmacyUnion, 5)
    engine.phase("Prelude")
    p1.playPrelude(BiosphereSupport)
  }

  @Test
  internal fun `Can copy Biosphere Support`() {
    p1.playPrelude(DoubleDown) { doTask("CopyPrelude<$BiosphereSupport>") }
        .expect("PROD[-Megacredit, 0 Steel, 0 Titanium, 2 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  internal fun `Cannot copy an absent Prelude`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<DependencyException> { doTask("CopyPrelude<$MartianIndustries>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy another player's Prelude`() {
    requireP2().playPrelude(UnmiContractor)
    p1.playPrelude(DoubleDown) {
      shouldThrow<DependencyException> { doTask("CopyPrelude<$UnmiContractor>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy a corporation`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<NarrowingException> { doTask("CopyPrelude<$PharmacyUnion>") }
      abort()
    }
  }

  @Test
  internal fun `Cannot copy itself`() {
    p1.playPrelude(DoubleDown) {
      shouldThrow<NarrowingException> { doTask("CopyPrelude<$DoubleDown>") }
      abort()
    }
  }
}
