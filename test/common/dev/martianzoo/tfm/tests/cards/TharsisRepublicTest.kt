package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.PreludeExpansion
import dev.martianzoo.tfm.tests.TestOption.PromoCardPack
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class TharsisRepublicTest : CardTest() {
  @Test
  internal fun `Original solo corporation gains production for the two opponent cities`() {
    newGame(players = 1, startingProjects = listOf(1))

    p1.playCorp(TharsisRepublic).expect("PROD[2 MC]")
  }

  @Test
  internal fun `Does not gain starting mc production in multiplayer mode`() {
    newGame(players = 2)

    playCorporationWithoutStartingProjects(p1, TharsisRepublic).expect("40 MC, PROD[0 MC]")
  }

  @Test
  internal fun `Does not gain the solo starting bonus when acquired through Merger`() {
    newGame(PreludeExpansion, PromoCardPack, players = 1)
    playCorporationWithoutStartingProjects(p1, CrediCor)
    admin.phase("Prelude")
    p1.runOperation("PreludeCard")

    p1.playPrelude(Merger) { p1.playCorp(TharsisRepublic) }.expect("PROD[0 MC]")
  }
}
