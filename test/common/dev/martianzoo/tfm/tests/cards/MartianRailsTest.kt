package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class MartianRailsTest : ProjectCardTest() {
  // FAQ: "even if there are NO cities on Mars (earning you 0 M€)."
  @Test
  internal fun `Can be used when every city is off Mars`() {
    kim.exMachina("$MartianRails")
    kim.exMachina("NormalCityTile<Kim, GanymedeColony_RemoteArea>")
    kim.setToExMachina(1, "Energy")

    kim.cardAction1(MartianRails).expect("-Energy, 0 MC")
  }
}
