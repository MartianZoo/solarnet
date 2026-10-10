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

    kim.playProject(GhgShipment, 3).expect("PROD[Heat], 3 Heat")
  }
}
