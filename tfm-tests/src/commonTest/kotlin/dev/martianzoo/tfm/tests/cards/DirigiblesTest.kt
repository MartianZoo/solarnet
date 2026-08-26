package dev.martianzoo.tfm.tests.cards

import dev.martianzoo.tfm.tests.TestOption.*
import dev.martianzoo.tfm.tests.cards.cardnames.*
import kotlin.test.Test

internal class DirigiblesTest : CardTest() {
  @Test
  internal fun `Can pay for a Venus card with two floaters`() {
    newGame(VenusNextExpansion)

    engine.phase("Action")
    p1.manual("ProjectCard, $Dirigibles, 2 Floater<$Dirigibles>, 5 MC")

    p1.playProject(AerialMappers, 5) {
          doTask("2 PayFromCard<$Dirigibles> FROM Floater<$Dirigibles>")
        }
        .expect("-2 Floater<$Dirigibles>, $AerialMappers")
  }
}
