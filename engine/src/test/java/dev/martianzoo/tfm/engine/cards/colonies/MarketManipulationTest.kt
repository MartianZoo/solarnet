package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.tfm.api.Exceptions.ExpressionException
import dev.martianzoo.tfm.api.Exceptions.LimitsException
import dev.martianzoo.tfm.api.Exceptions.NotNowException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class MarketManipulationTest : ColoniesCardTest() {
  @Test
  fun `a normal case`() {
    p1.assertCounts(
        1 to "ColonyProduction<Luna>",
        1 to "ColonyProduction<Triton>",
        4 to "ColonyProduction",
    )
    p1.playProject("MarketManipulation", 1) {
      doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
    }
    p1.assertCounts(
        2 to "ColonyProduction<Luna>",
        0 to "ColonyProduction<Triton>",
        4 to "ColonyProduction",
    )
  }

  @Test
  fun `you can't lower a track if it's already at the bottom`() {
    eng.godMode().manual("-ColonyProduction<Triton>")
    p1.playProject("MarketManipulation", 1) {
      assertThrows<LimitsException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
      abort()
    }
  }


  @Test
  fun `you can't raise a track if it's already at the top`() {
    eng.godMode().manual("5 ColonyProduction<Luna>")
    p1.playProject("MarketManipulation", 1) {
      assertThrows<LimitsException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Triton>")
      }
      abort()
    }
  }

  @Test
  fun `you can't raise and lower the same track`() {
    p1.playProject("MarketManipulation", 1) {
      // TODO is that the right exception type?
      assertThrows<ExpressionException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Luna>")
      }
      abort()
    }
  }

  @Test
  fun `you can't interact with a colony tile that's not in play yet`() {
    p1.playProject("MarketManipulation", 1) {
      assertThrows<NotNowException> {
        doTask("ColonyProduction<Titan> FROM ColonyProduction<Luna>")
      }
      assertThrows<NotNowException> {
        doTask("ColonyProduction<Luna> FROM ColonyProduction<Titan>")
      }
      abort()
    }
  }
}
