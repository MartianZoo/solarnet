package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TopsoilContractTest : CardTest() {
  @Test
  internal fun `Its own tag triggers Decomposers and pays for the resulting microbe`() {
    newGame(PromoCardPack)
    p1.playCorp(CrediCor, 5)
    admin.phase("Action")
    p1.runOperation("3 OxygenStep")
    p1.playProject(Decomposers, 5)

    p1.playProject(TopsoilContract, 8).expect("Microbe<$Decomposers>, 3 Plant, -7 MC")
  }
}
