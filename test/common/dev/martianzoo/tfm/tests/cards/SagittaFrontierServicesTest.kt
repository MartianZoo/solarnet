package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.state.TaskResult
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Ignore
import kotlin.test.Test

internal class SagittaFrontierServicesTest : TfmSandboxTest() {
  @Test
  internal fun `Its own tagless corporation play earns the rebate`() {
    newTestGame(
        "-PromoCardPack",
        kimCorporation = SagittaFrontierServices,
        startAtCorporation = true,
    )

    // 31 starting MC plus the 4 MC rebate, minus the ten retained projects.
    kim.playCorp(SagittaFrontierServices).expect("5 MC")
  }

  @Test
  internal fun `Pays four MC for a tagless project`() {
    newTestGame("-PromoCardPack", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(15, "MC")

    kim.playProject(AtmoCollectors, 15) { addCardResources(AtmoCollectors) }.expect("-11 MC")
  }

  // https://boardgamegeek.com/thread/3154781/do-event-tags-count-for-sagitta
  @Test
  internal fun `An event with no other tags earns the single-tag rebate`() {
    newTestGame("-PromoCardPack", kimCorporation = SagittaFrontierServices)

    kim.playProject(Sabotage, 1) { doTask("-7 MC<Stan>") }.expect("0 MC")
  }

  @Test
  internal fun `An event with another printed tag earns no rebate`() {
    newTestGame("-PromoCardPack, SmallAsteroid", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(10, "MC")

    kim.playProject(SmallAsteroid, 10).expect("-10 MC")
  }

  @Test
  internal fun `A single building tag earns the single-tag rebate`() {
    newTestGame("-PromoCardPack", kimCorporation = SagittaFrontierServices)

    kim.playProject(Mine, 4).expect("-3 MC")
  }

  @Test
  internal fun `Repeated tags still count separately for the rebate threshold`() {
    newTestGame("-PromoCardPack", kimCorporation = SagittaFrontierServices)
    kim.setToExMachina(11, "MC")

    kim.playProject(Research, 11).expect("-11 MC")
  }

  @Test
  internal fun `Ignores another players card even when it has a single tag`() {
    newTestGame("-PromoCardPack", kimCorporation = SagittaFrontierServices)

    stan.playProject(Mine, 4).expect("-4 MC, 0 MC<Kim>")
  }

  // https://boardgamegeek.com/thread/3335155/article/44575973#44575973
  @Ignore // Sagitta misses its enclosing tagless Merger.
  @Test
  internal fun `Pays for Merger when acquired during Prelude`() {
    acquireDuringPrelude().expect("-3 MC")
  }

  @Test
  internal fun `BUG - Misses Merger when acquired during Prelude`() {
    acquireDuringPrelude().expect("-7 MC")
  }

  private fun acquireDuringPrelude(): TaskResult {
    newTestGame("PreludeExpansion, SagittaFrontierServices, Unsafe")
    kim.setToExMachina(54, "MC")
    return kim.playPrelude(Merger) { kim.playCorp(SagittaFrontierServices) }
  }
}
