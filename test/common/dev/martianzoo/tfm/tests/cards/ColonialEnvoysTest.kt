package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import dev.martianzoo.tfm.tests.playCardTasks
import kotlin.test.Test

internal class ColonialEnvoysTest : TfmSandboxTest() {
  @Test
  internal fun `Each owned colony supplies a delegate even when colonies share a tile`() {
    newTestGame("ColonialEnvoys, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>, PartyLeader<Unity>, PartyFollower<Unity>")
    stan.exMachina("Colony<Io>")

    kim.inTurn {
          doTasks(
              *playCardTasks("ColonialEnvoys"),
              "4 Pay<> FROM MC",
              "ColonialEnvoys FROM ProjectCard",
          )
          // Expand the per-colony reward into separate delegate choices.
          selectOnlyTask()
          doTask("PartyLeader<Scientists>")
          doTask("PartyLeader<Greens>")
        }
        .expect("0 PartyDelegate<Stan>")
  }
}
