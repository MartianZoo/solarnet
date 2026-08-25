package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.engine.AutoExecMode.FIRST
import dev.martianzoo.engine.AutoExecMode.NONE
import dev.martianzoo.pets.api.Exceptions.TaskException
import dev.martianzoo.tfm.tests.cards.cardnames.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import kotlin.test.Test

internal class ArcticAlgaeTest : CardTest() {
  @Test
  internal fun `Triggers when an opponent places an ocean`() {
    newGame()
    val p2 = requireP2()
    p2.manual("$ArcticAlgae")
    p1.manual("OceanTile<Tharsis_1_2>").expect("2 Plant<Player2>")
  }

  @Test
  internal fun `Can resolve ocean placements and Arctic Algae before removing plants`() {
    newGame()
    val p2 = requireP2()
    p2.manual("$ArcticAlgae, Plant")
    p1.manual("ProjectCard, 36 Megacredit")
    engine.phase("Action")

    p1.playProject(GiantIceAsteroid, 36) {
      p1.autoExecMode = NONE
      p2.autoExecMode = NONE
      shouldThrow<TaskException> { p2.doTask("2 Plant") }
      doTask("OceanTile<Tharsis_1_2>")
      p2.doTask("2 Plant")
      doTask("OceanTile<Tharsis_1_4>")
      p2.doTask("2 Plant")
      doTask("-6 Plant<Player2>")
      p1.autoExecMode = FIRST
      p2.autoExecMode = FIRST
    }

    p2.count("Plant") shouldBe 0
  }

  @Test
  internal fun `Can remove plants before resolving ocean placements and Arctic Algae`() {
    newGame()
    val p2 = requireP2()
    p2.manual("$ArcticAlgae, Plant")
    p1.manual("ProjectCard, 36 Megacredit")
    engine.phase("Action")

    p1.playProject(GiantIceAsteroid, 36) {
      p1.autoExecMode = NONE
      p2.autoExecMode = NONE
      doTask("-2 Plant<Player2>")
      doTask("OceanTile<Tharsis_1_2>")
      doTask("OceanTile<Tharsis_1_4>")
      p1.autoExecMode = FIRST
      p2.autoExecMode = FIRST
    }

    p2.count("Plant") shouldBe 4
  }
}
