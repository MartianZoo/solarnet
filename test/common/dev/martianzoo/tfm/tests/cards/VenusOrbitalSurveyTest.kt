package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class VenusOrbitalSurveyTest : TfmSandboxTest() {
  @Test
  internal fun `Keeps a Venus card free and buys the other revealed card`() {
    newTestGame("VenusOrbitalSurvey")
    kim.exMachina("$VenusOrbitalSurvey")

    kim.inTurn {
      doTasks(
          "UseCardAction<$VenusOrbitalSurvey, Action1>",
          "2 ProjectCard<Selecting>",
          "TakeSelectedCard<TagFilter<Class<VenusTag>>>",
          "Ok", // Keep the other revealed card for purchase.
          "-3 MC",
          "ProjectCard<Hand FROM Selecting>",
      )
    }
  }
}
