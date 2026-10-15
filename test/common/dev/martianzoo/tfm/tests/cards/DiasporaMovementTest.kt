package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class DiasporaMovementTest : TfmSandboxTest() {
  @Test
  internal fun `Counts its own Jovian tag and ignores an opponents tag`() {
    newTestGame(addOptions = "TurmoilExpansion")
    kim.exMachina("2 PartyDelegate<Reds>")
    stan.exMachina("$ColonizerTrainingCamp")

    kim.inTurn {
          doTasks(
              "PlayProject<Class<DiasporaMovement>>",
              "7 Pay<> FROM MC",
              "DiasporaMovement FROM ProjectCard",
          )
          doTask("2 MC")
        }
        .expect("-5 MC")
  }
}
