package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TfmSandboxTest
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class GhgShipmentTest : TfmSandboxTest() {
  @Test
  internal fun `Counts its owners floaters and ignores the other players floaters`() {
    newTestGame("GhgShipment, TurmoilExpansion")
    kim.exMachina(
        "$ForcedPrecipitation, 3 Floater<$ForcedPrecipitation>, 2 PartyDelegate<Kelvinists>"
    )
    stan.exMachina("$AerialMappers, 2 Floater<$AerialMappers>")

    kim.inTurn {
          doTasks("PlayProject<Class<GhgShipment>>", "-3 MC")
          doTasks(
              "GhgShipment FROM ProjectCard",
              "PROD[Heat]",
          )
          selectOnlyTask()
          doTask("PlayedEvent<Class<GhgShipment>> FROM GhgShipment")
        }
        .expect("3 Heat")
  }
}
