package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class VenusOrbitalSurveyTest : TfmSandboxTest() {
  @Test
  internal fun `Keeps a Venus card free and buys the other revealed card`() {
    newTestGame("VenusOrbitalSurvey")
    kim.exMachina("$VenusOrbitalSurvey")

    kim.cardAction1(VenusOrbitalSurvey) {
          doTask("TakeSelectedCard<TagFilter<Class<VenusTag>>>")
          kim.buyCards(1)
        }
        .expect("2 ProjectCard, -3 MC")
  }
}
