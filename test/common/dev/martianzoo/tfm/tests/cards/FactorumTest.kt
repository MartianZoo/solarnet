package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.RequirementException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class FactorumTest : TfmSandboxTest() {
  @Test
  internal fun `Energy production does not prevent its action while stored energy is empty`() {
    newTestGame(kimCorporation = Factorum)
    kim.setToExMachina(3, "PROD[Energy]")

    kim.cardAction1(Factorum).expect("PROD[Energy]")
  }

  @Test
  internal fun `Stored energy prevents its energy-production action`() {
    newTestGame(kimCorporation = Factorum)
    kim.exMachina("Energy")

    shouldThrow<RequirementException> { kim.cardAction1(Factorum) }
  }
}
