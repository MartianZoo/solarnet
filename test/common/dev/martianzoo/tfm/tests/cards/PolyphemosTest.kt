package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class PolyphemosTest : TfmSandboxTest() {
  @Test
  internal fun `Applies its card-purchase surcharge to Inventors Guild`() {
    newTestGame(kimCorporation = Polyphemos)
    kim.exMachina("$InventorsGuild")
    kim.setToExMachina(5, "MC")

    kim.inTurn {
          doTask("UseCardAction<InventorsGuild, Action1>")
          selectOnlyTask()
          selectOnlyTask()
          doTasks("Ok", "-5 MC")
          selectTask("ProjectCard<Kim, Hand FROM Selecting>! / ProjectCard<Kim, Selecting>")
        }
        .expect("ProjectCard")
  }
}
