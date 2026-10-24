package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class FloatingRefineryTest : TfmSandboxTest() {
  @Test
  internal fun `Counts its own Venus tag when gaining starting floaters`() {
    newTestGame("FloatingRefinery")
    kim.exMachina("$ForcedPrecipitation")

    kim.inTurn {
          doTasks(
              *playCardTasks("FloatingRefinery"),
              "7 Pay<> FROM MC",
              "FloatingRefinery FROM ProjectCard",
          )
          doTask("2 Floater<FloatingRefinery>")
        }
        .expect("2 Floater<$FloatingRefinery>")
  }
}
