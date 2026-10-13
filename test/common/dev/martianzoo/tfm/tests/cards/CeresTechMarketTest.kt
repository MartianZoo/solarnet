package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.CeresTechMarket
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class CeresTechMarketTest : TfmSandboxTest() {
  @Test
  internal fun `Can sell the entire hand but cannot sell more cards than it contains`() {
    newTestGame(addOptions = "CeresTechMarket")
    kim.exMachina("$CeresTechMarket")
    kim.setToExMachina(4, "ProjectCard")

    shouldThrow<LimitsException> {
      kim.inTurn {
        selectOnlyTask()
        doTask("UseCardAction<CeresTechMarket, Action1>")
        selectOnlyTask()
        narrowTask("-5 ProjectCard THEN 10 MC")
      }
    }
    kim.inTurn {
          doTask("UseCardAction<CeresTechMarket, Action1>")
          selectOnlyTask()
          selectTask("-4! ProjectCard<Kim, Hand> THEN 8! MC")
          doTask("8 MC")
        }
        .expect("-4 ProjectCard")
  }
}
