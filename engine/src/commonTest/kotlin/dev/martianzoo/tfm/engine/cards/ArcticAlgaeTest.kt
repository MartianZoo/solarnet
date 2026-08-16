package dev.martianzoo.tfm.engine.cards

import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ArcticAlgaeTest : CardTest() {
  @Test
  fun `with Arctic Algae owned by p2, p1 places an ocean`() {
    newGame()
    val p2 = requireP2()
    p2.manual("ArcticAlgae")
    p1.manual("OceanTile<Tharsis_1_2>").expect("2 Plant<Player2>")
  }

  @Test
  fun `after Arctic Algae grants plants, p1 plays Giant Ice Asteroid`() {
    newGame()
    val p2 = requireP2()
    p2.manual("ArcticAlgae, Plant")
    p1.manual("ProjectCard, 36 Megacredit")
    engine.phase("Action")

    p1.playProject("GiantIceAsteroid", 36) {
      doTask("OceanTile<Tharsis_1_2>")
      doTask("OceanTile<Tharsis_1_4>")
      doTask("-6 Plant<Player2>")
    }

    p2.count("Plant") shouldBe 0
  }
}
