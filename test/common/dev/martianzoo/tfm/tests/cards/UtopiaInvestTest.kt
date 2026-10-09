package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.NarrowingException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UtopiaInvestTest : TfmSandboxTest() {
  @Test
  internal fun `Decreases and gains the same standard resource`() {
    newTestGame(kimCorporation = UtopiaInvest)
    kim.setToExMachina(2, "PROD[Plant]")

    kim.cardAction1(UtopiaInvest) { doTask("PROD[-Plant] THEN 4 Plant") }
        .expect("PROD[-Plant], 4 Plant")
  }

  @Test
  internal fun `Cannot sacrifice plant production to gain a different resource`() {
    newTestGame(kimCorporation = UtopiaInvest)
    kim.setToExMachina(2, "PROD[Plant]")

    shouldThrow<NarrowingException> {
      kim.cardAction1(UtopiaInvest) { doTask("PROD[-Plant] THEN 4 Steel") }
    }
  }
}
