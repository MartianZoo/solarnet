package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import kotlin.test.Test

class DirigiblesTest : CardTest() {
  @Test
  fun floatersPayThreeEachForAVenusCard() {
    val game = newGame("BMV", 2)
    val p1 = game.tfm(PLAYER1)

    p1.phase("Action")
    p1.sneak("5 ProjectCard, Dirigibles, 2 Floater<Dirigibles>, 20")

    p1.playProject("AerialMappers", 5) {
          doTask("-2 Floater<Dirigibles>! THEN -6 Owed.")
        }
        .expect("-2 Floater<Dirigibles>, AerialMappers")
  }
}
