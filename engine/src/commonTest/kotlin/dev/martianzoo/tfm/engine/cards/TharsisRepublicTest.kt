package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.TestOption.PreludeExpansion
import dev.martianzoo.tfm.engine.TestOption.PromoCardPack
import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class TharsisRepublicTest : CardTest() {
  @Test
  fun `Gains two megacredit production in solo mode`() {
    newGame(players = 1)

    p1.playCorp(TharsisRepublic, 1).expect("PROD[2]")
  }

  @Test
  fun `Does not gain starting megacredit production in multiplayer mode`() {
    newGame(players = 2)

    p1.playCorp(TharsisRepublic, 0).expect("40, PROD[0]")
  }

  @Test
  fun `Gains the solo megacredit production bonus when Merger plays it later`() {
    newGame(PreludeExpansion, PromoCardPack, players = 1)
    p1.playCorp(CrediCor, 0)
    engine.phase("Prelude")
    p1.manual("PreludeCard")

    p1.playPrelude(Merger) {
          doTask("PlayCard<Class<CorporationCard>, Class<$TharsisRepublic>>")
        }
        .expect("PROD[2]")
  }
}
