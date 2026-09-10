package dev.martianzoo.tfm.tests.rules

import dev.martianzoo.agenttestsupport.testTfm
import dev.martianzoo.engine.*
import dev.martianzoo.engine.Engine
import dev.martianzoo.testsupport.PLAYER1
import dev.martianzoo.tfm.engine.*
import dev.martianzoo.tfm.tests.*
import dev.martianzoo.tfm.tests.TestOption.*
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class MetricTest {
  @Test
  internal fun metricUnitsAndRequirementThresholdsHaveDifferentMeanings() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).testTfm(PLAYER1)
    p1.runOperation("8 Plant")

    p1.runOperation("Heat / 3 Plant")
    p1.count("Heat<Player1>") shouldBe 2
    p1.count("3 Plant<Player1>") shouldBe 2
    p1.has("3 Plant<Player1>") shouldBe true
    p1.has("MAX 3 Plant<Player1>") shouldBe false
  }

  @Test
  internal fun metricSubtractionComposesInCountsRequirementsAndInstructions() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).testTfm(PLAYER1)
    p1.runOperation("7 Plant, 2 Steel")

    p1.count("Plant MAX 5 - Steel") shouldBe 3
    p1.count("2 (Plant - Steel - 1) MAX 2") shouldBe 2
    p1.count("Plant - (Steel - 1)") shouldBe 6
    p1.count("Plant - 8") shouldBe 0
    p1.has("5 (Plant - Steel)") shouldBe true
    p1.has("6 (Plant - Steel)") shouldBe false

    p1.runOperation("Heat / Plant MAX 5 - Steel")
    p1.count("Heat<Player1>") shouldBe 3
    p1.runOperation("-Heat / Plant - 6")
    p1.count("Heat<Player1>") shouldBe 2
  }

  @Test
  internal fun metricsSupportConstantMinuendsAndDynamicCaps() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).testTfm(PLAYER1)
    p1.runOperation("7 Plant, 2 Steel")

    p1.count("6 - Plant") shouldBe 0
    p1.count("6 - Steel") shouldBe 4
    p1.count("Plant MAX Steel") shouldBe 2
    p1.count("Steel MAX Plant") shouldBe 2
  }

  @Test
  internal fun orCountsTheUnionOfMatchingComponents() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).testTfm(PLAYER1)
    p1.runOperation(
        "CityTile<Player1, Tharsis_4_2>, GreeneryTile<Player1, Tharsis_4_3>, " + "Victory<Player1>"
    )

    p1.count("OwnedTile<Player1>") shouldBe 2
    p1.count("CityTile<Player1>") shouldBe 1
    p1.count("OwnedTile<Player1> OR CityTile<Player1>") shouldBe 2
    p1.count("OwnedTile<Player1> OR Victory<Player1>") shouldBe 3
  }

  @Test
  internal fun neighborsAreDerivedFromLiveTilesAndMapGeometry() {
    val p1 = Engine.newGame(canonicalPremise(players = 2)).testTfm(PLAYER1)

    p1.count("Neighbor") shouldBe 0
    p1.runOperation("CityTile<Player1, Tharsis_4_4>")

    p1.count("Neighbor") shouldBe 6
    p1.count("Neighbor<CityTile<Player1>, Tharsis_4_5>") shouldBe 1
    p1.count("Neighbor<CityTile<Player1>, Tharsis_1_1>") shouldBe 0

    p1.runOperation("-CityTile<Player1, Tharsis_4_4>")
    p1.count("Neighbor") shouldBe 0
  }
}
