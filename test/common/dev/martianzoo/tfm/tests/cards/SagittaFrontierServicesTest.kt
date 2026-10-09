package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.matchers.shouldBe
import kotlin.test.Ignore
import kotlin.test.Test

internal class SagittaFrontierServicesTest : CardTest() {

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Head Start's nested action`() {
    acquireDuringHeadStart()
    p1.count("MC") shouldBe 39
  }

  @Test
  internal fun `BUG - Misses Merger during Head Start's nested action`() {
    acquireDuringHeadStart()
    p1.count("MC") shouldBe 35
  }

  private fun acquireDuringHeadStart() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, FakeStuffBundle, Unsafe)
    p1.runOperation("$BoardOfDirectors, 54 MC, 8 Heat")
    admin.phase("Prelude")
    p1.runOperation("2 PreludeCard")

    p1.turn {
      playPrelude(FakeHeadStart) {
        useStdAction("UseActionOnCardAction", payment = {}) {
          doTask("UseAction<$BoardOfDirectors, Action1>")
          doTask("-12 MC")
          playPrelude(Merger) { playCorp(SagittaFrontierServices) }
        }
        useStdAction("ConvertHeatAction", payment = { doTask("-8 Heat") })
      }
    }
  }

  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Prelude`() {
    acquireDuringPrelude()
    p1.count("MC") shouldBe 51
  }

  @Test
  internal fun `BUG - Misses Merger when acquired during Prelude`() {
    acquireDuringPrelude()
    p1.count("MC") shouldBe 47
  }

  private fun acquireDuringPrelude() {
    newGame(PreludeExpansion, Prelude2CardPack, PromoCardPack, Unsafe)
    p1.runOperation("54 MC")
    admin.phase("Prelude")

    p1.turn {
      playPrelude(Merger) { playCorp(SagittaFrontierServices) }
    }
  }
}
