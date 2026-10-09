package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AphroditeTest : TfmSandboxTest() {
  @Test
  internal fun `Pays when an opponent raises Venus`() {
    newTestGame(kimCorporation = Aphrodite)

    stan.stdProject("AirScrappingProject").expect("2 MC<Kim>")
  }
}
