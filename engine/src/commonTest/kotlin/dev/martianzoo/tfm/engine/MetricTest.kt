package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricTest {
  @Test
  fun orCountsTheUnionOfMatchingComponents() {
    val p1 = Engine.newGame(canonicalPremise("", 2)).tfm(PLAYER1)
    p1.godMode()
        .sneak(
            "CityTile<Player1, Tharsis_4_2>, GreeneryTile<Player1, Tharsis_4_3>, " +
                "Plant<Player1>"
        )

    p1.count("OwnedTile<Player1>") shouldBe 2
    p1.count("CityTile<Player1>") shouldBe 1
    p1.count("OwnedTile<Player1> OR CityTile<Player1>") shouldBe 2
    p1.count("OwnedTile<Player1> OR Plant<Player1>") shouldBe 3
  }
}
