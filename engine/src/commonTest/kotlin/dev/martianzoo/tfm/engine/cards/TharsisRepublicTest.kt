package dev.martianzoo.tfm.engine.cards

import kotlin.test.Test

class TharsisRepublicTest : CardTest() {
  @Test
  fun `gains two production in solo mode`() {
    newGame(players = 1)

    p1.playCorp("TharsisRepublic", 1) { doTask("PROD[2]") }.expect("PROD[2]")
  }

  @Test
  fun `does not gain starting production in multiplayer mode`() {
    newGame(players = 2)

    p1.playCorp("TharsisRepublic", 0).expect("40, PROD[0]")
  }
}
