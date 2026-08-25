package dev.martianzoo.tfm.engine.cards.colonies

import dev.martianzoo.api.Exceptions.RequirementException
import dev.martianzoo.tfm.engine.TestHelpers.assertCounts
import dev.martianzoo.tfm.engine.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import kotlin.test.Test

internal class SkyDocksTest : ColoniesCardTest() {
  @Test
  internal fun `Can be played with two Earth tags`() {
    p1.manual("ProjectCard, 18, $LunaGovernor")
    p1.playProject(SkyDocks, 18).expect("TradeFleet")
    p1.assertCounts(2 to "TradeFleet")
  }

  @Test
  internal fun `Allows a second trade in the same generation`() {
    p1.manual("$SkyDocks, 18")
    p1.stdAction("TradeSA", 1) { doTask("Trade<Luna>") }
    p1.stdAction("TradeSA", 1) { doTask("Trade<Triton>") }
    p1.assertCounts(2 to "TradeFleet")
  }

  @Test
  internal fun `Cannot be played with only one Earth tag`() {
    p1.manual("ProjectCard, 18, $HeavyTaxation")
    shouldThrow<RequirementException> { p1.playProject(SkyDocks, 18) }
  }
}
