package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class TopsoilContractTest : TfmSandboxTest() {
  @BeforeTest fun setUp() = newTestGame()

  @Test
  internal fun `Its microbe tag triggers Decomposers and pays for that microbe`() {
    kim.exMachina("$Decomposers")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<TopsoilContract>>",
              "-8 MC",
              "TopsoilContract FROM ProjectCard",
              "3 Plant",
          )
          doTask("Microbe<Decomposers>*")
          doTask("MC")
        }
        .expect("Microbe<$Decomposers>")
  }
}
