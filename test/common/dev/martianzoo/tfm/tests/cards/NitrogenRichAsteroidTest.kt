package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class NitrogenRichAsteroidTest : ProjectCardTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Can choose the lesser production despite three plant tags`() {
    kim.exMachina("$AdaptedLichen, $Lichen, $ArcticAlgae")

    kim.playProject(NitrogenRichAsteroid, 31) { doTask("PROD[Plant]") }.expect("PROD[Plant]")
  }
}
