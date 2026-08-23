package dev.martianzoo.tfm.engine

import dev.martianzoo.data.Player.Companion.PLAYER1
import dev.martianzoo.engine.Engine
import dev.martianzoo.tfm.engine.TestOption.*
import dev.martianzoo.tfm.engine.TfmGameplay.Companion.tfm
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricTest {
  @Test
  internal fun metricUnitsAndRequirementThresholdsHaveDifferentMeanings() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).tfm(PLAYER1)
    p1.godMode().manual("8 Plant")

    p1.godMode().manual("Heat / 3 Plant")
    p1.count("Heat<Player1>") shouldBe 2
    p1.count("3 Plant<Player1>") shouldBe 2
    p1.has("3 Plant<Player1>") shouldBe true
    p1.has("MAX 3 Plant<Player1>") shouldBe false
  }

  @Test
  internal fun metricSubtractionComposesInCountsRequirementsAndInstructions() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).tfm(PLAYER1)
    p1.godMode().manual("7 Plant, 2 Steel")

    p1.count("Plant MAX 5 - Steel") shouldBe 3
    p1.count("2 (Plant - Steel - 1) MAX 2") shouldBe 2
    p1.count("Plant - (Steel - 1)") shouldBe 6
    p1.count("Plant - 8") shouldBe 0
    p1.has("5 (Plant - Steel)") shouldBe true
    p1.has("6 (Plant - Steel)") shouldBe false

    p1.godMode().manual("Heat / Plant MAX 5 - Steel")
    p1.count("Heat<Player1>") shouldBe 3
    p1.godMode().manual("-Heat / Plant - 6")
    p1.count("Heat<Player1>") shouldBe 2
  }

  @Test
  internal fun metricsSupportConstantMinuendsAndDynamicCaps() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).tfm(PLAYER1)
    p1.godMode().manual("7 Plant, 2 Steel")

    p1.count("6 - Plant") shouldBe 0
    p1.count("6 - Steel") shouldBe 4
    p1.count("Plant MAX Steel") shouldBe 2
    p1.count("Steel MAX Plant") shouldBe 2
  }

  @Test
  internal fun orCountsTheUnionOfMatchingComponents() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).tfm(PLAYER1)
    p1.godMode()
        .manual(
            "CityTile<Player1, Tharsis_4_2>, GreeneryTile<Player1, Tharsis_4_3>, " +
                "Victory<Player1>"
        )

    p1.count("OwnedTile<Player1>") shouldBe 2
    p1.count("CityTile<Player1>") shouldBe 1
    p1.count("OwnedTile<Player1> OR CityTile<Player1>") shouldBe 2
    p1.count("OwnedTile<Player1> OR Victory<Player1>") shouldBe 3
  }
}
