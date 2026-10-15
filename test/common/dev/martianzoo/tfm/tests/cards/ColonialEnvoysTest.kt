package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ColonialEnvoysTest : TfmSandboxTest() {
  @Test
  internal fun `Each owned colony supplies a delegate even when colonies share a tile`() {
    newTestGame("ColonialEnvoys, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>, 2 PartyDelegate<Unity>")
    stan.exMachina("Colony<Io>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<ColonialEnvoys>>",
              "-4 MC",
              "ColonialEnvoys FROM ProjectCard",
          )
          // Expand the per-colony reward into separate delegate choices.
          selectOnlyTask()
          doTask("PartyDelegate<Scientists>")
          doTask("PartyDelegate<Greens>")
          doTask("PlayedEvent<Class<ColonialEnvoys>> FROM ColonialEnvoys")
        }
        .expect("0 PartyDelegate<Stan>")
  }
}
