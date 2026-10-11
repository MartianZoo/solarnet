package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class UtopiaInvestTest : TfmSandboxTest() {
  @Test
  internal fun `Decreases and gains the same standard resource`() {
    newTestGame(kimCorporation = UtopiaInvest)
    kim.setToExMachina(2, "PROD[Plant]")

    kim.inTurn {
      doTasks("UseCardAction<UtopiaInvest, Action1>", "PROD[-Plant] THEN 4 Plant", "4 Plant")
    }
  }

  @Test
  internal fun `Cannot sacrifice plant production to gain a different resource`() {
    newTestGame(kimCorporation = UtopiaInvest)
    kim.setToExMachina(2, "PROD[Plant]")

    shouldThrow<TaskException> {
      kim.inTurn {
        doTask("UseCardAction<UtopiaInvest, Action1>")
        doTask("PROD[-Plant] THEN 4 Steel")
      }
    }
  }
}
