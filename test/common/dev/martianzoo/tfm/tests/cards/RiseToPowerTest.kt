package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class RiseToPowerTest : TfmSandboxTest() {
  @Test
  internal fun `Delegates can lead three different parties and trigger Corridors of Power for each`() {
    newTestGame("PreludeExpansion, RiseToPower, CorridorsOfPower, TurmoilExpansion")
    kim.inTurn {
      doTasks(
          "PlayCard<Class<PreludeCard>, Class<CorridorsOfPower>>",
          "CorridorsOfPower FROM PreludeCard",
          "TerraformRating",
          "4 MC",
      )
    }

    kim.inTurn {
          doTasks(
              "PlayCard<Class<PreludeCard>, Class<RiseToPower>>",
              "RiseToPower FROM PreludeCard",
              "PROD[3 MC]",
              "PartyDelegate<Scientists>",
          )
          doTask("ProjectCard<Kim, Hand>")
          doTask("PartyDelegate<Unity>")
          doTask("ProjectCard<Kim, Hand>")
          doTask("PartyDelegate<Greens>")
          doTask("ProjectCard<Kim, Hand>")
        }
        .expect("3 ProjectCard, PartyLeader<Scientists>, PartyLeader<Unity>, PartyLeader<Greens>")
  }
}
