package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class AphroditeTest : CardTest() {
  @Test
  internal fun `Triggers when an opponent raises Venus`() {
    newGameWithAutoWorkflow(VenusNextExpansion)
    playUntilFirstActionPhase(UnitedNationsMarsInitiative, Aphrodite)

    p1.stdProject("AirScrappingSP").expect("2 M<Player2>")
  }
}
