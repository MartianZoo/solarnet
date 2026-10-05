package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.pets.api.Exceptions.LimitsException
import dev.martianzoo.tfm.tests.TestHelpers.testColonyTiles
import dev.martianzoo.tfm.tests.TestOption.ColoniesExpansion
import dev.martianzoo.tfm.tests.TestOption.Prelude2CardPack
import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.cards.cardnames.CeresTechMarket
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CeresTechMarketTest : CardTest() {
  @BeforeTest
  fun initializeGame() {
    newGame(
        PreludeExpansion,
        Prelude2CardPack,
        ColoniesExpansion,
        colonyTiles = testColonyTiles(2),
    )
    admin.phase("Action")
    p1.runOperation("$CeresTechMarket, 4 ProjectCard")
  }

  @Test
  internal fun `Can discard three cards for six mc`() {
    p1.cardAction1(CeresTechMarket, x = 3).expect("-3 ProjectCard, 6 MC")
  }

  @Test
  internal fun `Cannot discard more cards than are in hand`() {
    shouldThrow<LimitsException> { p1.cardAction1(CeresTechMarket, x = 5) }

    p1.cardAction1(CeresTechMarket, x = 4).expect("-4 ProjectCard, 8 MC")
  }
}
