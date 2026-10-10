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

    shouldThrow<LimitsException> { kim.cardAction1(CeresTechMarket, x = 5) }
    kim.cardAction1(CeresTechMarket, x = 4).expect("-4 ProjectCard, 8 MC")
  }
}
