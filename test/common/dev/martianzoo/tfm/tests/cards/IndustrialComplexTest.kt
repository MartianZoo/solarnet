package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class IndustrialComplexTest : TfmSandboxTest() {
  @Test
  internal fun `Raises production to 1 without lowering tracks already above it`() {
    newTestGame("PreludeExpansion, IndustrialComplex, -QuickStartVariant")
    kim.setToExMachina(-5, "PROD[MC]")
    kim.setToExMachina(2, "PROD[Titanium]")
    kim.setToExMachina(1, "PROD[Plant]")

    kim.playPrelude(IndustrialComplex)
        .expect("-18 MC, PROD[6 MC, Steel, 0 Titanium, 0 Plant, Energy, Heat]")
  }

  @Test
  internal fun `Quick Start raises the target without reducing tracks already above it`() {
    newTestGame("PreludeExpansion, IndustrialComplex")
    kim.setToExMachina(-5, "PROD[MC]")
    kim.setToExMachina(3, "PROD[Titanium]")
    kim.setToExMachina(0, "PROD[Plant]")

    kim.playPrelude(IndustrialComplex)
        .expect("-18 MC, PROD[7 MC, Steel, 0 Titanium, 2 Plant, Energy, Heat]")
  }
}
