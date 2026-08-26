package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TharsisRepublicTest : CardTest() {
  @Test
  internal fun `Gains two mc production in solo mode`() {
    newGame(players = 1)

    p1.playCorp(TharsisRepublic, 1).expect("PROD[2 MC]")
  }

  @Test
  internal fun `Does not gain starting mc production in multiplayer mode`() {
    newGame(players = 2)

    p1.playCorp(TharsisRepublic, 0).expect("40 MC, PROD[0 MC]")
  }

  @Test
  internal fun `Gains the solo mc production bonus when Merger plays it later`() {
    newGame(PreludeExpansion, PromoCardPack, players = 1)
    p1.playCorp(CrediCor, 0)
    engine.phase("Prelude")
    p1.manual("PreludeCard")

    p1.playPrelude(Merger) {
          doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>")
        }
        .expect("PROD[2 MC]")
  }
}
