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
          doTask("ProjectCard<Selecting>")
          doTasks("-0 ProjectCard<Selecting>", "5 Pay<> FROM MC")
          doTask("ProjectCard<Hand FROM Selecting>")
        }
        .expect("ProjectCard")
  }
}
