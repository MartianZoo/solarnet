package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class CorridorsOfPowerTest : TfmSandboxTest() {
  @Test
  internal fun `Draws for gaining leadership rather than for every delegate placed`() {
    newTestGame("PreludeExpansion, CorridorsOfPower, TurmoilExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<CorridorsOfPower>>",
          "CorridorsOfPower FROM PreludeCard",
          "TerraformRating",
          "4 MC",
      )
    }
    advanceUntilActionPhase()

    kim.inTurn {
          doTask("LobbyAction<Action1>")
          selectOnlyTask()
          doTask("PartyDelegate<Scientists>")
          selectTask("ProjectCard<Kim, Hand>!")
        }
        .expect("ProjectCard")
    kim.inTurn {
          doTasks("LobbyAction<Action2>", "-5 MC", "PartyDelegate<Scientists>")
        }
        .expect("0 ProjectCard")
  }
}
