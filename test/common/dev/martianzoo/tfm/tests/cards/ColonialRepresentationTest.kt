package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.agent.AutoExecPolicy.EAGER
import dev.martianzoo.tfm.tests.TfmGameplayTest
import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class ColonialRepresentationTest : TfmSandboxTest() {
  @Test
  internal fun `Pays for each owned colony rather than each tile or opponents colonies`() {
    newTestGame("ColonialRepresentation, TurmoilExpansion, Luna, Io")
    kim.exMachina("2 Colony<Luna>")
    stan.exMachina("Colony<Io>")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<ColonialRepresentation>>",
              "10 Pay<> FROM MC",
              "ColonialRepresentation FROM ProjectCard",
          )
          doTask("6 MC")
        }
        .expect("-4 MC")
  }

  internal class Gameplay : TfmGameplayTest() {
    @Test
    internal fun `Its influence improves a Global Event payout even without colonies`() {
      newTestGame("ColonialRepresentation, TurmoilExpansion", playerCount = 2)
      kim.doTasks("PlayProject<Class<$ColonialRepresentation>>")
      kim.selectTask("PassBaton") // Decline the second action.
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }
      kim.doTask("VenusStep! BY Admin")
      players.forEach { it.autoExecNow(EAGER) }

      admin.doTask("PartyLeader<Greens, Neutral>")
      admin.doTask("ExploreFirstDirective")
      admin.doTask("PartyFollower<Reds, Neutral>")
      kim.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("-4 ProjectCard<Selecting>")
      stan.doTask("Pass")
      kim.doTask("Pass")
      players.forEach { it.autoExecNow(EAGER) }

      expectChanges("1 Plant<Kim>, 1 Steel<Kim>") {
        stan.doTask("VenusStep! BY Admin")
        players.forEach { it.autoExecNow(EAGER) }

        // Aquifer Released by Public Council grants resources and a neutral ocean.
        stan.doTask("OceanTile<Tharsis_1_5> BY Admin")
        players.forEach { it.autoExecNow(EAGER) }
      }
    }
  }
}
