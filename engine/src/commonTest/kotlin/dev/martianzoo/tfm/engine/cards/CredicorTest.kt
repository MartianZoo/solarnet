package dev.martianzoo.tfm.engine.cards

import dev.martianzoo.tfm.engine.cardnames.*
import kotlin.test.Test

class CredicorTest : CardTest() {
  @Test
  fun `with Credicor, buys an expensive card and standard project`() {
    newGame()
    engine.phase("Action")
    p1.manual("40, 2 ProjectCard, $CrediCor")
    p1.playProject(EarthCatapult, 23).expect("-19")
    p1.stdProject("CitySP") { doTask("CityTile<Tharsis_2_1>") }.expect("-21")
  }
}
