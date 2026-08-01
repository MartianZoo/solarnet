package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.api.Exceptions.DependencyException
import dev.martianzoo.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.canon.Canon.Option.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

class DoubleDownTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(PreludeExpansion, PromoCardPack)
    p1.playCorp("PharmacyUnion", 5)
    engine.phase("Prelude")
    p1.playPrelude("BiosphereSupport")
  }

  @Test
  fun `after Biosphere Support, plays Double Down`() {
    p1.playPrelude("DoubleDown") { doFirstTask("CopyPrelude<BiosphereSupport>") }
        .expect("PROD[-Megacredit, 0 Steel, 0 Titanium, 2 Plant, 0 Energy, 0 Heat]")
  }

  @Test
  fun `without Martian Industries in play, tries to copy it using Double Down`() {
    p1.playPrelude("DoubleDown") {
      shouldThrow<DependencyException> { doFirstTask("CopyPrelude<MartianIndustries>") }
      abort()
    }
  }

  @Test
  fun `with Unmi Contractor owned by p2, tries to copy it using Double Down`() {
    requireP2().playPrelude("UnmiContractor")
    p1.playPrelude("DoubleDown") {
      shouldThrow<DependencyException> { doFirstTask("CopyPrelude<UnmiContractor>") }
      abort()
    }
  }

  @Test
  fun `with Pharmacy Union in play, tries to copy it using Double Down`() {
    p1.playPrelude("DoubleDown") {
      shouldThrow<NarrowingException> { doFirstTask("CopyPrelude<PharmacyUnion>") }
      abort()
    }
  }

  @Test
  fun `while playing Double Down, tries to copy itself`() {
    p1.playPrelude("DoubleDown") {
      shouldThrow<NarrowingException> { doFirstTask("CopyPrelude<DoubleDown>") }
      abort()
    }
  }
}
